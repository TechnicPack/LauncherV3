package net.technicpack.launchercore.modpacks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryModpackContainer implements IModpackContainer {
    private Map<String, ModpackModel> modpacks = new HashMap<String, ModpackModel>();
    private Map<IModpackContainer, IModpackContainer> passthroughContainers = new ConcurrentHashMap<IModpackContainer, IModpackContainer>();

    public MemoryModpackContainer() {
    }

    @Override
    public void clear() {
        modpacks.clear();

        for (IModpackContainer container : passthroughContainers.values())
            container.clear();
    }

    @Override
    public void addModpackToContainer(ModpackModel modpack) {
        modpacks.put(modpack.getName(), modpack);

        for (IModpackContainer container : passthroughContainers.values())
            container.addModpackToContainer(modpack);
    }

    @Override
    public void replaceModpackInContainer(ModpackModel modpack) {
        if (modpacks.containsKey(modpack.getName()))
            modpacks.put(modpack.getName(), modpack);

        for (IModpackContainer container : passthroughContainers.values())
            container.replaceModpackInContainer(modpack);
    }

    @Override
    public void refreshComplete() {
        for (IModpackContainer container : passthroughContainers.values())
            container.refreshComplete();
    }

    public void addPassthroughContainer(IModpackContainer container) {
        passthroughContainers.put(container, container);
    }

    public void removePassthroughContainer(IModpackContainer container) {
        passthroughContainers.remove(container);
    }

    public Collection<ModpackModel> getModpacks() {
        return modpacks.values();
    }
}
