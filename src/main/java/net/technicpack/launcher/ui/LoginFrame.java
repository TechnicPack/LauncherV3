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
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.launchercore.exception.ResponseException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
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
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

public class LoginFrame extends DraggableFrame implements IRelocalizableResource, KeyListener, IAuthListener {
    private ResourceLoader resources;
    private ImageRepository<IUserType> skinRepository;
    private UserModel userModel;
    private TechnicSettings settings;

    private RoundedButton addMojang;
    private RoundedButton addMicrosoft;
    private RoundedButton login;
    private JTextField username;
    private JLabel selectLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JLabel addAccounts;
    private JLabel visitBrowser;
    private JComboBox<IUserType> nameSelect;
    private JCheckBox rememberAccount;
    private JPasswordField password;
    private JComboBox<LanguageItem> languages;

    private static final int FRAME_WIDTH = 347;
    private static final int FRAME_HEIGHT = 399;

    public LoginFrame(ResourceLoader resources, TechnicSettings settings, UserModel userModel, ImageRepository<IUserType> skinRepository) {
        this.skinRepository = skinRepository;
        this.userModel = userModel;
        this.settings = settings;

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(LauncherFrame.COLOR_CENTRAL_BACK_OPAQUE);

//        this.setFocusTraversalPolicy(new SortingFocusTraversalPolicy(new Comparator<Component>() {
//            @Override
//            public int compare(Component o1, Component o2) {
//                //This long stupid stack of else/ifs enforces a tab order of
//                //Username -> Password -> Remember me -> any buttons -> everything else who cares
//                if (o1 == name || o1 == nameSelect)
//                    return -1;
//                else if (o2 == name || o2 == nameSelect)
//                    return 1;
//                else if (o1 == password)
//                    return -1;
//                else if (o2 == password)
//                    return 1;
//                else if (o1 == rememberAccount)
//                    return -1;
//                else if (o2 == rememberAccount)
//                    return 1;
//                else if (o1 instanceof AbstractButton)
//                    return -1;
//                else if (o2 instanceof AbstractButton)
//                    return 1;
//                else
//                    return 0;
//            }
//        }));

        //Handles rebuilding the frame, so use it to build the frame in the first place
        relocalize(resources);

        setLocationRelativeTo(null);
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

        refreshSelectedUsers();

        EventQueue.invokeLater(() -> {
            invalidate();
            repaint();
        });
    }

    @Override
    public void userChanged(IUserType user) {
        if (user == null) {
            this.setVisible(true);
            refreshSelectedUsers();

            if (nameSelect.isVisible())
                nameSelect.grabFocus();
            else
                username.grabFocus();

            EventQueue.invokeLater(() -> {
                invalidate();
                repaint();
            });
        } else
            this.setVisible(false);
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
            login();
        }
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
        closeButton.addActionListener(e -> closeButtonClicked());
        closeButton.setFocusable(false);
        add(closeButton, new GridBagConstraints(2,0,1,1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(7,0,0,7),0,0));

        //Logo at the top
        JLabel platformImage = new JLabel();
        platformImage.setIcon(resources.getIcon("platform_logo.png"));
        add(platformImage, new GridBagConstraints(0,0,3,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(30,0,0,0),0,0));

        //Select account label
        selectLabel = new JLabel(resources.getString("login.select"));
        selectLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        selectLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(selectLabel, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,20,0,20), 0,0));

//        addAccounts = new JLabel(resources.getString("login.instructions"));
//        addAccounts.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
//        addAccounts.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
//        add(addAccounts, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
//
//        visitBrowser = new JLabel(resources.getString("login.checkbrowser"));
//        visitBrowser.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
//        visitBrowser.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
//        add(visitBrowser,  new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        // Setup account select box
        nameSelect = new JComboBox<>();

        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac")) {
            nameSelect.setUI(new MetalComboBoxUI());
        }

        nameSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        nameSelect.setEditable(true);
        nameSelect.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        nameSelect.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        nameSelect.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        nameSelect.setVisible(false);
        UserCellRenderer userRenderer = new UserCellRenderer(resources, this.skinRepository);
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
        nameSelect.addActionListener(e -> setCurrentUser((IUserType) nameSelect.getSelectedItem()));

        add(nameSelect, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 20, 0, 20), 4, 4));

        // Login button
        login = new RoundedButton(resources.getString("login.button"));
        login.setBorder(BorderFactory.createEmptyBorder(5,17,10,17));
        login.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        login.setContentAreaFilled(false);
        login.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        login.setHoverForeground(LauncherFrame.COLOR_BLUE);
        login.addActionListener(e -> login());
        add(login, new GridBagConstraints(0, 6, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(12,20,0,20),0,0));

        // Microsoft login button
        addMicrosoft = new RoundedButton(resources.getString("login.addmicrosoft"));
        addMicrosoft.setBorder(BorderFactory.createEmptyBorder(5,17,10,17));
        addMicrosoft.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        addMicrosoft.setContentAreaFilled(false);
        addMicrosoft.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        addMicrosoft.setHoverForeground(LauncherFrame.COLOR_BLUE);
        addMicrosoft.addActionListener(e -> addMicrosoftAccount());
        add(addMicrosoft, new GridBagConstraints(0, 7, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(50,20,0,20),0,0));

        // Add mojang account button
        addMojang = new RoundedButton(resources.getString("login.addmojang"));
        addMojang.setBorder(BorderFactory.createEmptyBorder(5,17,10,17));
        addMojang.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        addMojang.setContentAreaFilled(false);
        addMojang.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        addMojang.setHoverForeground(LauncherFrame.COLOR_BLUE);
        addMojang.addActionListener(e -> addMojangAccount());
        add(addMojang, new GridBagConstraints(0, 8, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(12,20,0,20),0,0));

        // Mojang username label
        usernameLabel = new JLabel(resources.getString("login.username"));
        usernameLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        usernameLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(usernameLabel, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,20,0,20), 0,0));

        // Mojang username field
        username = new JTextField();
        username.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        username.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        username.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        username.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        username.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        username.addKeyListener(this);
        add(username, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3,20,0,20),4,17));

        // Mojang password label
        passwordLabel = new JLabel(resources.getString("login.password"));
        passwordLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        passwordLabel.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        add(passwordLabel, new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12,20,0,20),0,0));

        // Setup password box
        password = new JPasswordField();
        password.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        password.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        password.setBackground(LauncherFrame.COLOR_FORMELEMENT_INTERNAL);
        password.addKeyListener(this);
        password.setEchoChar('*');
        password.addActionListener(e -> login());
        password.setCaretColor(LauncherFrame.COLOR_BUTTON_BLUE);
        add(password, new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 20, 0, 20), 4, 17));

        // Disable Mojang login elements
        usernameLabel.setVisible(false);
        username.setVisible(false);
        passwordLabel.setVisible(false);
        password.setVisible(false);

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
        rememberAccount.addActionListener(e -> toggleRemember());
        rememberAccount.setFocusPainted(false);
//        add(rememberAccount, new GridBagConstraints(1,6,2,1,1.0,0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(24,20,0,20),0,0));

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
        languages.addActionListener(e -> languageChanged());
        linkPane.add(languages);

        linkPane.add(Box.createHorizontalGlue());

        JButton termsLink = new JButton(resources.getString("login.terms"));
        termsLink.setContentAreaFilled(false);
        termsLink.setBorder(BorderFactory.createEmptyBorder());
        termsLink.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
        termsLink.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        termsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        termsLink.addActionListener(e -> visitTerms());
        linkPane.add(termsLink);
        linkPane.add(Box.createHorizontalStrut(8));

        add(linkPane, new GridBagConstraints(0, 9, 3, 1, 1.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }

    protected void closeButtonClicked() {
        System.exit(0);
    }

    protected void toggleRemember() {
        if (!rememberAccount.isSelected() && nameSelect.isVisible() && nameSelect.getSelectedItem() instanceof MojangUser) {
            forgetUser((IUserType)nameSelect.getSelectedItem());
        }
    }

    protected void visitTerms() {
        DesktopUtils.browseUrl("https://www.technicpack.net/terms");
    }

    protected void refreshSelectedUsers() {
        Collection<IUserType> users = userModel.getUsers();
        IUserType lastUser = userModel.getLastUser();

        if (users.size() == 0) {
            setAccountSelectVisibility(false);
            setAddAccountVisibility(true);
            login.setVisible(false);
            clearCurrentUser();
        } else {
            nameSelect.setVisible(true);
            nameSelect.removeAllItems();

            for (IUserType account : users) {
                nameSelect.addItem(account);
            }

            if (lastUser == null)
                lastUser = users.iterator().next();

            setCurrentUser(lastUser);
        }
    }

    protected void login() {
        if (username.isVisible()) {
            newMojangLogin(username.getText());
            return;
        }

        IUserType user = (IUserType) nameSelect.getSelectedItem();

        // Can't login with a null user. This should never be hit.
        if (user == null) {
            Utils.getLogger().log(Level.WARNING, "Attempted to login with a null user.");
            return;
        }

        login(user);
    }


    protected void addMojangAccount() {
        setMojangLoginVisibility(true);
        setAddAccountVisibility(false);
        setAccountSelectVisibility(false);
        login.setVisible(true);
    }

    protected void addMicrosoftAccount() {
        // Java version check for the Xbox auth certificate, which requires Java >= 8u91
        final String javaVersion = System.getProperty("java.version");
        final String[] javaVersionParts = javaVersion.split("[._-]");
        // 1.8.0_201-1-ojdkbuild
        // 1  8  0  201  1  ojdkbuild
        // 1.8.0_91
        // 1  8  0  91
        if (javaVersionParts.length >= 4 && javaVersion.startsWith("1.8.0_") && Integer.parseInt(javaVersionParts[3]) < 91) {
            Utils.getLogger().log(Level.SEVERE, "Aborting MSA login, Java version is too old");

            final String steps = "Your Java version is too old for MSA login, and needs to updated. Follow these steps:\n\n"+
                    "1) Close Technic Launcher. It should not be running!\n" +
                    "2) Open https://java.com/en/download/manual.jsp in your web browser\n" +
                    "3) Download the one that says Windows Offline (64-Bit)\n" +
                    "4) After the download is complete, you must install it. You can do so by double clicking on the icon to run it.\n" +
                    "5) After it installs start up Technic Launcher again and try to login.\n\n"+
                    "When you close this message, the https://java.com/en/download/manual.jsp page will open automatically.";

            JOptionPane.showMessageDialog(null, steps, "Java version too old for MSA login", ERROR_MESSAGE);

            DesktopUtils.browseUrl("https://java.com/en/download/manual.jsp");

            return;
        }

        newMicrosoftLogin();
    }

    protected void clearCurrentUser() {
        password.setText("");
        password.setEditable(true);
        password.setForeground(LauncherFrame.COLOR_BUTTON_BLUE);
        password.setBorder(new RoundBorder(LauncherFrame.COLOR_BUTTON_BLUE, 1, 10));
        rememberAccount.setSelected(true);

        username.setText("");
        nameSelect.setSelectedItem(null);
    }

    protected void setCurrentUser(IUserType user) {
        if (user == null) {
            clearCurrentUser();
            return;
        }

        setAccountSelectVisibility(true);
        setAddAccountVisibility(true);
        setMojangLoginVisibility(false);
        login.setVisible(true);
        rememberAccount.setSelected(true);
        nameSelect.setSelectedItem(user);
    }

    protected void forgetUser(IUserType mojangUser) {
        userModel.removeUser(mojangUser);
        refreshSelectedUsers();
    }

    protected void languageChanged() {
        String langCode = ((LanguageItem)languages.getSelectedItem()).getLangCode();
        settings.setLanguageCode(langCode);
        resources.setLocale(langCode);
    }

    private void setMojangLoginVisibility(boolean visible) {
        usernameLabel.setVisible(visible);
        username.setVisible(visible);
        passwordLabel.setVisible(visible);
        password.setVisible(visible);
    }

    private void setAccountSelectVisibility(boolean visible) {
        selectLabel.setVisible(visible);
        nameSelect.setVisible(visible);

    }

    private void setAddAccountVisibility(boolean visible) {
        addMojang.setVisible(visible);
        addMicrosoft.setVisible(visible);
    }

    private void login(IUserType user) {
        try {
            user.login(userModel);
            userModel.addUser(user);
            userModel.setCurrentUser(user);
            setCurrentUser(user);
        } catch (SessionException e) {
            showMessageDialog(
                    this, "Please log in again for user " + user.getDisplayName(),
                    e.getMessage(), ERROR_MESSAGE);
            forgetUser(user);
        } catch (ResponseException e) {
            showMessageDialog(
                    this, e.getMessage(), e.getError(), ERROR_MESSAGE);
            forgetUser(user);
        } catch (AuthenticationException e) {
            Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);

            //Couldn't reach auth server- if we're running silently (we just started up and have a user session ready to roll)
            //Go ahead and just play offline automatically, like the minecraft client does.  If we're running loud (user
            //is actually at the login UI clicking the login button), give them a choice.
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                    "The auth servers at Minecraft.net are inaccessible.  Would you like to play offline?",
                    "Offline Play", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {

                // This is the last time we'll have access to the user's real username,
                // so we should set the last-used username now
                userModel.setLastUser(user);
            }
        }
    }

    private void newMicrosoftLogin() {
        setAccountSelectVisibility(false);
        setAddAccountVisibility(false);
        // TODO: Setup info message + cancel flow

        try {
            MicrosoftUser microsoftUser = userModel.getMicrosoftAuthenticator().loginNewUser();
            userModel.addUser(microsoftUser);
            userModel.setCurrentUser(microsoftUser);
            setCurrentUser(microsoftUser);
        } catch (MicrosoftAuthException e) {
            switch (e.getType()) {
                case UNDERAGE:
                    showMessageDialog(this,
                            "Your Xbox account is underage and will need to be added to a Family to play this game.",
                            "Underage Error", ERROR_MESSAGE);
                    break;
                case NO_XBOX_ACCOUNT:
                    showMessageDialog(this,
                            "You don't have an Xbox account associated with this Microsoft account.\n" +
                            "Please login at minecraft.net and set up an Xbox account, then try to login here again.",
                            "No Xbox Account", ERROR_MESSAGE);
                    DesktopUtils.browseUrl("https://www.minecraft.net/login");
                    break;
                case NO_MINECRAFT:
                    showMessageDialog(this,
                            "This account has not purchased Minecraft Java Edition.", "No Minecraft", ERROR_MESSAGE);
                    break;
                default:
                    e.printStackTrace();
                    showMessageDialog(this, e.getMessage(), "Add Microsoft Account Failed", ERROR_MESSAGE);
            }
        } finally {
            setAccountSelectVisibility(!userModel.getUsers().isEmpty());
            setAddAccountVisibility(true);
        }
    }
    private void newMojangLogin(String name) {
        try {
            MojangUser newUser;
            newUser = userModel.getMojangAuthenticator().loginNewUser(name, new String(this.password.getPassword()));
            userModel.addUser(newUser);
            userModel.setCurrentUser(newUser);
            setCurrentUser(newUser);
        } catch(ResponseException e) {
            showMessageDialog(this, e.getMessage(), e.getError(), ERROR_MESSAGE);
        } catch (AuthenticationException e) {
            //TODO: What else is uncaught here?
            showMessageDialog(this, e.getMessage(), "Authentication error", ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
