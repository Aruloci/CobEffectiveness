package com.cobeffectiveness.compat;

import com.cobeffectiveness.CobEffectivenessMod;

public final class Compat {
    public static final String MODID_GEB = "gravels_extended_battles";
    public static final String TEXTURE_PATH = "textures/gui/types.png";
    public static final String GEB_NAMESPACE = "gravelmon";
    public static final String COBBLEMON_NAMESPACE = "cobblemon";

    public enum Flavor { VANILLA, GEB }

    private static volatile Flavor flavor = Flavor.VANILLA;

    private Compat() {}

    public static void init(boolean gebLoaded) {
        flavor = gebLoaded ? Flavor.GEB : Flavor.VANILLA;
        CobEffectivenessMod.logInfo("Effectiveness flavor: {}", flavor);
    }

    public static Flavor flavor() {
        return flavor;
    }

    /** Namespace of the mod that owns the type icon sheet. */
    public static String namespace() {
        return flavor() == Flavor.GEB ? GEB_NAMESPACE : COBBLEMON_NAMESPACE;
    }

    public static String chartPath() {
        return "charts/" + (flavor() == Flavor.GEB ? "geb_default" : "vanilla") + ".json";
    }
}
