import java.util.Locale;
import javax.swing.UIManager;
import net.technicpack.launcher.ui.components.MemoryWarningDialog;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.MemoryPressure;

/**
 * Runs the production dialog with simulated readings, without launching Minecraft or loading
 * settings.
 */
public final class MemoryWarningPreview {
  private MemoryWarningPreview() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 0 && args.length != 2) {
      throw new IllegalArgumentException(
          "Usage: previewMemoryWarning --args=\"heapMiB availableMiB\"");
    }
    long heapMb = args.length == 0 ? 4096 : Long.parseLong(args[0]);
    long availableMb = args.length == 0 ? 2048 : Long.parseLong(args[1]);
    if (heapMb <= 0 || availableMb < -1) {
      throw new IllegalArgumentException(
          "Heap must be positive; available memory must be nonnegative or -1 (unknown).");
    }

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    ResourceLoader resources =
        new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
    resources.setLocale(Locale.getDefault());
    System.out.printf(
        "Memory warning preview: simulated heap=%d MiB, available=%d MiB%n", heapMb, availableMb);
    System.out.println(
        "No memory pressure is generated. No settings are loaded or saved, and no game is started.");
    boolean warning = MemoryPressure.shouldWarn(heapMb, availableMb);
    boolean accepted = MemoryWarningDialog.confirmLaunch(null, resources, heapMb, availableMb);
    if (!warning) {
      System.out.println(
          "No warning: the reading is unknown or covers the heap plus advisory headroom.");
    } else {
      System.out.println("Preview result: " + (accepted ? "Launch anyway" : "Cancel"));
    }
    System.exit(0);
  }
}
