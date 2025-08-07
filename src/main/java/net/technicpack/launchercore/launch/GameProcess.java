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

import java.util.concurrent.CompletableFuture;

public class GameProcess {
    private final Process process;
    private final CompletableFuture<Void> exitFuture = new CompletableFuture<>();
    private final ProcessMonitorThread monitorThread;

    public GameProcess(Process process, String userAccessToken) {
        this.process = process;

        this.monitorThread = new ProcessMonitorThread(this, userAccessToken);
        this.monitorThread.start();
    }

    public void setExitListener(ProcessExitListener exitListener) {
        exitFuture.thenRun(exitListener::onProcessExit);
    }

    public Process getProcess() {
        return process;
    }

    public void setProcessExited() {
        exitFuture.complete(null);
    }
}
