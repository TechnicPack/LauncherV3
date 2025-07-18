/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.components;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Console dialog for showing console messages.
 *
 * @author sk89q
 *         <p/>
 *         This code reused & relicensed as LGPL v 3 with permission.
 */
public class ConsoleFrame extends JFrame implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static String[] monospaceFontNames = {"Consolas", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Lucida Console"};
    private final SimpleAttributeSet highlightedAttributes;
    private final SimpleAttributeSet errorAttributes;
    private final SimpleAttributeSet warnAttributes;
    private final SimpleAttributeSet debugAttributes;
    private final SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();
    private JTextPane textComponent;
    private Document document;
    private int numLines;

    /**
     * Construct the frame.
     *
     * @param numLines number of lines to show at a time
     */
    public ConsoleFrame(int numLines, Image frameIcon) {
        super("Technic Launcher Console");
        setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);

        this.numLines = numLines;

        this.highlightedAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(highlightedAttributes, Color.BLACK);
        StyleConstants.setBackground(highlightedAttributes, Color.YELLOW);

        this.errorAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(errorAttributes, new Color(200, 0, 0));
        this.warnAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(warnAttributes, new Color(200, 200, 0));
        this.debugAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(debugAttributes, Color.DARK_GRAY);

        setSize(new Dimension(896, 504));
        buildUI();

        setIconImage(frameIcon);

        addMouseListener(this);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    public Document getDocument() { return document; }
    public AttributeSet getHighlightedAttributes() { return highlightedAttributes; }
    public AttributeSet getDefaultAttributes() { return defaultAttributes; }
    public AttributeSet getErrorAttributes() { return errorAttributes; }
    public AttributeSet getWarnAttributes() { return warnAttributes; }
    public AttributeSet getDebugAttributes() { return debugAttributes; }
    public void setCaretPosition(int position) { textComponent.setCaretPosition(position); }

    /**
     * Build the interface.
     */
    private void buildUI() {
        this.textComponent = new JTextPane();
        textComponent.addMouseListener(this);
        textComponent.setFont(getMonospaceFont());
        textComponent.setEditable(false);

        DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        document = textComponent.getDocument();
        document.addDocumentListener(new LimitLinesDocumentListener(numLines));

        textComponent.setBackground(Color.BLACK);
        textComponent.setForeground(Color.WHITE);

        JScrollPane scrollText = new JScrollPane(textComponent);
        scrollText.setBorder(null);
        scrollText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollText, BorderLayout.CENTER);
    }

    /**
     * Get a supported monospace font.
     *
     * @return font
     */
    public static Font getMonospaceFont() {
        for (String fontName : monospaceFontNames) {
            Font font = new Font(fontName, Font.PLAIN, 14);
            if (!font.getFamily().equalsIgnoreCase("Dialog")) {
                return font;
            }
        }
        return new Font("Monospace", Font.PLAIN, 14);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // unused
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            doPop(e);
        }
    }

    private void doPop(MouseEvent e) {
        // The component must be visible, or an exception is thrown in Component.getLocationOnScreen_NoTreeLock()
        if (!e.getComponent().isVisible()) {
            return;
        }

        ContextMenu menu = new ContextMenu();
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            doPop(e);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // unused
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // unused
    }

    private class ContextMenu extends JPopupMenu {
        private static final long serialVersionUID = 1L;
        JMenuItem copy;
        JMenuItem clear;

        public ContextMenu() {
            copy = new JMenuItem("Copy");
            add(copy);
            copy.addActionListener(e -> textComponent.copy());

            clear = new JMenuItem("Clear");
            add(clear);
            clear.addActionListener(e -> textComponent.setText(null));
        }
    }
}
