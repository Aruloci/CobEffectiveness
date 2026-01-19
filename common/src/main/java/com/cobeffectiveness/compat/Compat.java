package com.cobeffectiveness.compat;

import com.cobeffectiveness.CobEffectivenessMod;

public final class Compat {
    public static final String MODID_GEB = "gravels_extended_battles";
    public static final String TEXTURE_PATH = "textures/gui/types.png";
    public static final String GEB_NAMESPACE = "gravelmon";
    public static final String COBBLEMON_NAMESPACE = "cobblemon";

    public enum Flavor { VANILLA, GEB }

    private static Flavor flavor = Flavor.VANILLA;

    private Compat() {}

    public static void init(boolean gebLoaded) {
        flavor = gebLoaded ? Flavor.GEB : Flavor.VANILLA;
        CobEffectivenessMod.logInfo("Effectiveness flavor: {}", flavor);
    }

    public static String getNameSpace() {
        return flavor() == Flavor.GEB ? Compat.GEB_NAMESPACE : COBBLEMON_NAMESPACE;
    }

    public static String getJsonPath() {
        String name = (Compat.flavor() == Compat.Flavor.GEB) ? "geb_default" : "vanilla";
        return "charts/" + name + ".json";
    }

    public static Flavor flavor() { return flavor; }
}
