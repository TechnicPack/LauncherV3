package net.technicpack.minecraftcore.mojang.auth.io;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class UserProperties {
    private Multimap<String, String> internalMap;

    public UserProperties() {
        this.internalMap = HashMultimap.create();
    }

    public Iterable<String> keys() { return this.internalMap.keySet(); }
    public Iterable<String> values(String key) { return this.internalMap.get(key); }

    public void add(String name, String value) {
        this.internalMap.put(name, value);
    }

    public void merge(UserProperties properties) {
        for (String key : properties.keys()) {
            for (String value : properties.values(key)) {
                add(key,value);
            }
        }
    }
}
