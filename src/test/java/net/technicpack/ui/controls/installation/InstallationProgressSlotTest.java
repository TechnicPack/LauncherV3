package net.technicpack.ui.controls.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import org.junit.jupiter.api.Test;

class InstallationProgressSlotTest {
  @Test
  void slotKeepsStableFootprintAcrossVisibilityChanges() {
    InstallationProgressDisplay display = new InstallationProgressDisplay();
    InstallationProgressSlot slot = new InstallationProgressSlot(display, 560);

    Dimension initialSize = slot.getPreferredSize();

    assertTrue(initialSize.height >= display.getPreferredSize().height);
    assertFalse(slot.isProgressVisible());

    slot.showProgress();

    assertTrue(slot.isProgressVisible());
    assertEquals(initialSize, slot.getPreferredSize());

    slot.hideProgress();

    assertFalse(slot.isProgressVisible());
    assertEquals(initialSize, slot.getPreferredSize());
  }

  @Test
  void slotHeightCanFitSecondaryRowWhenItBecomesVisible() throws Exception {
    InstallationProgressDisplay display = new InstallationProgressDisplay();
    InstallationProgressSlot slot = new InstallationProgressSlot(display, 560);

    slot.showProgress();
    display.currentItemChanged(
        "Downloading file.zip",
        net.technicpack.launchercore.progress.CurrentItemMode.DETERMINATE,
        30.0f);
    javax.swing.SwingUtilities.invokeAndWait(() -> {});

    assertTrue(display.getCurrentItemRow().isVisible());
    assertTrue(slot.getPreferredSize().height >= display.getPreferredSize().height);
  }
}
