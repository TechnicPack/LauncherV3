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

package net.technicpack.ui.controls.list;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class SimpleScrollbarUI extends BasicScrollBarUI {

    private Color trackColor;
    private Color thumbColor;

    public SimpleScrollbarUI(Color trackColor, Color thumbColor) {
        this.trackColor = trackColor;
        this.thumbColor = thumbColor;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(trackColor);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        g.setColor(thumbColor);

        int x = thumbBounds.x+1;
        int y = thumbBounds.y+1;
        int w = thumbBounds.width-2;
        int h = thumbBounds.height-2;

        g.fillRect(x, y, w, h);
    }

    @Override
    protected void installComponents(){
        super.installComponents();
        scrollbar.remove(incrButton);
        scrollbar.remove(decrButton);
    }

    @Override
    protected void layoutVScrollbar(JScrollBar sb)
    {
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
        float trackH = sbSize.height - sbInsetsH;

        /* Compute the height and origin of the thumb.   The case
	 * where the thumb is at the bottom edge is handled specially
	 * to avoid numerical problems in computing thumbY.  Enforce
	 * the thumbs min/max dimensions.  If the thumb doesn't
	 * fit in the track (trackH) we'll hide it later.
	 */
        float min = sb.getMinimum();
        float extent = sb.getVisibleAmount();
        float range = sb.getMaximum() - min;
        float value = sb.getValue();

        int thumbH = (range <= 0)
                ? getMaximumThumbSize().height : (int)(trackH * (extent / range));
        thumbH = Math.max(thumbH, getMinimumThumbSize().height);
        thumbH = Math.min(thumbH, getMaximumThumbSize().height);

        int thumbY = endTrackY - thumbH;
        if (value < (sb.getMaximum() - sb.getVisibleAmount())) {
            float thumbRange = trackH - thumbH;
            thumbY = (int)(0.5f + (thumbRange * ((value - min) / (range - extent))));
        }

	/* Update the trackRect field.
	 */
        int itrackY = 0;
        int itrackH = endTrackY - itrackY;
        trackRect.setBounds(itemX, itrackY, itemW, itrackH);

	/* If the thumb isn't going to fit, zero it's bounds.  Otherwise
	 * make sure it fits between the buttons.  Note that setting the
	 * thumbs bounds will cause a repaint.
	 */
        if(thumbH >= (int)trackH)	{
            setThumbBounds(0, 0, 0, 0);
        }
        else {
            if ((thumbY + thumbH) > endTrackY) {
                thumbY = endTrackY - thumbH;
            }
            if (thumbY  < 0) {
                thumbY = 1;
            }
            setThumbBounds(itemX, thumbY, itemW, thumbH);
        }
    }
}
