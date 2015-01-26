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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class RotatingFileHandler extends StreamHandler {
    private final SimpleDateFormat date;
    private final String logFile;
    private String filename;

    public RotatingFileHandler(String logFile) {
        this.logFile = logFile;
        date = new SimpleDateFormat("yyyy-MM-dd");
        filename = calculateFilename();
        try {
            setOutputStream(new FileOutputStream(filename, true));
        } catch (FileNotFoundException ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String calculateFilename() {
        return logFile.replace("%D", date.format(new Date()));
    }

    @Override
    public synchronized void flush() {
        if (!filename.equals(calculateFilename())) {
            filename = calculateFilename();
            try {
                this.close();
                setOutputStream(new FileOutputStream(filename, true));
            } catch (FileNotFoundException ex) {
                Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        super.flush();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
    }
}