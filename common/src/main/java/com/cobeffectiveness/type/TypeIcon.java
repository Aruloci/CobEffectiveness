package com.cobeffectiveness.type;


import com.cobeffectiveness.CobEffectivenessMod;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class TypeIcon {
    public static int TILE_W = 12;
    public static int TILE_H = 12;
    public static int COLS = 18;
    public static int ROWS = 1;
    public static int SHEET_W = COLS * TILE_W;
    public static int SHEET_H = ROWS * TILE_H;

    private static final Map<Type, Pos> POS = new EnumMap<>(Type.class);

    private TypeIcon() {}

    public static synchronized void configure(int cols, int rows, int tileW, int tileH, List<String> order) {
        if (cols > 0) COLS = cols;
        if (rows > 0) ROWS = rows;
        if (tileW > 0) TILE_W = tileW;
        if (tileH > 0) TILE_H = tileH;

        SHEET_W = COLS * TILE_W;
        SHEET_H = ROWS * TILE_H;

        POS.clear();
        if (order == null) return;

        for (int i = 0; i < order.size(); i++) {
            String cobName = order.get(i);
            try {
                Type type = Type.fromCobblemon(cobName);
                int col = i % COLS;
                int row = i / COLS;
                POS.put(type, new Pos(col * TILE_W, row * TILE_H));
            } catch (IllegalArgumentException ignored) {
                CobEffectivenessMod.logWarn("Unknown type in JSON: {}", cobName);
            }
        }
    }

    public static Pos pos(Type t) {
        return POS.get(t);
    }

    public record Pos(int u, int v) {
    }
}
