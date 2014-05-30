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
import net.technicpack.launcher.settings.TechnicSettings;
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
import net.technicpack.launcher.ui.listitems.LanguageItem;
import net.technicpack.launcher.ui.listitems.OnLaunchItem;
import net.technicpack.launcher.ui.listitems.StreamItem;
import net.technicpack.launchercore.util.LaunchAction;
import net.technicpack.utilslib.Memory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

public class LauncherOptionsDialog extends LauncherDialog {

    private static final int DIALOG_WIDTH = 830;
    private static final int DIALOG_HEIGHT = 564;

    private TechnicSettings settings;

    private boolean hasShownStreamInfo = false;
    private ResourceLoader resources;

    private DocumentListener listener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            changeJavaArgs();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changeJavaArgs();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changeJavaArgs();
        }
    };

    JComboBox memSelect;
    JTextArea javaArgs;
    JComboBox streamSelect;
    JComboBox launchSelect;
    JComboBox langSelect;
    JTextField installField;
    JTextField clientId;
    JCheckBox showConsole;

    public LauncherOptionsDialog(Frame owner, TechnicSettings settings, ResourceLoader resourceLoader) {
        super(owner);

        this.settings = settings;
        this.resources = resourceLoader;

        initComponents();
        initControlValues();
    }

    protected void closeDialog() {
        dispose();
    }

    protected void changeJavaArgs() {
        settings.setJavaArgs(javaArgs.getText());
        settings.save();
    }

    protected void changeShowConsole() {
        settings.setShowConsole(showConsole.isSelected());
        settings.save();
    }

    protected void changeMemory() {
        settings.setMemory(((Memory)memSelect.getSelectedItem()).getSettingsId());
        settings.save();
    }

    protected void changeStream() {
        settings.setBuildStream(((StreamItem)streamSelect.getSelectedItem()).getStream());
        settings.save();

        if (!hasShownStreamInfo) {
            JOptionPane.showMessageDialog(this, resources.getString("launcheroptions.streamchange.text"), resources.getString("launcheroptions.streamchange.title"), JOptionPane.INFORMATION_MESSAGE);

            hasShownStreamInfo = true;
        }
    }

    protected void changeLaunchAction() {
        settings.setLaunchAction(((OnLaunchItem)launchSelect.getSelectedItem()).getLaunchAction());
        settings.save();
    }

    protected void changeLanguage() {
        settings.setLanguageCode(((LanguageItem)langSelect.getSelectedItem()).getLangCode());
        settings.save();
    }

    private void initControlValues() {

        javaArgs.getDocument().removeDocumentListener(listener);
        javaArgs.setText(settings.getJavaArgs());
        javaArgs.getDocument().addDocumentListener(listener);

        installField.setText(settings.getTechnicRoot().getAbsolutePath());
        clientId.setText(settings.getClientId());

        for (ActionListener listener : showConsole.getActionListeners())
            showConsole.removeActionListener(listener);
        showConsole.setSelected(settings.getShowConsole());
        showConsole.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeShowConsole();
            }
        });

        for (ActionListener listener : memSelect.getActionListeners())
            memSelect.removeActionListener(listener);

        memSelect.removeAllItems();
        for (int i = 0; i < Memory.memoryOptions.length; i++) {
            memSelect.addItem(Memory.memoryOptions[i]);
        }

        memSelect.setSelectedItem(Memory.getMemoryFromId(settings.getMemory()));
        memSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeMemory();
            }
        });

        for (ActionListener listener : streamSelect.getActionListeners()) {
            streamSelect.removeActionListener(listener);
        }
        streamSelect.removeAllItems();
        streamSelect.addItem(new StreamItem(resources.getString("launcheroptions.build.stable"), "stable"));
        streamSelect.addItem(new StreamItem(resources.getString("launcheroptions.build.beta"), "beta"));
        streamSelect.setSelectedIndex((settings.getBuildStream().equals("beta"))?1:0);
        streamSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeStream();
            }
        });

        for (ActionListener listener : launchSelect.getActionListeners())
            launchSelect.removeActionListener(listener);
        launchSelect.removeAllItems();
        launchSelect.addItem(new OnLaunchItem(resources.getString("launcheroptions.packlaunch.hide"), LaunchAction.HIDE));
        launchSelect.addItem(new OnLaunchItem(resources.getString("launcheroptions.packlaunch.close"), LaunchAction.CLOSE));
        launchSelect.addItem(new OnLaunchItem(resources.getString("launcheroptions.packlaunch.nothing"), LaunchAction.NOTHING));

        switch (settings.getLaunchAction()) {
            case HIDE:
                launchSelect.setSelectedIndex(0);
                break;
            case CLOSE:
                launchSelect.setSelectedIndex(1);
                break;
            case NOTHING:
                launchSelect.setSelectedIndex(2);
                break;
            default:
                launchSelect.setSelectedIndex(0);
        }
        launchSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeLaunchAction();
            }
        });

        for (ActionListener listener : langSelect.getActionListeners())
            langSelect.removeActionListener(listener);
        langSelect.removeAllItems();
        langSelect.addItem(new LanguageItem(ResourceLoader.DEFAULT_LOCALE, resources.getString("launcheroptions.language.default")));
        for (int i = 0; i < ResourceLoader.SUPPORTED_LOCALES.length; i++) {
            langSelect.addItem(new LanguageItem(resources.getCodeFromLocale(ResourceLoader.SUPPORTED_LOCALES[i]), ResourceLoader.SUPPORTED_LOCALES[i].getDisplayName(ResourceLoader.SUPPORTED_LOCALES[i])));
        }
        if (!settings.getLanguageCode().equalsIgnoreCase(ResourceLoader.DEFAULT_LOCALE)) {
            Locale loc = resources.getLocaleFromCode(settings.getLanguageCode());

            for (int i = 0; i < ResourceLoader.SUPPORTED_LOCALES.length; i++) {
                if (loc.equals(ResourceLoader.SUPPORTED_LOCALES[i])) {
                    langSelect.setSelectedIndex(i+1);
                    break;
                }
            }
        }
        langSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeLanguage();
            }
        });
    }

    private void initComponents() {
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

        setupGeneralPanel(general);

        JPanel javaOptions = new JPanel();
        javaOptions.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        setupJavaOptionsPanel(javaOptions);

        JPanel about = new JPanel();
        about.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        centerPanel.addTab(resources.getString("launcheroptions.tab.general").toUpperCase(), general);
        centerPanel.addTab(resources.getString("launcheroptions.tab.java").toUpperCase(), javaOptions);
        centerPanel.addTab(resources.getString("launcheroptions.tab.about").toUpperCase(), about);
    }

    private void setupGeneralPanel(JPanel panel) {

        panel.setLayout(new GridBagLayout());

        JLabel streamLabel = new JLabel(resources.getString("launcheroptions.general.build"));
        streamLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        streamLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(streamLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        // Setup stream select box
        streamSelect = new JComboBox();

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

        //Setup language box
        JLabel langLabel = new JLabel(resources.getString("launcheroptions.general.lang"));
        langLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        langLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(langLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        langSelect = new JComboBox();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            langSelect.setUI(new MetalComboBoxUI());
        }

        langSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        langSelect.setEditable(false);
        langSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        langSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        langSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        langSelect.setUI(new UserCellUI(resources));
        langSelect.setFocusable(false);

        child = langSelect.getAccessibleContext().getAccessibleChild(0);
        popup = (BasicComboPopup)child;
        list = popup.getList();
        list.setSelectionForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        list.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        list.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 0));

        panel.add(langSelect, new GridBagConstraints(1, 1, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        //Setup on pack launch box
        JLabel launchLabel = new JLabel(resources.getString("launcheroptions.general.onlaunch"));
        launchLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        launchLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(launchLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        launchSelect = new JComboBox();

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

        panel.add(launchSelect, new GridBagConstraints(1, 2, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        //Install folder field
        JLabel installLabel = new JLabel(resources.getString("launcheroptions.general.install"));
        installLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        installLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(installLabel, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        installField = new JTextField("C:\\Farts\\");
        installField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        installField.setForeground(LauncherFrame.COLOR_BLUE);
        installField.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        installField.setHighlighter(null);
        installField.setEditable(false);
        installField.setCursor(null);
        installField.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(installField, new GridBagConstraints(1, 3, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        RoundedButton reinstallButton = new RoundedButton(resources.getString("launcheroptions.install.change"));
        reinstallButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        reinstallButton.setContentAreaFilled(false);
        reinstallButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        reinstallButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        panel.add(reinstallButton, new GridBagConstraints(3, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));

        //Client ID field
        JLabel clientIdField = new JLabel(resources.getString("launcheroptions.general.id"));
        clientIdField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        clientIdField.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(clientIdField, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        clientId = new JTextField("abc123");
        clientId.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        clientId.setForeground(LauncherFrame.COLOR_BLUE);
        clientId.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        clientId.setHighlighter(null);
        clientId.setEditable(false);
        clientId.setCursor(null);
        clientId.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(clientId, new GridBagConstraints(1, 4, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        RoundedButton copyButton = new RoundedButton(resources.getString("launcheroptions.id.copy"));
        copyButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        copyButton.setContentAreaFilled(false);
        copyButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        copyButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        panel.add(copyButton, new GridBagConstraints(3, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));

        panel.add(Box.createRigidArea(new Dimension(60, 0)), new GridBagConstraints(4, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));

        //Add show console field
        JLabel showConsoleField = new JLabel(resources.getString("launcheroptions.general.console"));
        showConsoleField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        showConsoleField.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(showConsoleField, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 40, 0, 0), 0, 0));

        showConsole = new JCheckBox("", false);
        showConsole.setOpaque(false);
        showConsole.setHorizontalAlignment(SwingConstants.RIGHT);
        showConsole.setBorder(BorderFactory.createEmptyBorder());
        showConsole.setIconTextGap(0);
        showConsole.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        showConsole.setIcon(resources.getIcon("checkbox_open.png"));
        showConsole.setFocusPainted(false);

        panel.add(showConsole, new GridBagConstraints(1, 5, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(16, 16, 0, 0), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 6, 5, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));

        //Open logs button
        RoundedButton openLogs = new RoundedButton(resources.getString("launcheroptions.general.logs"));
        openLogs.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        openLogs.setContentAreaFilled(false);
        openLogs.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        openLogs.setHoverForeground(LauncherFrame.COLOR_BLUE);
        openLogs.setBorder(BorderFactory.createEmptyBorder(8, 13, 8, 13));
        panel.add(openLogs, new GridBagConstraints(0, 7, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
    }

    private void setupJavaOptionsPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel memLabel = new JLabel(resources.getString("launcheroptions.java.memory"));
        memLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        memLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(memLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        memSelect = new JComboBox();

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

        JLabel argsLabel = new JLabel(resources.getString("launcheroptions.java.arguments"));
        argsLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        argsLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(argsLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        javaArgs = new JTextArea(32, 4);
        javaArgs.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        javaArgs.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        javaArgs.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        javaArgs.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        javaArgs.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        javaArgs.setMargin(new Insets(16, 4, 16, 4));
        javaArgs.setLineWrap(true);
        javaArgs.setWrapStyleWord(true);
        javaArgs.setSelectionColor(LauncherFrame.COLOR_BUTTON_BLUE);
        javaArgs.setSelectedTextColor(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);

        panel.add(javaArgs, new GridBagConstraints(1, 1, 1, 2, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 64, 80), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 2, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }
}
