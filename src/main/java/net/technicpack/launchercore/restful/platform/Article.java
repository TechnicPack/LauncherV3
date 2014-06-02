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

package net.technicpack.launchercore.restful.platform;

import net.technicpack.launchercore.restful.PlatformConstants;
import net.technicpack.launchercore.restful.Resource;

public class Article {
    private String title;
    private String displayTitle;
    private Resource image;
    private String category;
    private String user;
    private String summary;
    private String date;

    public String getTitle() {
        return title;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public Resource getImage() {
        return image;
    }

    public String getCategory() {
        return category;
    }

    public String getUser() {
        return user;
    }

    public String getSummary() {
        return summary;
    }

    public String getDate() {
        return date;
    }

    public String getUrl() {
        return PlatformConstants.PLATFORM + "article/view/" + title;
    }
}
