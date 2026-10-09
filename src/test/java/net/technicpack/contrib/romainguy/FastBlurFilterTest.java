package net.technicpack.contrib.romainguy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FastBlurFilterTest {
  @Test
  void repeatedInPlaceBlurMatchesSeparatePassesIncludingClampedEdges() {
    Random random = new Random(81234);
    for (int[] size : new int[][] {{1, 1}, {2, 7}, {19, 13}}) {
      int width = size[0];
      int height = size[1];
      int[] source = new int[width * height];
      for (int i = 0; i < source.length; i++) source[i] = random.nextInt();
      BufferedImage actual = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      actual.setRGB(0, 0, width, height, source, 0, width);
      BufferedImage expected = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      expected.setRGB(0, 0, width, height, source, 0, width);
      FastBlurFilter filter = new FastBlurFilter(4);
      for (int i = 0; i < 4; i++) expected = filter.filter(expected, null);
      filter.filter(actual, actual, 4);
      assertArrayEquals(
          expected.getRGB(0, 0, width, height, null, 0, width),
          actual.getRGB(0, 0, width, height, null, 0, width));
    }
  }

  @Test
  void rejectsNonpositiveIterations() {
    FastBlurFilter filter = new FastBlurFilter(4);
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    assertThrows(IllegalArgumentException.class, () -> filter.filter(image, image, 0));
    assertThrows(IllegalArgumentException.class, () -> filter.filter(image, image, -1));
  }
}
