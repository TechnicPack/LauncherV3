package net.technicpack.minecraftcore.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

class ScreenResolutionTest {
  @Test
  void subtractsTaskbarAndNativeDecorationsFromPrimaryScreenSize() {
    assertEquals(
        new Dimension(1904, 1001),
        ScreenResolution.fitToScreen(
            new Rectangle(0, 0, 1920, 1080), new Insets(0, 0, 40, 0), new Insets(31, 8, 8, 8)));
  }

  @Test
  void handlesSideTaskbarsAndOffsetPrimaryScreenWithoutUsingDesktopCoordinatesAsSize() {
    assertEquals(
        new Dimension(1210, 746),
        ScreenResolution.fitToScreen(
            new Rectangle(-1280, 200, 1280, 800),
            new Insets(24, 60, 0, 0),
            new Insets(25, 5, 5, 5)));
  }

  @Test
  void clampsExcessiveInsetsWithoutIntegerOverflow() {
    assertEquals(
        new Dimension(1, 1),
        ScreenResolution.fitToScreen(
            new Rectangle(0, 0, 1920, 1080),
            new Insets(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
            new Insets(30, 8, 8, 8)));
  }

  @Test
  void ignoresNegativeInsetsRatherThanEnlargingWindowBeyondScreen() {
    assertEquals(
        new Dimension(1920, 1080),
        ScreenResolution.fitToScreen(
            new Rectangle(0, 0, 1920, 1080),
            new Insets(-1, -2, -3, -4),
            new Insets(-5, -6, -7, -8)));
  }

  @Test
  void invalidScreenBoundsUseSafeDefault() {
    for (Rectangle bounds :
        new Rectangle[] {new Rectangle(0, 0, 0, 1080), new Rectangle(0, 0, 1920, -1)}) {
      assertEquals(
          new Dimension(800, 600),
          ScreenResolution.fitToScreen(bounds, new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
    }
  }
}
