package net.technicpack.launcher.ui.components.modpacks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.SwingUtilities;
import net.technicpack.launcher.io.InstalledPackStore;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.image.IImageMapper;
import net.technicpack.launchercore.image.ImageJob;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.launchercore.modpacks.InstalledPack;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.ui.lang.ResourceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModpackInfoPanelDeleteTest {
  @TempDir Path tempDir;

  @Test
  void deletionIsHiddenBeforeAnyPackIsSelected() throws Exception {
    SwingUtilities.invokeAndWait(() -> assertFalse(panel().getDeleteButton().isVisible()));
  }

  @Test
  void clearingSelectionHidesDeletionAndIgnoresLatePackRefresh() throws Exception {
    InstalledPack installedPack =
        new InstalledPack(
            "testpack", InstalledPack.RECOMMENDED, tempDir.resolve("pack").toString());
    ModpackModel modpack =
        new ModpackModel(
            installedPack,
            null,
            new InstalledPackStore(tempDir.resolve("installedpacks.json")),
            new LauncherFileSystem(tempDir.resolve("launcher")));

    SwingUtilities.invokeAndWait(
        () -> {
          ModpackInfoPanel panel = panel();
          panel.setModpack(modpack);
          assertTrue(panel.getDeleteButton().isVisible());

          panel.clearSelection();
          assertFalse(panel.getDeleteButton().isVisible());

          panel.setModpackIfSame(modpack);
          assertFalse(panel.getDeleteButton().isVisible());

          panel.setModpack(modpack);
          assertTrue(panel.getDeleteButton().isVisible());
        });
  }

  private static ModpackInfoPanel panel() {
    ResourceLoader resources =
        new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
    resources.setSupportedLanguages(new Locale[] {Locale.ENGLISH});
    resources.setLocale(Locale.ENGLISH);
    IImageMapper<ModpackModel> mapper =
        new IImageMapper<>() {
          @Override
          public boolean shouldDownloadImage(ModpackModel key) {
            return false;
          }

          @Override
          public File getImageLocation(ModpackModel key) {
            return null;
          }

          @Override
          public BufferedImage getDefaultImage() {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
          }
        };
    ImageJob<ModpackModel> image = new ImageJob<>(mapper, null);
    ImageRepository<ModpackModel> images =
        new ImageRepository<>(mapper, null) {
          @Override
          public ImageJob<ModpackModel> startImageJob(ModpackModel key) {
            return image;
          }
        };
    return new ModpackInfoPanel(resources, images, images, images, null, null, e -> {}, e -> {});
  }
}
