package net.technicpack.ui.controls.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

class SplashScreenLayoutTest {
  @Test
  void splashProgressWidthStaysInsideImageBounds() {
    assertEquals(217, SplashScreen.computeProgressDisplayWidth(245));
  }

  @Test
  void splashFooterIsNonOpaqueSoCustomRibbonShows() {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    JPanel footer = SplashScreen.createProgressFooter(display);

    // The footer must be non-opaque so its overridden paintComponent draws the translucent
    // dark ribbon directly onto the splash frame's translucent background. If the footer were
    // opaque, Swing would prefill the panel with its UI default colour and the SRC-composite
    // ribbon would have nothing to blend against.
    assertFalse(footer.isOpaque());
    assertSame(display, footer.getComponent(0));
  }
}
