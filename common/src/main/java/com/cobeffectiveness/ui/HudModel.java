package com.cobeffectiveness.ui;

import com.cobeffectiveness.battle.BattleView;
import com.cobeffectiveness.battle.MoveInfo;
import com.cobeffectiveness.config.Multiplier;
import com.cobeffectiveness.type.EffectivenessChart;
import com.cobeffectiveness.type.IconSheet;
import com.cobeffectiveness.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the HUD draws, already resolved down to labels, colours and icon lists. Holds no
 * Minecraft types, so the effectiveness grouping that feeds the HUD can be unit tested directly.
 */
public record HudModel(List<Panel> panels) {
    public static final HudModel EMPTY = new HudModel(List.of());

    private static final String NO_VALUE = "—";
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int DIM_COLOR = 0xFF808080;
    private static final int NEUTRAL_VALUE_COLOR = 0xFFB8BCC6;

    /**
     * Which panel this model is for. Each is built and placed independently.
     *
     * <p>The opponent's name lives on {@link #NAME} alone. The other two only name a block when
     * there is more than one opponent to tell apart, so in singles the name is printed once.
     */
    public enum Content { NAME, TYPES, MOVES }

    /** Presentation choices, so callers do not grow another positional boolean each release. */
    public record Options(List<Multiplier> shown, Content content) {}

    /** @param label side heading, or null when the viewer is a battler */
    public record Panel(String label, List<Section> sections) {}

    /**
     * @param label the Pokemon this block is about
     * @param types that Pokemon's own types, empty when the header is switched off
     * @param rows  effectiveness by type; empty when move rows are shown instead
     * @param moves the player's moves scored against this Pokemon; empty in the type view
     */
    public record Section(String label, List<Type> types, List<Row> rows, List<MoveRow> moves) {
        /** A type-view section, which is the common case. */
        public Section(String label, List<Type> types, List<Row> rows) {
            this(label, types, rows, List.of());
        }
    }

    public record Row(double multiplier, String label, int color, List<Type> types) {}

    /** @param type icon to draw between name and value, or null when the sheet has none */
    public record MoveRow(String name, int nameColor, Type type, String value, int valueColor) {}

    public boolean isEmpty() {
        return panels.isEmpty();
    }

    public static HudModel from(BattleView view, List<MoveInfo> moves, EffectivenessChart chart,
                               IconSheet sheet, Options options) {
        Content content = options.content();

        // The moves panel simply does not draw when the moveset is unknown - spectating, or before
        // your first turn. Each panel is separate now, so there is nothing to fall back to.
        if (content == Content.MOVES && moves.isEmpty()) return EMPTY;
        if (content == Content.TYPES && options.shown().isEmpty()) return EMPTY;

        boolean showMoves = content == Content.MOVES;
        boolean showTypes = content == Content.TYPES;
        boolean showNames = content == Content.NAME;

        List<Panel> panels = new ArrayList<>();
        for (BattleView.Side side : view.sides()) {
            List<Section> sections = new ArrayList<>();

            for (BattleView.Target target : side.targets()) {
                List<Row> rows = showTypes ? rowsFor(target.types(), chart, sheet, options.shown()) : List.of();
                List<MoveRow> moveRows = showMoves ? movesFor(target.types(), chart, sheet, moves) : List.of();
                List<Type> named = showNames ? headerTypes(target, sheet) : List.of();

                if (!named.isEmpty() || !rows.isEmpty() || !moveRows.isEmpty()) {
                    sections.add(new Section(target.label(), named, rows, moveRows));
                }
            }
            if (!sections.isEmpty()) {
                panels.add(new Panel(side.label(), List.copyOf(sections)));
            }
        }

        return panels.isEmpty() ? EMPTY : new HudModel(List.copyOf(panels));
    }

    /** The opponent's own types, filtered to what the sheet can draw, like every other icon list. */
    private static List<Type> headerTypes(BattleView.Target target, IconSheet sheet) {
        return target.types().stream().filter(sheet::has).toList();
    }

    private static List<MoveRow> movesFor(Set<Type> defenders, EffectivenessChart chart,
                                          IconSheet sheet, List<MoveInfo> moves) {
        List<MoveRow> rows = new ArrayList<>(moves.size());

        for (MoveInfo move : moves) {
            Type icon = move.type() != null && sheet.has(move.type()) ? move.type() : null;

            // A status move has no effectiveness, and one that cannot be pressed is not a choice.
            if (!move.scoreable() || !move.usable()) {
                rows.add(new MoveRow(move.name(), DIM_COLOR, icon, NO_VALUE, DIM_COLOR));
                continue;
            }

            double multiplier = chart.multiplier(move.type(), defenders);
            rows.add(new MoveRow(move.name(), NAME_COLOR, icon,
                    multiplierLabel(multiplier), multiplierColor(multiplier)));
        }
        return List.copyOf(rows);
    }

    private static String multiplierLabel(double multiplier) {
        for (Multiplier known : Multiplier.values()) {
            if (matches(multiplier, known.value())) return known.label();
        }
        if (matches(multiplier, 1.0)) return "×1";

        String plain = String.valueOf(multiplier);
        return "×" + (plain.endsWith(".0") ? plain.substring(0, plain.length() - 2) : plain);
    }

    private static int multiplierColor(double multiplier) {
        for (Multiplier known : Multiplier.values()) {
            if (matches(multiplier, known.value())) return known.color();
        }
        return NEUTRAL_VALUE_COLOR;
    }

    private static boolean matches(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static List<Row> rowsFor(Set<Type> defenders, EffectivenessChart chart, IconSheet sheet,
                                     List<Multiplier> shown) {
        double[] wanted = new double[shown.size()];
        for (int i = 0; i < shown.size(); i++) {
            wanted[i] = shown.get(i).value();
        }
        Map<Double, Set<Type>> buckets = chart.attackersByMultiplier(defenders, wanted);

        List<Row> rows = new ArrayList<>(shown.size());
        for (Multiplier multiplier : shown) {
            // Keep only what the sheet can actually draw, so the width we measure is the width we
            // paint. Measuring the unfiltered set used to leave a gap for icons that never appeared.
            List<Type> types = buckets.get(multiplier.value()).stream().filter(sheet::has).toList();
            if (!types.isEmpty()) {
                rows.add(new Row(multiplier.value(), multiplier.label(), multiplier.color(), types));
            }
        }
        return List.copyOf(rows);
    }
}
