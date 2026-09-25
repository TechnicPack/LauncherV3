package net.technicpack.ui.controls.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SplashScreenLayoutTest {
  @Test
  void progressRepaintsReplacePreviousFrameWithoutTranslucentPixels() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          InstallationProgressDisplay display = new InstallationProgressDisplay();
          display.configureForSplash(220);
          JPanel footer = SplashScreen.createProgressFooter(display);
          footer.setSize(248, 56);
          layout(footer);
          BufferedImage reused = new BufferedImage(248, 56, BufferedImage.TYPE_INT_ARGB);
          display.overallChanged("Downloading launcher", 80);
          paint(footer, reused);
          display.overallChanged("Installing", 20);
          paint(footer, reused);
          BufferedImage fresh = new BufferedImage(248, 56, BufferedImage.TYPE_INT_ARGB);
          paint(footer, fresh);
          for (int y = 0; y < reused.getHeight(); y++) {
            for (int x = 0; x < reused.getWidth(); x++) {
              assertEquals(
                  255,
                  reused.getRGB(x, y) >>> 24,
                  "Footer must not blend with stale window pixels");
              assertEquals(
                  fresh.getRGB(x, y), reused.getRGB(x, y), "Previous progress must be erased");
            }
          }
        });
  }

  private static void layout(java.awt.Container container) {
    container.doLayout();
    for (java.awt.Component child : container.getComponents()) {
      if (child instanceof java.awt.Container) {
        layout((java.awt.Container) child);
      }
    }
  }

  private static void paint(JPanel footer, BufferedImage image) {
    Graphics2D graphics = image.createGraphics();
    try {
      footer.paint(graphics);
    } finally {
      graphics.dispose();
    }
  }
}
