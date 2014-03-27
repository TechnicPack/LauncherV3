/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.ui;

import net.technicpack.launcher.lang.IRelocalizableResource;
import net.technicpack.launcher.lang.ResourceLoader;
import net.technicpack.launcher.ui.controls.AAJLabel;
import net.technicpack.launcher.ui.controls.DraggableFrame;
import net.technicpack.launcher.ui.controls.RoundedButton;
import net.technicpack.launcher.ui.controls.borders.RoundBorder;
import net.technicpack.launcher.ui.controls.login.UserCellEditor;
import net.technicpack.launcher.ui.controls.login.UserCellRenderer;
import net.technicpack.launcher.ui.controls.login.UserCellUI;
import net.technicpack.launchercore.auth.User;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.image.SkinRepository;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Locale;

public class LoginFrame extends DraggableFrame implements IRelocalizableResource, KeyListener {
    private ResourceLoader resources;
    private SkinRepository skinRepository;
    private UserModel userModel;

    private JTextField name;
    private JComboBox nameSelect;
    private JCheckBox rememberAccount;
    private JPasswordField password;

    private static final int FRAME_WIDTH = 347;
    private static final int FRAME_HEIGHT = 409;

    public LoginFrame(ResourceLoader resources, UserModel userModel, SkinRepository skinRepository) {
        this.skinRepository = skinRepository;
        this.userModel = userModel;

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        //Handles rebuilding the frame, so use it to build the frame in the first place
        relocalize(resources);
    }

    protected void closeButtonClicked() {
        this.dispose();
    }

    protected void changeUser() {
        if (nameSelect.getSelectedItem() == null || nameSelect.getSelectedItem().equals("")) {
            clearCurrentUser();
        } else if (nameSelect.getSelectedItem() instanceof User) {
            setCurrentUser((User)nameSelect.getSelectedItem());
        }
    }

    protected void toggleRemember() {
        if (!rememberAccount.isSelected() && nameSelect.isVisible() && nameSelect.getSelectedItem() instanceof User) {
            forgetUser((User)nameSelect.getSelectedItem());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource() == rememberAccount && e.getKeyCode() == KeyEvent.VK_ENTER) {
            attemptLogin();
        }
    }

    protected void refreshUsers() {
        Collection<User> userAccounts = userModel.getUsers();
        User lastUser = userModel.getLastUser();

        if (userAccounts.size() == 0) {
            name.setVisible(true);
            nameSelect.setVisible(false);
            clearCurrentUser();
        } else {
            name.setVisible(false);
            nameSelect.setVisible(true);
            nameSelect.removeAllItems();

            for (User account : userAccounts) {
                nameSelect.addItem(account);
            }

            nameSelect.addItem(null);

            if (lastUser == null)
                lastUser = userAccounts.iterator().next();

            setCurrentUser(lastUser);
        }
    }

    protected void attemptLogin() {

    }

    protected void clearCurrentUser() {

    }

    protected void setCurrentUser(User user) {

    }

    protected void forgetUser(User user) {
        userModel.removeUser(user);
        refreshUsers();
    }

    /**
     * Generate & setup UI components for the frame
     */
    private void initComponents() {
        setLayout(new GridBagLayout());

        //Close button
        JButton closeButton = new JButton();
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder());
        closeButton.setIcon(resources.getIcon("close.png"));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeButtonClicked();
            }
        });
        closeButton.setFocusable(false);
        add(closeButton, new GridBagConstraints(2,0,1,1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(7,0,0,7),0,0));

        //Logo at the top
        JLabel platformImage = new JLabel();
        platformImage.setIcon(resources.getIcon("platform_logo.png"));
        add(platformImage, new GridBagConstraints(0,0,3,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(30,0,0,0),0,0));

        JLabel instructionText = new AAJLabel(resources.getString("login.instructions"));
        instructionText.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        instructionText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(instructionText, new GridBagConstraints(0,1,3,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(9,0,0,0),0,0));

        JLabel userLabel = new AAJLabel(resources.getString("login.username"));
        userLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        userLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(userLabel, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,20,0,20), 0,0));

        // Setup username box
        nameSelect = new JComboBox();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            nameSelect.setUI(new MetalComboBoxUI());
        }

        nameSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        nameSelect.setEditable(true);
        nameSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        nameSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        nameSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        nameSelect.setVisible(false);
        UserCellRenderer userRenderer= new UserCellRenderer(resources.getFont(ResourceLoader.FONT_OPENSANS, 16), resources, this.skinRepository);
        nameSelect.setRenderer(userRenderer);
        UserCellEditor userEditor = new UserCellEditor(resources.getFont(ResourceLoader.FONT_OPENSANS, 16), this.skinRepository);
        nameSelect.setEditor(userEditor);
        userEditor.addKeyListener(this);
        nameSelect.setUI(new UserCellUI(resources));
        nameSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeUser();
            }
        });

        Object child = nameSelect.getAccessibleContext().getAccessibleChild(0);
        BasicComboPopup popup = (BasicComboPopup)child;
        JList list = popup.getList();
        list.setBorder( new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 0) );

        add(nameSelect, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 20, 0, 20), 4, 4));

        name = new JTextField();
        name.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        name.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        name.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        name.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        name.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        name.addKeyListener(this);
        add(name, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3,20,0,20),4,17));

        AAJLabel passLabel = new AAJLabel(resources.getString("login.password"));
        passLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        passLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(passLabel, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12,20,0,20),0,0));

        // Setup password box
        password = new JPasswordField();
        password.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        password.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        password.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        password.addKeyListener(this);
        password.setEchoChar('*');
        password.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });
        password.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        add(password, new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 20, 0, 20), 4, 17));

        // "Remember this account"
        rememberAccount = new JCheckBox(resources.getString("login.remember"), false);
        rememberAccount.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        rememberAccount.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        rememberAccount.setOpaque(false);
        rememberAccount.setHorizontalTextPosition(SwingConstants.LEFT);
        rememberAccount.setHorizontalAlignment(SwingConstants.RIGHT);
        rememberAccount.setBorder(BorderFactory.createEmptyBorder());
        rememberAccount.setIconTextGap(12);
        rememberAccount.addKeyListener(this);
        rememberAccount.setSelectedIcon(resources.getIcon("checkbox_closed.png"));
        rememberAccount.setIcon(resources.getIcon("checkbox_open.png"));
        rememberAccount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleRemember();
            }
        });
        rememberAccount.setFocusPainted(false);
        add(rememberAccount, new GridBagConstraints(1,6,2,1,0.0,0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(24,20,0,20),0,0));

        //Login button
        RoundedButton button = new RoundedButton(resources.getString("login.button"));
        button.setBorder(BorderFactory.createEmptyBorder(4,20,8,20));
        button.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 18));
        button.setContentAreaFilled(false);
        button.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        button.setHoverForeground(LauncherFrame.COLOR_BLUE);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });
        add(button, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(24,20,0,0),0,0));

        add(Box.createVerticalGlue(), new GridBagConstraints(0, 8, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        JPanel linkPane = new JPanel();
        linkPane.setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        linkPane.setBorder(BorderFactory.createEmptyBorder(7,0,7,0));

        JButton termsLink = new JButton(resources.getString("login.terms"));
        termsLink.setContentAreaFilled(false);
        termsLink.setBorder(BorderFactory.createEmptyBorder());
        termsLink.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        termsLink.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        termsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkPane.add(termsLink);

        JLabel dash = new JLabel("-");
        dash.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        dash.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        dash.setOpaque(false);
        linkPane.add(dash);

        JButton privacyLink = new JButton(resources.getString("login.privacy"));
        privacyLink.setContentAreaFilled(false);
        privacyLink.setBorder(BorderFactory.createEmptyBorder());
        privacyLink.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        privacyLink.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        privacyLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        linkPane.add(privacyLink);

        add(linkPane, new GridBagConstraints(0, 9, 3, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }

    @Override
    public void relocalize(ResourceLoader loader) {
        this.resources = loader;
        this.resources.registerResource(this);

        setIconImage(this.resources.getImage("icon.png"));

        //Wipe controls
        this.getContentPane().removeAll();
        this.setLayout(null);

        //Clear references to existing controls
        nameSelect = null;
        rememberAccount = null;
        password = null;

        initComponents();

        refreshUsers();
    }
}
