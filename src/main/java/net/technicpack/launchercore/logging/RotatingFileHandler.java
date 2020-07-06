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

import net.technicpack.utilslib.Utils;
import org.joda.time.DateTime;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class RotatingFileHandler extends StreamHandler {
    private final DateTime date;
    private final String dateFormat = "yyyy-MM-dd";
    private final String unformattedLogFilePath;
    private final String buildNumber;
    private String filePath;
    private Boolean flushing;

    public RotatingFileHandler(String unformattedLogFilePath, String buildNumber) {
        this.unformattedLogFilePath = unformattedLogFilePath;
        this.buildNumber = buildNumber;
        date = new DateTime();
        filePath = unformattedLogFilePath.replace("%D", date.toString(dateFormat));
        try {
            setOutputStream(new FileOutputStream(filePath, true));
        } catch (FileNotFoundException ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String calculateFilename() {
        return unformattedLogFilePath.replace("%D", new DateTime().toString(dateFormat));
    }

    @Override
    public synchronized void flush() {
        String oldFilePath = filePath;
        String newFilePath = calculateFilename();
        if (!oldFilePath.equals(newFilePath)) {
            try {
                this.close();
                FileOutputStream fd = new FileOutputStream(newFilePath, true);
                if (!flushing) {
                    flushing = true;
                    BuildLogFormatter formattedLogger = new BuildLogFormatter(buildNumber);
                    fd.write((formattedLogger.format(new LogRecord(Level.INFO, "Continued from " + oldFilePath))).getBytes(Charset.forName("UTF-8")));
                }
                setOutputStream(fd);
            } catch (FileNotFoundException ex) {
                Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            } catch (IOException ex) {
                Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        } else {
            flushing = false;
        }
        super.flush();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
    }
}