package net.technicpack.launcher.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LauncherFrameTest {
  @Test
  void footerUserSectionUsesSharedGapAndTrimmedSeparator() {
    assertEquals(4, LauncherFrame.footerUserSectionGap());
    assertEquals("|", LauncherFrame.footerSeparatorText());
  }
}
