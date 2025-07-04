/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.controls.list;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class SimpleScrollbarUI extends BasicScrollBarUI {

    private final Color myTrackColor;
    private final Color myThumbColor;

    public SimpleScrollbarUI(Color trackColor, Color thumbColor) {
        this.myTrackColor = trackColor;
        this.myThumbColor = thumbColor;
    }

    @Override
    protected void configureScrollBarColors() {
        super.configureScrollBarColors();
        thumbColor = myThumbColor;
        trackColor = myTrackColor;
    }

    @Override
    protected void installComponents() {
        // Do nothing, so buttons never get created or added
    }

    @Override
    protected void uninstallComponents() {
        // Do nothing.
        // The buttons never get created or added, so this is here to prevent a NullPointerException, which happens
        // when BasicScrollBarUI.uninstallComponents() calls scrollbar.remove(x) with x being a null button
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(trackColor);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        // Draw the thumb with a 1 px transparent margin
        int x = thumbBounds.x + 1;
        int y = thumbBounds.y + 1;
        int width = thumbBounds.width - 2;
        int height = thumbBounds.height - 2;

        g.setColor(thumbColor);
        g.fillRect(x, y, width, height);
    }

    @Override
    protected void layoutVScrollbar(JScrollBar sb) {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        /*
         * Width and left edge of the buttons and thumb.
         */
        int itemW = sbSize.width - (sbInsets.left + sbInsets.right);
        int itemX = sbInsets.left;
        int endTrackY = sbSize.height - sbInsets.bottom;

        /* The thumb must fit within the height left over after we
         * subtract the preferredSize of the buttons and the insets
         * and the gaps
         */
        int sbInsetsH = sbInsets.top + sbInsets.bottom;
        float trackH = sbSize.height - (float) sbInsetsH;

        /* Compute the height and origin of the thumb.   The case
         * where the thumb is at the bottom edge is handled especially
         * to avoid numerical problems in computing thumbY.  Enforce
         * the thumb's min/max dimensions.  If the thumb doesn't
         * fit in the track (trackH), we'll hide it later.
         */
        float min = sb.getMinimum();
        float extent = sb.getVisibleAmount();
        float range = sb.getMaximum() - min;
        float value = sb.getValue();

        int thumbH = (range <= 0) ? getMaximumThumbSize().height : (int) (trackH * (extent / range));
        thumbH = Math.max(thumbH, getMinimumThumbSize().height);
        thumbH = Math.min(thumbH, getMaximumThumbSize().height);

        int thumbY = endTrackY - thumbH;
        if (value < (sb.getMaximum() - sb.getVisibleAmount())) {
            float thumbRange = trackH - thumbH;
            thumbY = (int) (0.5f + (thumbRange * ((value - min) / (range - extent))));
        }

        /* Update the trackRect field.
         */
        int itrackY = 0;
        int itrackH = endTrackY - itrackY;
        trackRect.setBounds(itemX, itrackY, itemW, itrackH);

        /* If the thumb isn't going to fit, zero its bounds.  Otherwise,
         * make sure it fits between the buttons.  Note that setting the
         * thumb bounds will cause a repaint.
         */
        if (thumbH >= (int) trackH) {
            setThumbBounds(0, 0, 0, 0);
        } else {
            if ((thumbY + thumbH) > endTrackY) {
                thumbY = endTrackY - thumbH;
            }
            if (thumbY < 0) {
                thumbY = 1;
            }
            setThumbBounds(itemX, thumbY, itemW, thumbH);
        }
    }
}
