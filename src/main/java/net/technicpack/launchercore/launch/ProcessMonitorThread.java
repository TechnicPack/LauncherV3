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

package net.technicpack.launchercore.launch;

import net.technicpack.utilslib.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ProcessMonitorThread extends Thread {

    private final GameProcess process;
    private final String userAccessToken;
    private final Logger logger;

    public ProcessMonitorThread(GameProcess process, String userAccessToken) {
        super("ProcessMonitorThread");
        this.process = process;
        // Used for redacting the access token in the logs
        this.userAccessToken = userAccessToken;
        this.logger = Utils.getLogger();
    }

    @Override
    public void run() {
        final Pattern redactPattern = Pattern.compile("(?i)" + Pattern.quote(userAccessToken));
        final boolean shouldRedact = !userAccessToken.equals("0");

        try (InputStreamReader reader = new InputStreamReader(this.process.getProcess().getInputStream());
                BufferedReader buf = new BufferedReader(reader)) {
            String line;
            StringBuilder lineBuilder = new StringBuilder(1024);
            while ((line = buf.readLine()) != null) {
                if (shouldRedact) {
                    line = redactPattern.matcher(line).replaceAll("USER_ACCESS_TOKEN");
                }

                lineBuilder.setLength(0);
                lineBuilder.append(' ').append(line);
                this.logger.info(lineBuilder.toString());
            }
        } catch (IOException e) {
            this.logger.log(Level.SEVERE,
                    "Error reading process output - process will continue running but output will not be logged", e);
        }

        try {
            process.getProcess().waitFor();
            this.logger.info(String.format("Process exited with code %d", process.getProcess().exitValue()));
        } catch (InterruptedException e) {
            this.logger.log(Level.SEVERE, "Interrupted while waiting for process to exit", e);
            this.interrupt();
        } finally {
            if (process.getExitListener() != null) {
                process.getExitListener().onProcessExit();
            }
        }
    }
}
