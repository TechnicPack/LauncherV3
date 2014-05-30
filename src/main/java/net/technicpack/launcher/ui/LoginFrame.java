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
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.User;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.DesktopUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

public class LoginFrame extends DraggableFrame implements IRelocalizableResource, KeyListener, IAuthListener {
    private ResourceLoader resources;
    private ImageRepository<User> skinRepository;
    private UserModel userModel;

    private JTextField name;
    private JComboBox nameSelect;
    private JCheckBox rememberAccount;
    private JPasswordField password;

    private static final int FRAME_WIDTH = 347;
    private static final int FRAME_HEIGHT = 409;

    public LoginFrame(ResourceLoader resources, UserModel userModel, ImageRepository<User> skinRepository) {
        this.skinRepository = skinRepository;
        this.userModel = userModel;

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

        this.setFocusTraversalPolicy(new SortingFocusTraversalPolicy(new Comparator<Component>() {
            @Override
            public int compare(Component o1, Component o2) {
                //This long stupid stack of else/ifs enforces a tab order of
                //Username -> Password -> Remember me -> any buttons -> everything else who cares
                if (o1 == name || o1 == nameSelect)
                    return -1;
                else if (o2 == name || o2 == nameSelect)
                    return 1;
                else if (o1 == password)
                    return -1;
                else if (o2 == password)
                    return 1;
                else if (o1 == rememberAccount)
                    return -1;
                else if (o2 == rememberAccount)
                    return 1;
                else if (o1 instanceof AbstractButton)
                    return -1;
                else if (o2 instanceof AbstractButton)
                    return 1;
                else
                    return 0;
            }
        }));

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

    protected void visitTerms() {
        DesktopUtils.browseUrl("http://www.technicpack.net/terms");
    }

    protected void visitPrivacy() {
        DesktopUtils.browseUrl("http://www.technicpack.net/privacy");
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
        if (nameSelect.isVisible()) {
            Object selected = nameSelect.getSelectedItem();

            if (selected instanceof User) {
                verifyExistingLogin((User) selected);
            } else {
                String username = selected.toString();

                User user = userModel.getUser(username);

                if (user == null)
                    attemptNewLogin(username);
                else {
                    setCurrentUser(user);
                    verifyExistingLogin(user);
                }
            }
        } else {
            attemptNewLogin(name.getText());
        }
    }

    private void verifyExistingLogin(User user) {
        User loginUser = user;
        boolean rejected = false;

        try {
            UserModel.AuthError error = userModel.attemptUserRefresh(user);

            if (error != null) {
                JOptionPane.showMessageDialog(this, error.getErrorDescription(), error.getError(), JOptionPane.ERROR_MESSAGE);
                loginUser = null;
                rejected = true;
            }
        } catch (AuthenticationNetworkFailureException ex) {
            ex.printStackTrace();

            //Couldn't reach auth server- if we're running silently (we just started up and have a user session ready to roll)
            //Go ahead and just play offline automatically, like the minecraft client does.  If we're running loud (user
            //is actually at the login UI clicking the login button), give them a choice.
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                    "The auth servers at Minecraft.net are inaccessible.  Would you like to play offline?",
                    "Offline Play", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {

                //This is the last time we'll have access to the user's real username, so we should set the last-used
                //username now
                userModel.setLastUser(user);

                //Create offline user
                loginUser = new User(user.getDisplayName());
            } else {
                //Use clicked 'no', so just pull the ripcord and get back to the UI
                loginUser = null;
            }
        }

        if (loginUser == null) {
            //If we actually failed to validate, we should remove the user from the list of saved users
            //and refresh the user list
            if (rejected) {
                userModel.removeUser(user);
                refreshUsers();
                setCurrentUser(user.getUsername());
            }
        } else {
            //We have a cleared user, start the launcher up
            userModel.setCurrentUser(loginUser);
        }
    }

    private void attemptNewLogin(String name) {
        UserModel.AuthError error = userModel.attemptInitialLogin(name, new String(this.password.getPassword()));

        if (error != null) {
            JOptionPane.showMessageDialog(this, error.getErrorDescription(), error.getError(), JOptionPane.ERROR_MESSAGE);
        } else if (rememberAccount.isSelected()) {
            userModel.addUser(userModel.getCurrentUser());
        }
    }

    protected void clearCurrentUser() {
        password.setText("");
        password.setEditable(true);
        password.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        rememberAccount.setSelected(false);

        name.setText("");
        nameSelect.setSelectedItem("");
    }

    protected void setCurrentUser(User user) {
        if (user == null) {
            clearCurrentUser();
            return;
        }

        password.setText("PASSWORD");
        password.setEditable(false);
        password.setForeground(LauncherFrame.COLOR_SCROLL_THUMB);
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_SCROLL_THUMB, 1, 10));
        rememberAccount.setSelected(true);

        nameSelect.setSelectedItem(user);
    }

    protected void setCurrentUser(String user) {
        if (this.name.isVisible())
            this.name.setText(user);
        else
            this.nameSelect.setSelectedItem(user);

        password.setText("");
        password.setEditable(true);
        password.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        rememberAccount.setSelected(true);
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

        JLabel instructionText = new AAJLabel("<html><body align=\"center\">"+ resources.getString("login.instructions") +"</body></html>", JLabel.CENTER);
        instructionText.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        instructionText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(instructionText, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 3, 0, 3), 0, 0));

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
        termsLink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                visitTerms();
            }
        });
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
        privacyLink.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                visitPrivacy();
            }
        });
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

    @Override
    public void userChanged(User user) {
        if (user == null) {
            this.setVisible(true);
            refreshUsers();

            if (nameSelect.isVisible())
                nameSelect.grabFocus();
            else
                name.grabFocus();
        } else
            this.setVisible(false);
    }
}
