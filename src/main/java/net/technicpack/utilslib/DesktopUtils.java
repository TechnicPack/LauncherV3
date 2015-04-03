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

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class DesktopUtils {
    public static void browseUrl(String url) {
        try {
            if (url.startsWith("mailto:"))
                Desktop.getDesktop().mail(new URI(url));
            else
                Desktop.getDesktop().browse(new URI(url));
        } catch (IOException ex) {
            //Thrown by Desktop.browse() - just log & ignore
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        } catch (URISyntaxException ex) {
            //If we got a bogus URL from the internet, then this will throw.  Log & Ignore
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            //browse() throws a bunch of runtime exceptions if you give it bad input
            //WHICH IS AWESOME
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static void open(final File file) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Utils.getLogger().info("Attempting to open "+file.getAbsolutePath());
                String asciiUri = file.toURI().toASCIIString();
                Utils.getLogger().info("Using "+asciiUri);
                if (asciiUri.startsWith("file:") && !asciiUri.startsWith("file://"))
                    asciiUri = asciiUri.replaceFirst("file:", "file://");
                Utils.getLogger().info("Intermediary path "+asciiUri);
                try {
                    Desktop.getDesktop().open(new File(new URI(asciiUri).getPath()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                return null;
            }
        }.execute();
    }
}
