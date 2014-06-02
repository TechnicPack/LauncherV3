/*
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
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

package net.technicpack.launchercore.minecraft;

import java.util.HashMap;
import java.util.Map;

public enum ReleaseType {
    SNAPSHOT("snapshot"),
    RELEASE("release"),
    OLD_BETA("old-beta"),
    OLD_ALPHA("old-alpha");

    private static final Map<String, ReleaseType> lookup = new HashMap<String, ReleaseType>();
    private final String name;

    private ReleaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ReleaseType get(String name) {
        return lookup.get(name);
    }

    static {
        for (ReleaseType type : values()) {
            lookup.put(type.getName(), type);
        }
    }
}
