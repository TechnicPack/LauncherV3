package net.technicpack.utilslib;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopUtilsTest {
  @Test
  void openFallsBackToXdgOpenWhenDesktopOpenIsUnavailable(@TempDir Path tempDir) throws Exception {
    File logsDirectory = tempDir.resolve("logs").toFile();
    List<String[]> launchedCommands = new ArrayList<>();

    invokeOpen(
        logsDirectory,
        () -> false,
        file -> fail("desktop open should not be used when the desktop action is unavailable"),
        launchedCommands::add,
        OperatingSystem.LINUX);

    assertEquals(1, launchedCommands.size());
    assertArrayEquals(
        new String[] {"xdg-open", logsDirectory.getAbsolutePath()}, launchedCommands.get(0));
  }

  @Test
  void openFallsBackToXdgOpenWhenDesktopOpenThrows(@TempDir Path tempDir) throws Exception {
    File logsDirectory = tempDir.resolve("logs").toFile();
    List<String[]> launchedCommands = new ArrayList<>();

    invokeOpen(
        logsDirectory,
        () -> true,
        file -> {
          throw new IllegalStateException("desktop integration failed");
        },
        launchedCommands::add,
        OperatingSystem.LINUX);

    assertEquals(1, launchedCommands.size());
    assertArrayEquals(
        new String[] {"xdg-open", logsDirectory.getAbsolutePath()}, launchedCommands.get(0));
  }

  private static void invokeOpen(
      File file,
      BooleanSupplier canOpenWithDesktop,
      Consumer<File> desktopOpen,
      Consumer<String[]> processLauncher,
      OperatingSystem operatingSystem)
      throws Exception {
    try {
      Method method =
          DesktopUtils.class.getDeclaredMethod(
              "open",
              File.class,
              BooleanSupplier.class,
              Consumer.class,
              Consumer.class,
              OperatingSystem.class);
      method.setAccessible(true);
      method.invoke(null, file, canOpenWithDesktop, desktopOpen, processLauncher, operatingSystem);
    } catch (NoSuchMethodException e) {
      fail(
          "DesktopUtils.open should support platform fallbacks when desktop integration is unavailable",
          e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw e;
    }
  }
}
