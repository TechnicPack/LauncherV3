package net.technicpack.ui.controls.installation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;
import net.technicpack.launchercore.progress.CurrentItemMode;
import org.junit.jupiter.api.Test;

class InstallationProgressDisplayTest {
  @Test
  void displayDefinesHeightsForBothProgressRows() {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    assertTrue(display.getOverallProgressBar().getPreferredSize().height > 0);
    assertTrue(display.getCurrentItemRow().getPreferredSize().height > 0);
  }

  @Test
  void overallProgressUpdatesPrimaryBar() throws Exception {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    display.overallChanged("Install", 42.5f);
    flushEdt();

    assertEquals("Install", display.getOverallProgressBar().getText());
    assertEquals(0.425f, display.getOverallProgressBar().progressPct, 0.0001f);
  }

  @Test
  void currentItemDeterminateProgressShowsSecondaryStatus() throws Exception {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    display.currentItemChanged("Downloading mod.zip", CurrentItemMode.DETERMINATE, 25.0f);
    flushEdt();

    assertTrue(display.getCurrentItemRow().isVisible());
    assertEquals("Downloading mod.zip", display.getCurrentItemNameLabel().getText());
    assertEquals(25.0f, display.getCurrentItemProgressBar().getProgress(), 0.0001f);
    assertFalse(display.getCurrentItemProgressBar().isIndeterminate());
  }

  @Test
  void currentItemIndeterminateProgressShowsSecondaryStatusWithoutPercent() throws Exception {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    display.currentItemChanged("Extracting metadata", CurrentItemMode.INDETERMINATE, null);
    flushEdt();

    assertTrue(display.getCurrentItemRow().isVisible());
    assertEquals("Extracting metadata", display.getCurrentItemNameLabel().getText());
    assertTrue(display.getCurrentItemProgressBar().isIndeterminate());
  }

  @Test
  void idleCurrentItemStateClearsSecondaryStatusWithoutCollapsingRow() throws Exception {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    display.currentItemChanged("Downloading mod.zip", CurrentItemMode.DETERMINATE, 25.0f);
    display.currentItemChanged("", CurrentItemMode.IDLE, null);
    flushEdt();

    assertTrue(display.getCurrentItemRow().isVisible());
    assertEquals("", display.getCurrentItemNameLabel().getText());
    assertEquals(0.0f, display.getCurrentItemProgressBar().getProgress(), 0.0001f);
    assertFalse(display.getCurrentItemProgressBar().isIndeterminate());
  }

  @Test
  void hidingDisplayResetsAndCollapsesSecondaryStatus() throws Exception {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    display.currentItemChanged("Downloading mod.zip", CurrentItemMode.INDETERMINATE, null);
    display.setVisible(false);
    flushEdt();

    assertFalse(display.getCurrentItemRow().isVisible());
    assertEquals("", display.getCurrentItemNameLabel().getText());
    assertFalse(display.getCurrentItemProgressBar().isIndeterminate());
  }

  @Test
  void splashModeHidesCaptionAndKeepsRowCompactlyVisible() {
    InstallationProgressDisplay display = new InstallationProgressDisplay();

    display.configureForSplash();

    // The row stays visible from the start so pack() reserves its 18px even before any
    // current-item update arrives; otherwise revealing the row mid-install would shrink the
    // splash image. "Compact" comes from the caption being width-zero, not from the row
    // itself being hidden or sub-18.
    assertTrue(display.getCurrentItemRow().isVisible());
    assertFalse(display.getCurrentItemCaptionLabel().isVisible());
    assertEquals(0, display.getCurrentItemCaptionLabel().getPreferredSize().width);
    assertEquals(24, display.getOverallProgressBar().getPreferredSize().height);
    assertEquals(18, display.getCurrentItemRow().getPreferredSize().height);
  }

  private static void flushEdt() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
  }
}
