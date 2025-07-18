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

import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.launchercore.exception.ResponseException;
import net.technicpack.launchercore.exception.SessionException;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
import net.technicpack.ui.UIUtils;
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
import net.technicpack.launchercore.auth.UserModel;
import net.technicpack.launchercore.exception.AuthenticationException;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.JavaUtils;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.*;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

public class LoginFrame extends DraggableFrame implements IRelocalizableResource, IAuthListener {
    private transient ResourceLoader resources;
    private final transient ImageRepository<IUserType> skinRepository;
    private final transient UserModel userModel;
    private final transient TechnicSettings settings;

    private RoundedButton addMicrosoft;
    private RoundedButton cancelMsa;
    private RoundedButton login;
    private JLabel selectLabel;
    private JLabel visitBrowser;
    private JComboBox<IUserType> nameSelect;
    private JComboBox<LanguageItem> languages;
    private transient MsaLoginSwingWorker msaLoginSwingWorker;

    private static final int FRAME_WIDTH = 347;
    private static final int FRAME_HEIGHT = 399;

    public LoginFrame(ResourceLoader resources, TechnicSettings settings, UserModel userModel, ImageRepository<IUserType> skinRepository) {
        this.skinRepository = skinRepository;
        this.userModel = userModel;
        this.settings = settings;

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setTitle("Technic Launcher - Login");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

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

            EventQueue.invokeLater(() -> {
                invalidate();
                repaint();
            });
        } else
            this.setVisible(false);
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
        selectLabel.setForeground(UIConstants.COLOR_WHITE_TEXT);
        add(selectLabel, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10,20,0,20), 0,0));

//        visitBrowser = new JLabel(resources.getString("login.checkbrowser"));
//        visitBrowser.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
//        visitBrowser.setForeground(LauncherFrame.COLOR_WHITE_TEXT);
//        add(visitBrowser,  new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        // Setup account select box
        nameSelect = new JComboBox<>();

        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) {
            nameSelect.setUI(new MetalComboBoxUI());
        }

        nameSelect.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        nameSelect.setEditable(true);
        nameSelect.setBorder(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 10));
        nameSelect.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        nameSelect.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        nameSelect.setVisible(false);
        UserCellRenderer userRenderer = new UserCellRenderer(resources, this.skinRepository);
        userRenderer.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        userRenderer.setSelectedBackgroundColor(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
        userRenderer.setSelectedForegroundColor(UIConstants.COLOR_BUTTON_BLUE);
        userRenderer.setUnselectedBackgroundColor(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
        userRenderer.setUnselectedForegroundColor(UIConstants.COLOR_BUTTON_BLUE);
        nameSelect.setRenderer(userRenderer);
        UserCellEditor userEditor = new UserCellEditor(resources.getFont(ResourceLoader.FONT_OPENSANS, 16), this.skinRepository, UIConstants.COLOR_BUTTON_BLUE);
        nameSelect.setEditor(userEditor);
        nameSelect.setUI(new SimpleButtonComboUI(new RoundedBorderFormatter(new RoundBorder(UIConstants.COLOR_BUTTON_BLUE, 1, 0)), resources, UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        nameSelect.addActionListener(e -> setCurrentUser((IUserType) nameSelect.getSelectedItem()));

        add(nameSelect, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 20, 0, 20), 4, 4));

        // Login button
        login = new RoundedButton(resources.getString("login.button"));
        login.setBorder(BorderFactory.createEmptyBorder(5,17,10,17));
        login.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 18));
        login.setContentAreaFilled(false);
        login.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        login.setHoverForeground(UIConstants.COLOR_BLUE);
        login.addActionListener(e -> login());
        add(login, new GridBagConstraints(0, 6, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(12,20,0,20),0,0));

        // Microsoft login button
        addMicrosoft = new RoundedButton(resources.getString("login.addmicrosoft"));
        addMicrosoft.setBorder(BorderFactory.createEmptyBorder(5,17,10,17));
        addMicrosoft.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        addMicrosoft.setContentAreaFilled(false);
        addMicrosoft.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        addMicrosoft.setHoverForeground(UIConstants.COLOR_BLUE);
        addMicrosoft.addActionListener(e -> addMicrosoftAccount());
        add(addMicrosoft, new GridBagConstraints(0, 7, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(50,20,0,20),0,0));

        // Microsoft cancel button
        cancelMsa = new RoundedButton(resources.getString("login.cancel"));
        cancelMsa.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        cancelMsa.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        cancelMsa.setContentAreaFilled(false);
        cancelMsa.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        cancelMsa.setHoverForeground(UIConstants.COLOR_BLUE);
        cancelMsa.setVisible(false);
        cancelMsa.addActionListener(e -> cancelMsaLogin());
        add(cancelMsa, new GridBagConstraints(0, 8, GridBagConstraints.REMAINDER, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(50,20,0,20),0,0));

        add(Box.createVerticalGlue(), new GridBagConstraints(0, 8, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0));

        JPanel linkPane = new JPanel();
        linkPane.setBackground(UIConstants.COLOR_SELECTOR_BACK);
        linkPane.setBorder(BorderFactory.createEmptyBorder(7,0,7,0));
        linkPane.setLayout(new BoxLayout(linkPane, BoxLayout.LINE_AXIS));

        linkPane.add(Box.createHorizontalStrut(8));

        languages = new JComboBox<>();
        String defaultLocaleText = resources.getString("launcheroptions.language.default");
        if (!resources.isDefaultLocaleSupported()) {
            defaultLocaleText = defaultLocaleText.concat(" (" + resources.getString("launcheroptions.language.unavailable") + ")");
        }

        UIUtils.populateLanguageSelector(defaultLocaleText, languages, resources, settings);
        languages.setBorder(BorderFactory.createEmptyBorder());
        languages.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
        languages.setUI(new LanguageCellUI(new RoundedBorderFormatter(new LineBorder(Color.black, 1)), UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        languages.setForeground(UIConstants.COLOR_WHITE_TEXT);
        languages.setBackground(UIConstants.COLOR_SELECTOR_BACK);
        languages.setRenderer(new LanguageCellRenderer(resources, "globe.png", UIConstants.COLOR_SELECTOR_BACK, UIConstants.COLOR_WHITE_TEXT));
        languages.setEditable(false);
        languages.setFocusable(false);
        languages.addActionListener(e -> languageChanged());
        linkPane.add(languages);

        linkPane.add(Box.createHorizontalGlue());

        JButton termsLink = new JButton(resources.getString("login.terms"));
        termsLink.setContentAreaFilled(false);
        termsLink.setBorder(BorderFactory.createEmptyBorder());
        termsLink.setForeground(UIConstants.COLOR_WHITE_TEXT);
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

    protected void visitTerms() {
        DesktopUtils.browseUrl("https://www.technicpack.net/terms");
    }

    protected void refreshSelectedUsers() {
        Collection<IUserType> users = userModel.getUsers();
        IUserType lastUser = userModel.getLastUser();

        if (users.isEmpty()) {
            setAccountSelectVisibility(false);
            setAddAccountVisibility(true);
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
        IUserType user = (IUserType) nameSelect.getSelectedItem();

        // Can't log in with a null user. This should never be hit.
        if (user == null) {
            Utils.getLogger().log(Level.WARNING, "Attempted to login with a null user.");
            return;
        }

        login(user);
    }


    protected void addMicrosoftAccount() {
        // Java version check for the Xbox auth certificate, which requires Java >= 8u91
        final String javaVersion = System.getProperty("java.version");
        if (JavaUtils.compareVersions(javaVersion, "1.8.0_91") < 0) {
            Utils.getLogger().log(Level.SEVERE,
                    String.format("Aborting MSA login, Java version is too old (%s)", javaVersion));

            JOptionPane.showMessageDialog(
                    this,
                    resources.getString("msa.update.steps"),
                    resources.getString("msa.update.title"),
                    ERROR_MESSAGE
            );

            DesktopUtils.browseUrl(TechnicConstants.JAVA_DOWNLOAD_URL);

            return;
        }

        newMicrosoftLogin();
    }

    protected void cancelMsaLogin() {
        if (msaLoginSwingWorker != null && !msaLoginSwingWorker.isDone()) {
            userModel.getMicrosoftAuthenticator().stopReceiver();
        }
    }

    protected void clearCurrentUser() {
        nameSelect.setSelectedItem(null);
    }

    protected void setCurrentUser(IUserType user) {
        if (user == null) {
            clearCurrentUser();
            return;
        }

        setAccountSelectVisibility(true);
        setAddAccountVisibility(true);
        nameSelect.setSelectedItem(user);
    }

    protected void forgetUser(IUserType user) {
        userModel.removeUser(user);
        refreshSelectedUsers();
    }

    protected void languageChanged() {
        String langCode = ((LanguageItem)languages.getSelectedItem()).getLangCode();
        settings.setLanguageCode(langCode);
        resources.setLocale(langCode);
    }

    private void setAccountSelectVisibility(boolean visible) {
        selectLabel.setVisible(visible);
        nameSelect.setVisible(visible);
        login.setVisible(visible);
    }

    private void setAddAccountVisibility(boolean visible) {
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

            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                    "The auth servers are inaccessible. Would you like to play in offline mode?",
                    "Offline mode", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
                userModel.setLastUser(user);
                userModel.setCurrentUser(new MicrosoftUser(user.getId(), user.getUsername()));
            }
        }
    }

    private class MsaLoginSwingWorker extends SwingWorker<Void, Void> {
        private final JFrame parent;

        public MsaLoginSwingWorker(JFrame parent) {
            this.parent = parent;
        }

        @Override
        protected Void doInBackground() {
            try {
                MicrosoftUser microsoftUser = userModel.getMicrosoftAuthenticator().loginNewUser();
                userModel.addUser(microsoftUser);
                userModel.setCurrentUser(microsoftUser);
                setCurrentUser(microsoftUser);
            } catch (MicrosoftAuthException e) {
                Utils.getLogger().log(Level.SEVERE, e.getMessage(), e);
                switch (e.getType()) {
                    case UNDERAGE:
                        showMessageDialog(parent,
                                "Your Xbox account is underage and will need to be added to a Family to play this game.",
                                "Underage Error", ERROR_MESSAGE);
                        break;
                    case NO_XBOX_ACCOUNT:
                        showMessageDialog(parent,
                                "You don't have an Xbox account associated with this Microsoft account.\n" +
                                        "Please login at minecraft.net and set up an Xbox account, then try to login here again.",
                                "No Xbox Account", ERROR_MESSAGE);
                        DesktopUtils.browseUrl("https://www.minecraft.net/login");
                        break;
                    case NO_PROFILE:
                        showMessageDialog(parent,
                                "You don't have a Minecraft profile set up yet.\nPlease open the Minecraft Launcher " +
                                        "or go to minecraft.net and set up a Minecraft profile before attempting to " +
                                        "use Technic Launcher.", "No Minecraft Profile", ERROR_MESSAGE);
                        DesktopUtils.browseUrl("https://www.minecraft.net/msaprofile/mygames/editprofile");
                        break;
                    case NO_MINECRAFT:
                        showMessageDialog(parent,
                                "This account has not purchased Minecraft Java Edition.", "No Minecraft", ERROR_MESSAGE);
                        break;
                    case DNS:
                        showMessageDialog(parent,
                                "DNS failure: " + e.getMessage(), "DNS failure during authentication", ERROR_MESSAGE);
                        break;
                    default:
                        showMessageDialog(parent, e.getMessage(), "Add Microsoft Account Failed", ERROR_MESSAGE);
                }
            }

            return null;
        }

        @Override
        protected void done() {
            cancelMsa.setVisible(false);
            setAccountSelectVisibility(!userModel.getUsers().isEmpty());
            setAddAccountVisibility(true);
        }
    }

    private void newMicrosoftLogin() {
        setAccountSelectVisibility(false);
        setAddAccountVisibility(false);
        // TODO: Setup info message
        cancelMsa.setVisible(true);

        if (msaLoginSwingWorker == null || msaLoginSwingWorker.isDone()) {
            msaLoginSwingWorker = new MsaLoginSwingWorker(this);
            msaLoginSwingWorker.execute();
        }
    }
}
