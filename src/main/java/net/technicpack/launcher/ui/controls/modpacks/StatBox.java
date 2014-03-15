/*
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
package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.controls.AAJLabel;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class StatBox extends JPanel {
    private ResourceLoader resources;

    JLabel fieldLabel;
    JLabel valueLabel;

    public StatBox(ResourceLoader resources, String fieldName, int value) {
        this.resources = resources;
        setOpaque(false);

        initComponents(fieldName);

        setValue(value);
    }

    public void setValue(int value) {
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
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 3));

        valueLabel = new AAJLabel("0");
        valueLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 22, Font.BOLD));
        valueLabel.setForeground(getForeground());
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);
        this.add(valueLabel);

        fieldLabel = new AAJLabel(fieldName);
        fieldLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        fieldLabel.setForeground(getForeground());
        fieldLabel.setHorizontalAlignment(SwingConstants.CENTER);
        fieldLabel.setAlignmentX(CENTER_ALIGNMENT);
        this.add(fieldLabel);
    }

    private String buildValueStr(int value) {
        if (value >= 1000000000) {
            return buildShortenedStr(value/1000000000,"G");
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
