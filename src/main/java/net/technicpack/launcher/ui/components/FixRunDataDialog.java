package net.technicpack.launcher.ui.components;

import net.technicpack.launcher.ui.LauncherFrame;
import net.technicpack.launchercore.launch.java.IJavaVersion;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.modpacks.RunData;
import net.technicpack.ui.controls.LauncherDialog;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.borders.RoundBorder;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.Memory;

import javax.swing.*;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FixRunDataDialog extends LauncherDialog {

    private static final int DIALOG_WIDTH = 620;

    private ResourceLoader resourceLoader;

    private RunData runData;
    private JavaVersionRepository javaVersionRepository;
    private Memory attemptedMemory;
    private boolean shouldAskFirst;

    private IJavaVersion recommendedVersion;
    private Memory recommendedMemory;

    private JCheckBox rememberThis;
    private Result result = Result.OK;

    public enum Result {
        OK,
        ACCEPT,
        CANCEL
    }

    public FixRunDataDialog(Frame owner, ResourceLoader resourceLoader, RunData runData, JavaVersionRepository javaVersionRepository, Memory attemptedMemory, boolean shouldAskFirst) {
        super(owner);
        this.runData = runData;
        this.javaVersionRepository = javaVersionRepository;
        this.attemptedMemory = attemptedMemory;
        this.resourceLoader = resourceLoader;
        this.shouldAskFirst = shouldAskFirst;

        recommendSettings();
        initComponents();
    }

    @Override
    public void setVisible(boolean visible) {
        if ((recommendedVersion != null && recommendedMemory != null && javaVersionRepository.getSelectedVersion().equals(recommendedVersion) && attemptedMemory.getMemoryMB() == recommendedMemory.getMemoryMB()) ||
                (!shouldAskFirst && recommendedMemory != null && recommendedVersion != null)) {
            result = Result.ACCEPT;
            dispose();
            return;
        }

        super.setVisible(visible);
    }

    public Result getResult() {
        return result;
    }

    public boolean shouldRemember() {
        if (rememberThis == null)
            return false;
        return rememberThis.isSelected();
    }

    public Memory getRecommendedMemory() {
        return recommendedMemory;
    }

    public IJavaVersion getRecommendedJavaVersion() {
        return recommendedVersion;
    }

    protected void recommendSettings() {
        if (!runData.isJavaValid(javaVersionRepository.getSelectedVersion().getVersionNumber())) {
            recommendedVersion = runData.getValidJavaVersion(javaVersionRepository);
        } else {
            recommendedVersion = javaVersionRepository.getSelectedVersion();
        }

        if (!runData.isMemoryValid(attemptedMemory.getMemoryMB())) {
            recommendedMemory = runData.getValidMemory(javaVersionRepository);
        } else {
            recommendedMemory = attemptedMemory;
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        createHeader(resourceLoader.getString("fixRunData.title"));

        JPanel centerPanel = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                Dimension dim = super.getMaximumSize();
                dim.width = DIALOG_WIDTH;
                return dim;
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = DIALOG_WIDTH;
                return dim;
            }
        };
        centerPanel.setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        centerPanel.setOpaque(true);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        add(centerPanel, BorderLayout.CENTER);

        centerPanel.setLayout(new GridBagLayout());

        Font font = resourceLoader.getFont(ResourceLoader.FONT_OPENSANS, 16);

        JLabel label = new JLabel(
            "<html>" +
                "<body style=\"font-family:"+font.getFamily()+";font-size:11px;font-color:#D0D0D0\">" +
                    resourceLoader.getString("fixRunData.header") +
                "</body>" +
            "</html>");
        label.setFont(font);
        label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        centerPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 1.0f, 0, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
        label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH-5));

        buildSuccessFailPanels(centerPanel, font.getFamily());
        boolean isFailure = recommendedMemory == null || recommendedVersion == null;

        int gridBagIndex = 3;
        if (isFailure) {
            gridBagIndex = buildFailureReasons(centerPanel, gridBagIndex, font.getFamily());
        } else {
            label = new JLabel(
                    "<html>" +
                        "<body style=\"font-family:" + font.getFamily() + ";font-size:11px;font-color:#D0D0D0\">" +
                            resourceLoader.getString("fixRunData.changeSettings") +
                        "</body>" +
                    "</html>");
            label.setFont(font);
            label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
            centerPanel.add(label, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(8, 0, 8, 0), 0, 0));
            label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH - 5));
        }

        JSeparator separator = new JSeparator();
        separator.setForeground(LauncherFrame.COLOR_REQUIREMENT_SEPARATOR);
        separator.setBackground(LauncherFrame.COLOR_REQUIREMENT_SEPARATOR);
        centerPanel.add(separator, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(isFailure?14:0, 0, 0,0),0,0));

        if (isFailure) {
            buildFailureButtons(centerPanel, gridBagIndex, font);
        } else {
            buildRecommendedButtons(centerPanel, gridBagIndex, font);
        }

        pack();
        setLocationRelativeTo(getParent());
    }

    private void createHeader(String text) {
        JPanel header = new JPanel();
        header.setBackground(Color.black);
        header.setLayout(new BoxLayout(header, BoxLayout.LINE_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        add(header, BorderLayout.PAGE_START);

        JLabel title = new JLabel(text);
        title.setFont(resourceLoader.getFont(ResourceLoader.FONT_RALEWAY, 26));
        title.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        title.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        title.setOpaque(false);
        title.setIcon(resourceLoader.getIcon("options_cog.png"));
        header.add(title);

        header.add(Box.createHorizontalGlue());

        JButton closeButton = new JButton();
        closeButton.setIcon(resourceLoader.getIcon("close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });
        closeButton.setFocusPainted(false);
        header.add(closeButton);
    }

    private void buildSuccessFailPanels(JPanel centerPanel, String fontFamily) {
        boolean memorySuccess = runData.isMemoryValid(attemptedMemory.getMemoryMB());
        boolean javaSuccess = runData.isJavaValid(javaVersionRepository.getSelectedVersion().getVersionNumber());

        String memRequirement = resourceLoader.getString("fixRunData.reqMemory", runData.getMemoryObject().toString());
        String javaRequirement = resourceLoader.getString("fixRunData.reqJava", runData.getJava());

        String currentMem = resourceLoader.getString("fixRunData.currentMemory", attemptedMemory.toString());
        String currentJavaBitness = javaVersionRepository.getSelectedVersion().is64Bit()?resourceLoader.getString("launcheroptions.java.64bit"):resourceLoader.getString("launcheroptions.java.32bit");
        String currentJava = resourceLoader.getString("fixRunData.currentJava", javaVersionRepository.getSelectedVersion().getVersionNumber(), currentJavaBitness);

        if (!memorySuccess && recommendedMemory != null) {
            currentMem += resourceLoader.getString("fixRunData.bestOption", recommendedMemory.toString());
        }

        if (!javaSuccess && recommendedVersion != null) {
            String javaVersion = recommendedVersion.getVersionNumber();
            String javaBitness = recommendedVersion.is64Bit()?resourceLoader.getString("launcheroptions.java.64bit"):resourceLoader.getString("launcheroptions.java.32bit");
            if (javaVersionRepository.getBest64BitVersion() == recommendedVersion)
                javaVersion = resourceLoader.getString("launcheroptions.java.best64version", javaVersion + " " + javaBitness);
            currentJava += resourceLoader.getString("fixRunData.bestOption", javaVersion);
        }

        addSuccessFailPanel(centerPanel, fontFamily, 1, memorySuccess, recommendedMemory != null, memRequirement, currentMem);
        addSuccessFailPanel(centerPanel, fontFamily, 2, javaSuccess, recommendedVersion != null, javaRequirement, currentJava);
    }

    private int buildFailureReasons(JPanel centerPanel, int gridBagIndex, String fontFamily) {
        JLabel label = new JLabel(
                "<html>" +
                    "<body style=\"font-family:"+fontFamily+";font-size:11px;font-color:#D0D0D0\">" +
                        resourceLoader.getString("fixRunData.cannotRun") +
                    "</body>" +
                "</html>");
        label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        centerPanel.add(label, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(8,0,4,0), 0,0));
        label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH-5));

        boolean javaVersionGood = recommendedVersion != null;
        boolean memoryGood = recommendedMemory != null;
        boolean requires64Bit = runData.getMemory() > Memory.MAX_32_BIT_MEMORY;
        boolean has64Bit = javaVersionRepository.getBest64BitVersion() != null;

        boolean get64BitJava = requires64Bit && !has64Bit;
        boolean getBetterJava = !javaVersionGood && !get64BitJava;
        boolean getMoreRam = !memoryGood && !get64BitJava;

        if (getBetterJava) {
            label = new JLabel(
                    "<html>" +
                            "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://www.technicpack.net/assets/css/launcher.css\" /></head>" +
                            "<body style=\"font-family:" + fontFamily + ";font-size:11px;font-color:#D0D0D0\">" +
                            resourceLoader.getString("fixRunData.needBetterJava") +
                            "</body>" +
                            "</html>", resourceLoader.getIcon("danger_icon.png"), SwingConstants.LEFT);
            label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
            centerPanel.add(label, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 8, 0, 0), 0, 0));
            label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH - 5));
        }

        if (get64BitJava) {
            label = new JLabel(
                    "<html>" +
                            "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"http://www.technicpack.net/assets/css/launcher.css\" /></head>" +
                            "<body style=\"font-family:" + fontFamily + ";font-size:11px;font-color:#D0D0D0\">" +
                            resourceLoader.getString("fixRunData.need64BitJava") +
                            "</body>" +
                            "</html>", resourceLoader.getIcon("danger_icon.png"), SwingConstants.LEFT);
            label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
            centerPanel.add(label, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 8, 0, 0), 0, 0));
            label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH - 5));
        }

        if (getMoreRam) {
            label = new JLabel(
                    "<html>" +
                            "<body style=\"font-family:" + fontFamily + ";font-size:11px;font-color:#D0D0D0\">" +
                            resourceLoader.getString("fixRunData.needMoreRAM") +
                            "</body>" +
                            "</html>", resourceLoader.getIcon("danger_icon.png"), SwingConstants.LEFT);
            label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
            centerPanel.add(label, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 8, 0, 0), 0, 0));
            label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH - 5));
        }

        return gridBagIndex;
    }

    private void addSuccessFailPanel(JPanel centerPanel, String fontFamily, int gridBagRow, boolean isSuccess, boolean hasRecommendation, String compareText, String contrastText) {
        JPanel successFailPanel = new JPanel();
        successFailPanel.setBorder(new RoundBorder(getPanelColor(isSuccess, hasRecommendation)));
        successFailPanel.setBackground(getPanelColor(isSuccess, hasRecommendation));
        centerPanel.add(successFailPanel, new GridBagConstraints(0, gridBagRow, 1, 1, 1.0f, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8,0,10,0),0,0));
        successFailPanel.setLayout(new GridBagLayout());

        JLabel checkbox = new JLabel(resourceLoader.getIcon(getPanelIcon(isSuccess, hasRecommendation)));
        successFailPanel.add(checkbox, new GridBagConstraints(0, 0, 1, 2, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,14,0,0), 0,0));

        JLabel label = new JLabel(
                "<html>" +
                    "<body style=\"font-family:"+fontFamily+";font-size:12px;font-color:#D0D0D0\">" +
                        compareText +
                    "</body>" +
                "</html>"
        );
        label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        successFailPanel.add(label, new GridBagConstraints(1, 0, 1, 1, 1.0f, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(8,10,6,0),0,0));
        label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH - 125));

        label = new JLabel(
                "<html>" +
                    "<body style=\"font-family:"+fontFamily+";font-size:12px;font-color:#D0D0D0\">" +
                        contrastText +
                    "</body>" +
                "</html>"
        );
        label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        successFailPanel.add(label, new GridBagConstraints(1, 1, 1, 1, 1.0f, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,10,8,0),0,0));
        label.setPreferredSize(getPreferredSize(label.getText(), DIALOG_WIDTH - 125));
    }

    private void buildFailureButtons(JPanel centerPanel, int gridBagIndex, Font font) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        centerPanel.add(buttonPanel, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 1.0f, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(8, 0, 0, 0), 0, 0));

        RoundedButton okButton = new RoundedButton(resourceLoader.getString("fixRunData.OK"));
        okButton.setFont(resourceLoader.getFont(ResourceLoader.FONT_OPENSANS, 16));
        okButton.setContentAreaFilled(false);
        okButton.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 25));
        okButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        okButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = Result.OK;
                dispose();
            }
        });
        buttonPanel.add(okButton);
    }

    private void buildRecommendedButtons(JPanel centerPanel, int gridBagIndex, Font font) {
        JPanel checkPanel = new JPanel();
        checkPanel.setOpaque(false);
        centerPanel.add(checkPanel, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        JLabel label = new JLabel(resourceLoader.getString("fixRunData.dontAskAgain"));
        label.setFont(font);
        label.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        checkPanel.add(label);

        rememberThis = new JCheckBox("", false);
        rememberThis.setHorizontalAlignment(SwingConstants.RIGHT);
        rememberThis.setBorder(BorderFactory.createEmptyBorder());
        rememberThis.setIconTextGap(0);
        rememberThis.setSelectedIcon(resourceLoader.getIcon("checkbox_closed.png"));
        rememberThis.setIcon(resourceLoader.getIcon("checkbox_open.png"));
        rememberThis.setFocusPainted(false);
        checkPanel.add(rememberThis);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        centerPanel.add(buttonPanel, new GridBagConstraints(0, gridBagIndex++, 1, 1, 1.0f, 1.0f, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(8, 0, 0, 0), 0, 0));

        RoundedButton cancelButton = new RoundedButton(resourceLoader.getString("fixRunData.cancel"));
        cancelButton.setFont(resourceLoader.getFont(ResourceLoader.FONT_OPENSANS, 16));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        cancelButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = Result.CANCEL;
                dispose();
            }
        });
        buttonPanel.add(cancelButton);

        buttonPanel.add(Box.createHorizontalGlue());

        RoundedButton okButton = new RoundedButton(resourceLoader.getString("fixRunData.OK"));
        okButton.setFont(resourceLoader.getFont(ResourceLoader.FONT_OPENSANS, 16));
        okButton.setContentAreaFilled(false);
        okButton.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        okButton.setHoverForeground(LauncherFrame.COLOR_BLUE);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = Result.ACCEPT;
                dispose();
            }
        });
        buttonPanel.add(okButton);
    }

    private Color getPanelColor(boolean isSuccess, boolean hasRecommended) {
        return isSuccess?LauncherFrame.COLOR_REQUIREMENT_SUCCEED:(hasRecommended?LauncherFrame.COLOR_REQUIREMENT_WARNING:LauncherFrame.COLOR_REQUIREMENT_FAIL);
    }

    public String getPanelIcon(boolean isSuccess, boolean hasRecommended) {
        return isSuccess?"req_success.png":(hasRecommended?"req_warning.png":"req_failed.png");
    }

    /**Returns the preferred size to set a component at in order to render
     * an html string.  You can specify the size of one dimension.*/
    private static JLabel resizer = new JLabel();
    private java.awt.Dimension getPreferredSize(String html, int width) {
        resizer.setText(html);

        View view = (View) resizer.getClientProperty(
                javax.swing.plaf.basic.BasicHTML.propertyKey);

        view.setSize(width,0);

        float w = view.getPreferredSpan(View.X_AXIS);
        float h = view.getPreferredSpan(View.Y_AXIS);

        return new java.awt.Dimension((int) Math.ceil(w),
                (int) Math.ceil(h));
    }

    protected void closeDialog() {
        dispose();
    }
}
