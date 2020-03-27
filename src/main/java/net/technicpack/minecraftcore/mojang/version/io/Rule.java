/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.version.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.launch.WindowType;
import net.technicpack.utilslib.OperatingSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Rule {

    public static boolean isAllowable(List<Rule> rules, ILaunchOptions opts) {
        for (int i = rules.size() - 1; i >= 0; i--) {
            Rule rule = rules.get(i);
            if (rule.isApplicable(opts)) {
                return !rule.isNegated();
            }
        }
        return false;
    }

    private final boolean negated;
    private final List<RuleCondition> conditions = new ArrayList<>();

    public Rule(JsonObject rule) {
        String action = rule.get("action").getAsString();
        switch (action) {
            case "allow":
                negated = false;
                break;
            case "disallow":
                negated = true;
                break;
            default:
                throw new JsonParseException("Expected rule action, got: " + action);
        }

        if (rule.has("os")) {
            JsonObject os = rule.get("os").getAsJsonObject();
            conditions.add(new OsCondition(
                    os.has("name") ? os.get("name").getAsString() : null,
                    os.has("version") ? os.get("version").getAsString() : null,
                    os.has("arch") ? os.get("arch").getAsString() : null));
        }
        if (rule.has("features")) {
            JsonObject features = rule.get("features").getAsJsonObject();
            if (features.has("has_custom_resolution")) {
                conditions.add(new CustomResolutionCondition(features.get("has_custom_resolution").getAsBoolean()));
            }
            if (features.has("is_demo_user")) {
                conditions.add(new DemoCondition(features.get("is_demo_user").getAsBoolean()));
            }
        }
    }

    public boolean isApplicable(ILaunchOptions opts) {
        if (conditions.isEmpty()) return true;
        for (RuleCondition condition : conditions) {
            if (!condition.test(opts)) return false;
        }
        return true;
    }

    public boolean isNegated() {
        return negated;
    }

    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        for (RuleCondition condition : conditions) {
            condition.serialize(json);
        }
        json.addProperty("action", negated ? "disallow" : "allow");
        return json;
    }

    private static JsonObject getFeatures(JsonObject root) {
        if (root.has("features")) return root.get("features").getAsJsonObject();
        JsonObject features = new JsonObject();
        root.add("features", features);
        return features;
    }

    private interface RuleCondition {

        boolean test(ILaunchOptions opts);

        void serialize(JsonObject root);

    }

    private static class OsCondition implements RuleCondition {

        private final String name;
        private final Pattern version;
        private final String arch;

        OsCondition(String name, String version, String arch) {
            this.name = name;
            this.version = version == null ? null : Pattern.compile(version, Pattern.CASE_INSENSITIVE);
            this.arch = arch;
        }

        @Override
        public boolean test(ILaunchOptions opts) {
            String os = OperatingSystem.getOperatingSystem().getName();
            String osVersion = System.getProperty("os.version");
            String archProp = System.getProperty("os.arch").toLowerCase();
            return (name == null || name.equalsIgnoreCase(os))
                    && (version == null || version.matcher(osVersion).matches())
                    && (arch == null || archProp.contains(arch.toLowerCase()));
        }

        @Override
        public void serialize(JsonObject root) {
            JsonObject os = new JsonObject();
            if (name != null) os.addProperty("name", name);
            if (version != null) os.addProperty("version", version.pattern());
            if (arch != null) os.addProperty("arch", arch);
            root.add("os", os);
        }

    }

    private static class CustomResolutionCondition implements RuleCondition {

        private final boolean shouldHaveCustomResolution;

        CustomResolutionCondition(boolean shouldHaveCustomResolution) {
            this.shouldHaveCustomResolution = shouldHaveCustomResolution;
        }

        @Override
        public boolean test(ILaunchOptions opts) {
            return opts != null
                    && (opts.getLaunchWindowType() == WindowType.CUSTOM) == shouldHaveCustomResolution;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty("has_custom_resolution", shouldHaveCustomResolution);
        }

    }

    private static class DemoCondition implements RuleCondition {

        private final boolean shouldBeDemoUser;

        DemoCondition(boolean shouldBeDemoUser) {
            this.shouldBeDemoUser = shouldBeDemoUser;
        }

        @Override
        public boolean test(ILaunchOptions opts) {
            return !shouldBeDemoUser;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty("is_demo_user", shouldBeDemoUser);
        }

    }

}
