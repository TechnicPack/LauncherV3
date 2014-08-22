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

package net.technicpack.platform.packsources;

import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.io.SearchResult;
import net.technicpack.platform.io.SearchResultsData;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SearchResultPackSource implements IPackSource {
    private IPlatformApi platformApi;
    private String searchTerms;

    public SearchResultPackSource(IPlatformApi platformApi, String searchTerms) {
        this.platformApi = platformApi;
        this.searchTerms = searchTerms;
    }

    @Override
    public String getSourceName() {
        return "Modpack search results for query '"+searchTerms+"'";
    }

    @Override
    //Get PlatformPackInfo objects for every result from the given search terms.
    public Collection<PackInfo> getPublicPacks() {
        //Get results from server
        SearchResultsData results = null;
        try {
            results = platformApi.getSearchResults(searchTerms);
        } catch (RestfulAPIException ex) {
            return Collections.emptySet();
        }

        //Set up a thread pulling a PlatformPackInfo for each result in the list
        final ArrayList<PackInfo> resultPacks = new ArrayList<PackInfo>(results.getResults().size());
        final ArrayList<Thread> hitThreads = new ArrayList<Thread>(results.getResults().size());
        final IPlatformApi api = this.platformApi;

        int i = 0;
        for (final SearchResult result : results.getResults()) {
            final int index = i;
            hitThreads.set(i, new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        resultPacks.set(index, api.getPlatformPackInfo(result.getSlug()));
                    } catch (RestfulAPIException ex) {
                        resultPacks.set(index, null);
                    }
                }
            }));
            i++;
        }

        //Wait for all result threads to complete
        for(Thread thread : hitThreads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
            }
        }

        //Remove null results (we end up with these if there was an exception while pulling the result)
        for (int j = resultPacks.size()-1; j >= 0; j--) {
            if (resultPacks.get(j) == null)
                resultPacks.remove(j);
        }

        return resultPacks;
    }
}
