package net.technicpack.ui.controls.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProgressBarTest {
  @Test
  void textInsetClearsRoundedCorner() {
    int inset = ProgressBar.computeTextEdgeInset(20);

    assertEquals(14, inset);
    assertTrue(inset > 10);
  }
}
