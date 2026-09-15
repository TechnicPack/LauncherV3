package net.technicpack.launcher.ui.components;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.MemoryPressure;

/** A per-launch warning only: neither the configured heap nor the settings file is changed. */
public final class MemoryWarningDialog {
  private MemoryWarningDialog() {}

  public static boolean confirmLaunch(
      Component parent, ResourceLoader resources, long heapMb, long availableMb)
      throws InterruptedException {
    if (!MemoryPressure.shouldWarn(heapMb, availableMb)) return true;
    AtomicBoolean cancelled = new AtomicBoolean();
    AtomicBoolean accepted = new AtomicBoolean();
    JDialog[] activeDialog = new JDialog[1];
    Runnable prompt =
        () -> {
          if (cancelled.get()) return;
          Object[] options = {
            resources.getString("launcher.memorywarning.continue"),
            resources.getString("launcher.pack.cancel")
          };
          JOptionPane pane =
              new JOptionPane(
                  resources.getString(
                      "launcher.memorywarning",
                      Long.toString(heapMb),
                      Long.toString(availableMb),
                      Long.toString(MemoryPressure.HEADROOM_MB)),
                  JOptionPane.WARNING_MESSAGE,
                  JOptionPane.YES_NO_OPTION,
                  null,
                  options,
                  options[1]);
          JDialog dialog =
              pane.createDialog(parent, resources.getString("launcher.memorywarning.title"));
          activeDialog[0] = dialog;
          try {
            dialog.setVisible(true);
            accepted.set(!cancelled.get() && options[0].equals(pane.getValue()));
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
        throw new IllegalStateException("Could not display the memory warning", e.getCause());
      }
    }
    return accepted.get();
  }
}
