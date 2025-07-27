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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import static java.nio.file.StandardOpenOption.APPEND;

public class RotatingFileHandler extends StreamHandler {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private Path logsDirectory;
    private final String filenameFormat;
    private String currentFilename;

    public RotatingFileHandler(Path logsDirectory, String filenameFormat) {
        this.filenameFormat = filenameFormat;
        setLogsDirectory(logsDirectory);
    }

    private synchronized void setLogsDirectory(Path logsDirectory) {
        if (logsDirectory == null) {
            throw new NullPointerException("logsDirectory cannot be null");
        }

        this.logsDirectory = logsDirectory;

        updateOutputFile();
    }

    private synchronized void updateOutputFile() {
        // Close the output stream, in case it's open
        this.close();

        currentFilename = buildFilename();
        try {
            OutputStream out = Files.newOutputStream(this.logsDirectory.resolve(currentFilename), APPEND);
            setOutputStream(out);
        } catch (IOException ignored) {
            // We can't really log exceptions if the logger doesn't work
            // TODO: implement ErrorManager that doesn't write to System.err
        }
    }

    private String buildFilename() {
        return String.format(filenameFormat, dateFormat.format(new Date()));
    }

    private synchronized void changeFileIfNeeded() {
        final String newFilename = buildFilename();
        if (!currentFilename.equals(newFilename)) {
            final String oldPath = currentFilename;

            currentFilename = newFilename;
            // TODO: make this not have to call buildFilename() again
            updateOutputFile();
            super.publish(new LogRecord(Level.INFO, String.format("Continued from %s", oldPath)));
        }
    }

    @Override
    public synchronized void publish(LogRecord record) {
        changeFileIfNeeded();
        super.publish(record);
        flush();
    }
}
