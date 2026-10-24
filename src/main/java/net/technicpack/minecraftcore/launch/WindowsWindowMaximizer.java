package net.technicpack.minecraftcore.launch;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import net.technicpack.utilslib.Utils;

/**
 * Maximizes the game's window once it appears. Minecraft has no maximized launch argument, so the
 * launcher watches for the first top-level window of the game process that the user could maximize
 * and maximizes it once; later size changes are left to the player.
 */
final class WindowsWindowMaximizer {
  private static final long POLL_INTERVAL_MS = 250;

  private static final int GWL_STYLE = -16;
  private static final int GW_OWNER = 4;
  private static final int SW_MAXIMIZE = 3;
  private static final int WS_THICKFRAME = 0x00040000;
  private static final int WS_MAXIMIZEBOX = 0x00010000;

  private WindowsWindowMaximizer() {}

  static void watch(Process process) {
    Thread thread = new Thread(() -> run(process), "Minecraft window maximizer");
    thread.setDaemon(true);
    thread.start();
  }

  private static void run(Process process) {
    try {
      NativeLibrary user32 = NativeLibrary.getInstance("user32");
      Function findWindowEx = user32.getFunction("FindWindowExW", Function.ALT_CONVENTION);
      long pid = getPid(process);

      while (process.isAlive()) {
        Pointer window = null;
        while ((window = findWindowEx.invokePointer(new Object[] {null, window, null, null}))
            != null) {
          if (!isMaximizableGameWindow(user32, window, pid)) continue;
          if (user32
                  .getFunction("IsZoomed", Function.ALT_CONVENTION)
                  .invokeInt(new Object[] {window})
              == 0) {
            user32
                .getFunction("ShowWindow", Function.ALT_CONVENTION)
                .invokeInt(new Object[] {window, SW_MAXIMIZE});
            Utils.getLogger().info("Maximized the Minecraft window");
          }
          return;
        }
        Thread.sleep(POLL_INTERVAL_MS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (LinkageError | RuntimeException | ReflectiveOperationException e) {
      Utils.getLogger().log(Level.WARNING, "Could not maximize the Minecraft window", e);
    }
  }

  private static boolean isMaximizableGameWindow(NativeLibrary user32, Pointer window, long pid) {
    try (Memory windowPid = new Memory(4)) {
      windowPid.clear();
      user32
          .getFunction("GetWindowThreadProcessId", Function.ALT_CONVENTION)
          .invokeInt(new Object[] {window, windowPid});
      if (Integer.toUnsignedLong(windowPid.getInt(0)) != pid) return false;
    }

    if (user32
                .getFunction("IsWindowVisible", Function.ALT_CONVENTION)
                .invokeInt(new Object[] {window})
            == 0
        || user32.getFunction("IsIconic", Function.ALT_CONVENTION).invokeInt(new Object[] {window})
            != 0
        || user32
                .getFunction("GetWindow", Function.ALT_CONVENTION)
                .invokePointer(new Object[] {window, GW_OWNER})
            != null) {
      return false;
    }

    // Only windows the player could maximize themselves: this skips dialogs and waits for the game
    // to make its window resizable before changing its size.
    int style =
        user32
            .getFunction("GetWindowLongW", Function.ALT_CONVENTION)
            .invokeInt(new Object[] {window, GWL_STYLE});
    if ((style & WS_THICKFRAME) == 0 || (style & WS_MAXIMIZEBOX) == 0) return false;

    // Skip Swing/AWT frames, such as error windows opened by mods; the game window is LWJGL/GLFW.
    char[] className = new char[256];
    int length =
        user32
            .getFunction("GetClassNameW", Function.ALT_CONVENTION)
            .invokeInt(new Object[] {window, className, className.length});
    return !new String(className, 0, Math.max(length, 0)).startsWith("SunAwt");
  }

  private static long getPid(Process process) throws ReflectiveOperationException {
    try {
      // Java 9+
      return (Long) Process.class.getMethod("pid").invoke(process);
    } catch (NoSuchMethodException java8) {
      Field handleField = process.getClass().getDeclaredField("handle");
      handleField.setAccessible(true);
      Pointer handle = Pointer.createConstant(handleField.getLong(process));
      return Integer.toUnsignedLong(
          NativeLibrary.getInstance("kernel32")
              .getFunction("GetProcessId", Function.ALT_CONVENTION)
              .invokeInt(new Object[] {handle}));
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("Could not read the Minecraft process ID", e.getCause());
    }
  }
}
