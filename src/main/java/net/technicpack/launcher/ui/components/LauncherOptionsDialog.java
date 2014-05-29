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
import net.technicpack.launcher.ui.controls.RoundedButton;
import net.technicpack.launcher.ui.controls.borders.RoundBorder;
import net.technicpack.launcher.ui.controls.login.UserCellEditor;
import net.technicpack.launcher.ui.controls.login.UserCellRenderer;
import net.technicpack.launcher.ui.controls.login.UserCellUI;
import net.technicpack.launcher.ui.controls.tabs.SimpleTabPane;
import net.technicpack.launcher.ui.controls.tabs.SimpleTabPaneUI;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

public class LauncherOptionsDialog extends LauncherDialog {

    private static final int DIALOG_WIDTH = 730;
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

        SimpleTabPane centerPanel = new SimpleTabPane();
        centerPanel.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        centerPanel.setForeground(LauncherFrame.COLOR_GREY_TEXT);
        centerPanel.setSelectedBackground(LauncherFrame.COLOR_BLUE);
        centerPanel.setSelectedForeground(LauncherFrame.COLOR_WHITE_TEXT);
        centerPanel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        centerPanel.setOpaque(true);
        add(centerPanel, BorderLayout.CENTER);

        JPanel general = new JPanel();
        general.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        setupGeneralPanel(general, resources);

        JPanel javaOptions = new JPanel();
        javaOptions.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        setupJavaOptionsPanel(javaOptions, resources);

        JPanel about = new JPanel();
        about.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        centerPanel.addTab(resources.getString("launcheroptions.tab.general").toUpperCase(), general);
        centerPanel.addTab(resources.getString("launcheroptions.tab.java").toUpperCase(), javaOptions);
        centerPanel.addTab(resources.getString("launcheroptions.tab.about").toUpperCase(), about);
    }

    private void setupGeneralPanel(JPanel panel, ResourceLoader resources) {

        panel.setLayout(new GridBagLayout());

        JLabel streamLabel = new JLabel(resources.getString("launcheroptions.general.build"));
        streamLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        streamLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(streamLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        // Setup stream select box
        JComboBox streamSelect = new JComboBox();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            streamSelect.setUI(new MetalComboBoxUI());
        }

        streamSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        streamSelect.setEditable(false);
        streamSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        streamSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        streamSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        streamSelect.setUI(new UserCellUI(resources));
        streamSelect.setFocusable(false);

        Object child = streamSelect.getAccessibleContext().getAccessibleChild(0);
        BasicComboPopup popup = (BasicComboPopup)child;
        JList list = popup.getList();
        list.setSelectionForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        list.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        list.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 0));

        panel.add(streamSelect, new GridBagConstraints(1, 0, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        streamSelect.addItem(resources.getString("launcheroptions.build.stable"));
        streamSelect.addItem(resources.getString("launcheroptions.build.beta"));

        //Setup on pack launch box
        JLabel launchLabel = new JLabel(resources.getString("launcheroptions.general.onlaunch"));
        launchLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        launchLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(launchLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        JComboBox launchSelect = new JComboBox();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            launchSelect.setUI(new MetalComboBoxUI());
        }

        launchSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        launchSelect.setEditable(false);
        launchSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        launchSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        launchSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        launchSelect.setUI(new UserCellUI(resources));
        launchSelect.setFocusable(false);

        child = launchSelect.getAccessibleContext().getAccessibleChild(0);
        popup = (BasicComboPopup)child;
        list = popup.getList();
        list.setSelectionForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        list.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        list.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 0));

        panel.add(launchSelect, new GridBagConstraints(1, 1, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        launchSelect.addItem(resources.getString("launcheroptions.packlaunch.hide"));
        launchSelect.addItem(resources.getString("launcheroptions.packlaunch.close"));
        launchSelect.addItem(resources.getString("launcheroptions.packlaunch.nothing"));

        //Install folder field
        JLabel installLabel = new JLabel(resources.getString("launcheroptions.general.install"));
        installLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        installLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(installLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        JTextField installField = new JTextField("C:\\Farts\\");
        installField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        installField.setForeground(LauncherFrame.COLOR_BLUE);
        installField.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        installField.setHighlighter(null);
        installField.setEditable(false);
        installField.setCursor(null);
        installField.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(installField, new GridBagConstraints(1, 2, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        RoundedButton reinstallButton = new RoundedButton(resources.getString("launcheroptions.install.change"));
        reinstallButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        reinstallButton.setContentAreaFilled(false);
        reinstallButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        reinstallButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        panel.add(reinstallButton, new GridBagConstraints(3, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));

        //Client ID field
        JLabel clientIdField = new JLabel(resources.getString("launcheroptions.general.id"));
        clientIdField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        clientIdField.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(clientIdField, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        JTextField clientId = new JTextField("abc123");
        clientId.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        clientId.setForeground(LauncherFrame.COLOR_BLUE);
        clientId.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        clientId.setHighlighter(null);
        clientId.setEditable(false);
        clientId.setCursor(null);
        clientId.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(clientId, new GridBagConstraints(1, 3, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        RoundedButton copyButton = new RoundedButton(resources.getString("launcheroptions.id.copy"));
        copyButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        copyButton.setContentAreaFilled(false);
        copyButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        copyButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        panel.add(copyButton, new GridBagConstraints(3, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));

        panel.add(Box.createRigidArea(new Dimension(80, 0)), new GridBagConstraints(4, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));

        //Add show console field
        JLabel showConsoleField = new JLabel(resources.getString("launcheroptions.general.console"));
        showConsoleField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        showConsoleField.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(showConsoleField, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 40, 0, 0), 0, 0));

        JCheckBox showConsole = new JCheckBox("", false);
        showConsole.setOpaque(false);
        showConsole.setHorizontalAlignment(SwingConstants.RIGHT);
        showConsole.setBorder(BorderFactory.createEmptyBorder());
        showConsole.setIconTextGap(0);
        showConsole.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        showConsole.setIcon(resources.getIcon("checkbox_open.png"));
        showConsole.setFocusPainted(false);
        panel.add(showConsole, new GridBagConstraints(1, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(16, 16, 0, 0), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 5, 5, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));

        //Open logs button
        RoundedButton openLogs = new RoundedButton(resources.getString("launcheroptions.general.logs"));
        openLogs.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        openLogs.setContentAreaFilled(false);
        openLogs.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        openLogs.setHoverForeground(LauncherFrame.COLOR_BLUE);
        openLogs.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
        panel.add(openLogs, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
    }

    private void setupJavaOptionsPanel(JPanel panel, ResourceLoader resources) {
        panel.setLayout(new GridBagLayout());

        JLabel memLabel = new JLabel(resources.getString("launcheroptions.java.memory"));
        memLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        memLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(memLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        JComboBox memSelect = new JComboBox();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            memSelect.setUI(new MetalComboBoxUI());
        }

        memSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        memSelect.setEditable(false);
        memSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        memSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        memSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        memSelect.setUI(new UserCellUI(resources));
        memSelect.setFocusable(false);

        Object child = memSelect.getAccessibleContext().getAccessibleChild(0);
        BasicComboPopup popup = (BasicComboPopup)child;
        JList list = popup.getList();
        list.setSelectionForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        list.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        list.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 0));

        panel.add(memSelect, new GridBagConstraints(1, 0, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 80), 0, 16));

        memSelect.addItem("1GB");
        memSelect.addItem("1.5GB");
        memSelect.addItem("2GB");
        memSelect.addItem("3GB");
        memSelect.addItem("4GB");
        memSelect.addItem("5GB");
        memSelect.addItem("6GB");
        memSelect.addItem("7GB");
        memSelect.addItem("8GB");

        JLabel argsLabel = new JLabel(resources.getString("launcheroptions.java.arguments"));
        argsLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        argsLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(argsLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        JTextArea args = new JTextArea(32, 4);
        args.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        args.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        args.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        args.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        args.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        args.setMargin(new Insets(16,4,16,4));
        args.setLineWrap(true);
        args.setWrapStyleWord(true);
        args.setSelectionColor(LauncherFrame.COLOR_BUTTON_BLUE);
        args.setSelectedTextColor(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        panel.add(args, new GridBagConstraints(1, 1, 1, 2, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 64, 80), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 2, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }
}
