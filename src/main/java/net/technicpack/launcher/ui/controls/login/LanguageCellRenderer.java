package net.technicpack.launcher.ui.controls.login;

import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.LauncherFrame;

import javax.swing.*;
import java.awt.*;

public class LanguageCellRenderer  extends JLabel implements ListCellRenderer {

    private ResourceLoader resources;
    private ImageIcon globe;

    public LanguageCellRenderer(ResourceLoader resourceLoader) {
        resources = resourceLoader;
        globe = resourceLoader.getIcon("globe.png");

        setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        setText(value.toString());

        setIcon((!isSelected && list.getSelectedValue().equals(value))?globe:null);

        return this;
    }
}
