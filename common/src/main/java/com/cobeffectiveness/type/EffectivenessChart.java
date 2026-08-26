package com.cobeffectiveness.type;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable attack-to-defend multiplier table, indexed by {@link Type#ordinal()}.
 *
 * <p>A chart is a value: {@link #from} builds one and {@link #publish} swaps the active one in with
 * a single volatile write. The render thread therefore never sees a half-built chart, which the
 * previous rebuild-in-place map could expose during a resource reload.
 *
 * <p>Parsing deliberately reports problems through {@link Parsed#warnings()} instead of logging, so
 * this class stays free of mod and Minecraft types and can be unit tested on its own.
 */
public final class EffectivenessChart {
    private static final int TYPES = Type.values().length;

    public static final EffectivenessChart NEUTRAL = new EffectivenessChart(filled(1.0));

    private static volatile EffectivenessChart active = NEUTRAL;

    private final double[][] table;

    private EffectivenessChart(double[][] table) {
        this.table = table;
    }

    public static EffectivenessChart current() {
        return active;
    }

    public static void publish(EffectivenessChart chart) {
        active = chart;
    }

    public record Parsed(EffectivenessChart chart, List<String> warnings) {}

    public static Parsed from(ChartJson json) {
        List<String> warnings = new ArrayList<>();
        double[][] table = filled(json.defaultMultiplier);

        if (json.rules != null) {
            for (ChartJson.Rule rule : json.rules) {
                Type attack = Type.fromName(rule.attack);
                Type defend = Type.fromName(rule.defend);
                if (attack == null || defend == null) {
                    warnings.add("Unknown type in rule: " + rule.attack + " -> " + rule.defend);
                    continue;
                }
                table[attack.ordinal()][defend.ordinal()] = rule.mult;
            }
        }

        return new Parsed(new EffectivenessChart(table), warnings);
    }

    /** Combined multiplier of one attacking type against a single Pokemon's type set. */
    public double multiplier(Type attack, Set<Type> defenders) {
        double result = 1.0;
        double[] row = table[attack.ordinal()];
        for (Type defender : defenders) {
            result *= row[defender.ordinal()];
        }
        return result;
    }

    /**
     * Buckets every attacking type by its multiplier against one defender, keeping only the
     * requested multipliers. Keys are returned in the order given.
     */
    public Map<Double, Set<Type>> attackersByMultiplier(Set<Type> defenders, double... targets) {
        Map<Double, Set<Type>> out = new LinkedHashMap<>();
        for (double target : targets) out.put(target, EnumSet.noneOf(Type.class));

        for (Type attack : Type.values()) {
            double multiplier = multiplier(attack, defenders);
            for (double target : targets) {
                if (Math.abs(multiplier - target) < 1e-6) {
                    out.get(target).add(attack);
                }
            }
        }
        return out;
    }

    private static double[][] filled(double value) {
        double[][] table = new double[TYPES][TYPES];
        for (double[] row : table) {
            java.util.Arrays.fill(row, value);
        }
        return table;
    }
}
