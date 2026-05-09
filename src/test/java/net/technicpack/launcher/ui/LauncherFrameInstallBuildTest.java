package net.technicpack.launcher.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import net.technicpack.launchercore.exception.InstallException;
import org.junit.jupiter.api.Test;

class LauncherFrameInstallBuildTest {

  private static final String PACK = "Test Pack";
  private static final String BUILD_A = "1.0.0";
  private static final String BUILD_B = "1.1.0";
  private static final String BUILD_C = "2.0.0";

  @Test
  void recommendedSentinelResolvesToRecommendedWhenSet() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            true, false, "recommended", BUILD_A, BUILD_B, List.of(BUILD_A, BUILD_B), null, PACK);
    assertEquals(BUILD_A, result);
  }

  @Test
  void recommendedFallsBackToOnlyAvailableBuild() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            true, false, "recommended", null, null, List.of(BUILD_A), null, PACK);
    assertEquals(BUILD_A, result);
  }

  @Test
  void recommendedThrowsWhenAmbiguousMultipleBuilds() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    true, false, "recommended", null, null, List.of(BUILD_A, BUILD_B), null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
    assertTrue(ex.getMessage().contains("recommended"));
    assertTrue(ex.getMessage().contains("pick a specific build"));
  }

  @Test
  void recommendedThrowsWhenBuildsListIsEmpty() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    true, false, "recommended", null, null, Collections.emptyList(), null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
    assertTrue(ex.getMessage().contains("recommended"));
    assertTrue(ex.getMessage().contains("no other builds"));
  }

  @Test
  void recommendedThrowsWhenBuildsListIsNull() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    true, false, "recommended", null, null, null, null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
    assertTrue(ex.getMessage().contains("recommended"));
  }

  @Test
  void latestSentinelResolvesToLatestWhenSet() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            true, false, "LATEST", BUILD_A, BUILD_B, List.of(BUILD_A, BUILD_B), null, PACK);
    assertEquals(BUILD_B, result);
  }

  @Test
  void latestFallsBackToOnlyAvailableBuild() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            true, false, "latest", null, null, List.of(BUILD_A), null, PACK);
    assertEquals(BUILD_A, result);
  }

  @Test
  void latestThrowsWhenAmbiguousMultipleBuilds() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    true, false, "latest", null, null, List.of(BUILD_A, BUILD_B), null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
    assertTrue(ex.getMessage().contains("latest"));
  }

  @Test
  void specificBuildIsReturnedAsIs() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            true, false, BUILD_A, BUILD_B, BUILD_C, List.of(BUILD_A, BUILD_B, BUILD_C), null, PACK);
    assertEquals(BUILD_A, result);
  }

  @Test
  void forceInstallThrowsWhenSelectedBuildIsNull() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    true, false, null, BUILD_A, BUILD_B, List.of(BUILD_A, BUILD_B), null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
  }

  @Test
  void forceInstallThrowsWhenSelectedBuildIsBlank() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    true, false, "   ", BUILD_A, BUILD_B, List.of(BUILD_A, BUILD_B), null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
  }

  @Test
  void localOnlyPackUsesInstalledBuildEvenWhenForceInstall() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            true, true, "recommended", null, null, null, BUILD_A, PACK);
    assertEquals(BUILD_A, result);
  }

  @Test
  void notForceInstallUsesInstalledBuild() throws InstallException {
    String result =
        LauncherFrame.resolveInstallBuild(
            false,
            false,
            "recommended",
            BUILD_B,
            BUILD_C,
            List.of(BUILD_A, BUILD_B),
            BUILD_A,
            PACK);
    assertEquals(BUILD_A, result);
  }

  @Test
  void notForceInstallThrowsWhenInstalledBuildNull() {
    InstallException ex =
        assertThrows(
            InstallException.class,
            () ->
                LauncherFrame.resolveInstallBuild(
                    false, false, "recommended", null, null, null, null, PACK));
    assertTrue(ex.getMessage().contains(PACK));
  }
}
