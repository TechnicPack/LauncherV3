package net.technicpack.launcher.ui.controls.modpacks;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.AAJLabel;

import javax.annotation.Resource;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

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

public class StatBox extends JPanel {
    private ResourceLoader resources;

    JLabel fieldLabel;
    JLabel valueLabel;

    public StatBox(ResourceLoader resources, String fieldName, int value) {
        this.resources = resources;

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

    private void initComponents(String fieldName) {
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(3, 24, 12, 24));

        valueLabel = new AAJLabel("0");
        valueLabel.setFont(resources.getFont("OpenSans-Bold.ttf", 26));
        valueLabel.setForeground(getForeground());
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setAlignmentX(CENTER_ALIGNMENT);
        this.add(valueLabel);

        fieldLabel = new JLabel(fieldName);
        fieldLabel.setFont(resources.getFont("Raleway-Light.ttf", 13));
        fieldLabel.setForeground(getForeground());
        fieldLabel.setHorizontalAlignment(SwingConstants.CENTER);
        fieldLabel.setAlignmentX(CENTER_ALIGNMENT);
        this.add(fieldLabel);
    }

    private String buildValueStr(int value) {
        if (value >= 1000000000) {
            return buildShortenedStr(value/100000000,"G");
        } else if (value >= 1000000) {
            return buildShortenedStr(value/100000, "M");
        } else if (value >= 2000) {
            return buildShortenedStr(value/100, "K");
        } else
            return NumberFormat.getInstance().format(value);
    }

    private String buildShortenedStr(int value, String suffix) {
        if (value >= 100) {
            return Integer.toString(value/10)+suffix;
        } else {
            double decimalValue = ((double)value)/10.0;
            DecimalFormat formatter = new DecimalFormat("#.#");
            return formatter.format(decimalValue) + suffix;
        }
    }
}
