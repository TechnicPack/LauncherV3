/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
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

package net.technicpack.launchercore.install;

import net.technicpack.launchercore.util.ResourceUtils;

import java.awt.image.BufferedImage;

public class AddPack extends InstalledPack {
    private final static BufferedImage icon = ResourceUtils.getImage("icon.png", 32, 32);
    private final static BufferedImage logo = ResourceUtils.getImage("addNewPack.png", 180, 110);
    private final static BufferedImage background = ResourceUtils.getImage("background.jpg", 880, 520);

    public AddPack() {
        super();
    }

    @Override
    public synchronized BufferedImage getIcon() {
        return icon;
    }

    @Override
    public synchronized BufferedImage getBackground() {
        return background;
    }

    @Override
    public synchronized BufferedImage getLogo() {
        return logo;
    }

    @Override
    public String getName() {
        return "addpack";
    }

    @Override
    public String getDisplayName() {
        return "Add Pack";
    }
}
