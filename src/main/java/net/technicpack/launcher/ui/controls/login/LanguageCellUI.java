package net.technicpack.launcher.ui.controls.login;

import net.technicpack.launcher.lang.ResourceLoader;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class LanguageCellUI extends BasicComboBoxUI {

    private ResourceLoader resources;

    public LanguageCellUI(ResourceLoader loader) {
        this.resources = loader;
    }

    @Override protected JButton createArrowButton() {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        return button;
    }
}