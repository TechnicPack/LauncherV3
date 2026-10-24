package net.technicpack.minecraftcore.launch;

import java.awt.AWTError;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;

final class ScreenResolution {
  private ScreenResolution() {}

  static Dimension fitToScreen() {
    Dimension[] result = {new Dimension(800, 600)};
    Runnable measure =
        () -> {
          try {
            if (GraphicsEnvironment.isHeadless()) {
              return;
            }
            GraphicsConfiguration screen =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
            result[0] = fitToScreen(screen.getBounds(), screenInsets, decorationInsets());
          } catch (RuntimeException | AWTError unavailable) {
            // Headless sessions and unavailable displays use the standard window size.
          }
        };
    if (SwingUtilities.isEventDispatchThread()) {
      measure.run();
    } else {
      try {
        SwingUtilities.invokeAndWait(measure);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        return new Dimension(800, 600);
      } catch (InvocationTargetException | RuntimeException | AWTError unavailable) {
        return new Dimension(800, 600);
      }
    }
    return result[0];
  }

  private static Insets decorationInsets() {
    for (Frame frame : Frame.getFrames()) {
      if (frame.isShowing() && !frame.isUndecorated()) {
        return frame.getInsets();
      }
    }
    // Custom launcher chrome is not native decoration. Measure a native peer without showing it.
    Frame probe = new Frame();
    try {
      probe.pack();
      return probe.getInsets();
    } finally {
      probe.dispose();
    }
  }

  static Dimension fitToScreen(Rectangle bounds, Insets screenInsets, Insets decorationInsets) {
    if (bounds.width <= 0 || bounds.height <= 0) {
      return new Dimension(800, 600);
    }
    // This estimates the game's client area; it is not native window maximization.
    return new Dimension(
        usableSize(
            bounds.width,
            screenInsets.left,
            screenInsets.right,
            decorationInsets.left,
            decorationInsets.right),
        usableSize(
            bounds.height,
            screenInsets.top,
            screenInsets.bottom,
            decorationInsets.top,
            decorationInsets.bottom));
  }

  private static int usableSize(int size, int first, int second, int third, int fourth) {
    long usable =
        (long) size
            - Math.max(0, first)
            - Math.max(0, second)
            - Math.max(0, third)
            - Math.max(0, fourth);
    return (int) Math.max(1L, usable);
  }
}
