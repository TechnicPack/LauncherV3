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

package net.technicpack.launchercore.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class RotatingFileHandler extends StreamHandler {
    private final String filenameFormat;
    private final BlockingQueue<LogRecord> logQueue = new LinkedBlockingQueue<>();
    private final Thread loggingThread;
    private Path logsDirectory;
    private LocalDate currentDate;
    private volatile boolean running = true;

    public RotatingFileHandler(Path logsDirectory, String filenameFormat) {
        this.filenameFormat = filenameFormat;
        setLogsDirectory(logsDirectory);

        // Start the logging thread
        loggingThread = new Thread(this::processQueue, "AsyncLoggerThread");
        loggingThread.setDaemon(true);
        loggingThread.start();
    }

    private synchronized void setLogsDirectory(Path logsDirectory) {
        if (logsDirectory == null) {
            throw new NullPointerException("logsDirectory cannot be null");
        }

        this.logsDirectory = logsDirectory;

        currentDate = LocalDate.now();
        updateOutputFile();
    }

    private synchronized void updateOutputFile() {
        updateOutputFile(buildFilename());
    }

    private synchronized void updateOutputFile(String currentFilename) {
        try {
            OutputStream out = Files.newOutputStream(this.logsDirectory.resolve(currentFilename), CREATE, APPEND);
            setOutputStream(out);
        } catch (IOException ex) {
            // We can't really log exceptions if the logger doesn't work
            // TODO: implement ErrorManager that doesn't write to System.err
        }
    }

    private String buildFilename() {
        return String.format(filenameFormat, currentDate.toString());
    }

    @Override
    public void publish(LogRecord record) {
        if (!running || !isLoggable(record)) return;
        logQueue.offer(record); // Add to queue
    }

    private void processQueue() {
        while (running || !logQueue.isEmpty()) {
            try {
                LogRecord record = logQueue.take();
                synchronized (this) {
                    // Rotate the file if the date has changed
                    final LocalDate today = LocalDate.now();

                    if (!today.equals(currentDate)) {
                        final String oldPath = buildFilename();

                        currentDate = today;
                        updateOutputFile(buildFilename());

                        publish(new LogRecord(Level.INFO, String.format("Continued from %s", oldPath)));
                    }

                    // Write the actual log record
                    super.publish(record);
                    flush();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdownHandler() {
        running = false;
        loggingThread.interrupt();
        try {
            loggingThread.join();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        super.close();
    }
}
