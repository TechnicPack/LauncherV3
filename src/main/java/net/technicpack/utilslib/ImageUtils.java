/*
 * This file is part of Technic Launcher.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.utilslib;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ImageUtils {

  public static BufferedImage scaleWithAspectWidth(BufferedImage img, int width) {
    int imgWidth = img.getWidth();
    int imgHeight = img.getHeight();
    int height = imgHeight * width / imgWidth;
    return scaleImage(img, width, height);
  }

  /** Fits artwork on a centered transparent canvas without upscaling. */
  public static BufferedImage fitImage(BufferedImage img, int width, int height) {
    if (img.getWidth() == width && img.getHeight() == height) {
      return img;
    }

    double scale =
        Math.min(1.0, Math.min((double) width / img.getWidth(), (double) height / img.getHeight()));
    int scaledWidth = Math.max(1, (int) Math.round(img.getWidth() * scale));
    int scaledHeight = Math.max(1, (int) Math.round(img.getHeight() * scale));
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = result.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.drawImage(
          img,
          (width - scaledWidth) / 2,
          (height - scaledHeight) / 2,
          scaledWidth,
          scaledHeight,
          null);
    } finally {
      g.dispose();
    }
    return result;
  }

  public static BufferedImage scaleImage(BufferedImage img, int width, int height) {
    if (img.getWidth() == width && img.getHeight() == height) {
      return img; // No scaling needed
    }

    BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = newImage.createGraphics();
    try {
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.drawImage(img, 0, 0, width, height, null);
    } finally {
      g.dispose();
    }
    return newImage;
  }
}
