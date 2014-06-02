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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractRules {
    private List<String> exclude = new ArrayList<String>();

    public ExtractRules() {

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
