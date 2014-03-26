package net.technicpack.minecraftcore;

public class TechnicConstants {
    public static final String technicURL = "http://mirror.technicpack.net/Technic/";
    public static final String technicVersions = technicURL + "version/";

    public static String getTechnicVersionJson(String version) {
        return technicVersions + version + "/" + version + ".json";
    }
}
