package net.technicpack.launcher.ui.components.discover;

import java.awt.Image;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import net.technicpack.utilslib.Urls;
import net.technicpack.utilslib.Utils;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.NaiveUserAgent;

final class DiscoverUserAgent extends NaiveUserAgent {
  private final Runnable onImageFailure;

  DiscoverUserAgent(Runnable onImageFailure) {
    this.onImageFailure = onImageFailure;
  }

  @Override
  public ImageResource getImageResource(String uri) {
    // The img factory resolves URLs before lookup, while CSS backgrounds can supply relative
    // URLs. NaiveUserAgent keys its cache by the input, so resolve both paths consistently.
    return super.getImageResource(resolveURI(uri));
  }

  @Override
  protected InputStream openStream(String uri) throws IOException {
    if (!isHttpUri(uri)) {
      return super.openStream(uri);
    }

    final HttpURLConnection connection =
        Utils.openHttpConnection(Urls.parseAndDiagnose(uri, "DiscoverUserAgent.openStream"));
    try {
      return new FilterInputStream(connection.getInputStream()) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } finally {
            connection.disconnect();
          }
        }
      };
    } catch (IOException | RuntimeException e) {
      connection.disconnect();
      throw e;
    }
  }

  @Override
  protected ImageResource createImageResource(String uri, Image image) {
    if (image == null && isHttpUri(uri)) {
      // CSS background painting swallows exceptions, so notify the panel before propagating the
      // failure. Its one-shot fallback also handles ordinary layout/render exceptions.
      onImageFailure.run();
      throw new IllegalStateException("Unable to load Discover image: " + uri);
    }
    return super.createImageResource(uri, image);
  }

  private static boolean isHttpUri(String uri) {
    return uri != null
        && (uri.regionMatches(true, 0, "http:", 0, 5)
            || uri.regionMatches(true, 0, "https:", 0, 6));
  }
}
