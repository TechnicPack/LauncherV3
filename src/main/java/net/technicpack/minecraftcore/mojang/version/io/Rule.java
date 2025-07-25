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
import com.google.gson.JsonSyntaxException;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.launch.WindowType;
import net.technicpack.utilslib.OperatingSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class Rule {
    // Feature keys
    private static final String HAS_CUSTOM_RESOLUTION = "has_custom_resolution";
    private static final String IS_DEMO_USER = "is_demo_user";
    private static final String HAS_QUICK_PLAYS_SUPPORT = "has_quick_plays_support";
    private static final String IS_QUICK_PLAY_SINGLEPLAYER = "is_quick_play_singleplayer";
    private static final String IS_QUICK_PLAY_MULTIPLAYER = "is_quick_play_multiplayer";
    private static final String IS_QUICK_PLAY_REALMS = "is_quick_play_realms";

    // Field names
    private static final String FEATURES_FIELD = "features";
    private static final String ACTION_FIELD = "action";
    private static final String OS_FIELD = "os";
    private static final String OS_NAME_FIELD = "name";
    private static final String OS_VERSION_FIELD = "version";
    private static final String OS_ARCH_FIELD = "arch";

    // Action values
    private static final String ACTION_ALLOW_VALUE = "allow";
    private static final String ACTION_DISALLOW_VALUE = "disallow";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return negated == rule.negated && Objects.equals(conditions, rule.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(negated, conditions);
    }

    public static boolean isAllowable(List<Rule> rules, ILaunchOptions options, IJavaRuntime runtime) {
        for (int i = rules.size() - 1; i >= 0; i--) {
            Rule rule = rules.get(i);
            if (rule.isApplicable(options, runtime)) {
                return !rule.isNegated();
            }
        }
        return false;
    }

    private final boolean negated;
    private final List<RuleCondition> conditions = new ArrayList<>();

    public Rule(JsonObject rule) {
        String action = rule.get(ACTION_FIELD).getAsString();
        switch (action) {
            case ACTION_ALLOW_VALUE:
                negated = false;
                break;
            case ACTION_DISALLOW_VALUE:
                negated = true;
                break;
            default:
                throw new JsonSyntaxException("Expected rule action, got: " + action);
        }

        if (rule.has(OS_FIELD)) {
            JsonObject os = rule.get(OS_FIELD).getAsJsonObject();
            conditions.add(new OsCondition(
                    os.has(OS_NAME_FIELD) ? os.get(OS_NAME_FIELD).getAsString() : null,
                    os.has(OS_VERSION_FIELD) ? os.get(OS_VERSION_FIELD).getAsString() : null,
                    os.has(OS_ARCH_FIELD) ? os.get(OS_ARCH_FIELD).getAsString() : null
            ));
        }
        if (rule.has(FEATURES_FIELD)) {
            JsonObject features = rule.get(FEATURES_FIELD).getAsJsonObject();
            for (String feature : features.keySet()) {
                switch (feature) {
                    case HAS_CUSTOM_RESOLUTION:
                        conditions.add(new CustomResolutionCondition(features.get(HAS_CUSTOM_RESOLUTION).getAsBoolean()));
                        break;
                    case IS_DEMO_USER:
                        conditions.add(new DemoUserCondition(features.get(IS_DEMO_USER).getAsBoolean()));
                        break;
                    case HAS_QUICK_PLAYS_SUPPORT:
                        conditions.add(new QuickPlaysSupportCondition(features.get(HAS_QUICK_PLAYS_SUPPORT).getAsBoolean()));
                        break;
                    case IS_QUICK_PLAY_SINGLEPLAYER:
                        conditions.add(new QuickPlaySingleplayerCondition(features.get(IS_QUICK_PLAY_SINGLEPLAYER).getAsBoolean()));
                        break;
                    case IS_QUICK_PLAY_MULTIPLAYER:
                        conditions.add(new QuickPlayMultiplayerCondition(features.get(IS_QUICK_PLAY_MULTIPLAYER).getAsBoolean()));
                        break;
                    case IS_QUICK_PLAY_REALMS:
                        conditions.add(new QuickPlayRealmsCondition(features.get(IS_QUICK_PLAY_REALMS).getAsBoolean()));
                        break;
                    default:
                        throw new JsonSyntaxException("Unknown feature: " + feature);
                }
            }
        }
    }

    public boolean isApplicable(ILaunchOptions options, IJavaRuntime runtime) {
        if (conditions.isEmpty()) return true;
        for (RuleCondition condition : conditions) {
            if (!condition.test(options, runtime)) return false;
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
        json.addProperty(ACTION_FIELD, negated ? ACTION_DISALLOW_VALUE : ACTION_ALLOW_VALUE);
        return json;
    }

    private static JsonObject getFeatures(JsonObject root) {
        if (root.has(FEATURES_FIELD)) return root.get(FEATURES_FIELD).getAsJsonObject();
        JsonObject features = new JsonObject();
        root.add(FEATURES_FIELD, features);
        return features;
    }

    private interface RuleCondition {

        boolean test(ILaunchOptions opts, IJavaRuntime runtime);

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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OsCondition that = (OsCondition) o;
            return Objects.equals(name, that.name) && Objects.equals(version, that.version) && Objects.equals(arch, that.arch);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version, arch);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            String os = OperatingSystem.getOperatingSystem().getName();
            String osVersion = System.getProperty("os.version");
            String archProp = runtime.getOsArch().toLowerCase(Locale.ROOT);
            return (name == null || name.equalsIgnoreCase(os))
                    && (version == null || version.matcher(osVersion).matches())
                    && (arch == null || archProp.contains(arch.toLowerCase()));
        }

        @Override
        public void serialize(JsonObject root) {
            JsonObject os = new JsonObject();
            if (name != null) os.addProperty(OS_NAME_FIELD, name);
            if (version != null) os.addProperty(OS_VERSION_FIELD, version.pattern());
            if (arch != null) os.addProperty(OS_ARCH_FIELD, arch);
            root.add(OS_FIELD, os);
        }

    }

    private static class CustomResolutionCondition implements RuleCondition {

        private final boolean shouldHaveCustomResolution;

        CustomResolutionCondition(boolean shouldHaveCustomResolution) {
            this.shouldHaveCustomResolution = shouldHaveCustomResolution;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomResolutionCondition that = (CustomResolutionCondition) o;
            return shouldHaveCustomResolution == that.shouldHaveCustomResolution;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(shouldHaveCustomResolution);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            return opts != null
                    && (opts.getLaunchWindowType() == WindowType.CUSTOM) == shouldHaveCustomResolution;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty(HAS_CUSTOM_RESOLUTION, shouldHaveCustomResolution);
        }

    }

    private static class DemoUserCondition implements RuleCondition {

        private final boolean shouldBeDemoUser;

        DemoUserCondition(boolean shouldBeDemoUser) {
            this.shouldBeDemoUser = shouldBeDemoUser;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DemoUserCondition that = (DemoUserCondition) o;
            return shouldBeDemoUser == that.shouldBeDemoUser;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(shouldBeDemoUser);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            return !shouldBeDemoUser;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty(IS_DEMO_USER, shouldBeDemoUser);
        }

    }

    private static class QuickPlaysSupportCondition implements RuleCondition {

        private final boolean shouldHaveQuickPlaysSupport;

        QuickPlaysSupportCondition(boolean shouldHaveQuickPlaysSupport) {
            this.shouldHaveQuickPlaysSupport = shouldHaveQuickPlaysSupport;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuickPlaysSupportCondition that = (QuickPlaysSupportCondition) o;
            return shouldHaveQuickPlaysSupport == that.shouldHaveQuickPlaysSupport;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(shouldHaveQuickPlaysSupport);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            return !shouldHaveQuickPlaysSupport;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty(HAS_QUICK_PLAYS_SUPPORT, shouldHaveQuickPlaysSupport);
        }

    }

    private static class QuickPlaySingleplayerCondition implements RuleCondition {

        private final boolean shouldQuickPlayBeSingleplayer;

        QuickPlaySingleplayerCondition(boolean shouldQuickPlayBeSingleplayer) {
            this.shouldQuickPlayBeSingleplayer = shouldQuickPlayBeSingleplayer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuickPlaySingleplayerCondition that = (QuickPlaySingleplayerCondition) o;
            return shouldQuickPlayBeSingleplayer == that.shouldQuickPlayBeSingleplayer;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(shouldQuickPlayBeSingleplayer);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            return !shouldQuickPlayBeSingleplayer;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty(IS_QUICK_PLAY_SINGLEPLAYER, shouldQuickPlayBeSingleplayer);
        }

    }

    private static class QuickPlayMultiplayerCondition implements RuleCondition {

        private final boolean shouldQuickPlayBeMultiplayer;

        QuickPlayMultiplayerCondition(boolean shouldQuickPlayBeMultiplayer) {
            this.shouldQuickPlayBeMultiplayer = shouldQuickPlayBeMultiplayer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuickPlayMultiplayerCondition that = (QuickPlayMultiplayerCondition) o;
            return shouldQuickPlayBeMultiplayer == that.shouldQuickPlayBeMultiplayer;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(shouldQuickPlayBeMultiplayer);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            return !shouldQuickPlayBeMultiplayer;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty(IS_QUICK_PLAY_MULTIPLAYER, shouldQuickPlayBeMultiplayer);
        }

    }

    private static class QuickPlayRealmsCondition implements RuleCondition {

        private final boolean shouldQuickPlayBeRealms;

        QuickPlayRealmsCondition(boolean shouldQuickPlayBeRealms) {
            this.shouldQuickPlayBeRealms = shouldQuickPlayBeRealms;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QuickPlayRealmsCondition that = (QuickPlayRealmsCondition) o;
            return shouldQuickPlayBeRealms == that.shouldQuickPlayBeRealms;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(shouldQuickPlayBeRealms);
        }

        @Override
        public boolean test(ILaunchOptions opts, IJavaRuntime runtime) {
            return !shouldQuickPlayBeRealms;
        }

        @Override
        public void serialize(JsonObject root) {
            getFeatures(root).addProperty(IS_QUICK_PLAY_REALMS, shouldQuickPlayBeRealms);
        }

    }

}
