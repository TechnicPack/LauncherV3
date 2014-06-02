/*
 * This file is part of Technic Launcher.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ResourceUtils {

    public static File getResourceAsFile(String path) {
        File file = new File(".\\src\\main\\resources\\" + path);
        if (file.exists())
            return file;
        else
            return null;
    }

    public static ImageIcon getIcon(String iconName) {
        return new ImageIcon(ResourceUtils.class.getResource("/org/spoutcraft/launcher/resources/" + iconName));
    }

    public static BufferedImage getImage(String imageName) {
        try {
            return ImageIO.read(getResourceAsStream("/org/spoutcraft/launcher/resources/" + imageName));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static InputStream getResourceAsStream(String path) {
        InputStream stream = ResourceUtils.class.getResourceAsStream(path);
        String[] split = path.split("/");
        path = split[split.length - 1];
        if (stream == null) {
            File resource = new File(".\\src\\main\\resources\\" + path);
            if (resource.exists()) {
                try {
                    stream = new BufferedInputStream(new FileInputStream(resource));
                } catch (IOException ignore) {
                }
            }
        }
        return stream;
    }

    public static BufferedImage getImage(String imageName, int w, int h) {
        try {
            return ImageUtils.scaleImage(ImageIO.read(getResourceAsStream("/org/spoutcraft/launcher/resources/" + imageName)), w, h);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ImageIcon getIcon(String iconName, int w, int h) {
        try {
            return new ImageIcon(ImageUtils.scaleImage(ImageIO.read(getResourceAsStream("/org/spoutcraft/launcher/resources/" + iconName)), w, h));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
