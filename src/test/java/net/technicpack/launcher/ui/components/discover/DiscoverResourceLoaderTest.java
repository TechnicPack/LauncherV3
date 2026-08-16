package net.technicpack.launcher.ui.components.discover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.technicpack.launchercore.TechnicConstants;
import org.junit.jupiter.api.Test;
import org.xhtmlrenderer.resource.ImageResource;

class DiscoverResourceLoaderTest {
  @Test
  void uppercaseHttpSchemeUsesLauncherUserAgent() throws Exception {
    TechnicConstants.setBuildNumber(() -> "test");

    AtomicReference<String> userAgent = new AtomicReference<>();
    byte[] image = createPng();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/image.png",
        exchange -> {
          userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
          respond(exchange, 200, image);
        });
    server.start();

    try {
      String uri = "HTTP://127.0.0.1:" + server.getAddress().getPort() + "/image.png";

      ImageResource resource = DiscoverResourceLoader.loadImageResourceFromUri(uri);

      assertNotNull(resource);
      assertNotNull(resource.getImage());
      assertEquals(TechnicConstants.getUserAgent(), userAgent.get());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void failedHttpImageRequestDisconnectsConnection() throws Exception {
    ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
    try (ServerSocket server = new ServerSocket()) {
      server.bind(new InetSocketAddress("127.0.0.1", 0));
      Future<Boolean> connectionClosed =
          serverExecutor.submit(
              () -> {
                try (Socket socket = server.accept()) {
                  socket.setSoTimeout(2_000);
                  InputStream input = socket.getInputStream();
                  readRequestHeaders(input);

                  OutputStream output = socket.getOutputStream();
                  output.write(
                      ("HTTP/1.1 404 Not Found\r\n"
                              + "Content-Length: 1\r\n"
                              + "Connection: keep-alive\r\n"
                              + "\r\n"
                              + "x")
                          .getBytes(StandardCharsets.ISO_8859_1));
                  output.flush();

                  try {
                    return input.read() == -1;
                  } catch (SocketTimeoutException e) {
                    return false;
                  }
                }
              });

      String uri = "http://127.0.0.1:" + server.getLocalPort() + "/missing.png";
      assertNull(DiscoverResourceLoader.loadImageResourceFromUri(uri));
      assertTrue(
          connectionClosed.get(5, TimeUnit.SECONDS),
          "failed image request should close its HTTP connection");
    } finally {
      serverExecutor.shutdownNow();
    }
  }

  private static byte[] createPng() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", output);
    return output.toByteArray();
  }

  private static void respond(HttpExchange exchange, int statusCode, byte[] body)
      throws IOException {
    exchange.sendResponseHeaders(statusCode, body.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(body);
    }
  }

  private static void readRequestHeaders(InputStream input) throws IOException {
    int matched = 0;
    int[] terminator = {'\r', '\n', '\r', '\n'};
    while (matched < terminator.length) {
      int value = input.read();
      if (value == -1) {
        throw new IOException("connection closed before request headers completed");
      }
      matched = value == terminator[matched] ? matched + 1 : value == terminator[0] ? 1 : 0;
    }
  }
}
