package net.technicpack.minecraftcore.microsoft.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MicrosoftAuthenticatorTest {
  @ParameterizedTest
  @ValueSource(ints = {200, 204, 401, 403, 429, 500, 503})
  void emptyXboxResponseBecomesCheckedErrorWithStatus(int status) throws Exception {
    withResponse(
        status,
        "",
        response -> {
          IOException error =
              assertThrows(
                  IOException.class, () -> MicrosoftAuthenticator.parseXboxResponse(response));
          assertTrue(error.getMessage().contains("HTTP " + status));
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", " ", "{", "[]", "{\"Token\":[]}"})
  void invalidXboxResponseBecomesCheckedError(String body) throws Exception {
    withResponse(
        200,
        body,
        response -> {
          IOException error =
              assertThrows(
                  IOException.class, () -> MicrosoftAuthenticator.parseXboxResponse(response));
          assertTrue(error.getMessage().contains("HTTP 200"));
        });
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 401})
  void xboxResponseParsingRemainsIndependentOfHttpStatus(int status) throws Exception {
    withResponse(
        status,
        "{\"Token\":\"xbox-token\"}",
        response ->
            assertEquals("xbox-token", MicrosoftAuthenticator.parseXboxResponse(response).token));
  }

  private static void withResponse(int status, String body, ResponseCheck check) throws Exception {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) exchange.getResponseBody().write(bytes);
          }
        });
    server.start();
    try {
      var request =
          new NetHttpTransport()
              .createRequestFactory()
              .buildGetRequest(
                  new GenericUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
      request.setParser(new JsonObjectParser(new GsonFactory()));
      request.setThrowExceptionOnExecuteError(false);
      HttpResponse response = request.execute();
      try {
        check.accept(response);
      } finally {
        response.disconnect();
      }
    } finally {
      server.stop(0);
    }
  }

  private interface ResponseCheck {
    void accept(HttpResponse response) throws Exception;
  }
}
