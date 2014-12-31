package net.technicpack.launchercore.modpacks.sources;

import net.technicpack.launchercore.modpacks.MemoryModpackContainer;
import net.technicpack.launchercore.modpacks.ModpackModel;
import net.technicpack.rest.io.PackInfo;

import java.util.Collection;
import java.util.LinkedList;

public class NameFilterPackSource implements IPackSource {
    private MemoryModpackContainer baseModpacks;
    private String filterTerms;

    public NameFilterPackSource(MemoryModpackContainer modpacks, String filter) {
        this.baseModpacks = modpacks;
        this.filterTerms = filter.toUpperCase();
    }

    @Override
    public String getSourceName() {
        return "Installed packs filtered by '" + filterTerms + "'";
    }

    @Override
    public Collection<PackInfo> getPublicPacks() {
        LinkedList<PackInfo> info = new LinkedList<PackInfo>();

        for (ModpackModel modpack : baseModpacks.getModpacks()) {
            if (modpack.getDisplayName().toUpperCase().contains(filterTerms)) {
                info.add(modpack.getPackInfo());
            }
        }

        return info;
    }

    @Override
    public int getPriority(PackInfo info) {
        for (ModpackModel modpack : baseModpacks.getModpacks()) {
            if (modpack.getName().equals(info.getName()))
                return modpack.getPriority();
        }

        return 0;
    }

    @Override
    public boolean isOfficialPack(String slug) {
        for (ModpackModel model : baseModpacks.getModpacks()) {
            if (model.getName().equals(slug))
                return model.isOfficial();
        }

        return false;
    }
}
