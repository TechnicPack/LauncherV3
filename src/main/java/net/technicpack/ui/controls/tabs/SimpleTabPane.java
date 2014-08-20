package net.technicpack.ui.controls.tabs;

import javax.swing.*;
import java.awt.*;

public class SimpleTabPane extends JTabbedPane {

    private Color selectedForeground;
    private Color selectedBackground;

    public SimpleTabPane() {
        super();

        setUI(new SimpleTabPaneUI());
    }

    public Color getSelectedForeground() { return selectedForeground; }
    public void setSelectedForeground(Color color) {
        selectedForeground = color;
    }

    public Color getSelectedBackground() { return selectedBackground; }
    public void setSelectedBackground(Color color) {
        selectedBackground = color;
    }

    @Override
    public Color getBackgroundAt(int index) {
        if (getSelectedIndex() == index)
            return getSelectedBackground();
        else
            return getBackground();
    }

    @Override
    public Color getForegroundAt(int index) {
        if (getSelectedIndex() == index)
            return getSelectedForeground();
        else
            return getForeground();
    }
}
