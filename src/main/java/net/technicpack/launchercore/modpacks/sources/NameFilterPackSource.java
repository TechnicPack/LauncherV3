package net.technicpack.launchercore.modpacks.sources;

import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.rest.io.PackInfo;

import java.util.Collection;
import java.util.LinkedList;

public class NameFilterPackSource implements IPackSource {
    private Collection<ModpackModel> baseModpacks;
    private String filterTerms;

    public NameFilterPackSource(Collection<ModpackModel> modpacks, String filter) {
        this.baseModpacks = modpacks;
        this.filterTerms = filter.toUpperCase();
    }

    @Override
    public String getSourceName() {
        return "Installed packs filtered by '"+filterTerms+"'";
    }

    @Override
    public Collection<PackInfo> getPublicPacks() {
        LinkedList<PackInfo> info = new LinkedList<PackInfo>();

        for (ModpackModel modpack : baseModpacks) {
            if (modpack.getDisplayName().toUpperCase().contains(filterTerms)) {
                info.add(modpack.getPackInfo());
            }
        }

        return info;
    }
}
