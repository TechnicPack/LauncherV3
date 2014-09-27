package net.technicpack.ui.controls.list;

import javax.swing.*;
import java.awt.*;

public class AdvancedCellRenderer extends JLabel implements ListCellRenderer {

    private Color selectedBackground;
    private Color selectedForeground;
    private Color unselectedBackground;
    private Color unselectedForeground;

    public AdvancedCellRenderer() {
        setOpaque(true);
    }

    public Color getSelectedBackgroundColor() { return selectedBackground; }
    public void setSelectedBackgroundColor(Color color) { selectedBackground = color; }

    public Color getSelectedForegroundColor() { return selectedForeground; }
    public void setSelectedForegroundColor(Color color) { selectedForeground = color; }

    public Color getUnselectedBackgroundColor() { return unselectedBackground; }
    public void setUnselectedBackgroundColor(Color color) { unselectedBackground = color; }

    public Color getUnselectedForegroundColor() { return unselectedForeground; }
    public void setUnselectedForegroundColor(Color color) { unselectedForeground = color; }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(getSelectedBackgroundColor());
            setForeground(getSelectedForegroundColor());
        } else {
            setBackground(getUnselectedBackgroundColor());
            setForeground(getUnselectedForegroundColor());
        }

        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        if (value == null)
            this.setText("");
        else
            this.setText(value.toString());

        return this;
    }
}
