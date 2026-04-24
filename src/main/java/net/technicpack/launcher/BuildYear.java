/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher;

import java.nio.charset.StandardCharsets;
import net.technicpack.ui.lang.ResourceLoader;
import org.apache.commons.io.IOUtils;

/**
 * Reads the four-digit build year written to {@code /buildyear} at build time by the launcher
 * packaging plugin's {@code processResources} task. Used to render an accurate copyright range in
 * the About tab without requiring a yearly source edit.
 */
public final class BuildYear {
  private BuildYear() {}

  /**
   * Returns the build year string, or {@code "2015"} (the founding year) if the resource is
   * unreadable — typically only when running the launcher without having passed the packaging
   * plugin's resource pipeline (e.g., a bare {@code compileJava}-only dev run).
   */
  public static String read(ResourceLoader resources) {
    try {
      return IOUtils.toString(resources.getResourceAsStream("/buildyear"), StandardCharsets.UTF_8)
          .trim();
    } catch (Exception e) {
      return "2015";
    }
  }
}
