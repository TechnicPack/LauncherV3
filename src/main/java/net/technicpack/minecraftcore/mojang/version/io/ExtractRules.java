/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.version.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"unused"})
public class ExtractRules {
	private List<String> exclude = new ArrayList<>();

	public ExtractRules() {

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ExtractRules that = (ExtractRules) o;
		return Objects.equals(exclude, that.exclude);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(exclude);
	}

	public ExtractRules(String[] exclude) {
		if (exclude != null) {
			Collections.addAll(this.exclude, exclude);
		}
	}

	public List<String> getExclude() {
		return exclude;
	}

	public boolean shouldExtract(String path) {
		if (this.exclude != null) {
			for (String rule : this.exclude) {
				if (path.startsWith(rule)) {
					return false;
				}
			}
		}

		return true;
	}
}
