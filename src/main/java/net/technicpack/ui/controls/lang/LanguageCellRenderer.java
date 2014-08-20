package net.technicpack.ui.controls.lang;

import net.technicpack.ui.lang.ResourceLoader;

import javax.swing.*;
import java.awt.*;

public class LanguageCellRenderer  extends JLabel implements ListCellRenderer {

    private ResourceLoader resources;
    private ImageIcon globe;

    private Color background;
    private Color foreground;

    public LanguageCellRenderer(ResourceLoader resourceLoader, String langIcon, Color background, Color foreground) {
        resources = resourceLoader;
        globe = resourceLoader.getIcon(langIcon);
        this.background = background;
        this.foreground = foreground;

        setForeground(foreground);
        setBackground(background);
        setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setForeground(this.foreground);
        setBackground(this.background);
        setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        setText(value.toString());

        setIcon((!isSelected && list.getSelectedValue().equals(value))?globe:null);

        return this;
    }
}
