package net.technicpack.launcher.ui.components.discover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launchercore.TechnicConstants;
import net.technicpack.ui.lang.ResourceLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.XHTMLPanel;

class DiscoverInfoPanelTest {
  private static String previousXmlReader;

  @BeforeAll
  static void configureHtmlParser() {
    previousXmlReader = System.getProperty("xr.load.xml-reader");
    // Match LauncherMain: the bundled fallback is HTML, not strict XHTML.
    System.setProperty("xr.load.xml-reader", "org.ccil.cowan.tagsoup.Parser");
  }

  @AfterAll
  static void restoreHtmlParser() {
    if (previousXmlReader == null) {
      System.clearProperty("xr.load.xml-reader");
    } else {
      System.setProperty("xr.load.xml-reader", previousXmlReader);
    }
  }

  @TempDir Path directory;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void remoteImagesShareCacheWithoutChangingBackgroundSize(boolean relative) throws Exception {
    TechnicConstants.setBuildNumber(() -> "test");
    AtomicReference<String> userAgent = new AtomicReference<>();
    AtomicInteger requests = new AtomicInteger();
    byte[] image = createPng();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/image.png",
        exchange -> {
          userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
          requests.incrementAndGet();
          respond(exchange, 200, image);
        });
    server.start();

    try {
      String baseUri = "HTTP://127.0.0.1:" + server.getAddress().getPort() + "/";
      String uri = relative ? "/image.png" : baseUri + "image.png";
      XHTMLPanel panel = createPanel(imageDocument(uri), baseUri);
      assertImageSizes(render(panel));
      assertImageSizes(render(panel));
      assertEquals(TechnicConstants.getUserAgent(), userAgent.get());
      assertEquals(1, requests.get(), "image elements and CSS backgrounds should share the image");
    } finally {
      server.stop(0);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"file", "jar", "base64"})
  void localAndEmbeddedImagesKeepNaturalSizeAndAspectRatio(String source) throws Exception {
    byte[] image = createPng();
    String uri;
    if (source.equals("base64")) {
      uri = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
    } else if (source.equals("jar")) {
      Path jar = directory.resolve("images.jar");
      try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
        output.putNextEntry(new JarEntry("image.png"));
        output.write(image);
        output.closeEntry();
      }
      uri = "jar:" + jar.toUri() + "!/image.png";
    } else {
      Path file = directory.resolve("image.png");
      Files.write(file, image);
      uri = file.toUri().toString();
    }

    XHTMLPanel panel = createPanel(imageDocument(uri));
    assertImageSizes(render(panel));
    assertImageSizes(render(panel));
  }

  @ParameterizedTest
  @CsvSource({"false, 404", "false, 200", "true, 404", "true, 200"})
  void remoteImageFailuresShowBundledOfflinePage(boolean background, int status) throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/image.png",
        exchange -> respond(exchange, status, "not an image".getBytes(StandardCharsets.UTF_8)));
    server.start();

    try {
      String uri = "http://127.0.0.1:" + server.getAddress().getPort() + "/image.png";
      String body =
          background
              ? "<div style=\"width:40px;height:20px;background-image:url('" + uri + "')\"></div>"
              : "<img src=\"" + uri + "\" />";
      XHTMLPanel panel = createPanel("<html><head></head><body>" + body + "</body></html>");
      render(panel);
      // The failure listener installs the fallback in a subsequent EDT task.
      SwingUtilities.invokeAndWait(
          () -> {
            try (InputStream input = resources().getResourceAsStream("/discoverFallback.html")) {
              Document fallback = XMLResource.load(input).getDocument();
              assertTrue(
                  fallback.isEqualNode(panel.getDocument()),
                  "failed image loading should display the bundled offline page");
            } catch (IOException e) {
              throw new AssertionError(e);
            }
          });
      render(panel);
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
      assertThrows(
          IllegalStateException.class, () -> new DiscoverUserAgent(() -> {}).getImageResource(uri));
      assertTrue(
          connectionClosed.get(5, TimeUnit.SECONDS),
          "failed image request should close its HTTP connection");
    } finally {
      serverExecutor.shutdownNow();
    }
  }

  private XHTMLPanel createPanel(String html) throws Exception {
    return createPanel(html, directory.toUri().toString());
  }

  private XHTMLPanel createPanel(String html, String baseUri) throws Exception {
    AtomicReference<XHTMLPanel> result = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          DiscoverInfoPanel discover =
              new DiscoverInfoPanel(
                  resources(), baseUri, null, new LauncherFileSystem(directory), null) {
                @Override
                protected Document getDiscoverDocument(String url, Path localCache) {
                  return XMLResource.load(new StringReader(html)).getDocument();
                }
              };
          discover.setSize(320, 240);
          discover.doLayout();
          result.set((XHTMLPanel) discover.getComponent(0));
        });
    // The constructor queues initial document loading on the EDT.
    SwingUtilities.invokeAndWait(() -> {});
    return result.get();
  }

  private static ResourceLoader resources() {
    return new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
  }

  private static String imageDocument(String uri) {
    return "<html><head></head><body style=\"margin:0;background:white\">"
        + "<img src=\""
        + uri
        + "\" style=\"position:absolute;left:10px;top:10px;width:20px\" />"
        + "<img src=\""
        + uri
        + "\" style=\"position:absolute;left:10px;top:50px\" />"
        + "<div style=\"position:absolute;left:10px;top:90px;width:60px;height:40px;"
        + "background:url('"
        + uri
        + "') no-repeat\"></div>"
        + "<img src=\""
        + uri
        + "\" style=\"position:absolute;left:10px;top:140px;height:5px\" />"
        + "<img src=\""
        + uri
        + "\" style=\"position:absolute;left:10px;top:180px;width:12px;height:8px\" />"
        + "</body></html>";
  }

  private static BufferedImage render(XHTMLPanel panel) throws Exception {
    BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
    SwingUtilities.invokeAndWait(
        () -> {
          Graphics2D graphics = image.createGraphics();
          try {
            panel.paint(graphics);
          } finally {
            graphics.dispose();
          }
        });
    return image;
  }

  private static void assertImageSizes(BufferedImage image) {
    assertRedRectangle(image, 10, 10, 20, 10);
    assertRedRectangle(image, 10, 50, 40, 20);
    assertRedRectangle(image, 10, 90, 40, 20);
    assertRedRectangle(image, 10, 140, 10, 5);
    assertRedRectangle(image, 10, 180, 12, 8);
  }

  private static void assertRedRectangle(BufferedImage image, int x, int y, int width, int height) {
    assertEquals(Color.RED.getRGB(), image.getRGB(x, y));
    assertEquals(Color.RED.getRGB(), image.getRGB(x + width - 1, y + height - 1));
    assertEquals(Color.WHITE.getRGB(), image.getRGB(x + width, y));
    assertEquals(Color.WHITE.getRGB(), image.getRGB(x, y + height));
  }

  private static byte[] createPng() throws IOException {
    BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(Color.RED);
      graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    } finally {
      graphics.dispose();
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, "png", output);
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
