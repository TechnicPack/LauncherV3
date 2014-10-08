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
import net.technicpack.utilslib.DesktopUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.LinkListener;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedList;
import java.util.List;

public class DiscoverLinkListener extends LinkListener {

    private HttpPlatformApi platform;
    private List<Box> mousedLinks = new LinkedList<Box>();

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
            try {
                StringSelection selection = new StringSelection(platformUri);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            } catch (Exception ex) {
                //The clipboard is really temperamental if we mess with it too much, just ignore it
            }
        } else
            DesktopUtils.browseUrl(uri);
    }

    @Override
    public void onMouseOver(org.xhtmlrenderer.swing.BasicPanel panel, org.xhtmlrenderer.render.Box box) {
        if (isLink(panel, box)) {
            mousedLinks.add(box);
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void onMouseOut(org.xhtmlrenderer.swing.BasicPanel panel, org.xhtmlrenderer.render.Box box) {
        if (mousedLinks.contains(box)) {
            mousedLinks.remove(box);

            if (mousedLinks.size() == 0) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    protected boolean isLink(BasicPanel panel, Box box) {
        if (box == null || box.getElement() == null) {
            return false;
        }

        return findLink(panel, box.getElement());
    }

    private boolean findLink(BasicPanel panel, Element e) {
        String uri = null;

        for (Node node = e; node.getNodeType() == Node.ELEMENT_NODE; node = node.getParentNode()) {
            uri = panel.getSharedContext().getNamespaceHandler().getLinkUri((Element) node);

            if (uri != null) {
                return true;
            }
        }

        return false;
    }
}
