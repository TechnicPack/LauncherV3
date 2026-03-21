package net.technicpack.launchercore.install.plan.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadFilePlanActionTest {
    @Test
    void prefixesRawFileNamesWithDownloading() {
        assertEquals("Downloading modpack.zip",
                DownloadFilePlanAction.formatProgressLabel("modpack.zip"));
    }

    @Test
    void preservesExistingActionLabels() {
        assertEquals("Extracting libraries.zip...",
                DownloadFilePlanAction.formatProgressLabel("Extracting libraries.zip..."));
        assertEquals("Download failed, retries remaining: 2",
                DownloadFilePlanAction.formatProgressLabel("Download failed, retries remaining: 2"));
        assertEquals("Downloading Launcher Asset: discover.json",
                DownloadFilePlanAction.formatProgressLabel("Downloading Launcher Asset: discover.json"));
    }

    @Test
    void prefixesLauncherUpdateLabel() {
        assertEquals("Downloading Launcher Update",
                DownloadFilePlanAction.formatProgressLabel("Launcher Update"));
    }

    @Test
    void usesTaskDescriptionVerbatimWhenConfiguredForPlannedLabels() {
        assertEquals("Launcher Update",
                DownloadFilePlanAction.buildProgressLabel("temp.jar", "Launcher Update", true));
        assertEquals("Launcher Asset: discover.json",
                DownloadFilePlanAction.buildProgressLabel("discover.json", "Launcher Asset: discover.json", true));
        assertEquals("Downloading temp.jar",
                DownloadFilePlanAction.buildProgressLabel("temp.jar", "   ", true));
    }
}
