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

package net.technicpack.launcher.ui;

import net.technicpack.autoupdate.Relauncher;
import net.technicpack.autoupdate.tasks.MoveLauncherPackage;
import net.technicpack.launcher.LauncherMain;
import net.technicpack.launcher.autoupdate.TechnicRelauncher;
import net.technicpack.launcher.io.TechnicLauncherDirectories;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.ui.controls.list.popupformatters.RoundedBorderFormatter;
import net.technicpack.ui.controls.DraggableFrame;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.borders.RoundBorder;
import net.technicpack.ui.controls.lang.LanguageCellRenderer;
import net.technicpack.ui.controls.lang.LanguageCellUI;
import net.technicpack.ui.controls.tabs.SimpleTabPane;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.listitems.LanguageItem;
import net.technicpack.utilslib.OperatingSystem;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.logging.Level;

public class InstallerFrame extends DraggableFrame implements IRelocalizableResource {

    private static final int DIALOG_WIDTH = 610;
    private static final int DIALOG_HEIGHT = 320;

    private ResourceLoader resources;

    private Window mainFrame;

    private JCheckBox standardDefaultDirectory;
    private JTextField standardInstallDir;
    private RoundedButton standardSelectButton;
    private JTextField portableInstallDir;
    private RoundedButton portableInstallButton;
    private StartupParameters params;

    private JComboBox standardLanguages;
    private JComboBox portableLanguages;

    private TechnicSettings settings;

    private JPanel glassPane;

    public InstallerFrame(ResourceLoader resources, StartupParameters params) {
        this.resources = resources;
        this.params = params;
        this.settings = new TechnicSettings();
        this.settings.setFilePath(new File(OperatingSystem.getOperatingSystem().getUserDirectoryForApp("technic"), "settings.json"));
        this.settings.getTechnicRoot();

        addGlassPane();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        relocalize(resources);
    }

    public InstallerFrame(ResourceLoader resources, StartupParameters params, TechnicSettings settings, Window mainFrame) {
        this.settings = settings;
        this.resources = resources;
        this.params = params;
        this.mainFrame = mainFrame;

        mainFrame.setVisible(false);

        addGlassPane();

        relocalize(resources);
    }

    private void addGlassPane() {
        glassPane = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(LauncherFrame.COLOR_CENTRAL_BACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        glassPane.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                e.consume();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                e.consume();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                e.consume();
            }
        });
        glassPane.setOpaque(false);
        glassPane.setLayout(new GridBagLayout());

        JLabel spinner = new JLabel(resources.getIcon("loader.gif"));
        glassPane.add(spinner);
        setGlassPane(glassPane);
    }

    protected void standardLanguageChanged() {
        String langCode = ((LanguageItem)standardLanguages.getSelectedItem()).getLangCode();
        settings.setLanguageCode(langCode);
        resources.setLocale(langCode);
    }

    protected void portableLanguageChanged() {
        String langCode = ((LanguageItem)portableLanguages.getSelectedItem()).getLangCode();
        settings.setLanguageCode(langCode);
        resources.setLocale(langCode);
    }

    protected void standardInstall() {
        glassPane.setVisible(true);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File oldSettings = settings.getFilePath();
                File newSettings = new File(OperatingSystem.getOperatingSystem().getUserDirectoryForApp("technic"), "settings.json");

                if (oldSettings.exists() && !oldSettings.getAbsolutePath().equals(newSettings.getAbsolutePath())) {
                    oldSettings.delete();
                }

                File oldRoot = settings.getTechnicRoot();
                File newRoot = new File(standardInstallDir.getText());
                boolean rootHasChanged = false;

                if (oldRoot.exists() && !oldRoot.getAbsolutePath().equals(newRoot.getAbsolutePath())) {
                    rootHasChanged = true;
                    try {
                        if (!newRoot.exists())
                            newRoot.mkdirs();

                        FileUtils.copyDirectory(oldRoot, newRoot);
                        FileUtils.deleteDirectory(oldRoot);
                    } catch (IOException ex) {
                        Utils.getLogger().log(Level.SEVERE, "Copying install to new directory failed: ",ex);
                    }
                }

                settings.setFilePath(newSettings);

                if (settings.isPortable() || rootHasChanged || !standardInstallDir.getText().equals(OperatingSystem.getOperatingSystem().getUserDirectoryForApp("technic").getAbsolutePath()))
                    settings.installTo(standardInstallDir.getText());
                settings.getTechnicRoot();
                settings.setLanguageCode(((LanguageItem)standardLanguages.getSelectedItem()).getLangCode());
                settings.save();

                Relauncher relauncher = new TechnicRelauncher(null, settings.getBuildStream(), 0, new TechnicLauncherDirectories(settings.getTechnicRoot()), resources, params);
                try {
                    String currentPath = relauncher.getRunningPath();
                    relauncher.launch(currentPath, params.getArgs());
                    System.exit(0);
                    return;
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
        });
        thread.start();
    }

    protected void portableInstall() {
        String targetPath = null;
        final Relauncher relauncher = new TechnicRelauncher(null, settings.getBuildStream(), 0, new TechnicLauncherDirectories(settings.getTechnicRoot()), resources, params);
        try {
            String currentPath = relauncher.getRunningPath();
            String launcher = (currentPath.endsWith(".exe"))?"TechnicLauncher.exe":"TechnicLauncher.jar";

            targetPath = new File(portableInstallDir.getText(), launcher).getAbsolutePath();

            File targetExe = new File(portableInstallDir.getText(), launcher);

            if (!(new File(currentPath).equals(targetExe))) {
                if (targetExe.exists() && !targetExe.delete()) {
                    JOptionPane.showMessageDialog(this, resources.getString("installer.portable.replacefailed"), resources.getString("installer.portable.replacefailtitle"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                MoveLauncherPackage moveTask = new MoveLauncherPackage("", targetExe, relauncher);
                moveTask.runTask(null);
            }


        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return;
        }

        glassPane.setVisible(true);

        final String threadTargetPath = targetPath;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File oldRoot = settings.getTechnicRoot();
                File newRoot = new File(portableInstallDir.getText(), "technic");

                File oldSettingsFile = settings.getFilePath();
                File newSettingsFile = new File(newRoot, "settings.json");

                if (oldSettingsFile.exists() && !oldSettingsFile.getAbsolutePath().equals(newSettingsFile.getAbsolutePath()))
                    oldSettingsFile.delete();

                boolean rootHasChanged = false;

                if (oldRoot.exists() && !oldRoot.getAbsolutePath().equals(newRoot.getAbsolutePath())) {
                    rootHasChanged = true;
                    try {
                        if (!newRoot.exists())
                            newRoot.mkdirs();

                        FileUtils.copyDirectory(oldRoot, newRoot);
                        FileUtils.deleteDirectory(oldRoot);
                    } catch (IOException ex) {
                        Utils.getLogger().log(Level.SEVERE, "Copying install to new directory failed: ",ex);
                    }
                }

                settings.setPortable();
                settings.setFilePath(newSettingsFile);
                settings.getTechnicRoot();
                settings.setLanguageCode(((LanguageItem)portableLanguages.getSelectedItem()).getLangCode());
                settings.save();

                relauncher.launch(threadTargetPath, params.getArgs());
                System.exit(0);
            }
        });
        thread.start();
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
            standardInstallDir.setText(OperatingSystem.getOperatingSystem().getUserDirectoryForApp("technic").getAbsolutePath());
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

        JLabel title = new JLabel(resources.getString("launcher.installer.title"));
        title.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 26));
        title.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
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
                if (mainFrame != null)
                    mainFrame.setVisible(true);
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

        if (settings.isPortable()) {
            centerPanel.setSelectedIndex(1);
        } else
            centerPanel.setSelectedIndex(0);

        setLocationRelativeTo(null);
    }

    private void setupStandardInstall(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel standardSpiel = new JLabel("<html><body align=\"left\" style='margin-right:10px;'>"+resources.getString("launcher.installer.standardspiel")+"</body></html>");
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
        standardDefaultDirectory.setSelected(settings.isPortable() || settings.getTechnicRoot().getAbsolutePath().equals(OperatingSystem.getOperatingSystem().getUserDirectoryForApp("technic").getAbsolutePath()));
        standardDefaultDirectory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useDefaultDirectoryChanged();
            }
        });
        panel.add(standardDefaultDirectory, new GridBagConstraints(0, 2, 3, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,24,12,0),0,0));

        JLabel installFolderLabel = new JLabel(resources.getString("launcher.installer.folder"));
        installFolderLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        installFolderLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(installFolderLabel, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,24,0,8), 0,0));

        String installDir = OperatingSystem.getOperatingSystem().getUserDirectoryForApp("technic").getAbsolutePath();

        if (!settings.isPortable())
            installDir = settings.getTechnicRoot().getAbsolutePath();

        standardInstallDir = new JTextField(installDir);
        standardInstallDir.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        standardInstallDir.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        standardInstallDir.setHighlighter(null);
        standardInstallDir.setEditable(false);
        standardInstallDir.setCursor(null);
        panel.add(standardInstallDir, new GridBagConstraints(1, 3, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,5),0,16));

        standardSelectButton = new RoundedButton(resources.getString("launcher.installer.select"));
        standardSelectButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        standardSelectButton.setContentAreaFilled(false);
        standardSelectButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        standardSelectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectStandard();
            }
        });
        panel.add(standardSelectButton, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,16), 0,0));

        useDefaultDirectoryChanged();

        panel.add(Box.createGlue(), new GridBagConstraints(0, 4, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        String defaultLocaleText = resources.getString("launcheroptions.language.default");
        if (!resources.isDefaultLocaleSupported()) {
            defaultLocaleText = defaultLocaleText.concat(" (" + resources.getString("launcheroptions.language.unavailable") + ")");
        }

        standardLanguages = new JComboBox();
        standardLanguages.addItem(new LanguageItem(ResourceLoader.DEFAULT_LOCALE, defaultLocaleText, resources));
        for (int i = 0; i < LauncherMain.supportedLanguages.length; i++) {
            standardLanguages.addItem(new LanguageItem(resources.getCodeFromLocale(LauncherMain.supportedLanguages[i]), LauncherMain.supportedLanguages[i].getDisplayName(LauncherMain.supportedLanguages[i]), resources.getVariant(LauncherMain.supportedLanguages[i])));
        }
        if (!settings.getLanguageCode().equalsIgnoreCase(ResourceLoader.DEFAULT_LOCALE)) {
            Locale loc = resources.getLocaleFromCode(settings.getLanguageCode());

            for (int i = 0; i < LauncherMain.supportedLanguages.length; i++) {
                if (loc.equals(LauncherMain.supportedLanguages[i])) {
                    standardLanguages.setSelectedIndex(i+1);
                    break;
                }
            }
        }
        standardLanguages.setBorder(new RoundBorder(LauncherFrame.COLOR_SCROLL_THUMB, 1, 10));
        standardLanguages.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        standardLanguages.setUI(new LanguageCellUI(resources, new RoundedBorderFormatter(new LineBorder(Color.black, 1)), LauncherFrame.COLOR_SCROLL_TRACK, LauncherFrame.COLOR_SCROLL_THUMB));
        standardLanguages.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        standardLanguages.setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        standardLanguages.setRenderer(new LanguageCellRenderer(resources, "globe.png", LauncherFrame.COLOR_SELECTOR_BACK, LauncherFrame.COLOR_WHITE_TEXT));
        standardLanguages.setEditable(false);
        standardLanguages.setFocusable(false);
        standardLanguages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                standardLanguageChanged();
            }
        });
        panel.add(standardLanguages, new GridBagConstraints(0, 5, 1, 0, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0,8,8,0), 0,0));

        RoundedButton install = new RoundedButton(resources.getString("launcher.installer.install"));
        install.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        install.setContentAreaFilled(false);
        install.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        install.setHoverForeground(LauncherFrame.COLOR_BLUE);
        install.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        install.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                standardInstall();
            }
        });
        panel.add(install, new GridBagConstraints(1, 5, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0, 0, 8, 8), 0, 0));

    }

    private void setupPortableMode(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel portableSpiel = new JLabel("<html><body align=\"left\" style='margin-right:10px;'>"+resources.getString("launcher.installer.portablespiel")+"</body></html>");
        portableSpiel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        portableSpiel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        portableSpiel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        panel.add(portableSpiel, new GridBagConstraints(0, 0, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 8, 9, 3), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 1, 3, 1, 1.0, 0.7, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        JLabel installFolderLabel = new JLabel(resources.getString("launcher.installer.folder"));
        installFolderLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        installFolderLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        panel.add(installFolderLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,24,0,8), 0,0));

        String installDir = "";
        if (settings.isPortable())
            installDir = settings.getTechnicRoot().getAbsolutePath();

        portableInstallDir = new JTextField(installDir);
        portableInstallDir.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        portableInstallDir.setForeground(LauncherFrame.COLOR_BLUE);
        portableInstallDir.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        portableInstallDir.setHighlighter(null);
        portableInstallDir.setEditable(false);
        portableInstallDir.setCursor(null);
        portableInstallDir.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(portableInstallDir, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,5,0,5),0,16));

        RoundedButton selectInstall = new RoundedButton(resources.getString("launcher.installer.select"));
        selectInstall.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
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

        String defaultLocaleText = resources.getString("launcheroptions.language.default");
        if (!resources.isDefaultLocaleSupported()) {
            defaultLocaleText = defaultLocaleText.concat(" (" + resources.getString("launcheroptions.language.unavailable") + ")");
        }

        portableLanguages = new JComboBox();
        portableLanguages.addItem(new LanguageItem(ResourceLoader.DEFAULT_LOCALE, defaultLocaleText, resources));
        for (int i = 0; i < LauncherMain.supportedLanguages.length; i++) {
            portableLanguages.addItem(new LanguageItem(resources.getCodeFromLocale(LauncherMain.supportedLanguages[i]), LauncherMain.supportedLanguages[i].getDisplayName(LauncherMain.supportedLanguages[i]), resources.getVariant(LauncherMain.supportedLanguages[i])));
        }
        if (!settings.getLanguageCode().equalsIgnoreCase(ResourceLoader.DEFAULT_LOCALE)) {
            Locale loc = resources.getLocaleFromCode(settings.getLanguageCode());

            for (int i = 0; i < LauncherMain.supportedLanguages.length; i++) {
                if (loc.equals(LauncherMain.supportedLanguages[i])) {
                    portableLanguages.setSelectedIndex(i+1);
                    break;
                }
            }
        }
        portableLanguages.setBorder(new RoundBorder(LauncherFrame.COLOR_SCROLL_THUMB, 1, 10));
        portableLanguages.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        portableLanguages.setUI(new LanguageCellUI(resources, new RoundedBorderFormatter(new LineBorder(Color.black, 1)), LauncherFrame.COLOR_SCROLL_TRACK, LauncherFrame.COLOR_SCROLL_THUMB));
        portableLanguages.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        portableLanguages.setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        portableLanguages.setRenderer(new LanguageCellRenderer(resources, "globe.png", LauncherFrame.COLOR_SELECTOR_BACK, LauncherFrame.COLOR_WHITE_TEXT));
        portableLanguages.setEditable(false);
        portableLanguages.setFocusable(false);
        portableLanguages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                portableLanguageChanged();
            }
        });
        panel.add(portableLanguages, new GridBagConstraints(0, 4, 1, 0, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(0,8,8,0), 0,0));

        portableInstallButton = new RoundedButton(resources.getString("launcher.installer.install"));
        portableInstallButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        portableInstallButton.setContentAreaFilled(false);
        portableInstallButton.setForeground(LauncherFrame.COLOR_GREY_TEXT);
        portableInstallButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        portableInstallButton.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        portableInstallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                portableInstall();
            }
        });
        portableInstallButton.setEnabled(false);

        if (!installDir.equals("")) {
            portableInstallButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
            portableInstallButton.setEnabled(true);
        }

        panel.add(portableInstallButton, new GridBagConstraints(1, 4, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0,0,8,8), 0,0));
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
        invalidate();
    }
}
