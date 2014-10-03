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

package net.technicpack.launcher.ui;

import net.technicpack.autoupdate.Relauncher;
import net.technicpack.launcher.LauncherMain;
import net.technicpack.launcher.settings.SettingsFactory;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.ui.controls.AAJLabel;
import net.technicpack.ui.controls.DraggableFrame;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.borders.RoundBorder;
import net.technicpack.ui.controls.tabs.SimpleTabPane;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

public class InstallerFrame extends DraggableFrame implements IRelocalizableResource {

    private static final int DIALOG_WIDTH = 610;
    private static final int DIALOG_HEIGHT = 320;

    private ResourceLoader resources;

    private JCheckBox standardDefaultDirectory;
    private JTextField standardInstallDir;
    private RoundedButton standardSelectButton;
    private JTextField portableInstallDir;
    private RoundedButton portableInstallButton;
    private StartupParameters params;

    public InstallerFrame(ResourceLoader resources, StartupParameters params) {
        this.resources = resources;
        this.params = params;

        initComponents();
    }

    protected void standardInstall() {
        TechnicSettings settings = new TechnicSettings();
        settings.setFilePath(new File(standardInstallDir.getText(), "settings.json"));
        settings.getTechnicRoot();
        settings.save();

        Relauncher relauncher = new Relauncher(null);
        try {
            String currentPath = relauncher.getRunningPath(LauncherMain.class);
            relauncher.launch(currentPath, LauncherMain.class, params.getParameters().toArray(new String[params.getParameters().size()]));
            System.exit(0);
            return;
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        }
    }

    protected void portableInstall() {
        TechnicSettings settings = new TechnicSettings();
        settings.setFilePath(new File(new File(portableInstallDir.getText(), "technic"), "settings.json"));
        settings.getTechnicRoot();
        settings.save();

        Relauncher relauncher = new Relauncher(null);
        try {
            String currentPath = relauncher.getRunningPath(LauncherMain.class);
            String launcher = (currentPath.endsWith(".exe"))?"TechnicLauncher.exe":"TechnicLauncher.jar";

            String targetPath = new File(portableInstallDir.getText(), launcher).getAbsolutePath();
            relauncher.replacePackage(LauncherMain.class, new File(portableInstallDir.getText(), launcher).getAbsolutePath());
            relauncher.launch(targetPath, LauncherMain.class, params.getParameters().toArray(new String[params.getParameters().size()]));
            System.exit(0);
            return;
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        }
    }

    protected void selectPortable() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            portableInstallDir.setText(chooser.getSelectedFile().getAbsolutePath());
            portableInstallButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
            portableInstallButton.setEnabled(true);
        }
    }

    protected void selectStandard() {
        File installDir = new File(standardInstallDir.getText());

        while (!installDir.exists()) {
            installDir = installDir.getParentFile();
        }

        JFileChooser chooser = new JFileChooser(installDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().listFiles().length > 0) {
                JOptionPane.showMessageDialog(this, resources.getString("modpackoptions.move.errortext"), resources.getString("modpackoptions.move.errortitle"), JOptionPane.OK_OPTION);
                return;
            }

            standardInstallDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    protected void useDefaultDirectoryChanged() {
        if (!standardDefaultDirectory.isSelected()) {
            standardInstallDir.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
            standardInstallDir.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
            standardSelectButton.setEnabled(true);
            standardSelectButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        } else {
            standardInstallDir.setForeground(LauncherFrame.COLOR_SCROLL_THUMB);
            standardInstallDir.setBorder(new RoundBorder(LauncherFrame.COLOR_SCROLL_THUMB, 1, 10));
            standardSelectButton.setEnabled(false);
            standardSelectButton.setForeground(LauncherFrame.COLOR_GREY_TEXT);
            standardInstallDir.setText(SettingsFactory.getTechnicHomeDir().getAbsolutePath());
        }
    }

    private void initComponents() {
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setIconImage(resources.getImage("icon.png"));
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(Color.black);
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        add(header, BorderLayout.PAGE_START);

        AAJLabel title = new AAJLabel(resources.getString("launcher.installer.title"));
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
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
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

        JPanel standardInstallPanel = new JPanel();
        standardInstallPanel.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        setupStandardInstall(standardInstallPanel);

        JPanel portableModePanel = new JPanel();
        portableModePanel.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        setupPortableMode(portableModePanel);

        centerPanel.addTab(resources.getString("launcher.installer.standard").toUpperCase(), standardInstallPanel);
        centerPanel.addTab(resources.getString("launcher.installer.portable").toUpperCase(), portableModePanel);

        setLocationRelativeTo(null);
    }

    private void setupStandardInstall(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel standardSpiel = new AAJLabel("<html><body align=\"left\" style='margin-right:10px;'>"+resources.getString("launcher.installer.standardspiel")+"</body></html>");
        standardSpiel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        standardSpiel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        standardSpiel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        panel.add(standardSpiel, new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 0, 0, 3), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 1, 3, 1, 1.0, 0.7, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        standardDefaultDirectory = new JCheckBox(resources.getString("launcher.installer.default"));
        standardDefaultDirectory.setOpaque(false);
        standardDefaultDirectory.setHorizontalAlignment(SwingConstants.RIGHT);
        standardDefaultDirectory.setBorder(BorderFactory.createEmptyBorder());
        standardDefaultDirectory.setIconTextGap(0);
        standardDefaultDirectory.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        standardDefaultDirectory.setIcon(resources.getIcon("checkbox_open.png"));
        standardDefaultDirectory.setFocusPainted(false);
        standardDefaultDirectory.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        standardDefaultDirectory.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        standardDefaultDirectory.setIconTextGap(6);
        standardDefaultDirectory.setSelected(true);
        standardDefaultDirectory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useDefaultDirectoryChanged();
            }
        });
        panel.add(standardDefaultDirectory, new GridBagConstraints(0, 2, 3, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,24,12,0),0,0));

        JLabel installFolderLabel = new AAJLabel(resources.getString("launcher.installer.folder"));
        installFolderLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        installFolderLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(installFolderLabel, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,24,0,8), 0,0));

        standardInstallDir = new JTextField(SettingsFactory.getTechnicHomeDir().getAbsolutePath());
        standardInstallDir.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        standardInstallDir.setForeground(LauncherFrame.COLOR_SCROLL_THUMB);
        standardInstallDir.setBorder(new RoundBorder(LauncherFrame.COLOR_SCROLL_THUMB, 1, 10));
        standardInstallDir.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        standardInstallDir.setHighlighter(null);
        standardInstallDir.setEditable(false);
        standardInstallDir.setCursor(null);
        panel.add(standardInstallDir, new GridBagConstraints(1, 3, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,5),0,0));

        standardSelectButton = new RoundedButton(resources.getString("launcher.installer.select"));
        standardSelectButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        standardSelectButton.setContentAreaFilled(false);
        standardSelectButton.setForeground(LauncherFrame.COLOR_GREY_TEXT);
        standardSelectButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        standardSelectButton.setEnabled(false);
        standardSelectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectStandard();
            }
        });
        panel.add(standardSelectButton, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,16), 0,0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 4, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        RoundedButton install = new RoundedButton(resources.getString("launcher.installer.install"));
        install.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        install.setContentAreaFilled(false);
        install.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        install.setHoverForeground(LauncherFrame.COLOR_BLUE);
        install.setBorder(BorderFactory.createEmptyBorder(8, 56, 8, 56));
        install.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                standardInstall();
            }
        });
        panel.add(install, new GridBagConstraints(0, 5, 3, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0, 0, 8, 8), 0, 0));

    }

    private void setupPortableMode(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel portableSpiel = new AAJLabel("<html><body align=\"left\" style='margin-right:10px;'>"+resources.getString("launcher.installer.portablespiel")+"</body></html>");
        portableSpiel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        portableSpiel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        portableSpiel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        panel.add(portableSpiel, new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 8, 9, 3), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 1, 3, 1, 1.0, 0.7, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        JLabel installFolderLabel = new AAJLabel(resources.getString("launcher.installer.folder"));
        installFolderLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        installFolderLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(installFolderLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,24,0,8), 0,0));

        portableInstallDir = new JTextField("");
        portableInstallDir.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        portableInstallDir.setForeground(LauncherFrame.COLOR_BLUE);
        portableInstallDir.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        portableInstallDir.setHighlighter(null);
        portableInstallDir.setEditable(false);
        portableInstallDir.setCursor(null);
        portableInstallDir.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(portableInstallDir, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,5),0,0));

        RoundedButton selectInstall = new RoundedButton(resources.getString("launcher.installer.select"));
        selectInstall.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        selectInstall.setContentAreaFilled(false);
        selectInstall.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        selectInstall.setHoverForeground(LauncherFrame.COLOR_BLUE);
        selectInstall.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectPortable();
            }
        });
        panel.add(selectInstall, new GridBagConstraints(2, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,16), 0,0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 3, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        portableInstallButton = new RoundedButton(resources.getString("launcher.installer.install"));
        portableInstallButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        portableInstallButton.setContentAreaFilled(false);
        portableInstallButton.setForeground(LauncherFrame.COLOR_GREY_TEXT);
        portableInstallButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        portableInstallButton.setBorder(BorderFactory.createEmptyBorder(8, 56, 8, 56));
        portableInstallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                portableInstall();
            }
        });
        portableInstallButton.setEnabled(false);
        panel.add(portableInstallButton, new GridBagConstraints(0, 4, 3, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0,0,8,8), 0,0));
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        setIconImage(this.resources.getImage("icon.png"));

        //Wipe controls
        this.getContentPane().removeAll();
        this.setLayout(null);

        //Clear references to existing controls

        initComponents();
    }
}
