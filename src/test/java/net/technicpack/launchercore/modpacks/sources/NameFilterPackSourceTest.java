package net.technicpack.launchercore.modpacks.sources;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.MemoryModpackContainer;
import net.technicpack.launchercore.modpacks.ModpackModel;
import org.junit.jupiter.api.Test;

class NameFilterPackSourceTest {
  @Test
  void skipsInstalledPacksWithoutPackInfoWhenFilteringByName() {
    MemoryModpackContainer modpacks = new MemoryModpackContainer();
    modpacks.addModpackToContainer(
        new ModpackModel(new InstalledPack("tekkit", "recommended"), null, null, null));

    NameFilterPackSource source = new NameFilterPackSource(modpacks, "TEK");

    assertTrue(source.getPublicPacks().isEmpty());
  }
}
