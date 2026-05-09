package net.technicpack.solder.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.technicpack.launchercore.exception.BuildInaccessibleException;
import net.technicpack.rest.RestfulAPIException;
import org.junit.jupiter.api.Test;

class HttpSolderPackApiTest {

  private static final String BASE_URL = "https://solder.example.com/api/";
  private static final String MIRROR_URL = "https://mirror.example.com/";
  private static final String SLUG = "test-pack";
  private static final String CLIENT_ID = "00000000-0000-0000-0000-000000000000";

  @Test
  void constructorThrowsWhenModpackSlugIsNull() {
    RestfulAPIException ex =
        assertThrows(
            RestfulAPIException.class,
            () -> new HttpSolderPackApi(BASE_URL, null, CLIENT_ID, MIRROR_URL));
    assertTrue(
        ex.getMessage().contains("modpack slug"),
        "exception message should identify modpack slug, was: " + ex.getMessage());
  }

  @Test
  void constructorThrowsWhenBaseUrlIsNull() {
    RestfulAPIException ex =
        assertThrows(
            RestfulAPIException.class,
            () -> new HttpSolderPackApi(null, SLUG, CLIENT_ID, MIRROR_URL));
    assertTrue(
        ex.getMessage().contains("base URL"),
        "exception message should identify base URL, was: " + ex.getMessage());
  }

  @Test
  void constructorThrowsWhenMirrorUrlIsNull() {
    RestfulAPIException ex =
        assertThrows(
            RestfulAPIException.class,
            () -> new HttpSolderPackApi(BASE_URL, SLUG, CLIENT_ID, null));
    assertTrue(
        ex.getMessage().contains("mirror URL"),
        "exception message should identify mirror URL, was: " + ex.getMessage());
  }

  @Test
  void constructorThrowsWhenClientIdIsNull() {
    RestfulAPIException ex =
        assertThrows(
            RestfulAPIException.class,
            () -> new HttpSolderPackApi(BASE_URL, SLUG, null, MIRROR_URL));
    assertTrue(
        ex.getMessage().contains("client ID"),
        "exception message should identify client ID, was: " + ex.getMessage());
  }

  @Test
  void constructorAcceptsAllNonNullArguments() {
    assertDoesNotThrow(() -> new HttpSolderPackApi(BASE_URL, SLUG, CLIENT_ID, MIRROR_URL));
  }

  @Test
  void getPackBuildThrowsBuildInaccessibleWhenBuildIsNull() throws RestfulAPIException {
    HttpSolderPackApi api = new HttpSolderPackApi(BASE_URL, SLUG, CLIENT_ID, MIRROR_URL);
    BuildInaccessibleException ex =
        assertThrows(BuildInaccessibleException.class, () -> api.getPackBuild(null));
    assertNotNull(ex.getCause());
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    assertEquals("build name must not be null", ex.getCause().getMessage());
    assertTrue(
        ex.getMessage().contains(SLUG),
        "exception message should name the modpack, was: " + ex.getMessage());
  }
}
