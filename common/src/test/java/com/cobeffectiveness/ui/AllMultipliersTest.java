package com.cobeffectiveness.ui;

import com.cobeffectiveness.battle.BattleView;
import com.cobeffectiveness.config.Multiplier;
import com.cobeffectiveness.type.ChartJson;
import com.cobeffectiveness.type.Charts;
import com.cobeffectiveness.type.EffectivenessChart;
import com.cobeffectiveness.type.IconSheet;
import com.cobeffectiveness.type.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With every row switched on, which rows actually appear depends on the opponent, not on the
 * config. x4 and x0.25 need two types stacking the same way, and x0 needs an immunity, so a
 * single-type opponent can only ever produce x2 and x0.5.
 */
class AllMultipliersTest {

    private static final List<Multiplier> ALL = List.of(Multiplier.values());

    private static EffectivenessChart chart;
    private static IconSheet sheet;

    @BeforeAll
    static void loadVanillaChart() throws Exception {
        ChartJson json = Charts.load("vanilla");
        chart = EffectivenessChart.from(json).chart();
        sheet = IconSheet.from(json.tiles, json.order).sheet();
    }

    private static List<HudModel.Row> rowsFor(Set<Type> types) {
        BattleView view = new BattleView(List.of(new BattleView.Side(null,
                List.of(new BattleView.Target("Opponent", types)))));
        return HudModel.from(view, List.of(), chart, sheet, new HudModel.Options(ALL, HudModel.Content.TYPES)).panels().get(0).sections().get(0).rows();
    }

    private static HudModel.Row row(List<HudModel.Row> rows, double multiplier) {
        return rows.stream().filter(r -> r.multiplier() == multiplier).findFirst().orElse(null);
    }

    @Test
    void allFiveRowsAppearWhenTheOpponentCanProduceThemAll() {
        // Ground/Rock is the case that exercises every row at once.
        List<HudModel.Row> rows = rowsFor(EnumSet.of(Type.GROUND, Type.ROCK));

        assertEquals(5, rows.size(), () -> "expected every row, got " + rows.size());

        assertTrue(row(rows, 4.0).types().containsAll(List.of(Type.WATER, Type.GRASS)));
        assertTrue(row(rows, 2.0).types().contains(Type.FIGHTING));
        assertTrue(row(rows, 0.5).types().contains(Type.NORMAL));
        assertTrue(row(rows, 0.25).types().contains(Type.POISON));
        assertTrue(row(rows, 0.0).types().contains(Type.ELECTRIC));
    }

    @Test
    void rowsAreOrderedStrongestFirst() {
        List<HudModel.Row> rows = rowsFor(EnumSet.of(Type.GROUND, Type.ROCK));

        assertEquals(List.of(4.0, 2.0, 0.5, 0.25, 0.0),
                rows.stream().map(HudModel.Row::multiplier).toList());
    }

    @Test
    void aSingleTypeOpponentCanOnlyEverProduceTwoRows() {
        // Nothing stacks, so x4 and x0.25 are arithmetically impossible, and pure Water has no
        // immunity. Seeing only x2 and x0.5 here is correct, not a missing row.
        List<HudModel.Row> rows = rowsFor(EnumSet.of(Type.WATER));

        assertEquals(List.of(2.0, 0.5), rows.stream().map(HudModel.Row::multiplier).toList());
    }

    @Test
    void aSingleTypeWithAnImmunityStillGetsItsZeroRow() {
        // Normal is immune to Ghost, so x0 appears even without a second type.
        List<HudModel.Row> rows = rowsFor(EnumSet.of(Type.NORMAL));

        HudModel.Row zero = row(rows, 0.0);
        assertNotNull(zero, "Normal is immune to Ghost, so the x0 row must be present");
        assertTrue(zero.types().contains(Type.GHOST));
    }
}
