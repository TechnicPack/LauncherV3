/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.launch;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;

public class LaunchOptions {

  /**
   * The window size passed to the game. A maximized window starts at the primary screen's usable
   * area (excluding the taskbar or dock); on Windows it is then maximized once it appears.
   */
  public static Dimension getLaunchResolution(ILaunchOptions options) {
    if (options.getLaunchWindowType() == WindowType.MAXIMIZED) {
      try {
        if (!GraphicsEnvironment.isHeadless()) {
          Rectangle bounds =
              GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
          if (bounds.width > 0 && bounds.height > 0) {
            return new Dimension(bounds.width, bounds.height);
          }
        }
      } catch (HeadlessException ignored) {
        // Fall through to the configured dimensions.
      }
    }
    return new Dimension(options.getCustomWidth(), options.getCustomHeight());
  }

  private String title;
  private String iconPath;
  private ILaunchOptions options;

  public LaunchOptions(String title, String iconPath, ILaunchOptions options) {
    this.options = options;
    this.title = title;
    this.iconPath = iconPath;
  }

  public String getTitle() {
    return title;
  }

  public String getIconPath() {
    return iconPath;
  }

  public ILaunchOptions getOptions() {
    return options;
  }

  public void appendToCommands(LaunchCommandCollector commands) {
    if (getTitle() != null) {
      commands.addUnique("--title", title);
    }

    WindowType windowType = options.getLaunchWindowType();
    if (windowType == WindowType.FULLSCREEN) commands.addUnique("--fullscreen");
    else if (windowType != null && windowType.hasCustomResolution()) {
      Dimension size = getLaunchResolution(options);
      commands.addUnique("--width", Integer.toString(size.width));
      commands.addUnique("--height", Integer.toString(size.height));
    }

    if (getIconPath() != null) {
      commands.addUnique("--icon", getIconPath());
    }
  }
}
