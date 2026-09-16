package net.technicpack.launcher.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.launchercore.exception.InstallException;
import net.technicpack.launchercore.exception.JavaRuntimeException;
import net.technicpack.launchercore.install.ModpackInstaller;
import net.technicpack.launchercore.install.ModpackVersion;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.platform.io.FeedItem;
import net.technicpack.rest.io.Modpack;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.rest.io.Resource;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.Memory;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class InstallerTest {
  @Test
  void detectsCreateProcessAccessDeniedMessage() {
    IOException exception = new IOException("CreateProcess error=5, Access is denied");
    assertTrue(Installer.isCreateProcessAccessDenied(exception));
  }

  @Test
  void doesNotThrowOnNullMessage() {
    IOException exception = new IOException((String) null);
    assertFalse(Installer.isCreateProcessAccessDenied(exception));
  }

  @Test
  void ignoresOtherIoMessages() {
    IOException exception =
        new IOException("CreateProcess error=2, The system cannot find the file specified");
    assertFalse(Installer.isCreateProcessAccessDenied(exception));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Cannot run program \"java\": error=86, Bad CPU type in executable",
        "Exec failed, error: 86 (Bad CPU type in executable)",
        "error=86, incompatible executable",
        "Bad CPU type in executable"
      })
  void appleSiliconStartupFailureExplainsHowToInstallRosetta(String message) {
    JavaRuntimeException failure =
        new JavaRuntimeException(
            "The downloaded Java runtime could not be validated.",
            new JavaRuntimeException("Could not query Java", new IOException(message)));

    assertTrue(
        Installer.getJavaRuntimeErrorMessage(failure, errorResources(), true)
            .contains("softwareupdate --install-rosetta"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"error=2, No such file", "error=13, Permission denied", "error=860, other"})
  @NullSource
  void unrelatedStartupFailuresKeepTheirDiagnostics(String message) {
    JavaRuntimeException failure =
        new JavaRuntimeException("Runtime validation failed", new IOException(message));

    assertEquals(
        failure.getMessage(),
        Installer.getJavaRuntimeErrorMessage(failure, errorResources(), true));
  }

  @Test
  void unsupportedCpuOnOtherHostsDoesNotRecommendRosetta() {
    JavaRuntimeException failure =
        new JavaRuntimeException(
            "Runtime validation failed", new IOException("error=86, Bad CPU type in executable"));

    assertEquals(
        failure.getMessage(),
        Installer.getJavaRuntimeErrorMessage(failure, errorResources(), false));
  }

  @Test
  void probeOutputWithoutAnIoFailureDoesNotRecommendRosetta() {
    JavaRuntimeException failure =
        new JavaRuntimeException("Java runtime probe failed: Bad CPU type in executable");

    assertEquals(
        failure.getMessage(),
        Installer.getJavaRuntimeErrorMessage(failure, errorResources(), true));
  }

  private static ResourceLoader errorResources() {
    ResourceLoader resources =
        new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
    resources.setLocale(Locale.GERMAN);
    return resources;
  }

  @Test
  void preparePackForInstallRejectsMissingPackInfoBeforePreparingPack() throws Exception {
    RecordingModpackInstaller modpackInstaller = new RecordingModpackInstaller();
    Installer installer = new Installer(null, null, modpackInstaller, null, null, null);

    InstallException exception =
        assertThrows(
            InstallException.class,
            () -> installer.preparePackForInstall(new TestModpackModel(null), "recommended"));

    assertEquals(
        "No modpack information found, cannot install or launch modpack.", exception.getMessage());
    assertEquals(0, modpackInstaller.preparePackCalls);
  }

  @Test
  void preparePackForInstallRejectsMissingGameVersionBeforePreparingPack() throws Exception {
    RecordingModpackInstaller modpackInstaller = new RecordingModpackInstaller();
    Installer installer = new Installer(null, null, modpackInstaller, null, null, null);
    Modpack modpack = new com.google.gson.Gson().fromJson("{\"mods\":[]}", Modpack.class);

    InstallException exception =
        assertThrows(
            InstallException.class,
            () ->
                installer.preparePackForInstall(
                    new TestModpackModel(new TestPackInfo(modpack)), "recommended"));

    assertEquals(
        "No game version found for modpack, cannot install or launch modpack.",
        exception.getMessage());
    assertEquals(0, modpackInstaller.preparePackCalls);
  }

  @Test
  void preparePackForInstallReturnsResolvedModpackAfterPreparingPack() throws Exception {
    RecordingModpackInstaller modpackInstaller = new RecordingModpackInstaller();
    Installer installer = new Installer(null, null, modpackInstaller, null, null, null);
    Modpack modpack =
        new com.google.gson.Gson()
            .fromJson("{\"minecraft\":\"1.20.1\",\"mods\":[]}", Modpack.class);
    TestModpackModel pack = new TestModpackModel(new TestPackInfo(modpack));

    assertSame(modpack, installer.preparePackForInstall(pack, "recommended"));
    assertEquals(1, modpackInstaller.preparePackCalls);
    assertSame(pack, modpackInstaller.lastPreparedPack);
  }

  @Test
  void getLaunchMemoryClampsToHeapCeilingWithoutChangingSavedSelection() {
    TechnicSettings settings = new TechnicSettings();
    int savedSelection = settings.getMemory();

    Memory launchMemory = Installer.getLaunchMemory(settings, 1024);

    assertEquals(1024, launchMemory.getMemoryMB());
    assertEquals(savedSelection, settings.getMemory());
  }

  private static final class RecordingModpackInstaller extends ModpackInstaller {
    private int preparePackCalls;
    private ModpackModel lastPreparedPack;

    private RecordingModpackInstaller() {
      super(null, "test-client");
    }

    @Override
    public void preparePack(ModpackModel modpack) {
      preparePackCalls++;
      lastPreparedPack = modpack;
    }

    @Override
    public void completeInstall(
        ModpackModel modpack, String build, ModpackVersion installedVersion) {}
  }

  private static final class TestModpackModel extends ModpackModel {
    private final PackInfo packInfo;

    private TestModpackModel(PackInfo packInfo) {
      this.packInfo = packInfo;
    }

    @Override
    public PackInfo getPackInfo() {
      return packInfo;
    }
  }

  private static final class TestPackInfo implements PackInfo {
    private final Modpack modpack;

    private TestPackInfo(Modpack modpack) {
      this.modpack = modpack;
    }

    @Override
    public String getName() {
      return "test-pack";
    }

    @Override
    public String getDisplayName() {
      return "Test Pack";
    }

    @Override
    public String getWebSite() {
      return null;
    }

    @Override
    public Resource getIcon() {
      return null;
    }

    @Override
    public Resource getBackground() {
      return null;
    }

    @Override
    public Resource getLogo() {
      return null;
    }

    @Override
    public String getRecommended() {
      return "recommended";
    }

    @Override
    public String getLatest() {
      return "latest";
    }

    @Override
    public List<String> getBuilds() {
      return Collections.singletonList("recommended");
    }

    @Override
    public ArrayList<FeedItem> getFeed() {
      return new ArrayList<>();
    }

    @Override
    public String getDescription() {
      return "";
    }

    @Override
    public Integer getRuns() {
      return null;
    }

    @Override
    public Integer getInstalls() {
      return null;
    }

    @Override
    public Integer getLikes() {
      return null;
    }

    @Override
    public Modpack getModpack(String build) throws BuildInaccessibleException {
      return modpack;
    }

    @Override
    public boolean isComplete() {
      return true;
    }

    @Override
    public boolean isLocal() {
      return false;
    }

    @Override
    public boolean isServerPack() {
      return false;
    }

    @Override
    public boolean isOfficial() {
      return false;
    }

    @Override
    public boolean hasSolder() {
      return false;
    }

    @Override
    public @Nullable String getDiscordId() {
      return null;
    }
  }
}
