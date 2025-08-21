package com.cobeffectiveness.type;

import java.util.EnumMap;
import java.util.Map;

public final class TypeIcon {
    public static final int TILE_W = 12;
    public static final int TILE_H = 12;

    public static final int COLS = 18;
    public static final int ROWS = 1;
    public static final int ATLAS_W = COLS * TILE_W;
    public static final int ATLAS_H = ROWS * TILE_H;

    private static final Map<Type, Pos> POS = new EnumMap<>(Type.class);

    static {
        Type[] order = {
                Type.NORMAL, Type.FIRE, Type.WATER, Type.GRASS, Type.ELECTRIC, Type.ICE,
                Type.FIGHTING, Type.POISON, Type.GROUND, Type.FLYING, Type.PSYCHIC, Type.BUG,
                Type.ROCK, Type.GHOST, Type.DRAGON, Type.DARK, Type.STEEL, Type.FAIRY
        };
        for (int i = 0; i < order.length; i++) {
            int col = i % COLS;
            POS.put(order[i], new Pos(col * TILE_W, 0));
        }
    }

    public static Pos pos(Type t) {
        return POS.get(t);
    }

    public record Pos(int u, int v) {
    }
}
