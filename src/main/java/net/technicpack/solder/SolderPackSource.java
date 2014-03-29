package net.technicpack.solder;

import net.technicpack.launchercore.modpacks.sources.IPackSource;
import net.technicpack.rest.RestfulAPIException;
import net.technicpack.rest.io.PackInfo;
import net.technicpack.solder.io.SolderPackInfo;
import net.technicpack.utilslib.Utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;

public class SolderPackSource implements IPackSource {
    private String baseUrl;
    private ISolderApi solder;

    public SolderPackSource(String baseUrl, ISolderApi solder) {
        this.baseUrl = baseUrl;
        this.solder = solder;
    }

    @Override
    public String getSourceName() {
        return "Public packs for solder "+baseUrl;
    }

    @Override
    public Collection<PackInfo> getPublicPacks() {
        LinkedList<PackInfo> returnValue = new LinkedList<PackInfo>();

        try {
            Collection<SolderPackInfo> packs = solder.getPublicSolderPacks(baseUrl);

            for(SolderPackInfo info : packs) {
                returnValue.add(info);
            }
        } catch (RestfulAPIException ex) {
            Utils.getLogger().log(Level.WARNING, "Unable to load technic modpacks", ex);
        }

        return returnValue;
    }
}
