package net.technicpack.launcher.lang;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ResourceLoader {
    private Collection<IRelocalizableResource> resources = new LinkedList<IRelocalizableResource>();

    public ImageIcon getIcon(String iconName) {
        return new ImageIcon(ResourceLoader.class.getResource("/" + iconName));
    }

    public BufferedImage getImage(String imageName) {
        try {
            return ImageIO.read(ResourceLoader.class.getResourceAsStream("/"+imageName));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public Font getFont(String name, int size) {
        return getFont(name,size,0);
    }

    public Font getFont(String name, int size, int style) {
        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.class.getResourceAsStream("/fonts/"+name)).deriveFont((float) size).deriveFont(style);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            font = new Font("Arial", Font.PLAIN, 12);
        }
        return font;
    }

    private void relocalizeResources() {
        for(IRelocalizableResource resource : resources) {
            resource.Relocalize(this);
        }
    }

    public void registerResource(IRelocalizableResource resource) {
        if (!resources.contains(resource))
            resources.add(resource);
    }
}
