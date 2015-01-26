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

package net.technicpack.ui.controls.feeds;

import net.technicpack.ui.lang.ResourceLoader;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

public class StatBox extends JButton {
    private ResourceLoader resources;

    JLabel fieldLabel;
    JLabel valueLabel;

    public StatBox(ResourceLoader resources, String fieldName, Integer value) {
        this.resources = resources;
        setOpaque(false);

        initComponents(fieldName);

        setValue(value);
    }

    public void setValue(Integer value) {
        String valueStr = buildValueStr(value);
        valueLabel.setText(valueStr);
    }

    @Override
    public void setForeground(Color color) {
        super.setForeground(color);

        if (fieldLabel != null)
            fieldLabel.setForeground(color);

        if (valueLabel != null)
            valueLabel.setForeground(color);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension arcs = new Dimension(5,5); //Border corners arcs {width,height}, change this to whatever you want
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //Draws the rounded panel with borders.
        graphics.setColor(getBackground());
        graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height);//paint background
    }

    private void initComponents(String fieldName) {
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 3));

        valueLabel = new JLabel("");
        valueLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 22, Font.BOLD));
        valueLabel.setForeground(getForeground());
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        this.add(valueLabel);

        fieldLabel = new JLabel(fieldName);
        fieldLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        fieldLabel.setForeground(getForeground());
        fieldLabel.setHorizontalAlignment(SwingConstants.CENTER);
        fieldLabel.setAlignmentX(CENTER_ALIGNMENT);
        this.add(fieldLabel);
    }

    private String buildValueStr(Integer value) {
        if (value == null)
            return "??";

        if (value >= 1000000000) {
            return buildShortenedStr(value/1000000000,"B");
        } else if (value >= 1000000) {
            return buildShortenedStr(value/1000000, "M");
        } else if (value >= 1000) {
            return buildShortenedStr(value/1000, "K");
        } else
            return NumberFormat.getInstance().format(value);
    }

    private String buildShortenedStr(int value, String suffix) {
        return Integer.toString(value)+suffix;
    }
}
