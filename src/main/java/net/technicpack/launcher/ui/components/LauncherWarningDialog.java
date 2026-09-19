package net.technicpack.launcher.ui.components;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.lang.ResourceLoader;

/** Launcher-styled, per-attempt confirmation with cancellation as the default. */
final class LauncherWarningDialog {
  private LauncherWarningDialog() {}

  static boolean confirm(
      Component parent,
      ResourceLoader resources,
      String title,
      String message,
      String continueLabel,
      String technicalDetails)
      throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    AtomicBoolean cancelled = new AtomicBoolean();
    AtomicBoolean accepted = new AtomicBoolean();
    JDialog[] activeDialog = new JDialog[1];
    Runnable prompt =
        () -> {
          if (cancelled.get()) return;
          JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), title, true);
          activeDialog[0] = dialog;
          try {
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);
            dialog.setIconImage(resources.getImage("icon.png"));
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);
            content.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JLabel heading =
                new JLabel(title, resources.getIcon("warning_icon.png"), JLabel.LEADING);
            heading.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 24));
            heading.setForeground(UIConstants.COLOR_WHITE_TEXT);
            heading.setIconTextGap(12);
            heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
            content.add(heading, constraints);
            constraints.gridy++;
            content.add(textArea(resources, message), constraints);

            if (technicalDetails != null) {
              JTextArea details = textArea(resources, technicalDetails);
              details.setVisible(false);
              JButton disclosure =
                  button(resources, resources.getString("launcher.warning.showDetails"));
              disclosure.setDefaultCapable(false);
              JPanel disclosureRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
              disclosureRow.setOpaque(false);
              disclosureRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
              disclosureRow.add(disclosure);
              constraints.gridy++;
              content.add(disclosureRow, constraints);
              constraints.gridy++;
              constraints.insets = new Insets(12, 0, 0, 0);
              content.add(details, constraints);
              disclosure.addActionListener(
                  event -> {
                    boolean expanded = !details.isVisible();
                    details.setVisible(expanded);
                    disclosure.setText(
                        resources.getString(
                            expanded
                                ? "launcher.warning.hideDetails"
                                : "launcher.warning.showDetails"));
                    dialog.pack();
                    dialog.setLocationRelativeTo(parent);
                  });
            }

            JButton cancel = button(resources, resources.getString("launcher.pack.cancel"));
            JButton proceed = button(resources, continueLabel);
            cancel.addActionListener(event -> dialog.dispose());
            proceed.addActionListener(
                event -> {
                  if (!cancelled.get()) accepted.set(true);
                  dialog.dispose();
                });
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
            actions.setOpaque(false);
            actions.add(cancel);
            actions.add(proceed);
            constraints.gridy++;
            constraints.insets = new Insets(20, 0, 0, 0);
            content.add(actions, constraints);
            dialog.setContentPane(content);
            dialog.getRootPane().setDefaultButton(cancel);
            dialog
                .getRootPane()
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelWarning");
            dialog
                .getRootPane()
                .getActionMap()
                .put(
                    "cancelWarning",
                    new AbstractAction() {
                      @Override
                      public void actionPerformed(ActionEvent event) {
                        dialog.dispose();
                      }
                    });
            dialog.addWindowListener(
                new WindowAdapter() {
                  @Override
                  public void windowOpened(WindowEvent event) {
                    cancel.requestFocusInWindow();
                  }
                });
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            if (cancelled.get()) return;
            dialog.setVisible(true);
          } finally {
            dialog.dispose();
            activeDialog[0] = null;
          }
        };
    if (SwingUtilities.isEventDispatchThread()) {
      prompt.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(prompt);
      } catch (InterruptedException e) {
        cancelled.set(true);
        SwingUtilities.invokeLater(
            () -> {
              if (activeDialog[0] != null) activeDialog[0].dispose();
            });
        throw e;
      } catch (InvocationTargetException e) {
        throw new IllegalStateException("Could not display the launcher warning", e.getCause());
      }
    }
    return !cancelled.get() && accepted.get();
  }

  private static JTextArea textArea(ResourceLoader resources, String text) {
    JTextArea area = new JTextArea(text, 0, 44);
    area.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
    area.setForeground(UIConstants.COLOR_WHITE_TEXT);
    area.setSelectionColor(UIConstants.COLOR_BUTTON_BLUE);
    area.setSelectedTextColor(UIConstants.COLOR_HEADER_TEXT);
    area.setEditable(false);
    area.setOpaque(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setCaretPosition(0);
    area.setSize(area.getPreferredSize().width, Short.MAX_VALUE);
    return area;
  }

  private static RoundedButton button(ResourceLoader resources, String label) {
    RoundedButton button = new RoundedButton(label);
    button.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
    button.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 14));
    button.setContentAreaFilled(false);
    button.setForeground(UIConstants.COLOR_BUTTON_BLUE);
    button.setHoverForeground(UIConstants.COLOR_BLUE);
    button.setBackground(UIConstants.COLOR_BUTTON_BLUE);
    button.setHoverBackground(UIConstants.COLOR_BLUE);
    button.setClickBackground(UIConstants.COLOR_BLUE_DARKER);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    // RoundedButton paints itself, so explicitly preserve a visible keyboard-focus indicator.
    button.addFocusListener(
        new FocusAdapter() {
          @Override
          public void focusGained(FocusEvent event) {
            button.setShouldShowBackground(true);
            button.setForeground(UIConstants.COLOR_HEADER_TEXT);
            button.setHoverForeground(UIConstants.COLOR_HEADER_TEXT);
            button.repaint();
          }

          @Override
          public void focusLost(FocusEvent event) {
            button.setShouldShowBackground(false);
            button.setForeground(UIConstants.COLOR_BUTTON_BLUE);
            button.setHoverForeground(UIConstants.COLOR_BLUE);
            button.repaint();
          }
        });
    return button;
  }
}
