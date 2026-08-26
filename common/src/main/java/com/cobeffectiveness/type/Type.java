package com.cobeffectiveness.type;

import java.util.Locale;

public enum Type {
    NORMAL, FIRE, WATER, ELECTRIC, GRASS, ICE,
    FIGHTING, POISON, GROUND, FLYING, PSYCHIC, BUG,
    ROCK, GHOST, DRAGON, DARK, STEEL, FAIRY,
    SHADOW, COSMIC, CRYSTAL, DIGITAL, LIGHT, NUCLEAR, // Gravels Extended Battles Types
    PLASTIC, SLIME, SOUND, WIND, ELDRITCH, BLOOD; // Gravels Extended Battles Types

    /**
     * Resolves a type name as written by Cobblemon or by a chart JSON.
     *
     * <p>Returns null for an unknown or absent name rather than throwing, so callers can report a
     * bad entry and carry on instead of aborting a whole chart. {@link Locale#ROOT} matters here:
     * the default locale would turn "Fire" into "FİRE" on a Turkish JVM and lose every type whose
     * name contains an i.
     */
    public static Type fromName(String name) {
        if (name == null) return null;
        try {
            return Type.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
