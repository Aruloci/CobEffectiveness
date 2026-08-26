package com.cobeffectiveness.type;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable description of the type-icon sheet: tile geometry plus where each {@link Type} sits on
 * it. The sheet itself belongs to Cobblemon or Gravelmon, so its layout has to be described by the
 * chart JSON rather than assumed.
 *
 * <p>Published the same way as {@link EffectivenessChart}: one volatile write, no mutable statics.
 */
public final class IconSheet {
    public static final IconSheet DEFAULT = new IconSheet(18, 1, 12, 12, new EnumMap<>(Type.class));

    private static volatile IconSheet active = DEFAULT;

    private final int cols;
    private final int rows;
    private final int tileW;
    private final int tileH;
    private final Map<Type, Pos> positions;

    private IconSheet(int cols, int rows, int tileW, int tileH, Map<Type, Pos> positions) {
        this.cols = cols;
        this.rows = rows;
        this.tileW = tileW;
        this.tileH = tileH;
        this.positions = positions;
    }

    public static IconSheet current() {
        return active;
    }

    public static void publish(IconSheet sheet) {
        active = sheet;
    }

    public record Pos(int u, int v) {}

    public record Parsed(IconSheet sheet, List<String> warnings) {}

    public static Parsed from(ChartJson.Tiles tiles, List<String> order) {
        List<String> warnings = new ArrayList<>();

        int cols = tiles != null && tiles.cols > 0 ? tiles.cols : DEFAULT.cols;
        int rows = tiles != null && tiles.rows > 0 ? tiles.rows : DEFAULT.rows;
        int tileW = tiles != null && tiles.tile_w > 0 ? tiles.tile_w : DEFAULT.tileW;
        int tileH = tiles != null && tiles.tile_h > 0 ? tiles.tile_h : DEFAULT.tileH;

        Map<Type, Pos> positions = new EnumMap<>(Type.class);
        if (order != null) {
            for (int i = 0; i < order.size(); i++) {
                String name = order.get(i);
                // A placeholder still consumes its slot so later icons keep their offsets.
                if (name == null || ChartJson.PLACEHOLDER.equals(name)) continue;

                Type type = Type.fromName(name);
                if (type == null) {
                    warnings.add("Unknown type in sheet order: " + name);
                    continue;
                }
                positions.put(type, new Pos((i % cols) * tileW, (i / cols) * tileH));
            }
        }

        return new Parsed(new IconSheet(cols, rows, tileW, tileH, positions), warnings);
    }

    /** Position of this type on the sheet, or null when the sheet has no icon for it. */
    public Pos pos(Type type) {
        return positions.get(type);
    }

    public boolean has(Type type) {
        return positions.containsKey(type);
    }

    public int tileW() { return tileW; }

    public int tileH() { return tileH; }

    public int sheetW() { return cols * tileW; }

    public int sheetH() { return rows * tileH; }
}
