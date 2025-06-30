/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.mirror.download;

import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.exception.PermissionDeniedException;
import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.utilslib.Utils;

import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Download implements Runnable {
    private static final long TIMEOUT = 30000;

    private URL url;
    private long size = -1;
    private long downloaded = 0;
    private String outPath;
    private String name;
    private DownloadListener listener;
    private Result result = Result.FAILURE;
    private File outFile = null;
    private Exception exception = null;

    private final Object timeoutLock = new Object();
    private final Object progressLock = new Object();
    private boolean isTimedOut = false;

    public Download(URL url, String name, String outPath) {
        this.url = url;
        this.outPath = outPath;
        this.name = name;
    }

    public float getProgress() {
        if (size > 0) {
            return (float) ((double) downloaded / size * 100);
        } else {
            return 0;
        }
    }

    public Exception getException() {
        return exception;
    }

    @Override
    @SuppressWarnings("unused")
    public void run() {
        try {
            HttpURLConnection conn = Utils.openHttpConnection(url);
            int response = conn.getResponseCode();
            int responseFamily = response / 100;

            if (responseFamily == 3) {
                String redirUrlText = conn.getHeaderField("Location");
                if (redirUrlText != null && !redirUrlText.isEmpty()) {
                    URL redirectUrl;
                    try {
                        redirectUrl = new URL(redirUrlText);
                    } catch (MalformedURLException ex) {
                        throw new DownloadException("Invalid Redirect URL: " + url, ex);
                    }

                    conn = Utils.openHttpConnection(redirectUrl);
                    response = conn.getResponseCode();
                    responseFamily = response / 100;
                }
            }

            if (response == 429) {
                throw new DownloadException("The download is being rate limited (HTTP 429). Try again later.");
            } else if (response == 404) {
                throw new DownloadException("The specified URL does not exist (HTTP 404).");
            } else if (responseFamily != 2) {
                throw new DownloadException("The server issued a " + response + " response code.");
            }

            InputStream in = getConnectionInputStream(conn);

            size = conn.getContentLengthLong();
            outFile = new File(outPath);
            outFile.delete();

            long startTime = System.nanoTime();

            try (ReadableByteChannel rbc = Channels.newChannel(in);
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.getChannel().lock();

                stateChanged();

                Thread progress = new MonitorThread(rbc);
                progress.start();

                fos.getChannel().transferFrom(rbc, 0, size > 0 ? size : Long.MAX_VALUE);

                in.close();
                rbc.close();
                progress.interrupt();

                synchronized (timeoutLock) {
                    if (isTimedOut) {
                        return;
                    }
                }

                if (size > 0) {
                    long fileLength = outFile.length();
                    if (size == fileLength) {
                        result = Result.SUCCESS;
                    } else {
                        throw new DownloadException("File size doesn't match. Expected " + size + ", got " + fileLength);
                    }
                } else {
                    result = Result.SUCCESS;
                }
            }

            long endTime = System.nanoTime();

            long durationNs = endTime - startTime;
            double durationSeconds = durationNs / 1_000_000_000.0;

            // Use actual file size for speed calc
            long fileLength = outFile.length();

            Utils.getLogger().fine(String.format("Download completed: %d bytes in %.3f seconds (%.2f MB/s)",
                    fileLength, durationSeconds, fileLength / durationSeconds / (1024 * 1024)));
        } catch (ClosedByInterruptException ex) {
            result = Result.FAILURE;
        } catch (PermissionDeniedException e) {
            exception = e;
            result = Result.PERMISSION_DENIED;
        } catch (OverlappingFileLockException e) {
            exception = e;
            result = Result.LOCK_FAILED;
        } catch (DownloadException e) {
            exception = e;
            result = Result.FAILURE;
        } catch (Exception e) {
            exception = e;
            e.printStackTrace();
        }
    }

    protected InputStream getConnectionInputStream(final URLConnection urlconnection) throws DownloadException {
        final AtomicReference<InputStream> is = new AtomicReference<>();

        for (int j = 0; (j < 3) && (is.get() == null); j++) {
            StreamThread stream = new StreamThread(urlconnection, is);
            stream.start();
            int iterationCount = 0;
            while ((is.get() == null) && (iterationCount++ < 5)) {
                try {
                    stream.join(1000L);
                } catch (InterruptedException ignore) {
                }
            }

            if (stream.permDenied.get()) {
                throw new PermissionDeniedException("Permission denied!");
            }

            if (is.get() != null) {
                break;
            }
            try {
                stream.interrupt();
                stream.join();
            } catch (InterruptedException ignore) {
            }
        }

        if (is.get() == null) {
            throw new DownloadException("Unable to download file from " + urlconnection.getURL());
        }
        return new BufferedInputStream(is.get());
    }

    private void stateChanged() {
        if (listener != null) {
            listener.stateChanged(name, getProgress());
        }

        synchronized (progressLock) {
            progressLock.notifyAll();
        }
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }

    public Result getResult() {
        return result;
    }

    public File getOutFile() {
        return outFile;
    }

    private static class StreamThread extends Thread {
        private final URLConnection urlconnection;
        private final AtomicReference<InputStream> is;
        public final AtomicBoolean permDenied = new AtomicBoolean(false);

        public StreamThread(URLConnection urlconnection, AtomicReference<InputStream> is) {
            this.urlconnection = urlconnection;
            this.is = is;
        }

        @Override
        public void run() {
            try {
                is.set(urlconnection.getInputStream());
            } catch (SocketException e) {
                if (e.getMessage().equalsIgnoreCase("Permission denied: connect")) {
                    permDenied.set(true);
                }
            } catch (IOException ignore) {
            }
        }
    }

    private class MonitorThread extends Thread {
        private final ReadableByteChannel rbc;
        private long last = System.currentTimeMillis();

        public MonitorThread(ReadableByteChannel rbc) {
            super("Download Monitor Thread");
            this.setDaemon(true);
            this.rbc = rbc;
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                long fileLength = outFile.length();
                long diff = fileLength - downloaded;
                downloaded = fileLength;
                if (diff == 0) {
                    if ((System.currentTimeMillis() - last) > TIMEOUT) {
                        try {
                            rbc.close();
                            timeout();
                        } catch (IOException | NullPointerException ignore) {
                            // We catch IOException and NullPointerException here because sometimes ReadableByteChannel
                            // can throw an NPE if we try to close it after the connection gets reset or otherwise
                            // broken, which can cause the ReadableByteChannel internals to be in an inconsistent state.
                        }
                        return;
                    }
                } else {
                    last = System.currentTimeMillis();
                }

                stateChanged();
                synchronized (progressLock) {
                    try {
                        progressLock.wait(50);
                    } catch (InterruptedException e) {
                        this.interrupt();
                        return;
                    }
                }
            }
        }

        private void timeout() {
            synchronized (timeoutLock) {
                isTimedOut = true;
            }
        }
    }

    public enum Result {
        SUCCESS, FAILURE, PERMISSION_DENIED, LOCK_FAILED
    }
}
