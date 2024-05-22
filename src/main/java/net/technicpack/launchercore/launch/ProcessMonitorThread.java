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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessMonitorThread extends Thread {

    private final GameProcess process;
    private final String userAccessToken;

    public ProcessMonitorThread(GameProcess process, String userAccessToken) {
        super("ProcessMonitorThread");
        this.process = process;
        // Used for censoring the access token in the logs
        this.userAccessToken = userAccessToken;
    }

    public void run() {
        InputStreamReader reader = new InputStreamReader(this.process.getProcess().getInputStream());
        BufferedReader buf = new BufferedReader(reader);
        String line = null;
        String censoredLine;

        while (true) {
            try {
                while ((line = buf.readLine()) != null) {
                    if (userAccessToken.equals("0")) {
                        censoredLine = line;
                    } else {
                        censoredLine = line.replace(userAccessToken, "USER_ACCESS_TOKEN");
                    }

                    System.out.println(" " + censoredLine);
                }
            } catch (IOException ex) {
//				Logger.getLogger(ProcessMonitorThread.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    buf.close();
                } catch (IOException ex) {
//					Logger.getLogger(ProcessMonitorThread.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        process.getProcess().waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        System.out.println("Process exited with code " + process.getProcess().exitValue());

        if (process.getExitListener() != null) {
            process.getExitListener().onProcessExit();
        }
    }
}
