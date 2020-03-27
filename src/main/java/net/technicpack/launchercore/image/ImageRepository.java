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

package net.technicpack.launchercore.image;

import java.util.HashMap;
import java.util.Map;

public class ImageRepository<T> {
    private IImageMapper<T> mapper;
    private IImageStore<T> store;
    private Map<String, ImageJob> allJobs = new HashMap<String, ImageJob>();

    public ImageRepository(IImageMapper<T> mapper, IImageStore<T> store) {
        this.mapper = mapper;
        this.store = store;
    }

    public ImageJob startImageJob(T key) {
        String jobKey = store.getJobKey(key);

        ImageJob<T> job = null;
        if (allJobs.containsKey(jobKey))
            job = allJobs.get(jobKey);
        else {
            job = new ImageJob<T>(mapper, store);
            allJobs.put(jobKey, job);
        }

        if (job.canRetry())
            job.start(key);

        return job;
    }

    public void refreshRetry(T key) {
        String jobKey = store.getJobKey(key);

        if (allJobs.containsKey(jobKey))
            allJobs.get(jobKey).refreshRetry();
    }
}
