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

package net.technicpack.launcher.ui.components;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launcher.ui.controls.AAJLabel;
import net.technicpack.launcher.ui.controls.LauncherDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LauncherOptionsDialog extends LauncherDialog {

    private static final int DIALOG_WIDTH = 994;
    private static final int DIALOG_HEIGHT = 502;

    public LauncherOptionsDialog(Frame owner, ResourceLoader resourceLoader) {
        super(owner);

        initComponents(resourceLoader);
    }

    protected void closeDialog() {
        dispose();
    }

    private void initComponents(ResourceLoader resources) {
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(Color.black);
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        add(header, BorderLayout.PAGE_START);

        AAJLabel title = new AAJLabel(resources.getString("launcher.title.options"));
        title.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 34));
        title.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        title.setOpaque(false);
        title.setIcon(resources.getIcon("options_cog.png"));
        header.add(title);

        header.add(Box.createHorizontalGlue());

        JButton closeButton = new JButton();
        closeButton.setIcon(resources.getIcon("close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });
        header.add(closeButton);

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        add(centerPanel, BorderLayout.CENTER);
    }
}
