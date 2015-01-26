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

package net.technicpack.ui.controls.login;

import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.launchercore.image.IImageJobListener;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.ImageUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class UserCellEditor implements ComboBoxEditor, DocumentListener, IImageJobListener<IUserType> {
    private Font textFont;

    private static final int ICON_WIDTH=32;
    private static final int ICON_HEIGHT=32;

    private JPanel parentPanel;
    private JLabel userLabel;
    private JTextField textField;
    private CardLayout layout;

    private Object currentObject;
    private HashMap<String, Icon> headMap = new HashMap<String, Icon>();

    Collection<ActionListener> actionListeners = new HashSet<ActionListener>();
    private ImageRepository<IUserType> mSkinRepo;

    private static final String USER = "user";
    private static final String STRING = "string";

    public UserCellEditor(Font font, ImageRepository<IUserType> skinRepo, Color textColor) {
        this.textFont = font;
        this.mSkinRepo = skinRepo;

        layout = new CardLayout();

        parentPanel = new JPanel();
        parentPanel.setLayout(layout);
        parentPanel.setOpaque(false);

        userLabel = new JLabel();
        userLabel.setOpaque(false);
        userLabel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        userLabel.setFont(textFont);
        userLabel.setForeground(textColor);
        parentPanel.add(userLabel, USER);

        textField = new JTextField();
        textField.setOpaque(false);
        textField.setFont(textFont);
        textField.setBorder(null);
        textField.setForeground(textColor);
        textField.getDocument().addDocumentListener(this);
        textField.setCaretColor(textColor);
        parentPanel.add(textField, STRING);
    }

    @Override
    public Component getEditorComponent() {
        return parentPanel;
    }

    @Override
    public void setItem(Object anObject) {
        currentObject = anObject;

        if (anObject instanceof IUserType) {
            IUserType mojangUser = (IUserType)anObject;
            userLabel.setText(mojangUser.getDisplayName());
            userLabel.setIconTextGap(8);

            if (!headMap.containsKey(mojangUser.getUsername())) {
                ImageJob<IUserType> job = mSkinRepo.startImageJob(mojangUser);
                job.addJobListener(this);
                headMap.put(mojangUser.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));
            }

            Icon head = headMap.get(mojangUser.getUsername());
            userLabel.setIcon(head);

            layout.show(parentPanel, USER);
        } else {
            String newText = "";

            if (anObject != null) {
                newText = anObject.toString();
            }

            if (!textField.getText().equals(newText))
                textField.setText(newText);

            layout.show(parentPanel, STRING);
            textField.requestFocus();
        }
    }

    @Override
    public Object getItem() {
        return currentObject;
    }

    @Override
    public void selectAll() {
    }

    @Override
    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        actionListeners.remove(l);
    }

    public void addKeyListener(KeyListener k) {
        userLabel.addKeyListener(k);
    }

    public void removeKeyListener(KeyListener k) {
        userLabel.removeKeyListener(k);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        currentObject = textField.getText();
        for(ActionListener listener : actionListeners) {
            listener.actionPerformed(new ActionEvent(this, 0, "edited"));
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        currentObject = textField.getText();
        for(ActionListener listener : actionListeners) {
            listener.actionPerformed(new ActionEvent(this, 0, "edited"));
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        currentObject = textField.getText();
        for(ActionListener listener : actionListeners) {
            listener.actionPerformed(new ActionEvent(this, 0, "edited"));
        }
    }

    @Override
    public void jobComplete(ImageJob<IUserType> job) {
        IUserType mojangUser = job.getJobData();
        if (headMap.containsKey(mojangUser.getUsername()))
            headMap.remove(mojangUser.getUsername());

        headMap.put(mojangUser.getUsername(), new ImageIcon(ImageUtils.scaleImage(job.getImage(), ICON_WIDTH, ICON_HEIGHT)));
        this.parentPanel.revalidate();
    }
}
