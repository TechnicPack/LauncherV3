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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * Adapted from http://tips4java.wordpress.com/2008/10/15/limit-lines-in-document/,
 * with updates from https://github.com/SKCraft/Launcher/pull/82
 */
public class LimitLinesDocumentListener implements DocumentListener {
    private int maximumLines;
    private boolean isRemoving;

    /**
     * Specify the number of lines to be stored in the Document. Extra lines
     * will be removed from the start of the Document.
     *
     * @param maximumLines number of lines
     */
    public LimitLinesDocumentListener(int maximumLines) {
        setLimitLines(maximumLines);
        this.isRemoving = false;
    }

    /**
     * Set the maximum number of lines to be stored in the Document
     *
     * @param maximumLines number of lines
     */
    public void setLimitLines(int maximumLines) {
        if (maximumLines < 1) {
            throw new IllegalArgumentException("Maximum lines must be greater than 0");
        }

        this.maximumLines = maximumLines;
    }

    @Override
    public void insertUpdate(final DocumentEvent e) {
        // Changes to the Document can not be done within the listener
        // so we need to add the processing to the end of the EDT
        if (!this.isRemoving) {
            this.isRemoving = true;
            SwingUtilities.invokeLater(() -> removeLines(e));
        }
    }

    private void removeLines(DocumentEvent e) {
        try {
            // The root Element of the Document will tell us the total number
            // of line in the Document.
            Document document = e.getDocument();
            Element root = document.getDefaultRootElement();
            int excess = root.getElementCount() - maximumLines;

            if (excess > 0) {
                Element line = root.getElement(excess - 1);
                int end = line.getEndOffset();

                try {
                    document.remove(0, end);
                } catch (BadLocationException ble) {
                    System.out.println(ble);
                }
            }
        } finally {
            this.isRemoving = false;
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        // unused
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // unused
    }
}
