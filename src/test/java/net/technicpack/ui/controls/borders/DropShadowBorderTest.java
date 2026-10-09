package net.technicpack.ui.controls.borders;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.technicpack.contrib.romainguy.FastBlurFilter;
import org.junit.jupiter.api.Test;

class DropShadowBorderTest {
  @Test
  void cachedShadowMatchesFourBlursAfterResizingAndMoving() {
    for (Color color : new Color[] {Color.BLACK, new Color(60, 120, 200, 150)}) {
      DropShadowBorder border = new DropShadowBorder(color, 4);
      for (int[] size : new int[][] {{80, 64}, {80, 64}, {127, 91}, {51, 33}, {80, 64}}) {
        int width = size[0];
        int height = size[1];
        BufferedImage shadow = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fill = shadow.createGraphics();
        fill.setColor(color);
        fill.fillRect(16, 16, width - 32, height - 32);
        fill.dispose();
        FastBlurFilter blur = new FastBlurFilter(4);
        for (int i = 0; i < 4; i++) shadow = blur.filter(shadow, null);
        for (int offset : new int[] {0, 7}) {
          BufferedImage expected =
              new BufferedImage(width + 14, height + 14, BufferedImage.TYPE_INT_ARGB);
          Graphics2D expectedGraphics = expected.createGraphics();
          expectedGraphics.drawImage(shadow, offset, offset, null);
          expectedGraphics.dispose();
          BufferedImage actual =
              new BufferedImage(width + 14, height + 14, BufferedImage.TYPE_INT_ARGB);
          Graphics2D actualGraphics = actual.createGraphics();
          border.paintBorder(null, actualGraphics, offset, offset, width, height);
          actualGraphics.dispose();
          assertArrayEquals(pixels(expected), pixels(actual));
        }
      }
    }
  }

  private static int[] pixels(BufferedImage image) {
    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
  }
}
