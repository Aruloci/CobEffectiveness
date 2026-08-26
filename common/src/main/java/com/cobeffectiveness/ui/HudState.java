package com.cobeffectiveness.ui;

import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.battle.BattleReader;
import com.cobeffectiveness.battle.MoveInfo;
import com.cobeffectiveness.battle.MoveReader;
import com.cobeffectiveness.battle.BattleView;
import com.cobeffectiveness.config.CobConfig;
import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.type.EffectivenessChart;
import com.cobeffectiveness.type.IconSheet;

import java.util.List;
import java.util.Objects;

/**
 * Keeps the drawn model between frames.
 *
 * <p>The battle screen posts its render event every frame, but the answer only changes on a switch,
 * a form change or a resource reload. Re-reading the battle and re-bucketing 30 attacking types at
 * the frame rate was pure garbage, so the battle is sampled on an interval and the model rebuilt
 * only when the sample or the published chart actually differs.
 *
 * <p>Render thread only; no synchronisation needed.
 */
public final class HudState {
    private static final long SAMPLE_INTERVAL_MS = 200;

    private static long lastSampleAt;
    private static BattleView lastView;
    private static EffectivenessChart lastChart;
    private static IconSheet lastSheet;
    private static CobConfig lastConfig;
    private static List<MoveInfo> lastMoves = List.of();
    private static HudModel nameModel = HudModel.EMPTY;
    private static HudModel typesModel = HudModel.EMPTY;
    private static HudModel movesModel = HudModel.EMPTY;

    private HudState() {}

    public static HudModel nameModel() {
        return nameModel;
    }

    public static HudModel typesModel() {
        return typesModel;
    }

    public static HudModel movesModel() {
        return movesModel;
    }

    public static void refresh() {
        EffectivenessChart chart = EffectivenessChart.current();
        IconSheet sheet = IconSheet.current();
        CobConfig config = ConfigStore.get();
        // Saving the config publishes a new instance, so an identity check catches every edit.
        boolean chartChanged = chart != lastChart || sheet != lastSheet || config != lastConfig;

        long now = System.currentTimeMillis();
        if (!chartChanged && now - lastSampleAt < SAMPLE_INTERVAL_MS) return;
        lastSampleAt = now;

        BattleView view = BattleReader.read();
        // Moves change without the battle view changing: PP drops, a move gets disabled.
        List<MoveInfo> moves = MoveReader.currentMoves();
        if (!chartChanged && Objects.equals(view, lastView) && Objects.equals(moves, lastMoves)) return;

        lastView = view;
        lastChart = chart;
        lastSheet = sheet;
        lastConfig = config;
        lastMoves = moves;
        if (view == null) {
            nameModel = HudModel.EMPTY;
            typesModel = HudModel.EMPTY;
            movesModel = HudModel.EMPTY;
        } else {
            nameModel = HudModel.from(view, moves, chart, sheet,
                    new HudModel.Options(config.shownInOrder(), HudModel.Content.NAME));
            typesModel = HudModel.from(view, moves, chart, sheet,
                    new HudModel.Options(config.shownInOrder(), HudModel.Content.TYPES));
            movesModel = HudModel.from(view, moves, chart, sheet,
                    new HudModel.Options(config.shownInOrder(), HudModel.Content.MOVES));
        }

        // Only runs when the model actually changed, so it is cheap. Doubles and spectating are
        // awkward to reproduce on demand, so a debug log is what makes a user report actionable.
        CobEffectivenessMod.logDebug("HUD rebuilt: name [{}] types [{}] moves [{}]",
                describe(nameModel), describe(typesModel), describe(movesModel));
    }

    private static String describe(HudModel model) {
        if (model.isEmpty()) return "nothing to draw";

        StringBuilder out = new StringBuilder();
        for (HudModel.Panel panel : model.panels()) {
            if (out.length() > 0) out.append(" | ");
            out.append(panel.label() == null ? "opponent" : panel.label()).append(": ");

            for (int i = 0; i < panel.sections().size(); i++) {
                HudModel.Section section = panel.sections().get(i);
                if (i > 0) out.append(", ");
                out.append(section.label()).append(' ');

                for (HudModel.Row row : section.rows()) {
                    out.append(row.label()).append('=').append(row.types().size()).append(' ');
                }
            }
        }
        return out.toString().trim();
    }

    public static void clear() {
        lastSampleAt = 0L;
        lastView = null;
        lastMoves = List.of();
        MoveReader.clear();
        nameModel = HudModel.EMPTY;
        typesModel = HudModel.EMPTY;
        movesModel = HudModel.EMPTY;
    }
}
