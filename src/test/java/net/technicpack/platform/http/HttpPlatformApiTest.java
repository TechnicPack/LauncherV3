package net.technicpack.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import net.technicpack.platform.io.PlatformPackInfo;
import net.technicpack.rest.RestfulAPIException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpPlatformApiTest {
  @ParameterizedTest
  @ValueSource(
      strings = {"{}", "{\"name\":null}", "{\"name\":\"   \"}", "{\"name\":\"other-pack\"}"})
  void rejectsInvalidIdentityAndAcceptsLaterMatchingResponse(String invalidJson) throws Exception {
    AtomicReference<byte[]> response =
        new AtomicReference<>(invalidJson.getBytes(StandardCharsets.UTF_8));
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    try {
      server.createContext(
          "/api/modpack/test-pack",
          exchange -> {
            try (exchange) {
              byte[] body = response.get();
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, body.length);
              exchange.getResponseBody().write(body);
            }
          });
      server.start();
      HttpPlatformApi api =
          new HttpPlatformApi(
              "http://127.0.0.1:" + server.getAddress().getPort() + "/api/", "test");

      assertThrows(RestfulAPIException.class, () -> api.getPlatformPackInfo("test-pack"));
      assertThrows(RestfulAPIException.class, () -> api.getPlatformPackInfoForBulk("test-pack"));

      response.set("{\"name\":\"test-pack\"}".getBytes(StandardCharsets.UTF_8));
      PlatformPackInfo recovered = api.getPlatformPackInfo("test-pack");
      assertEquals("test-pack", recovered.getName());
      assertFalse(recovered.isLocal());
    } finally {
      server.stop(0);
    }
  }
}
