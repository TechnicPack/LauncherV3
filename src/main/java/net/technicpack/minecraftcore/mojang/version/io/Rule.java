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

import net.technicpack.utilslib.OperatingSystem;

public class Rule {
	private Action action = Action.ALLOW;
	private OSRestriction os;

	public Action getAction() {
		if (this.os != null && !this.os.matchesCurrentOperatingSystem()) {
			return null;
		}

		return action;
	}

	public static enum Action {
		ALLOW,
		DISALLOW
	}

	public class OSRestriction {
		private OperatingSystem name;
		private String version;

		public boolean matchesCurrentOperatingSystem() {
			if (this.name != null && (this.name != OperatingSystem.getOperatingSystem())) {
				return false;
			}

			boolean matched = true;

			if (this.version != null) {
				String osVersion = System.getProperty("os.version");
				matched = osVersion.matches(this.version);
			}

			return matched;
		}
	}
}
