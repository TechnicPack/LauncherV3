/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.ui.components.discover;

import net.technicpack.platform.http.HttpPlatformApi;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.LinkListener;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class DiscoverLinkListener extends LinkListener {

    private HttpPlatformApi platform;

    public DiscoverLinkListener(HttpPlatformApi platform ) {
        this.platform = platform;
    }

    @Override
    public void linkClicked(BasicPanel panel, String uri) {
        if (uri.startsWith("platform://")) {
            if (uri.length() < 12)
                return;
            String slug = uri.substring(11);

            String platformUri = this.platform.getPlatformUri(slug);
            StringSelection selection = new StringSelection(platformUri);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
    }
}
