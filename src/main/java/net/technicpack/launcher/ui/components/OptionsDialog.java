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

package net.technicpack.launcher.ui.components;

import net.technicpack.autoupdate.IBuildNumber;
import net.technicpack.launcher.LauncherMain;
import net.technicpack.launcher.settings.StartupParameters;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launcher.ui.InstallerFrame;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.launcher.ui.listitems.OnLaunchItem;
import net.technicpack.launcher.ui.listitems.StreamItem;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.source.FileJavaSource;
import net.technicpack.launchercore.launch.java.version.FileBasedJavaRuntime;
import net.technicpack.launchercore.util.LaunchAction;
import net.technicpack.minecraftcore.launch.WindowType;
import net.technicpack.ui.UIUtils;
import net.technicpack.ui.controls.LauncherDialog;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.TooltipWarning;
import net.technicpack.ui.controls.borders.RoundBorder;
import net.technicpack.ui.controls.lang.LanguageCellRenderer;
import net.technicpack.ui.controls.list.SimpleButtonComboUI;
import net.technicpack.ui.controls.list.popupformatters.RoundedBorderFormatter;
import net.technicpack.ui.controls.tabs.SimpleTabPane;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.listitems.LanguageItem;
import net.technicpack.ui.listitems.javaversion.Best64BitVersionItem;
import net.technicpack.ui.listitems.javaversion.DefaultVersionItem;
import net.technicpack.ui.listitems.javaversion.JavaVersionItem;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Memory;
import net.technicpack.utilslib.OperatingSystem;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Locale;

public class OptionsDialog extends LauncherDialog implements IRelocalizableResource {

    private static final int DIALOG_WIDTH = 830;
    private static final int DIALOG_HEIGHT = 564;

    private final TechnicSettings settings;

    private boolean hasShownStreamInfo = false;
    private ResourceLoader resources;
    private final JavaVersionRepository javaVersions;
    private final FileJavaSource fileJavaSource;
    private final IBuildNumber buildNumber;

    private final DocumentListener javaArgsListener = new DocumentListener() {
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

    private final DocumentListener dimensionListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            changeWindowDimensions();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changeWindowDimensions();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changeWindowDimensions();
        }
    };

    private final DocumentListener wrapperCommandListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            changeWrapperCommand();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changeWrapperCommand();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changeWrapperCommand();
        }
    };


    JComboBox<JavaVersionItem> versionSelect;
    JComboBox<Memory> memSelect;
    JTextArea javaArgs;
    JComboBox<StreamItem> streamSelect;
    JComboBox<OnLaunchItem> launchSelect;
    JComboBox<LanguageItem> langSelect;
    JTextField installField;
    JTextField clientId;
    JCheckBox showConsole;
    JCheckBox launchToModpacks;
    StartupParameters params;
    Component ramWarning;
    JCheckBox askFirstBox;
    JComboBox<String> useStencil;
    JComboBox<String> windowSelect;
    JTextField widthInput;
    JTextField heightInput;
    JTextField wrapperCommand;
    JCheckBox useMojangJava;

    public OptionsDialog(final Frame owner, final TechnicSettings settings, final ResourceLoader resourceLoader, final StartupParameters params, final JavaVersionRepository javaVersions, final FileJavaSource fileJavaSource, final IBuildNumber buildNumber) {
        super(owner);

        this.settings = settings;
        this.params = params;
        this.javaVersions = javaVersions;
        this.fileJavaSource = fileJavaSource;
        this.buildNumber = buildNumber;

        relocalize(resourceLoader);
    }

    protected void closeDialog() {
        resources.unregisterResource(this);
        dispose();
    }

    protected void changeJavaArgs() {
        settings.setJavaArgs(javaArgs.getText().trim());
        settings.save();
    }

    protected void changeWrapperCommand() {
        settings.setWrapperCommand(wrapperCommand.getText().trim());
        settings.save();
    }

    protected void copyCid() {
        StringSelection selection = new StringSelection(clientId.getText());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    protected void changeShowConsole() {
        settings.setShowConsole(showConsole.isSelected());
        LauncherMain.setConsoleVisible(showConsole.isSelected());
        settings.save();
    }

    protected void changeAskFirst() {
        settings.setAutoAcceptModpackRequirements(!askFirstBox.isSelected());
        settings.save();
    }

    protected void changeUseMojangJava() {
        if (!useMojangJava.isSelected()) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    resources.getString("launcheroptions.java.mojangJreWarning.text"),
                    resources.getString("launcheroptions.java.mojangJreWarning.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result != JOptionPane.YES_OPTION) {
                // Reset to current value
                useMojangJava.setSelected(settings.shouldUseMojangJava());
                return;
            }
        }

        settings.setUseMojangJava(useMojangJava.isSelected());
        settings.save();
    }

    protected void changeLaunchToModpacks() {
        settings.setLaunchToModpacks(launchToModpacks.isSelected());
        settings.save();
    }

    protected void changeJavaVersion() {
        JavaVersionItem javaVersionItem = (JavaVersionItem)versionSelect.getSelectedItem();
        String version = javaVersionItem.getVersionNumber();
        boolean is64 = javaVersionItem.is64Bit();
        javaVersions.setSelectedVersion(javaVersionItem.getJavaVersion());
        settings.setJavaVersion(version);
        settings.setPrefer64Bit(is64);
        settings.save();
        rebuildMemoryList();
    }

    protected void selectOtherVersion() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        final String osJavaName = new File(OperatingSystem.getJavaDir()).getName();
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory())
                    return true;
                return (f.getName().equals(osJavaName));
            }

            @Override
            public String getDescription() {
                return resources.getString("launcheroptions.java.filter", osJavaName);
            }
        });

        int result = chooser.showOpenDialog(this);


        if (result == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile() == null || !chooser.getSelectedFile().exists() || !chooser.getSelectedFile().canExecute()) {
                JOptionPane.showMessageDialog(this, resources.getString("launcheroptions.java.badfile"));
                return;
            }

            FileBasedJavaRuntime chosenJava = new FileBasedJavaRuntime(chooser.getSelectedFile());
            if (!chosenJava.isValid()) {
                JOptionPane.showMessageDialog(this, resources.getString("launcheroptions.java.badfile"));
                return;
            }

            if (!javaVersions.addVersion(chosenJava)) {
                JOptionPane.showMessageDialog(this, resources.getString("launcheroptions.java.versionexists"));
                return;
            }

            fileJavaSource.addVersion(chosenJava);
            javaVersions.setSelectedVersion(chosenJava);
            JavaVersionItem item = new JavaVersionItem(chosenJava, resources);
            versionSelect.addItem(item);
            versionSelect.setSelectedItem(item);
            settings.setJavaVersion(chosenJava.getVersion());
            settings.setPrefer64Bit(chosenJava.is64Bit());
            settings.save();
        }
    }

    protected void changeMemory() {
        settings.setMemory(((Memory) memSelect.getSelectedItem()).getSettingsId());
        settings.save();
    }

    protected void changeStream() {
        settings.setBuildStream(((StreamItem) streamSelect.getSelectedItem()).getStream());
        settings.save();

        if (!hasShownStreamInfo) {
            JOptionPane.showMessageDialog(this, resources.getString("launcheroptions.streamchange.text"), resources.getString("launcheroptions.streamchange.title"), JOptionPane.INFORMATION_MESSAGE);

            hasShownStreamInfo = true;
        }
    }

    protected void changeLaunchAction() {
        settings.setLaunchAction(((OnLaunchItem) launchSelect.getSelectedItem()).getLaunchAction());
        settings.save();
    }

    protected void changeLanguage() {
        settings.setLanguageCode(((LanguageItem) langSelect.getSelectedItem()).getLangCode());

        resources.setLocale(((LanguageItem) langSelect.getSelectedItem()).getLangCode());
    }

    protected void changeWindowType() {
        switch(windowSelect.getSelectedIndex()) {
            case 0:
                settings.setLaunchWindowType(WindowType.DEFAULT);
                break;
            case 1:
                settings.setLaunchWindowType(WindowType.FULLSCREEN);
                break;
            case 2:
                settings.setLaunchWindowType(WindowType.CUSTOM);
                changeWindowDimensions();
                break;
        }

        updateDimensionsEnabled();
    }

    protected void changeEnableStencil() {
        settings.setUseStencilBuffer(useStencil.getSelectedIndex() == 0);
    }

    protected void changeWindowDimensions() {
        String widthStr = widthInput.getText();
        String heightStr = heightInput.getText();
        int width = 800;
        int height = 600;

        try {
            width = Integer.parseInt(widthStr);
        } catch (NumberFormatException e) {
            //Not important
        }

        try {
            height = Integer.parseInt(heightStr);
        } catch (NumberFormatException e) {
            //Not important
        }

        settings.setLaunchWindowDimensions(width, height);
    }

    protected void reinstall() {
        final InstallerFrame frame = new InstallerFrame(resources, params, settings, getOwner());
        frame.setVisible(true);

        SwingUtilities.invokeLater(frame::requestFocus);

        this.dispose();
    }

    protected void openLogs() {
        DesktopUtils.open(new File(settings.getTechnicRoot().getAbsolutePath(), "logs"));
    }

    private void initControlValues() {

        javaArgs.getDocument().removeDocumentListener(javaArgsListener);
        javaArgs.setText(settings.getJavaArgs());
        javaArgs.getDocument().addDocumentListener(javaArgsListener);

        wrapperCommand.getDocument().removeDocumentListener(wrapperCommandListener);
        wrapperCommand.setText(settings.getWrapperCommand());
        wrapperCommand.getDocument().addDocumentListener(wrapperCommandListener);

        installField.setText(settings.getTechnicRoot().getAbsolutePath());
        clientId.setText(settings.getClientId());

        for (ActionListener listener : showConsole.getActionListeners())
            showConsole.removeActionListener(listener);
        showConsole.setSelected(settings.getShowConsole());
        showConsole.addActionListener(e -> changeShowConsole());

        for (ActionListener listener : askFirstBox.getActionListeners())
            askFirstBox.removeActionListener(listener);
        askFirstBox.setSelected(!settings.shouldAutoAcceptModpackRequirements());
        askFirstBox.addActionListener(e -> changeAskFirst());

        for (ActionListener listener : useMojangJava.getActionListeners())
            useMojangJava.removeActionListener(listener);
        useMojangJava.setSelected(settings.shouldUseMojangJava());
        useMojangJava.addActionListener(e -> changeUseMojangJava());

        for (ActionListener listener : launchToModpacks.getActionListeners())
            launchToModpacks.removeActionListener(listener);
        launchToModpacks.setSelected(settings.getLaunchToModpacks());
        launchToModpacks.addActionListener(e -> changeLaunchToModpacks());

        for (ActionListener listener : versionSelect.getActionListeners())
            versionSelect.removeActionListener(listener);

        versionSelect.removeAllItems();
        versionSelect.addItem(new DefaultVersionItem(javaVersions.getDefaultVersion(), resources));

        IJavaRuntime best64Bit = javaVersions.getBest64BitVersion();
        if (best64Bit != null)
            versionSelect.addItem(new Best64BitVersionItem(best64Bit, resources));

        for (IJavaRuntime version : javaVersions.getVersions()) {
            versionSelect.addItem(new JavaVersionItem(version, resources));
        }

        String settingsVersion = settings.getJavaVersion();
        boolean settingsBitness = settings.getPrefer64Bit();
        if (settingsVersion == null || settingsVersion.isEmpty() || settingsVersion.equals(JavaVersionRepository.VERSION_DEFAULT))
            versionSelect.setSelectedIndex(0);
        else if (settingsVersion.equals(JavaVersionRepository.VERSION_LATEST_64BIT))
            versionSelect.setSelectedIndex(1);
        else {
            for (int i = 2; i < versionSelect.getItemCount(); i++) {
                if ((versionSelect.getItemAt(i)).getVersionNumber().equals(settingsVersion) && (versionSelect.getItemAt(i)).is64Bit() == settingsBitness) {
                    versionSelect.setSelectedIndex(i);
                    break;
                }
            }
        }

        versionSelect.addActionListener(e -> changeJavaVersion());

        rebuildMemoryList();

        for (ActionListener listener : streamSelect.getActionListeners()) {
            streamSelect.removeActionListener(listener);
        }
        streamSelect.removeAllItems();
        streamSelect.addItem(new StreamItem(resources.getString("launcheroptions.build.stable"), "stable"));
        streamSelect.addItem(new StreamItem(resources.getString("launcheroptions.build.beta"), "beta"));
        streamSelect.setSelectedIndex((settings.getBuildStream().equals("beta"))?1:0);
        streamSelect.addActionListener(e -> changeStream());

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
        }
        launchSelect.addActionListener(e -> changeLaunchAction());

        for (ActionListener listener : langSelect.getActionListeners())
            langSelect.removeActionListener(listener);
        langSelect.removeAllItems();

        String defaultLocaleText = resources.getString("launcheroptions.language.default");
        if (!resources.isDefaultLocaleSupported()) {
            defaultLocaleText = defaultLocaleText.concat(" (" + resources.getString("launcheroptions.language.unavailable") + ")");
        }

        langSelect.setRenderer(new LanguageCellRenderer(resources, null, langSelect.getBackground(), langSelect.getForeground()));
        UIUtils.populateLanguageSelector(defaultLocaleText, langSelect, resources, settings);
        langSelect.addActionListener(e -> changeLanguage());

        widthInput.getDocument().removeDocumentListener(dimensionListener);
        heightInput.getDocument().removeDocumentListener(dimensionListener);
        int width = settings.getCustomWidth();
        int height = settings.getCustomHeight();

        width = (width<1)?800:width;
        height = (height<1)?600:height;
        widthInput.setText(Integer.toString(width));
        heightInput.setText(Integer.toString(height));
        widthInput.getDocument().addDocumentListener(dimensionListener);
        heightInput.getDocument().addDocumentListener(dimensionListener);

        for (ActionListener listener : windowSelect.getActionListeners()) {
            windowSelect.removeActionListener(listener);
        }
        windowSelect.removeAllItems();
        windowSelect.addItem(resources.getString("launcheroptions.video.windowSize.default"));
        windowSelect.addItem(resources.getString("launcheroptions.video.windowSize.fullscreen"));
        windowSelect.addItem(resources.getString("launcheroptions.video.windowSize.custom"));
        switch (settings.getLaunchWindowType()) {
            case DEFAULT:
                windowSelect.setSelectedIndex(0);
                break;
            case FULLSCREEN:
                windowSelect.setSelectedIndex(1);
                break;
            case CUSTOM:
                windowSelect.setSelectedIndex(2);
                break;
        }
        windowSelect.addActionListener(e -> changeWindowType());
        updateDimensionsEnabled();

        for (ActionListener listener : useStencil.getActionListeners()) {
            useStencil.removeActionListener(listener);
        }
        useStencil.removeAllItems();
        useStencil.addItem(resources.getString("launcheroptions.video.stencil.enabled"));
        useStencil.addItem(resources.getString("launcheroptions.video.stencil.disabled"));
        if (settings.shouldUseStencilBuffer())
            useStencil.setSelectedIndex(0);
        else
            useStencil.setSelectedIndex(1);
        useStencil.addActionListener(e -> changeEnableStencil());
    }

    private void rebuildMemoryList() {
        for (ActionListener listener : memSelect.getActionListeners())
            memSelect.removeActionListener(listener);

        Container parent = null;
        if (memSelect.getParent() != null) {
            parent = memSelect.getParent();
            parent.remove(memSelect);

            if (ramWarning != null) {
                parent.remove(ramWarning);
                ramWarning = null;
            }
        }

        memSelect.removeAllItems();
        boolean is64Bit = javaVersions.getSelectedVersion().is64Bit();
        long maxMemory = Memory.getAvailableMemory(is64Bit);
        for (int i = 0; i < Memory.memoryOptions.length; i++) {
            if (Memory.memoryOptions[i].getMemoryMB() <= maxMemory)
                memSelect.addItem(Memory.memoryOptions[i]);
        }

        Memory currentMem = Memory.getMemoryFromId(settings.getMemory());
        Memory availableMem = Memory.getClosestAvailableMemory(currentMem, is64Bit);

        if (currentMem.getMemoryMB() != availableMem.getMemoryMB()) {
            settings.setMemory(availableMem.getSettingsId());
            settings.save();
        }
        memSelect.setSelectedItem(availableMem);
        memSelect.addActionListener(e -> changeMemory());

        if (parent != null) {
            boolean has64Bit = javaVersions.getBest64BitVersion() != null;

            if (is64Bit) {
                parent.add(memSelect, new GridBagConstraints(1, 1, 6, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 80), 0, 16));
            } else {
                parent.add(memSelect, new GridBagConstraints(1, 1, 5, 1, 5, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 0), 0, 16));

                JToolTip toolTip = new JToolTip();
                toolTip.setBackground(UIConstants.COLOR_FOOTER);
                toolTip.setForeground(UIConstants.COLOR_GREY_TEXT);
                toolTip.setBorder(BorderFactory.createCompoundBorder(new LineBorder(UIConstants.COLOR_GREY_TEXT), BorderFactory.createEmptyBorder(5,5,5,5)));
                toolTip.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));


                String text;
                Icon icon;

                if (has64Bit) {
                    text = resources.getString("launcheroptions.java.use64bit");
                    icon = resources.getIcon("danger_icon.png");
                } else {
                    text = resources.getString("launcheroptions.java.get64bit");
                    icon = resources.getIcon("warning_icon.png");
                }

                ramWarning = new TooltipWarning(icon, toolTip);
                ((TooltipWarning)ramWarning).setToolTipText(text);
                parent.add(ramWarning, new GridBagConstraints(6, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(8,8,8,80),0,0));
            }
            repaint();
        }
    }

    private void initComponents() {
        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.setBackground(Color.black);
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(header, BorderLayout.PAGE_START);

        JLabel title = new JLabel(resources.getString("launcher.title.options"));
        title.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 26));
        title.setForeground(UIConstants.COLOR_WHITE_TEXT);
        title.setOpaque(false);
        title.setIcon(resources.getIcon("options_cog.png"));
        title.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        header.add(title);

        header.add(Box.createHorizontalGlue());

        JButton closeButton = new JButton();
        closeButton.setIcon(resources.getIcon("close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> closeDialog());
        header.add(closeButton);

        SimpleTabPane centerPanel = new SimpleTabPane();
        centerPanel.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        centerPanel.setForeground(UIConstants.COLOR_GREY_TEXT);
        centerPanel.setSelectedBackground(UIConstants.COLOR_BLUE);
        centerPanel.setSelectedForeground(UIConstants.COLOR_WHITE_TEXT);
        centerPanel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        centerPanel.setOpaque(true);
        add(centerPanel, BorderLayout.CENTER);

        JPanel general = new JPanel();
        general.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        setupGeneralPanel(general);

        JPanel javaOptions = new JPanel();
        javaOptions.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        setupJavaOptionsPanel(javaOptions);

        JPanel videoOptions = new JPanel();
        videoOptions.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        setupVideoOptionsPanel(videoOptions);

        JPanel about = new JPanel();
        about.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        String linkText = "<a href=\"https://github.com/TechnicPack/\">"+resources.getString("launcheroptions.about.linktext")+"</a>";
        String aboutText = "<html><head><style type=\"text/css\">a{color:#309aeb}body{font-family: "+resources.getFont(ResourceLoader.FONT_OPENSANS, 12).getFamily()+";color:#D0D0D0}</style></head><body>";
        aboutText += "<p>" + resources.getString("launcheroptions.about.copyright", buildNumber.getBuildNumber(), linkText) + "</p>";
        aboutText += "<p>" + resources.getString("launcheroptions.about.romainguy") + "</p>";
        aboutText += "<p>" + resources.getString("launcheroptions.about.summary") + "</p>";

        about.setLayout(new BorderLayout());

        JLabel buildCtrl = new JLabel(resources.getString("launcher.build.text", buildNumber.getBuildNumber(), resources.getString("launcher.build." + settings.getBuildStream())));
        buildCtrl.setForeground(UIConstants.COLOR_WHITE_TEXT);
        buildCtrl.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        buildCtrl.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 0));
        about.add(buildCtrl, BorderLayout.SOUTH);

        JTextPane textPane = new JTextPane();
        textPane.setBorder(BorderFactory.createEmptyBorder(0, 24, 9, 24));
        textPane.setOpaque(false);
        textPane.setForeground(UIConstants.COLOR_WHITE_TEXT);
        textPane.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        textPane.setEditable(false);
        textPane.setHighlighter(null);
        textPane.setAlignmentX(LEFT_ALIGNMENT);
        textPane.setContentType("text/html");
        textPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                DesktopUtils.browseUrl(e.getURL().toString());
        });
        MutableAttributeSet attributes = new SimpleAttributeSet(textPane.getParagraphAttributes());
        StyleConstants.setLineSpacing(attributes, StyleConstants.getLineSpacing(attributes) * 1.3f);
        textPane.setParagraphAttributes(attributes, true);

        textPane.setText(aboutText);
        about.add(textPane, BorderLayout.CENTER);

        centerPanel.addTab(resources.getString("launcheroptions.tab.general").toUpperCase(), general);
        centerPanel.addTab(resources.getString("launcheroptions.tab.java").toUpperCase(), javaOptions);
        centerPanel.addTab(resources.getString("launcheroptions.tab.video").toUpperCase(), videoOptions);
        centerPanel.addTab(resources.getString("launcheroptions.tab.about").toUpperCase(), about);
        centerPanel.setFocusable(false);
    }

    private void setupGeneralPanel(JPanel panel) {

        panel.setLayout(new GridBagLayout());

        JLabel streamLabel = new JLabel(resources.getString("launcheroptions.general.build"));
        streamLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        streamLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(streamLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        // Setup stream select box
        streamSelect = new JComboBox<>();
        streamSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        streamSelect.setEditable(false);
        streamSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        streamSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        streamSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        streamSelect.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        streamSelect.setFocusable(false);

        Object child = streamSelect.getAccessibleContext().getAccessibleChild(0);
        BasicComboPopup popup = (BasicComboPopup)child;
        JList list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        panel.add(streamSelect, new GridBagConstraints(1, 0, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        //Setup language box
        JLabel langLabel = new JLabel(resources.getString("launcheroptions.general.lang"));
        langLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        langLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(langLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        langSelect = new JComboBox<>();
        langSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        langSelect.setEditable(false);
        langSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        langSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        langSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        langSelect.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        langSelect.setFocusable(false);

        child = langSelect.getAccessibleContext().getAccessibleChild(0);
        popup = (BasicComboPopup)child;
        list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        panel.add(langSelect, new GridBagConstraints(1, 1, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        //Setup on pack launch box
        JLabel launchLabel = new JLabel(resources.getString("launcheroptions.general.onlaunch"));
        launchLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        launchLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(launchLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        launchSelect = new JComboBox<>();
        launchSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        launchSelect.setEditable(false);
        launchSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        launchSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        launchSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        launchSelect.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        launchSelect.setFocusable(false);

        child = launchSelect.getAccessibleContext().getAccessibleChild(0);
        popup = (BasicComboPopup)child;
        list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        panel.add(launchSelect, new GridBagConstraints(1, 2, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        //Install folder field
        JLabel installLabel = new JLabel(resources.getString("launcheroptions.general.install"));
        installLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        installLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(installLabel, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        installField = new JTextField("");
        installField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        installField.setForeground(UIConstants.COLOR_BLUE);
        installField.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        installField.setHighlighter(null);
        installField.setEditable(false);
        installField.setCursor(null);
        installField.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(installField, new GridBagConstraints(1, 3, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        RoundedButton reinstallButton = new RoundedButton(resources.getString("launcheroptions.install.change"));
        reinstallButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        reinstallButton.setContentAreaFilled(false);
        reinstallButton.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        reinstallButton.setHoverForeground(UIConstants.COLOR_BLUE);
        reinstallButton.addActionListener(e -> reinstall());
        panel.add(reinstallButton, new GridBagConstraints(3, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));

        //Client ID field
        JLabel clientIdField = new JLabel(resources.getString("launcheroptions.general.id"));
        clientIdField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        clientIdField.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(clientIdField, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        clientId = new JTextField("abc123");
        clientId.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        clientId.setForeground(UIConstants.COLOR_BLUE);
        clientId.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        clientId.setHighlighter(null);
        clientId.setEditable(false);
        clientId.setCursor(null);
        clientId.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        panel.add(clientId, new GridBagConstraints(1, 4, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        RoundedButton copyButton = new RoundedButton(resources.getString("launcheroptions.id.copy"));
        copyButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        copyButton.setContentAreaFilled(false);
        copyButton.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        copyButton.setHoverForeground(UIConstants.COLOR_BLUE);
        copyButton.addActionListener(e -> copyCid());
        panel.add(copyButton, new GridBagConstraints(3, 4, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));

        panel.add(Box.createRigidArea(new Dimension(60, 0)), new GridBagConstraints(4, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));

        //Add show console field
        JLabel showConsoleField = new JLabel(resources.getString("launcheroptions.general.console"));
        showConsoleField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        showConsoleField.setForeground(UIConstants.COLOR_WHITE_TEXT);
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

        //Add launch to modpacks
        JLabel launchToModpacksField = new JLabel(resources.getString("launcheroptions.general.modpacktab"));
        launchToModpacksField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        launchToModpacksField.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(launchToModpacksField, new GridBagConstraints(0,6,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(10,40,0,0),0,0));

        launchToModpacks = new JCheckBox("", false);
        launchToModpacks.setOpaque(false);
        launchToModpacks.setHorizontalAlignment(SwingConstants.RIGHT);
        launchToModpacks.setBorder(BorderFactory.createEmptyBorder());
        launchToModpacks.setIconTextGap(0);
        launchToModpacks.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        launchToModpacks.setIcon(resources.getIcon("checkbox_open.png"));
        launchToModpacks.setFocusPainted(false);

        panel.add(launchToModpacks, new GridBagConstraints(1, 6, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(16, 16 ,0, 0), 0,0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 7, 5, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));

        //Open logs button
        RoundedButton openLogs = new RoundedButton(resources.getString("launcheroptions.general.logs"));
        openLogs.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        openLogs.setContentAreaFilled(false);
        openLogs.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        openLogs.setHoverForeground(UIConstants.COLOR_BLUE);
        openLogs.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        openLogs.addActionListener(e -> openLogs());
        panel.add(openLogs, new GridBagConstraints(0, 8, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 0), 0, 0));
    }

    private void updateDimensionsEnabled() {
        if (windowSelect.getSelectedIndex() == 2) {
            widthInput.setEnabled(true);
            heightInput.setEnabled(true);
            widthInput.setForeground(UIConstants.COLOR_BUTTON_BLUE);
            heightInput.setForeground(UIConstants.COLOR_BUTTON_BLUE);
            widthInput.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
            heightInput.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        } else {
            widthInput.setEnabled(false);
            heightInput.setEnabled(false);
            widthInput.setForeground(UIConstants.COLOR_GREY_TEXT);
            heightInput.setForeground(UIConstants.COLOR_GREY_TEXT);
            widthInput.setBorder(new RoundBorder(UIConstants.COLOR_GREY_TEXT, 1, 8));
            heightInput.setBorder(new RoundBorder(UIConstants.COLOR_GREY_TEXT, 1, 8));
        }
    }

    private void setupVideoOptionsPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel streamLabel = new JLabel(resources.getString("launcheroptions.video.windowSize"));
        streamLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        streamLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(streamLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 40, 0, 0), 0, 0));

        windowSelect = new JComboBox<>();

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            windowSelect.setUI(new MetalComboBoxUI());
        }

        windowSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        windowSelect.setEditable(false);
        windowSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        windowSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        windowSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        windowSelect.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        windowSelect.setFocusable(false);

        Object child = windowSelect.getAccessibleContext().getAccessibleChild(0);
        BasicComboPopup popup = (BasicComboPopup)child;
        JList list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        panel.add(windowSelect, new GridBagConstraints(1, 0, 1, 1, 0.5f, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        JLabel widthLabel = new JLabel(resources.getString("launcheroptions.video.windowSize.width"));
        widthLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        widthLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(widthLabel, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        widthInput = new JTextField(3);
        widthInput.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        widthInput.setForeground(UIConstants.COLOR_BLUE);
        widthInput.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        widthInput.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        widthInput.setCaretColor(UIConstants.COLOR_BLUE);
        widthInput.setText("800");
        panel.add(widthInput, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 6, 8, 16), 0, 0));

        JLabel heightLabel = new JLabel(resources.getString("launcheroptions.video.windowSize.height"));
        heightLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        heightLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(heightLabel, new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        heightInput = new JTextField(3);
        heightInput.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        heightInput.setForeground(UIConstants.COLOR_BLUE);
        heightInput.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        heightInput.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        heightInput.setCaretColor(UIConstants.COLOR_BLUE);
        heightInput.setText("600");
        panel.add(heightInput, new GridBagConstraints(5, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(8, 6, 8, 16), 0,0));

        //Add show console field
        JLabel useStencilField = new JLabel(resources.getString("launcheroptions.video.stencil"));
        useStencilField.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        useStencilField.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(useStencilField, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 40, 0, 0), 0, 0));

        useStencil = new JComboBox<>();

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            useStencil.setUI(new MetalComboBoxUI());
        }

        useStencil.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        useStencil.setEditable(false);
        useStencil.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        useStencil.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        useStencil.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        useStencil.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        useStencil.setFocusable(false);

        child = useStencil.getAccessibleContext().getAccessibleChild(0);
        popup = (BasicComboPopup)child;
        list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
        panel.add(useStencil, new GridBagConstraints(1, 1, 1, 1, 0.5f, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        JLabel stencilInfo = new JLabel("") {
            @Override
            public Dimension getMaximumSize() {
                return getMinimumSize();
            }

            @Override
            public Dimension getPreferredSize() {
                return getMinimumSize();
            }
        };
        stencilInfo.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
        stencilInfo.setForeground(UIConstants.COLOR_WHITE_TEXT);

        stencilInfo.setText("<html><body style=\"font-family:" + stencilInfo.getFont().getFamily() + ";color:#D0D0D0\">" + resources.getString("launcheroptions.video.stencil.info") + "</body></html>");

        panel.add(stencilInfo, new GridBagConstraints(2, 1, 4, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        panel.add(Box.createHorizontalStrut(60), new GridBagConstraints(7, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 30,0));

        panel.add(Box.createGlue(), new GridBagConstraints(0, 2, 8, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }

    private void setupJavaOptionsPanel(JPanel panel) {
        panel.setLayout(new GridBagLayout());

        JLabel versionLabel = new JLabel(resources.getString("launcheroptions.java.version"));
        versionLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        versionLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(versionLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        versionSelect = new JComboBox<>();

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            versionSelect.setUI(new MetalComboBoxUI());
        }

        versionSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        versionSelect.setEditable(false);
        versionSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        versionSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        versionSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        SimpleButtonComboUI ui = new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB);
        versionSelect.setUI(ui);
        versionSelect.setFocusable(false);

        Object child = versionSelect.getAccessibleContext().getAccessibleChild(0);
        BasicComboPopup popup = (BasicComboPopup)child;
        JList list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        panel.add(versionSelect, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 8), 0, 16));

        RoundedButton otherVersionButton = new RoundedButton(resources.getString("launcheroptions.java.otherversion"));
        otherVersionButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        otherVersionButton.setContentAreaFilled(false);
        otherVersionButton.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        otherVersionButton.setHoverForeground(UIConstants.COLOR_BLUE);
        otherVersionButton.addActionListener(e -> selectOtherVersion());
        panel.add(otherVersionButton, new GridBagConstraints(2, 0, 5, 1, 2, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 8, 8, 80), 0, 0));

        JLabel memLabel = new JLabel(resources.getString("launcheroptions.java.memory"));
        memLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        memLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(memLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        memSelect = new JComboBox<>();

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            memSelect.setUI(new MetalComboBoxUI());
        }

        memSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        memSelect.setEditable(false);
        memSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        memSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        memSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
         ui = new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB);
        memSelect.setUI(ui);
        memSelect.setFocusable(false);

        child = memSelect.getAccessibleContext().getAccessibleChild(0);
        popup = (BasicComboPopup)child;
        list = popup.getList();
        list.setSelectionForeground(UIConstants.COLOR_BUTTON_BLUE);
        list.setSelectionBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        list.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
        panel.add(memSelect, new GridBagConstraints(1, 1, 6, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 80), 0, 16));

        JLabel argsLabel = new JLabel(resources.getString("launcheroptions.java.arguments"));
        argsLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        argsLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(argsLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        javaArgs = new JTextArea(32, 4);
        javaArgs.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        javaArgs.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        javaArgs.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        javaArgs.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        javaArgs.setCaretColor(UIConstants.COLOR_BUTTON_BLUE);
        javaArgs.setMargin(new Insets(16, 4, 16, 4));
        javaArgs.setLineWrap(true);
        javaArgs.setWrapStyleWord(true);
        javaArgs.setSelectionColor(UIConstants.COLOR_BUTTON_BLUE);
        javaArgs.setSelectedTextColor(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);

        panel.add(javaArgs, new GridBagConstraints(1, 2, 6, 2, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 6, 80), 0, 0));

        JLabel wrapperCmdLabel = new JLabel(resources.getString("launcheroptions.java.wrapper"));
        wrapperCmdLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        wrapperCmdLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(wrapperCmdLabel, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 60, 0, 0), 0, 0));

        wrapperCommand = new JTextField("");
        wrapperCommand.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        wrapperCommand.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        wrapperCommand.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        wrapperCommand.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 8));
        wrapperCommand.setCaretColor(UIConstants.COLOR_BUTTON_BLUE);
        wrapperCommand.setSelectionColor(UIConstants.COLOR_BUTTON_BLUE);
        wrapperCommand.setSelectedTextColor(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        panel.add(wrapperCommand, new GridBagConstraints(1, 4, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 16, 8, 16), 0, 16));

        JLabel autoApprovalLabel = new JLabel(resources.getString("launcheroptions.java.autoApprove"));
        autoApprovalLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        autoApprovalLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(autoApprovalLabel, new GridBagConstraints(0, 5, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));

        askFirstBox = new JCheckBox("", false);
        askFirstBox.setOpaque(false);
        askFirstBox.setHorizontalAlignment(SwingConstants.RIGHT);
        askFirstBox.setBorder(BorderFactory.createEmptyBorder());
        askFirstBox.setIconTextGap(0);
        askFirstBox.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        askFirstBox.setIcon(resources.getIcon("checkbox_open.png"));
        askFirstBox.setFocusPainted(false);
        panel.add(askFirstBox, new GridBagConstraints(1, 5, 6, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 16, 8, 8), 0, 0));

        JLabel useMojangJavaLabel = new JLabel(resources.getString("launcheroptions.java.useMojangJava"));
        useMojangJavaLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        useMojangJavaLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        panel.add(useMojangJavaLabel, new GridBagConstraints(0, 6, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 20, 0, 0), 0, 0));

        useMojangJava = new JCheckBox("", false);
        useMojangJava.setOpaque(false);
        useMojangJava.setHorizontalAlignment(SwingConstants.RIGHT);
        useMojangJava.setBorder(BorderFactory.createEmptyBorder());
        useMojangJava.setIconTextGap(0);
        useMojangJava.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        useMojangJava.setIcon(resources.getIcon("checkbox_open.png"));
        useMojangJava.setFocusPainted(false);
        panel.add(useMojangJava, new GridBagConstraints(1, 6, 6, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 16, 8, 8), 0, 0));

        panel.add(Box.createGlue(), new GridBagConstraints(4, 7, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        //Wipe controls
        this.getContentPane().removeAll();
        this.setLayout(null);

        initComponents();
        initControlValues();

        SwingUtilities.invokeLater(() -> {
            invalidate();
            repaint();
        });
    }
}
