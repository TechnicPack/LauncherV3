package net.technicpack.launcher.ui.components;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.technicpack.ui.lang.ResourceLoader;

/** A per-installation warning only: no launcher settings are changed. */
public final class ProcessorHashWarningDialog {
  public enum Result {
    CANCEL,
    DISABLE_CHECKS
  }

  private ProcessorHashWarningDialog() {}

  public static Result show(Component parent, ResourceLoader resources, String javaExecutable)
      throws InterruptedException {
    if (Thread.interrupted()) throw new InterruptedException();
    AtomicBoolean cancelled = new AtomicBoolean();
    AtomicReference<Result> result = new AtomicReference<>(Result.CANCEL);
    JDialog[] activeDialog = new JDialog[1];
    Runnable prompt =
        () -> {
          if (cancelled.get()) return;
          JTextArea message = textArea(resources.getString("launcher.processorhashwarning"));
          JTextArea details =
              textArea(
                  resources.getString("launcher.processorhashwarning.details", javaExecutable));
          details.setVisible(false);
          JButton disclosure =
              new JButton(resources.getString("launcher.processorhashwarning.showDetails"));
          disclosure.setDefaultCapable(false);
          JPanel disclosureRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
          disclosureRow.setOpaque(false);
          disclosureRow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
          disclosureRow.add(disclosure);

          JButton cancel = new JButton(resources.getString("launcher.pack.cancel"));
          JButton disable =
              new JButton(resources.getString("launcher.processorhashwarning.disableChecks"));
          JOptionPane pane =
              new JOptionPane(
                  new Object[] {message, disclosureRow, details},
                  JOptionPane.WARNING_MESSAGE,
                  JOptionPane.DEFAULT_OPTION,
                  null,
                  new Object[] {cancel, disable});
          cancel.addActionListener(event -> pane.setValue(Result.CANCEL));
          disable.addActionListener(event -> pane.setValue(Result.DISABLE_CHECKS));
          JDialog dialog =
              pane.createDialog(parent, resources.getString("launcher.processorhashwarning.title"));
          disclosure.addActionListener(
              event -> {
                boolean expanded = !details.isVisible();
                details.setVisible(expanded);
                disclosure.setText(
                    resources.getString(
                        expanded
                            ? "launcher.processorhashwarning.hideDetails"
                            : "launcher.processorhashwarning.showDetails"));
                dialog.pack();
                dialog.setLocationRelativeTo(parent);
              });
          activeDialog[0] = dialog;
          try {
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
                        pane.setValue(Result.CANCEL);
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
            if (cancelled.get()) return;
            dialog.setVisible(true);
            if (!cancelled.get()) {
              if (pane.getValue() == Result.DISABLE_CHECKS) result.set(Result.DISABLE_CHECKS);
            }
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
        throw new IllegalStateException(
            "Could not display the processor hash warning", e.getCause());
      }
    }
    return result.get();
  }

  private static JTextArea textArea(String text) {
    JTextArea area = new JTextArea(text, 0, 48);
    area.setFont(UIManager.getFont("Label.font"));
    area.setForeground(UIManager.getColor("Label.foreground"));
    area.setEditable(false);
    area.setOpaque(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    area.setCaretPosition(0);
    // Measure wrapped lines before packing so text and actions are not clipped.
    area.setSize(area.getPreferredSize().width, Short.MAX_VALUE);
    return area;
  }
}
