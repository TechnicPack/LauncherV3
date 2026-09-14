package net.technicpack.launchercore.modpacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.utilslib.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackLoadJobTest {
  @TempDir Path tempDir;

  private MemoryModpackContainer container;
  private PackLoadJob job;

  @BeforeEach
  void setUp() {
    container = new MemoryModpackContainer();
    job =
        new PackLoadJob(
            new LauncherFileSystem(tempDir.resolve("launcher-root")),
            new InstalledPackStore(tempDir.resolve("installedpacks.json")),
            null,
            null,
            container,
            null,
            false);
  }

  @Test
  void unidentifiedEntriesDoNotReachContainer() {
    job.addPack(null, null, 1);
    job.addPack(null, new PlatformPackInfo(), 1);
    job.addPack(new InstalledPack(), new PlatformPackInfo(), 1);
    job.addPack(new InstalledPack("", InstalledPack.RECOMMENDED), new PlatformPackInfo(), 1);
    job.addPack(null, Utils.getGson().fromJson("{\"name\":\"   \"}", PlatformPackInfo.class), 1);

    assertTrue(container.getModpacks().isEmpty());
  }

  @Test
  void installedIdentityKeepsNamelessMetadataInTheSameContainerEntry() {
    InstalledPack installed = new InstalledPack("testpack", InstalledPack.RECOMMENDED);
    job.addPack(
        installed, Utils.getGson().fromJson("{\"name\":\"testpack\"}", PlatformPackInfo.class), 1);
    ModpackModel original = container.getModpacks().iterator().next();

    job.addPack(installed, new PlatformPackInfo(), 2);

    assertEquals(1, container.getModpacks().size());
    ModpackModel refreshed = container.getModpacks().iterator().next();
    assertSame(original, refreshed);
    assertEquals("testpack", refreshed.getName());
  }

  @Test
  void installedIdentityAdmitsNamelessMetadata() {
    job.addPack(
        new InstalledPack("testpack", InstalledPack.RECOMMENDED), new PlatformPackInfo(), 1);

    assertEquals(1, container.getModpacks().size());
    assertEquals("testpack", container.getModpacks().iterator().next().getName());
  }
}
