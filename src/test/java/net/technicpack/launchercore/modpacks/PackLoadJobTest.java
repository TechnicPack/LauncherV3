package net.technicpack.launchercore.modpacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.modpacks.sources.IAuthoritativePackSource;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.io.SolderPackInfo;
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
            new OfflinePackSource(),
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

  @Test
  void installedEntryResolvesUsingExistingDiscoveryMetadata() throws Exception {
    CountDownLatch firstLookup = new CountDownLatch(1);
    CountDownLatch resolved = new CountDownLatch(1);
    AtomicInteger lookups = new AtomicInteger();
    PlatformPackInfo complete =
        Utils.getGson()
            .fromJson(
                "{\"name\":\"testpack\",\"displayName\":\"Resolved title\",\"version\":\"1\"}",
                PlatformPackInfo.class);
    IAuthoritativePackSource source =
        new OfflinePackSource() {
          @Override
          public PackInfo getCompletePackInfo(PackInfo info) {
            if (lookups.incrementAndGet() == 1) {
              firstLookup.countDown();
              return null;
            }
            return info != null && "testpack".equals(info.getName()) ? complete : null;
          }
        };
    PackLoadJob resolvingJob =
        new PackLoadJob(
            new LauncherFileSystem(tempDir.resolve("launcher-root")),
            new InstalledPackStore(tempDir.resolve("installedpacks.json")),
            source,
            null,
            container,
            null,
            false) {
          @Override
          protected void addPackThreadSafe(InstalledPack pack, PackInfo info, int priority) {
            super.addPackThreadSafe(pack, info, priority);
            resolved.countDown();
          }
        };
    SolderPackInfo discovered =
        Utils.getGson()
            .fromJson("{\"name\":\"testpack\",\"builds\":[\"1\"]}", SolderPackInfo.class);
    SwingUtilities.invokeAndWait(() -> resolvingJob.addPack(null, discovered, 1));
    assertTrue(firstLookup.await(5, TimeUnit.SECONDS));

    SwingUtilities.invokeAndWait(
        () ->
            resolvingJob.addPack(
                new InstalledPack("testpack", InstalledPack.RECOMMENDED), null, 1));

    assertTrue(resolved.await(5, TimeUnit.SECONDS));
    SwingUtilities.invokeAndWait(
        () -> {
          assertEquals(1, container.getModpacks().size());
          assertEquals(
              "Resolved title", container.getModpacks().iterator().next().getDisplayName());
        });
  }

  private static class OfflinePackSource implements IAuthoritativePackSource {
    @Override
    public PackInfo getPackInfo(InstalledPack pack) {
      return null;
    }

    @Override
    public PackInfo getCompletePackInfo(PackInfo pack) {
      return null;
    }
  }
}
