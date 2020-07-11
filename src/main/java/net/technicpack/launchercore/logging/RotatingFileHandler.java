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
    private final SimpleDateFormat dateFormat;
    private final String logPathFormat;
    private String currentLogPath;

    public RotatingFileHandler(String logPathFormat) {
        this.logPathFormat = logPathFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        currentLogPath = calculatePath();
        try {
            setOutputStream(new FileOutputStream(currentLogPath, true));
        } catch (FileNotFoundException ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String calculatePath() {
        return logPathFormat.replace("%D", dateFormat.format(new Date()));
    }

    private void changeFileIfNeeded() {
        final String newPath = calculatePath();
        if (!currentLogPath.equals(newPath)) {
            final String oldPath = currentLogPath;
            currentLogPath = newPath;
            try {
                this.close();
                setOutputStream(new FileOutputStream(currentLogPath, true));
                super.publish(new LogRecord(Level.INFO, "Continued from " + oldPath));
            } catch (FileNotFoundException ex) {
                Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public synchronized void publish(LogRecord record) {
        changeFileIfNeeded();
        super.publish(record);
        flush();
    }
}