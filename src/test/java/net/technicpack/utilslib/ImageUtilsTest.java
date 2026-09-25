package net.technicpack.utilslib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ImageUtilsTest {
  @ParameterizedTest
  @CsvSource({
    "500, 500, 220, 220",
    "740, 370, 370, 185",
    "250, 500, 110, 220",
    "100, 50, 100, 50",
    "370, 220, 370, 220",
    "1, 1000, 1, 220"
  })
  void fitsArtworkWithoutDistortionOrUpscaling(
      int sourceWidth, int sourceHeight, int expectedWidth, int expectedHeight) {
    BufferedImage source =
        new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < sourceHeight; y++) {
      for (int x = 0; x < sourceWidth; x++) {
        source.setRGB(x, y, 0x80ff0000);
      }
    }

    BufferedImage result = ImageUtils.fitImage(source, 370, 220);

    assertEquals(370, result.getWidth());
    assertEquals(220, result.getHeight());
    int left = (370 - expectedWidth) / 2;
    int top = (220 - expectedHeight) / 2;
    for (int y = 0; y < 220; y++) {
      for (int x = 0; x < 370; x++) {
        boolean artwork =
            x >= left && x < left + expectedWidth && y >= top && y < top + expectedHeight;
        assertEquals(artwork ? 0x80ff0000 : 0, result.getRGB(x, y));
      }
    }
  }
}
