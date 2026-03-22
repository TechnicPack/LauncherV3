package net.technicpack.ui.controls.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class SplashScreenLayoutTest {
  @Test
  void splashProgressWidthStaysInsideImageBounds() {
    assertEquals(217, SplashScreen.computeProgressDisplayWidth(245));
  }

  @Test
  void splashFooterUsesOpaqueBlackBackground() {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    JPanel footer = SplashScreen.createProgressFooter(display);

    assertTrue(footer.isOpaque());
    assertEquals(Color.BLACK, footer.getBackground());
    assertSame(display, footer.getComponent(0));
  }
}
