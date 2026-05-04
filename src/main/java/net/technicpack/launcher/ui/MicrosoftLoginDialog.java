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

import io.nayuki.qrcodegen.QrCode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import net.technicpack.launchercore.exception.MicrosoftAuthException;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftAuthenticator;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftAuthenticator.DeviceCodeChallenge;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.DesktopUtils;
import net.technicpack.utilslib.Utils;

/**
 * Modal "Add a Microsoft account" dialog that offers both sign-in flows simultaneously: the
 * browser-redirect localhost callback, and Microsoft's device code flow. The first flow to complete
 * successfully wins; the other is cancelled.
 *
 * <p>Both flows run on separate {@link SwingWorker} threads. The dialog pre-fetches a device code
 * as soon as it opens (so the user sees it immediately) and starts polling in the background. The
 * browser-based flow runs only when the user clicks the top button.
 */
public class MicrosoftLoginDialog extends JDialog {

  private final transient MicrosoftAuthenticator authenticator;
  private final ResourceLoader resources;

  /** Result for the caller. Null if the user cancelled or all flows errored out. */
  private final AtomicReference<MicrosoftUser> result = new AtomicReference<>();

  /**
   * Terminal auth exception that closed the dialog, if any. The caller can read this after {@link
   * #awaitResult()} returns null to decide whether to show a follow-up error dialog (UNDERAGE,
   * NO_MINECRAFT, etc.) or just quietly restore the UI (plain cancellation).
   */
  private final AtomicReference<MicrosoftAuthException> terminalException = new AtomicReference<>();

  /**
   * Set to true by either flow completing (success or failure) or by the user cancelling. All flows
   * must check this between steps and abort if set.
   */
  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  private final AtomicReference<SwingWorker<MicrosoftUser, Void>> localServerFlow =
      new AtomicReference<>();
  private final AtomicReference<SwingWorker<MicrosoftUser, Void>> deviceCodeFlow =
      new AtomicReference<>();

  private RoundedButton signInWithBrowserButton;
  private JLabel qrLabel;
  private JTextField deviceCodeField;
  private RoundedButton copyCodeButton;
  private RoundedButton openBrowserButton;
  private RoundedButton getNewCodeButton;
  private JProgressBar expiryBar;
  private JLabel statusLabel;
  private RoundedButton cancelButton;

  /** True while the current device code has expired and the user hasn't requested a new one. */
  private volatile boolean deviceCodeExpired = false;

  /** Unix-millis deadline by which the device code expires, set when the challenge arrives. */
  private volatile long expiryDeadlineMillis;

  /** Swing timer that ticks every second to update the progress bar and countdown text. */
  private Timer expiryTimer;

  /** Filled in when the device-code request succeeds. Null until then. */
  private volatile DeviceCodeChallenge challenge;

  public MicrosoftLoginDialog(
      Frame owner, MicrosoftAuthenticator authenticator, ResourceLoader resources) {
    // Null owner + APPLICATION_MODAL: KWin (and most WMs) hide windows with WM_TRANSIENT_FOR
    // set from the taskbar, so passing a non-null owner to JDialog's super constructor makes
    // this dialog unreachable via the KDE task manager. A null owner drops the transient-for
    // hint while APPLICATION_MODAL preserves the "block the rest of the app" behaviour.
    // The owner Frame is still used below for setLocationRelativeTo() positioning only.
    super(null, resources.getString("msa.dialog.title"), Dialog.ModalityType.APPLICATION_MODAL);
    this.authenticator = authenticator;
    this.resources = resources;

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    // Type.NORMAL helps on GNOME/Mutter, which classifies taskbar entries by _NET_WM_WINDOW_TYPE.
    setType(Window.Type.NORMAL);
    Image icon = resources.getImage("icon.png");
    if (icon != null) {
      setIconImage(icon);
    }
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            cancelAndClose();
          }
        });
    buildUi();
    pack();
    setLocationRelativeTo(owner);

    startDeviceCodeFlow();
  }

  /**
   * Opens the dialog and returns the resulting user (or null if the dialog was cancelled or every
   * flow errored out). Blocks until the dialog is dismissed.
   */
  public MicrosoftUser awaitResult() {
    setVisible(true);
    return result.get();
  }

  /**
   * Returns the terminal auth exception that closed the dialog, or null if the dialog was closed by
   * plain cancellation (or the result was a success).
   */
  public MicrosoftAuthException getTerminalException() {
    return terminalException.get();
  }

  // --- UI construction ----------------------------------------------------------------------

  private void buildUi() {
    JPanel content = new JPanel(new GridBagLayout());
    content.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
    content.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(0, 0, 12, 0);

    signInWithBrowserButton = styledButton(resources.getString("msa.dialog.browser"), 16);
    signInWithBrowserButton.addActionListener(e -> startLocalServerFlow());
    content.add(signInWithBrowserButton, gbc);

    gbc.gridy++;
    content.add(buildOrDivider(), gbc);

    gbc.gridy++;
    JLabel instructions = new JLabel(resources.getString("msa.dialog.instructions"));
    instructions.setHorizontalAlignment(SwingConstants.CENTER);
    instructions.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 13));
    instructions.setForeground(UIConstants.COLOR_WHITE_TEXT);
    // The entire label is clickable (opens the verification URL). We can't trivially
    // restrict the click region to just the anchor text inside a JLabel's HTML renderer,
    // so the full label reacts to the cursor hand and the click. In practice this matches
    // what users expect when the text clearly signals a link.
    instructions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    instructions.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openVerificationUrl();
          }
        });
    content.add(instructions, gbc);

    gbc.gridy++;
    qrLabel = new JLabel();
    qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
    qrLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    content.add(qrLabel, gbc);

    gbc.gridy++;
    deviceCodeField = new JTextField(resources.getString("msa.dialog.fetching"));
    deviceCodeField.setEditable(false);
    deviceCodeField.setHorizontalAlignment(JTextField.CENTER);
    deviceCodeField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
    deviceCodeField.setForeground(UIConstants.COLOR_HEADER_TEXT);
    deviceCodeField.setCaretColor(UIConstants.COLOR_HEADER_TEXT);
    deviceCodeField.setOpaque(false);
    deviceCodeField.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
    deviceCodeField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    content.add(deviceCodeField, gbc);

    gbc.gridy++;
    expiryBar = new JProgressBar(0, 1);
    expiryBar.setValue(0);
    expiryBar.setVisible(false);
    expiryBar.setForeground(UIConstants.COLOR_BUTTON_BLUE);
    expiryBar.setBackground(UIConstants.COLOR_FORM_ELEMENT_INTERNAL);
    expiryBar.setBorderPainted(false);
    expiryBar.setPreferredSize(new Dimension(100, 4));
    content.add(expiryBar, gbc);

    gbc.gridy++;
    JPanel codeButtons = new JPanel();
    codeButtons.setOpaque(false);
    copyCodeButton = styledButton(resources.getString("msa.dialog.copy"), 13);
    copyCodeButton.setEnabled(false);
    copyCodeButton.addActionListener(e -> copyCodeToClipboard());
    openBrowserButton = styledButton(resources.getString("msa.dialog.open"), 13);
    openBrowserButton.setEnabled(false);
    openBrowserButton.addActionListener(e -> openVerificationUrl());
    getNewCodeButton = styledButton(resources.getString("msa.dialog.getnewcode"), 13);
    getNewCodeButton.setVisible(false);
    getNewCodeButton.addActionListener(e -> requestNewDeviceCode());
    codeButtons.add(copyCodeButton);
    codeButtons.add(openBrowserButton);
    codeButtons.add(getNewCodeButton);
    content.add(codeButtons, gbc);

    gbc.gridy++;
    statusLabel = new JLabel(" ");
    statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    statusLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
    statusLabel.setForeground(UIConstants.COLOR_DIM_TEXT);
    content.add(statusLabel, gbc);

    gbc.gridy++;
    gbc.insets = new Insets(16, 0, 0, 0);
    JPanel cancelRow = new JPanel(new BorderLayout());
    cancelRow.setOpaque(false);
    cancelButton = styledButton(resources.getString("msa.dialog.cancel"), 13);
    cancelButton.addActionListener(e -> cancelAndClose());
    cancelRow.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    cancelRow.add(cancelButton, BorderLayout.EAST);
    content.add(cancelRow, gbc);

    setContentPane(content);
    setMinimumSize(new Dimension(440, 0));
  }

  private JPanel buildOrDivider() {
    JPanel row = new JPanel();
    row.setOpaque(false);
    row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

    row.add(buildHorizontalRule());

    JLabel orLabel = new JLabel(resources.getString("msa.dialog.or"));
    orLabel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 12));
    orLabel.setForeground(UIConstants.COLOR_DIM_TEXT);
    orLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    row.add(orLabel);

    row.add(buildHorizontalRule());

    return row;
  }

  private JSeparator buildHorizontalRule() {
    JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
    sep.setForeground(UIConstants.COLOR_GREY_TEXT);
    sep.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
    return sep;
  }

  private RoundedButton styledButton(String label, int fontSize) {
    RoundedButton button = new RoundedButton(label);
    button.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
    button.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, fontSize));
    button.setContentAreaFilled(false);
    button.setForeground(UIConstants.COLOR_BUTTON_BLUE);
    button.setHoverForeground(UIConstants.COLOR_BLUE);
    return button;
  }

  // --- Device code flow ---------------------------------------------------------------------

  private void startDeviceCodeFlow() {
    SwingWorker<MicrosoftUser, Void> worker =
        new SwingWorker<MicrosoftUser, Void>() {
          @Override
          protected MicrosoftUser doInBackground() throws Exception {
            BooleanSupplier cancelCheck = cancelled::get;
            return authenticator.loginNewUserViaDeviceCode(
                MicrosoftLoginDialog.this::onChallengeReady, cancelCheck);
          }

          @Override
          protected void done() {
            handleFlowCompletion(this, "device code");
          }
        };
    deviceCodeFlow.set(worker);
    worker.execute();
  }

  private void onChallengeReady(DeviceCodeChallenge c) {
    if (cancelled.get()) {
      return;
    }
    this.challenge = c;
    this.expiryDeadlineMillis = System.currentTimeMillis() + (c.expiresInSeconds * 1000L);
    BufferedImage qrImage = renderQrCode(c.verificationUri, 160);
    SwingUtilities.invokeLater(
        () -> {
          deviceCodeField.setText(c.userCode);
          copyCodeButton.setEnabled(true);
          openBrowserButton.setEnabled(true);
          deviceCodeExpired = false;
          getNewCodeButton.setVisible(false);
          if (qrImage != null) {
            qrLabel.setIcon(new ImageIcon(qrImage));
          }
          expiryBar.setMinimum(0);
          expiryBar.setMaximum((int) c.expiresInSeconds);
          expiryBar.setValue((int) c.expiresInSeconds);
          expiryBar.setVisible(true);
          startExpiryTimer();
          updateExpiryCountdown();
          pack();
        });
  }

  /**
   * Render {@code text} as a QR code into a roughly {@code targetPixels}x{@code targetPixels}
   * image, with a white background and black modules plus a 4-module quiet zone (per the QR spec).
   * Returns null if encoding fails (e.g. the text is longer than any QR version can hold), in which
   * case the caller silently skips the QR display.
   */
  static BufferedImage renderQrCode(String text, int targetPixels) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    QrCode qr;
    try {
      qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
    } catch (RuntimeException encodeFailed) {
      // encodeText throws DataTooLongException (a RuntimeException subclass) when the text
      // exceeds the capacity of the largest QR version at the chosen ecc level. Swallow and
      // return null so the caller falls back to "no QR, just the code text".
      return null;
    }
    int modules = qr.size;
    int quietZone = 4;
    int totalModules = modules + 2 * quietZone;
    int scale = Math.max(1, targetPixels / totalModules);
    int pixelSize = totalModules * scale;
    BufferedImage img = new BufferedImage(pixelSize, pixelSize, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, pixelSize, pixelSize);
      g.setColor(Color.BLACK);
      for (int y = 0; y < modules; y++) {
        for (int x = 0; x < modules; x++) {
          if (qr.getModule(x, y)) {
            g.fillRect((x + quietZone) * scale, (y + quietZone) * scale, scale, scale);
          }
        }
      }
    } finally {
      g.dispose();
    }
    return img;
  }

  private void startExpiryTimer() {
    if (expiryTimer != null) {
      expiryTimer.stop();
    }
    expiryTimer = new Timer(1000, e -> updateExpiryCountdown());
    expiryTimer.setRepeats(true);
    expiryTimer.start();
  }

  private void updateExpiryCountdown() {
    long remainingMillis = expiryDeadlineMillis - System.currentTimeMillis();
    if (remainingMillis <= 0) {
      expiryBar.setValue(0);
      statusLabel.setText(resources.getString("msa.dialog.expired"));
      if (expiryTimer != null) {
        expiryTimer.stop();
      }
      deviceCodeExpired = true;
      copyCodeButton.setEnabled(false);
      openBrowserButton.setEnabled(false);
      getNewCodeButton.setVisible(true);
      return;
    }
    int remainingSeconds = (int) Math.ceil(remainingMillis / 1000.0);
    expiryBar.setValue(remainingSeconds);
    int minutes = remainingSeconds / 60;
    int seconds = remainingSeconds % 60;
    statusLabel.setText(
        resources.getString(
            "msa.dialog.waiting.countdown",
            String.valueOf(minutes),
            String.format("%02d", seconds)));
  }

  private void copyCodeToClipboard() {
    if (challenge == null) {
      return;
    }
    Toolkit.getDefaultToolkit()
        .getSystemClipboard()
        .setContents(new StringSelection(challenge.userCode), null);
    statusLabel.setText(resources.getString("msa.dialog.copied"));
  }

  private void openVerificationUrl() {
    if (challenge == null) {
      return;
    }
    DesktopUtils.browseUrl(challenge.verificationUri);
  }

  private void requestNewDeviceCode() {
    SwingWorker<MicrosoftUser, Void> old = deviceCodeFlow.get();
    if (old != null) {
      old.cancel(true);
    }
    deviceCodeField.setText(resources.getString("msa.dialog.fetching"));
    qrLabel.setIcon(null);
    expiryBar.setVisible(false);
    copyCodeButton.setEnabled(false);
    openBrowserButton.setEnabled(false);
    getNewCodeButton.setVisible(false);
    statusLabel.setText(" ");
    startDeviceCodeFlow();
  }

  // --- Localhost flow -----------------------------------------------------------------------

  private void startLocalServerFlow() {
    signInWithBrowserButton.setEnabled(false);
    statusLabel.setText(resources.getString("msa.dialog.browser.waiting"));

    SwingWorker<MicrosoftUser, Void> worker =
        new SwingWorker<MicrosoftUser, Void>() {
          @Override
          protected MicrosoftUser doInBackground() throws Exception {
            return authenticator.loginNewUser();
          }

          @Override
          protected void done() {
            handleFlowCompletion(this, "browser");
          }
        };
    localServerFlow.set(worker);
    worker.execute();
  }

  // --- Shared completion / cancellation -----------------------------------------------------

  private void handleFlowCompletion(SwingWorker<MicrosoftUser, Void> flow, String label) {
    if (cancelled.get()) {
      // Already closed by the other flow or by the user; ignore this one's outcome.
      return;
    }
    try {
      MicrosoftUser user = flow.get();
      if (user == null) {
        return;
      }
      if (result.compareAndSet(null, user)) {
        // We won the race; tear down and close.
        cancelled.set(true);
        cancelOtherFlows();
        SwingUtilities.invokeLater(this::dispose);
      }
    } catch (Exception e) {
      if (flow.isCancelled()) {
        // Worker was cancelled via SwingWorker.cancel(true). Happens when the user clicks
        // "Get a new code" (device-code flow) or in paths where cancelOtherFlows runs without
        // cancelled.get() being set yet. Not an error; nothing to log or surface.
        return;
      }
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      Utils.getLogger().log(Level.WARNING, label + " sign-in flow failed", cause);
      // Don't tear the dialog down on a single-flow failure; let the other flow keep running.
      // Keep the message in English for now (matches msa.update error strings elsewhere).
      if (cause instanceof MicrosoftAuthException) {
        SwingUtilities.invokeLater(
            () -> handleAuthException((MicrosoftAuthException) cause, label, flow));
      } else {
        SwingUtilities.invokeLater(
            () -> statusLabel.setText(label + " sign-in failed: " + cause.getMessage()));
      }
      // If the failed flow was the browser one, re-enable its button so the user can retry.
      if ("browser".equals(label)) {
        SwingUtilities.invokeLater(() -> signInWithBrowserButton.setEnabled(true));
      }
    }
  }

  private void handleAuthException(
      MicrosoftAuthException e, String label, SwingWorker<MicrosoftUser, Void> flow) {
    // A previous device-code worker (cancelled by "Get a new code") can finish late and
    // route its interruption/expiry here after a fresh worker has already taken over.
    // Ignore those stale reports so they don't overwrite the new worker's UI state.
    if ("device code".equals(label) && deviceCodeFlow.get() != flow) {
      return;
    }
    // The terminal error classes (NO_MINECRAFT, UNDERAGE, etc.) should tear down the dialog;
    // transient ones (OAUTH polling failure, REQUEST) let the user try the other flow.
    switch (e.getType()) {
      case NO_MINECRAFT:
      case UNDERAGE:
      case NO_XBOX_ACCOUNT:
      case NO_PROFILE:
        // Terminal: store for caller, cancel everything, close. LoginFrame decides the UX.
        terminalException.set(e);
        cancelled.set(true);
        cancelOtherFlows();
        dispose();
        break;
      default:
        // If the UI timer already surfaced the localized "expired" message for this flow,
        // don't overwrite it with the worker's English exception text.
        if ("device code".equals(label)
            && deviceCodeExpired
            && e.getType() == MicrosoftAuthException.ExceptionType.OAUTH) {
          return;
        }
        // Transient: keep dialog open so the user can try the other flow or cancel.
        statusLabel.setText(label + ": " + e.getMessage());
    }
  }

  private void cancelAndClose() {
    cancelled.set(true);
    cancelOtherFlows();
    dispose();
  }

  private void cancelOtherFlows() {
    // Device-code polling respects the cancelled AtomicBoolean; localhost needs stopReceiver.
    try {
      authenticator.stopReceiver();
    } catch (Exception ignored) {
      // stopReceiver has never thrown historically, but be defensive on dialog teardown.
    }
    SwingWorker<MicrosoftUser, Void> d = deviceCodeFlow.get();
    if (d != null) {
      d.cancel(true);
    }
    SwingWorker<MicrosoftUser, Void> l = localServerFlow.get();
    if (l != null) {
      l.cancel(true);
    }
    if (expiryTimer != null) {
      expiryTimer.stop();
    }
  }
}
