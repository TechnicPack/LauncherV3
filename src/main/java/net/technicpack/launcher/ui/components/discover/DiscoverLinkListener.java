/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
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

import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.utilslib.DesktopUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xhtmlrenderer.context.StyleReference;
import org.xhtmlrenderer.layout.Layer;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.PaintingInfo;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.LinkListener;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedList;
import java.util.List;

public class DiscoverLinkListener extends LinkListener {

    private IPlatformApi platform;
    private ModpackSelector modpackSelector;
    private List<Box> mousedLinks = new LinkedList<Box>();
    private Box _previouslyHovered;

    public DiscoverLinkListener(IPlatformApi platform, ModpackSelector modpackSelector ) {
        this.platform = platform;
        this.modpackSelector = modpackSelector;
    }

    @Override
    public void linkClicked(BasicPanel panel, String uri) {
        if (uri.startsWith("platform://")) {
            if (uri.length() < 12)
                return;
            String slug = uri.substring(11);

            String platformUri = this.platform.getPlatformUri(slug);
            try {
                modpackSelector.setFilter(platformUri);
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

        LayoutContext c = panel.getLayoutContext();

        if (c == null) {
            return;
        }

        boolean needRepaint = false;

        Element currentlyHovered = getHoveredElement(c.getCss(), box);

        if (currentlyHovered == panel.hovered_element) {
            return;
        }

        panel.hovered_element = currentlyHovered;
        Box hoverBox = findBoxForElement(currentlyHovered, panel.getRootLayer());

        boolean targetedRepaint = true;
        Rectangle repaintRegion = null;

        // If we moved out of the old block then unstyle it
        if (_previouslyHovered != null) {
            needRepaint = true;
            _previouslyHovered.restyle(c);

            PaintingInfo paintInfo = _previouslyHovered.getPaintingInfo();

            if (paintInfo == null) {
                targetedRepaint = false;
            } else {
                repaintRegion = new Rectangle(paintInfo.getAggregateBounds());
            }

            _previouslyHovered = null;
        }

        if (currentlyHovered != null) {
            needRepaint = true;
            Box target = hoverBox.getRestyleTarget();
            target.restyle(c);

            if (targetedRepaint) {
                PaintingInfo paintInfo = target.getPaintingInfo();

                if (paintInfo == null) {
                    targetedRepaint = false;
                } else {
                    if (repaintRegion == null) {
                        repaintRegion = new Rectangle(paintInfo.getAggregateBounds());
                    } else {
                        repaintRegion.add(paintInfo.getAggregateBounds());
                    }
                }
            }

            _previouslyHovered = target;
        }

        if (needRepaint) {
            if (targetedRepaint) {
                panel.repaint(repaintRegion);
            } else {
                panel.repaint();
            }
        }
    }

    // look up the Element that corresponds to the Box we are hovering over
    private Element getHoveredElement(StyleReference style, Box ib) {
        if (ib == null) {
            return null;
        }

        Element element = ib.getElement();

        while (element != null && !style.isHoverStyled(element)) {
            Node node = element.getParentNode();
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                element = (Element) node;
            } else {
                element = null;
            }
        }

        return element;
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

    private Box findBoxForElement(Element e, Layer layer) {
        Box find = findBoxForElement(e, layer.getMaster());

        if (find != null)
            return find;

        for (Object l : layer.getChildren()) {
            find = findBoxForElement(e, (Layer)l);

            if (find != null)
                return find;
        }

        return null;
    }

    private Box findBoxForElement(Element e, Box box) {
        if (box.getElement() == e)
            return box;

        for(Object b : box.getChildren()) {
            Box find = findBoxForElement(e,(Box)b);

            if (find != null)
                return find;
        }

        return null;
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
