/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.ui;

import net.technicpack.launcher.LauncherMain;
import net.technicpack.ui.controls.list.popupformatters.RoundedBorderFormatter;
import net.technicpack.ui.controls.lang.LanguageCellRenderer;
import net.technicpack.ui.controls.lang.LanguageCellUI;
import net.technicpack.ui.controls.list.SimpleButtonComboUI;
import net.technicpack.ui.lang.IRelocalizableResource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.ui.controls.DraggableFrame;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.borders.RoundBorder;
import net.technicpack.ui.controls.login.*;
import net.technicpack.ui.listitems.LanguageItem;
import net.technicpack.launchercore.auth.IAuthListener;
import net.technicpack.launchercore.auth.IUserType;
import net.technicpack.minecraftcore.mojang.auth.MojangUser;
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.AuthenticationNetworkFailureException;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Level;

public class LoginFrame extends DraggableFrame implements IRelocalizableResource, KeyListener, IAuthListener<MojangUser> {
    private ResourceLoader resources;
    private ImageRepository<IUserType> skinRepository;
    private UserModel<MojangUser> userModel;
    private TechnicSettings settings;

    private JTextField name;
    private JComboBox nameSelect;
    private JCheckBox rememberAccount;
    private JPasswordField password;
    private JComboBox languages;

    private static final int FRAME_WIDTH = 347;
    private static final int FRAME_HEIGHT = 409;

    public LoginFrame(ResourceLoader resources, TechnicSettings settings, UserModel userModel, ImageRepository<IUserType> skinRepository) {
        this.skinRepository = skinRepository;
        this.userModel = userModel;
        this.settings = settings;

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

        setLocationRelativeTo(null);
    }

    protected void closeButtonClicked() {
        System.exit(0);
    }

    protected void changeUser() {
        if (nameSelect.getSelectedItem() == null || nameSelect.getSelectedItem().equals("")) {
            clearCurrentUser();
        } else if (nameSelect.getSelectedItem() instanceof MojangUser) {
            setCurrentUser((MojangUser)nameSelect.getSelectedItem());
        }
    }

    protected void toggleRemember() {
        if (!rememberAccount.isSelected() && nameSelect.isVisible() && nameSelect.getSelectedItem() instanceof MojangUser) {
            forgetUser((MojangUser)nameSelect.getSelectedItem());
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
        Collection<MojangUser> mojangUserAccounts = userModel.getUsers();
        MojangUser lastMojangUser = userModel.getLastUser();

        if (mojangUserAccounts.size() == 0) {
            name.setVisible(true);
            nameSelect.setVisible(false);
            clearCurrentUser();
        } else {
            name.setVisible(false);
            nameSelect.setVisible(true);
            nameSelect.removeAllItems();

            for (MojangUser account : mojangUserAccounts) {
                nameSelect.addItem(account);
            }

            nameSelect.addItem(null);

            if (lastMojangUser == null)
                lastMojangUser = mojangUserAccounts.iterator().next();

            setCurrentUser(lastMojangUser);
        }
    }

    protected void attemptLogin() {
        if (nameSelect.isVisible()) {
            Object selected = nameSelect.getSelectedItem();

            if (selected instanceof MojangUser) {
                verifyExistingLogin((MojangUser) selected);
            } else {
                String username = selected.toString();

                MojangUser mojangUser = userModel.getUser(username);

                if (mojangUser == null)
                    attemptNewLogin(username);
                else {
                    setCurrentUser(mojangUser);
                    verifyExistingLogin(mojangUser);
                }
            }
        } else {
            attemptNewLogin(name.getText());
        }
    }

    private void verifyExistingLogin(MojangUser mojangUser) {
        MojangUser loginMojangUser = mojangUser;
        boolean rejected = false;

        try {
            UserModel.AuthError error = userModel.attemptUserRefresh(mojangUser);

            if (error != null) {
                JOptionPane.showMessageDialog(this, error.getErrorDescription(), error.getError(), JOptionPane.ERROR_MESSAGE);
                loginMojangUser = null;
                rejected = true;
            }
        } catch (AuthenticationNetworkFailureException ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);

            //Couldn't reach auth server- if we're running silently (we just started up and have a user session ready to roll)
            //Go ahead and just play offline automatically, like the minecraft client does.  If we're running loud (user
            //is actually at the login UI clicking the login button), give them a choice.
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                    "The auth servers at Minecraft.net are inaccessible.  Would you like to play offline?",
                    "Offline Play", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {

                //This is the last time we'll have access to the user's real username, so we should set the last-used
                //username now
                userModel.setLastUser(mojangUser);

                //Create offline user
                loginMojangUser = new MojangUser(mojangUser.getDisplayName());
            } else {
                //Use clicked 'no', so just pull the ripcord and get back to the UI
                loginMojangUser = null;
            }
        }

        if (loginMojangUser == null) {
            //If we actually failed to validate, we should remove the user from the list of saved users
            //and refresh the user list
            if (rejected) {
                userModel.removeUser(mojangUser);
                refreshUsers();
                setCurrentUser(mojangUser.getUsername());
            }
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

    protected void setCurrentUser(MojangUser mojangUser) {
        if (mojangUser == null) {
            clearCurrentUser();
            return;
        }

        password.setText("PASSWORD");
        password.setEditable(false);
        password.setForeground(LauncherFrame.COLOR_SCROLL_THUMB);
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_SCROLL_THUMB, 1, 10));
        rememberAccount.setSelected(true);

        nameSelect.setSelectedItem(mojangUser);
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

    protected void forgetUser(MojangUser mojangUser) {
        userModel.removeUser(mojangUser);
        refreshUsers();
    }

    protected void languageChanged() {
        String langCode = ((LanguageItem)languages.getSelectedItem()).getLangCode();
        settings.setLanguageCode(langCode);
        resources.setLocale(langCode);
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

        JLabel instructionText = new JLabel("<html><body align=\"center\">"+ resources.getString("login.instructions") +"</body></html>", JLabel.CENTER);
        instructionText.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        instructionText.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(instructionText, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(9, 3, 0, 3), 0, 0));

        JLabel userLabel = new JLabel(resources.getString("login.username"));
        userLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        userLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(userLabel, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,20,0,20), 0,0));

        // Setup username box
        nameSelect = new JComboBox();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            nameSelect.setUI(new MetalComboBoxUI());
        }

        nameSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        nameSelect.setEditable(true);
        nameSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        nameSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        nameSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        nameSelect.setVisible(false);
        UserCellRenderer userRenderer= new UserCellRenderer(resources, this.skinRepository);
        userRenderer.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        userRenderer.setSelectedBackgroundColor(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        userRenderer.setSelectedForegroundColor(LauncherFrame.COLOR_BUTTON_BLUE);
        userRenderer.setUnselectedBackgroundColor(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);
        userRenderer.setUnselectedForegroundColor(LauncherFrame.COLOR_BUTTON_BLUE);
        nameSelect.setRenderer(userRenderer);
        UserCellEditor userEditor = new UserCellEditor(resources.getFont(ResourceLoader.FONT_OPENSANS, 16), this.skinRepository, LauncherFrame.COLOR_BUTTON_BLUE);
        nameSelect.setEditor(userEditor);
        userEditor.addKeyListener(this);
        nameSelect.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 0)), resources, LauncherFrame.COLOR_SCROLL_TRACK, LauncherFrame.COLOR_SCROLL_THUMB));
        nameSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeUser();
            }
        });

        add(nameSelect, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 20, 0, 20), 4, 4));

        name = new JTextField();
        name.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        name.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        name.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        name.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        name.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        name.addKeyListener(this);
        add(name, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3,20,0,20),4,17));

        JLabel passLabel = new JLabel(resources.getString("login.password"));
        passLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        passLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(passLabel, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12,20,0,20),0,0));

        // Setup password box
        password = new JPasswordField();
        password.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
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
        Font rememberFont = resources.getFont(ResourceLoader.FONT_OPENSANS, 14);
        rememberAccount = new JCheckBox("<html><body style=\"color:#D0D0D0\">"+resources.getString("login.remember")+"</body></html>", false);
        rememberAccount.setFont(rememberFont);
        rememberAccount.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        rememberAccount.setOpaque(false);
        rememberAccount.setHorizontalTextPosition(SwingConstants.LEFT);
        rememberAccount.setHorizontalAlignment(SwingConstants.RIGHT);
        rememberAccount.setBorder(BorderFactory.createEmptyBorder());
        rememberAccount.setIconTextGap(6);
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
        add(rememberAccount, new GridBagConstraints(1,6,2,1,1.0,0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(24,20,0,20),0,0));

        //Login button
        RoundedButton button = new RoundedButton(resources.getString("login.button"));
        button.setBorder(BorderFactory.createEmptyBorder(5,17,10,17));
        button.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
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
        linkPane.setLayout(new BoxLayout(linkPane, BoxLayout.LINE_AXIS));

        linkPane.add(Box.createHorizontalStrut(8));

        languages = new JComboBox();
        String defaultLocaleText = resources.getString("launcheroptions.language.default");
        if (!resources.isDefaultLocaleSupported()) {
            defaultLocaleText = defaultLocaleText.concat(" (" + resources.getString("launcheroptions.language.unavailable") + ")");
        }

        languages.addItem(new LanguageItem(ResourceLoader.DEFAULT_LOCALE, defaultLocaleText, resources));
        for (int i = 0; i < LauncherMain.supportedLanguages.length; i++) {
            languages.addItem(new LanguageItem(resources.getCodeFromLocale(LauncherMain.supportedLanguages[i]), LauncherMain.supportedLanguages[i].getDisplayName(LauncherMain.supportedLanguages[i]), resources.getVariant(LauncherMain.supportedLanguages[i])));
        }
        if (!settings.getLanguageCode().equalsIgnoreCase(ResourceLoader.DEFAULT_LOCALE)) {
            Locale loc = resources.getLocaleFromCode(settings.getLanguageCode());

            for (int i = 0; i < LauncherMain.supportedLanguages.length; i++) {
                if (loc.equals(LauncherMain.supportedLanguages[i])) {
                    languages.setSelectedIndex(i+1);
                    break;
                }
            }
        }
        languages.setBorder(BorderFactory.createEmptyBorder());
        languages.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        languages.setUI(new LanguageCellUI(resources, new RoundedBorderFormatter(new LineBorder(Color.black, 1)), LauncherFrame.COLOR_SCROLL_TRACK, LauncherFrame.COLOR_SCROLL_THUMB));
        languages.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        languages.setBackground(LauncherFrame.COLOR_SELECTOR_BACK);
        languages.setRenderer(new LanguageCellRenderer(resources, "globe.png", LauncherFrame.COLOR_SELECTOR_BACK, LauncherFrame.COLOR_WHITE_TEXT));
        languages.setEditable(false);
        languages.setFocusable(false);
        languages.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                languageChanged();
            }
        });
        linkPane.add(languages);

        linkPane.add(Box.createHorizontalGlue());

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
        linkPane.add(Box.createHorizontalStrut(8));

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

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                invalidate();
                repaint();
            }
        });
    }

    @Override
    public void userChanged(MojangUser mojangUser) {
        if (mojangUser == null) {
            this.setVisible(true);
            refreshUsers();

            if (nameSelect.isVisible())
                nameSelect.grabFocus();
            else
                name.grabFocus();

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    invalidate();
                    repaint();
                }
            });
        } else
            this.setVisible(false);
    }
}
