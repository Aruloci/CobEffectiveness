package com.cobeffectiveness.type;


import com.cobeffectiveness.CobEffectivenessMod;

import java.util.*;
import java.util.function.Function;

public final class EffectivenessChart {
    private static final Map<Type, Map<Type, Double>> MAP = new EnumMap<>(Type.class);

    private EffectivenessChart() {}

    public static void configure(ChartJson json, Function<String, Type> nameMapper) {
        MAP.clear();

        for (Type atk : Type.values()) {
            Map<Type, Double> inner = new EnumMap<>(Type.class);
            for (Type def : Type.values()) {
                inner.put(def, json.default_);
            }
            MAP.put(atk, inner);
        }

        if (json.rules != null) {
            for (ChartJson.Rule r : json.rules) {
                Type atk = nameMapper.apply(r.attack);
                Type def = nameMapper.apply(r.defend);
                if (atk == null || def == null) {
                    CobEffectivenessMod.logWarn("Unknown type in rule: {} -> {}", String.valueOf(r.attack), String.valueOf(r.defend));
                    continue;
                }
                MAP.get(atk).put(def, r.mult);
            }
        }
    }

    public static double effectiveness(Type attack, Set<Type> defender) {
        double m = 1.0;
        Map<Type, Double> row = MAP.get(attack);
        if (row == null) return 1.0;
        for (Type d : defender) {
            m *= row.getOrDefault(d, 1.0);
        }
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
