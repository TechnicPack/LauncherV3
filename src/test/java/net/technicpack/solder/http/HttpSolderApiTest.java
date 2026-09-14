package net.technicpack.solder.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.technicpack.rest.RestfulAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpSolderApiTest {

  @Test
  void publicPacksUrlNeverContainsClientId() {
    String url = HttpSolderApi.buildPublicPacksUrl("https://solder.example.com/api/");
    assertEquals("https://solder.example.com/api/modpack?include=full", url);
    assertFalse(url.contains("cid"), "pack list URL must not contain a cid parameter, was: " + url);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"modpacks\":null}",
        "{\"modpacks\":{\"test-pack\":null}}",
        "{\"modpacks\":{\"test-pack\":{\"builds\":[]}}}",
        "{\"modpacks\":{\"test-pack\":{\"name\":\"other-pack\",\"builds\":[]}}}",
        "{\"modpacks\":{\" \":{\"name\":\" \",\"builds\":[]}}}",
        "{\"modpacks\":{\"test-pack\":{\"name\":\"test-pack\"}}}",
        "{\"modpacks\":{\"test-pack\":{\"name\":\"test-pack\",\"builds\":[null]}}}",
        "{\"modpacks\":{\"test-pack\":{\"name\":\"test-pack\",\"builds\":[\" \"]}}}"
      })
  void discoveryRejectsMalformedCatalogsAndMismatchedMapKeys(String json) throws Exception {
    HttpServer server = publicCatalogServer(json);
    try {
      HttpSolderApi api = new HttpSolderApi(slug -> null);
      String root = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/";
      assertThrows(RestfulAPIException.class, () -> api.getPublicSolderPacks(root));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void discoveryAcceptsEmptyBuildAndPublicCatalogs() throws Exception {
    HttpServer server =
        publicCatalogServer(
            "{\"mirror_url\":\"https://mirror.example/\","
                + "\"modpacks\":{\"test-pack\":{\"name\":\"test-pack\",\"builds\":[]}}}");
    try {
      HttpSolderApi api = new HttpSolderApi(slug -> null);
      var packs =
          api.getPublicSolderPacks("http://127.0.0.1:" + server.getAddress().getPort() + "/api/");
      assertEquals(List.of("test-pack"), packs.stream().map(info -> info.getName()).toList());
      assertEquals(List.of(), packs.iterator().next().getBuilds());
    } finally {
      server.stop(0);
    }

    HttpServer empty = publicCatalogServer("{\"modpacks\":{}}");
    try {
      assertEquals(
          List.of(),
          new HttpSolderApi(slug -> null)
              .getPublicSolderPacks("http://127.0.0.1:" + empty.getAddress().getPort() + "/api/"));
    } finally {
      empty.stop(0);
    }
  }

  private HttpServer publicCatalogServer(String json) throws Exception {
    byte[] response = json.getBytes(StandardCharsets.UTF_8);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/modpack",
        exchange -> {
          try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
          }
        });
    server.start();
    return server;
  }
}
