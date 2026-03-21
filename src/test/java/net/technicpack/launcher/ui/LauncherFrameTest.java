package net.technicpack.launcher.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LauncherFrameTest {
    @Test
    void footerUserSectionUsesSharedGapAndTrimmedSeparator() {
        assertEquals(4, LauncherFrame.footerUserSectionGap());
        assertEquals("|", LauncherFrame.footerSeparatorText());
    }
}
