/**
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

package net.technicpack.utilslib;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DesktopUtils {
    private static void showBrowseUrlDialog(String url) {
        JOptionPane.showInputDialog(null,
                "Unable to open browser, please visit the URL manually (copy it from the box):",
                "Unable to open browser",
                JOptionPane.ERROR_MESSAGE,
                null,
                null,
                url);
    }

    public static void browseUrl(String url) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    if (url.startsWith("mailto:"))
                        Desktop.getDesktop().mail(new URI(url));
                    else {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                            Desktop.getDesktop().browse(new URI(url));
                        else if (OperatingSystem.getOperatingSystem() == OperatingSystem.LINUX)
                            new ProcessBuilder("xdg-open", url).start();
                        else if (OperatingSystem.getOperatingSystem() == OperatingSystem.OSX)
                            new ProcessBuilder("open", url).start();
                        else
                            SwingUtilities.invokeLater(() -> showBrowseUrlDialog(url));
                    }
                } catch (IOException e) {
                    Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
                    SwingUtilities.invokeLater(() -> showBrowseUrlDialog(url));
                } catch (URISyntaxException e) {
                    //If we got a bogus URL from the internet, then this will throw.  Log & Ignore
                    Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
                } catch (RuntimeException e) {
                    //browse() throws a bunch of runtime exceptions if you give it bad input
                    //WHICH IS AWESOME
                    Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
                }

                return null;
            }
        }.execute();
    }

    public static void open(final File file) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                open(file, DesktopUtils::canOpenWithDesktop, DesktopUtils::openWithDesktop, DesktopUtils::launchProcess, OperatingSystem.getOperatingSystem());

                return null;
            }
        }.execute();
    }

    static void open(File file,
                     BooleanSupplier canOpenWithDesktop,
                     Consumer<File> desktopOpen,
                     Consumer<String[]> processLauncher,
                     OperatingSystem operatingSystem) {
        Utils.getLogger().info("Attempting to open " + file.getAbsolutePath());

        try {
            if (canOpenWithDesktop.getAsBoolean()) {
                desktopOpen.accept(file);
                return;
            }
        } catch (RuntimeException e) {
            Utils.getLogger().log(Level.WARNING, String.format("Desktop open failed for %s", file.getAbsolutePath()), e);
        }

        try {
            openWithFallback(file, processLauncher, operatingSystem);
        } catch (RuntimeException e) {
            Utils.getLogger().log(Level.SEVERE, String.format("Unable to open %s", file.getAbsolutePath()), e);
        }
    }

    private static boolean canOpenWithDesktop() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
    }

    private static void openWithDesktop(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void launchProcess(String[] command) {
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void openWithFallback(File file, Consumer<String[]> processLauncher, OperatingSystem operatingSystem) {
        String path = file.getAbsolutePath();
        switch (operatingSystem) {
            case LINUX:
                processLauncher.accept(new String[]{"xdg-open", path});
                break;
            case OSX:
                processLauncher.accept(new String[]{"open", path});
                break;
            case WINDOWS:
                processLauncher.accept(new String[]{"explorer.exe", path});
                break;
            case UNKNOWN:
            default:
                throw new IllegalStateException(String.format("No folder-opening fallback available for operating system %s", operatingSystem));
        }
    }
}
