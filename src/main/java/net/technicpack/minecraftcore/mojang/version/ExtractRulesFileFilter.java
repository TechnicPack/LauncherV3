package net.technicpack.minecraftcore.mojang.version;

import net.technicpack.minecraftcore.mojang.version.io.ExtractRules;
import net.technicpack.utilslib.IZipFileFilter;

public class ExtractRulesFileFilter implements IZipFileFilter {
    private ExtractRules rules;

    public ExtractRulesFileFilter(ExtractRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean shouldExtract(String fileName) {
        return this.rules.shouldExtract(fileName);
    }
}
