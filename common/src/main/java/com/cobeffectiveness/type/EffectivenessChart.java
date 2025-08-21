package com.cobeffectiveness.type;

import java.util.*;

public final class EffectivenessChart {
    private static final Map<Type, Map<Type, Double>> MAP = new EnumMap<>(Type.class);

    private EffectivenessChart() {
    }

    static {
        for (Type atk : Type.values()) {
            MAP.put(atk, new EnumMap<>(Type.class));
            for (Type def : Type.values()) {
                set(atk, def, 1.0);
            }
        }
        // --- EFFECTIVENESS Chart
        // Fire
        set(Type.FIRE, Type.GRASS, 2);
        set(Type.FIRE, Type.ICE, 2);
        set(Type.FIRE, Type.BUG, 2);
        set(Type.FIRE, Type.STEEL, 2);
        set(Type.FIRE, Type.FIRE, 0.5);
        set(Type.FIRE, Type.WATER, 0.5);
        set(Type.FIRE, Type.ROCK, 0.5);
        set(Type.FIRE, Type.DRAGON, 0.5);

        // Water
        set(Type.WATER, Type.FIRE, 2);
        set(Type.WATER, Type.GROUND, 2);
        set(Type.WATER, Type.ROCK, 2);
        set(Type.WATER, Type.WATER, 0.5);
        set(Type.WATER, Type.GRASS, 0.5);
        set(Type.WATER, Type.DRAGON, 0.5);

        // Electric
        set(Type.ELECTRIC, Type.WATER, 2);
        set(Type.ELECTRIC, Type.FLYING, 2);
        set(Type.ELECTRIC, Type.ELECTRIC, 0.5);
        set(Type.ELECTRIC, Type.GRASS, 0.5);
        set(Type.ELECTRIC, Type.DRAGON, 0.5);
        set(Type.ELECTRIC, Type.GROUND, 0);

        // Grass
        set(Type.GRASS, Type.WATER, 2);
        set(Type.GRASS, Type.GROUND, 2);
        set(Type.GRASS, Type.ROCK, 2);
        set(Type.GRASS, Type.FIRE, 0.5);
        set(Type.GRASS, Type.GRASS, 0.5);
        set(Type.GRASS, Type.POISON, 0.5);
        set(Type.GRASS, Type.FLYING, 0.5);
        set(Type.GRASS, Type.BUG, 0.5);
        set(Type.GRASS, Type.DRAGON, 0.5);
        set(Type.GRASS, Type.STEEL, 0.5);

        // Ice
        set(Type.ICE, Type.GRASS, 2);
        set(Type.ICE, Type.GROUND, 2);
        set(Type.ICE, Type.FLYING, 2);
        set(Type.ICE, Type.DRAGON, 2);
        set(Type.ICE, Type.FIRE, 0.5);
        set(Type.ICE, Type.WATER, 0.5);
        set(Type.ICE, Type.ICE, 0.5);
        set(Type.ICE, Type.STEEL, 0.5);

        // Fighting
        set(Type.FIGHTING, Type.NORMAL, 2);
        set(Type.FIGHTING, Type.ICE, 2);
        set(Type.FIGHTING, Type.ROCK, 2);
        set(Type.FIGHTING, Type.DARK, 2);
        set(Type.FIGHTING, Type.STEEL, 2);
        set(Type.FIGHTING, Type.POISON, 0.5);
        set(Type.FIGHTING, Type.FLYING, 0.5);
        set(Type.FIGHTING, Type.PSYCHIC, 0.5);
        set(Type.FIGHTING, Type.BUG, 0.5);
        set(Type.FIGHTING, Type.FAIRY, 0.5);
        set(Type.FIGHTING, Type.GHOST, 0);

        // Poison
        set(Type.POISON, Type.GRASS, 2);
        set(Type.POISON, Type.FAIRY, 2);
        set(Type.POISON, Type.POISON, 0.5);
        set(Type.POISON, Type.GROUND, 0.5);
        set(Type.POISON, Type.ROCK, 0.5);
        set(Type.POISON, Type.GHOST, 0.5);
        set(Type.POISON, Type.STEEL, 0);

        // Ground
        set(Type.GROUND, Type.FIRE, 2);
        set(Type.GROUND, Type.ELECTRIC, 2);
        set(Type.GROUND, Type.POISON, 2);
        set(Type.GROUND, Type.ROCK, 2);
        set(Type.GROUND, Type.STEEL, 2);
        set(Type.GROUND, Type.GRASS, 0.5);
        set(Type.GROUND, Type.BUG, 0.5);
        set(Type.GROUND, Type.FLYING, 0);

        // Flying
        set(Type.FLYING, Type.GRASS, 2);
        set(Type.FLYING, Type.FIGHTING, 2);
        set(Type.FLYING, Type.BUG, 2);
        set(Type.FLYING, Type.ELECTRIC, 0.5);
        set(Type.FLYING, Type.ROCK, 0.5);
        set(Type.FLYING, Type.STEEL, 0.5);

        // Psychic
        set(Type.PSYCHIC, Type.FIGHTING, 2);
        set(Type.PSYCHIC, Type.POISON, 2);
        set(Type.PSYCHIC, Type.PSYCHIC, 0.5);
        set(Type.PSYCHIC, Type.STEEL, 0.5);
        set(Type.PSYCHIC, Type.DARK, 0);

        // Bug
        set(Type.BUG, Type.GRASS, 2);
        set(Type.BUG, Type.PSYCHIC, 2);
        set(Type.BUG, Type.DARK, 2);
        set(Type.BUG, Type.FIRE, 0.5);
        set(Type.BUG, Type.FIGHTING, 0.5);
        set(Type.BUG, Type.POISON, 0.5);
        set(Type.BUG, Type.FLYING, 0.5);
        set(Type.BUG, Type.GHOST, 0.5);
        set(Type.BUG, Type.STEEL, 0.5);
        set(Type.BUG, Type.FAIRY, 0.5);

        // Rock
        set(Type.ROCK, Type.FIRE, 2);
        set(Type.ROCK, Type.ICE, 2);
        set(Type.ROCK, Type.FLYING, 2);
        set(Type.ROCK, Type.BUG, 2);
        set(Type.ROCK, Type.FIGHTING, 0.5);
        set(Type.ROCK, Type.GROUND, 0.5);
        set(Type.ROCK, Type.STEEL, 0.5);

        // Ghost
        set(Type.GHOST, Type.PSYCHIC, 2);
        set(Type.GHOST, Type.GHOST, 2);
        set(Type.GHOST, Type.DARK, 0.5);
        set(Type.GHOST, Type.NORMAL, 0);

        // Dragon
        set(Type.DRAGON, Type.DRAGON, 2);
        set(Type.DRAGON, Type.STEEL, 0.5);
        set(Type.DRAGON, Type.FAIRY, 0);

        // Dark
        set(Type.DARK, Type.PSYCHIC, 2);
        set(Type.DARK, Type.GHOST, 2);
        set(Type.DARK, Type.FIGHTING, 0.5);
        set(Type.DARK, Type.DARK, 0.5);
        set(Type.DARK, Type.FAIRY, 0.5);

        // Steel
        set(Type.STEEL, Type.ICE, 2);
        set(Type.STEEL, Type.ROCK, 2);
        set(Type.STEEL, Type.FAIRY, 2);
        set(Type.STEEL, Type.FIRE, 0.5);
        set(Type.STEEL, Type.WATER, 0.5);
        set(Type.STEEL, Type.ELECTRIC, 0.5);
        set(Type.STEEL, Type.STEEL, 0.5);

        // Fairy
        set(Type.FAIRY, Type.FIGHTING, 2);
        set(Type.FAIRY, Type.DRAGON, 2);
        set(Type.FAIRY, Type.DARK, 2);
        set(Type.FAIRY, Type.FIRE, 0.5);
        set(Type.FAIRY, Type.POISON, 0.5);
        set(Type.FAIRY, Type.STEEL, 0.5);

    }

    private static void set(Type atk, Type def, double m) {
        MAP.get(atk).put(def, m);
    }

    public static double effectiveness(Type attack, Set<Type> defender) {
        double m = 1.0;
        for (Type d : defender) m *= MAP.get(attack).getOrDefault(d, 1.0);
        return m;
    }

    public static Map<Double, Set<Type>> attackersByMultipliers(Set<Type> defender, double... targets) {
        Map<Double, Set<Type>> out = new LinkedHashMap<>();
        for (double t : targets) out.put(t, EnumSet.noneOf(Type.class));

        for (Type atk : Type.values()) {
            double m = effectiveness(atk, defender);
            for (double t : targets) {
                if (Math.abs(m - t) < 1e-6) {
                    out.get(t).add(atk);
                }
            }
        }
        return out;
    }
}
