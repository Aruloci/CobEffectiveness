package com.cobeffectiveness.ui;

import com.cobeffectiveness.battle.BattleView;
import com.cobeffectiveness.battle.MoveInfo;
import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.config.Multiplier;
import com.cobeffectiveness.type.ChartJson;
import com.cobeffectiveness.type.Charts;
import com.cobeffectiveness.type.EffectivenessChart;
import com.cobeffectiveness.type.IconSheet;
import com.cobeffectiveness.type.Type;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Scoring the player's own moves instead of listing types. */
class MoveViewTest {

    private static final Set<Type> CHARIZARD = EnumSet.of(Type.FIRE, Type.FLYING);
    private static final Set<Type> BLASTOISE = EnumSet.of(Type.WATER);
    private static final List<Multiplier> SHOWN = List.of(Multiplier.X4, Multiplier.X2);

    private static final MoveInfo ROCK_SLIDE = new MoveInfo("Rock Slide", Type.ROCK, true, true);
    private static final MoveInfo SURF = new MoveInfo("Surf", Type.WATER, true, true);
    private static final MoveInfo EARTHQUAKE = new MoveInfo("Earthquake", Type.GROUND, true, true);
    private static final MoveInfo GROWL = new MoveInfo("Growl", Type.NORMAL, false, true);
    private static final MoveInfo NO_PP = new MoveInfo("Fly", Type.FLYING, true, false);

    private static final HudLayout.Metrics METRICS = new HudLayout.Metrics() {
        @Override
        public int textWidth(String text) {
            return text.length() * 6;
        }

        @Override
        public int fontHeight() {
            return 9;
        }

        @Override
        public int iconRowWidth(int count) {
            return count <= 0 ? 0 : count * 12 + (count - 1) * 2;
        }

        @Override
        public int iconHeight() {
            return 12;
        }
    };

    private static final HudLayout.Viewport SCREEN =
            new HudLayout.Viewport(480, 270, Anchor.TOP_RIGHT, 12, 30);

    private static EffectivenessChart chart;
    private static IconSheet sheet;

    @BeforeAll
    static void loadVanillaChart() throws Exception {
        ChartJson json = Charts.load("vanilla");
        chart = EffectivenessChart.from(json).chart();
        sheet = IconSheet.from(json.tiles, json.order).sheet();
    }

    private static HudModel model(List<MoveInfo> moves, HudModel.Content mode, IconSheet withSheet,
                                  BattleView.Target... targets) {
        BattleView view = new BattleView(List.of(new BattleView.Side(null, List.of(targets))));
        return HudModel.from(view, moves, chart, withSheet,
                new HudModel.Options(SHOWN, mode));
    }

    private static List<HudModel.MoveRow> movesAgainst(Set<Type> defender, List<MoveInfo> moves) {
        return model(moves, HudModel.Content.MOVES, sheet, new BattleView.Target("Opponent", defender))
                .panels().get(0).sections().get(0).moves();
    }

    private static HudModel.MoveRow named(List<HudModel.MoveRow> rows, String name) {
        return rows.stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void movesAreScoredAgainstTheOpponentInFront() {
        List<HudModel.MoveRow> rows =
                movesAgainst(CHARIZARD, List.of(ROCK_SLIDE, SURF, EARTHQUAKE, GROWL));

        assertEquals("×4", named(rows, "Rock Slide").value());
        assertEquals("×2", named(rows, "Surf").value());
        // Ground is x2 into Fire but x0 into Flying, so Earthquake does nothing at all.
        assertEquals("×0", named(rows, "Earthquake").value());
    }

    @Test
    void aNeutralMoveReadsTimesOne() {
        List<HudModel.MoveRow> rows = movesAgainst(BLASTOISE, List.of(ROCK_SLIDE));

        assertEquals("×1", named(rows, "Rock Slide").value());
    }

    @Test
    void statusMovesGetADashRatherThanAMultiplier() {
        HudModel.MoveRow growl = named(movesAgainst(CHARIZARD, List.of(GROWL)), "Growl");

        assertEquals("—", growl.value());
        assertEquals(growl.nameColor(), growl.valueColor(), "a status move is dimmed throughout");
    }

    @Test
    void unusableMovesAreDimmedEvenThoughTheyWouldBeEffective() {
        // Fly is Flying, which is neutral here, but it has no PP so it is not a choice.
        HudModel.MoveRow fly = named(movesAgainst(CHARIZARD, List.of(NO_PP)), "Fly");
        HudModel.MoveRow usable = named(movesAgainst(CHARIZARD, List.of(ROCK_SLIDE)), "Rock Slide");

        assertEquals("—", fly.value());
        assertNotEquals(usable.nameColor(), fly.nameColor(), "an unusable move should look different");
    }

    @Test
    void everyMoveSlotIsListedSoTheRowsMatchTheMoveButtons() {
        List<HudModel.MoveRow> rows =
                movesAgainst(CHARIZARD, List.of(ROCK_SLIDE, GROWL, NO_PP, SURF));

        assertEquals(4, rows.size());
        assertEquals(List.of("Rock Slide", "Growl", "Fly", "Surf"),
                rows.stream().map(HudModel.MoveRow::name).toList());
    }

    @Test
    void withNoMovesKnownTheMovesPanelDrawsNothing() {
        // Spectating, or before your first turn. The effectiveness panel is separate and unaffected.
        HudModel movesOnly = model(List.of(), HudModel.Content.MOVES, sheet,
                new BattleView.Target("Charizard", CHARIZARD));

        assertTrue(movesOnly.isEmpty());
    }

    @Test
    void doublesRepeatsTheMoveListWithEachOpponentsMultipliers() {
        HudModel model = model(List.of(ROCK_SLIDE, SURF), HudModel.Content.MOVES, sheet,
                new BattleView.Target("Charizard", CHARIZARD),
                new BattleView.Target("Blastoise", BLASTOISE));

        List<HudModel.Section> sections = model.panels().get(0).sections();
        assertEquals(2, sections.size());

        assertEquals("×4", named(sections.get(0).moves(), "Rock Slide").value());
        assertEquals("×1", named(sections.get(1).moves(), "Rock Slide").value());
        assertEquals("×2", named(sections.get(0).moves(), "Surf").value());
        assertEquals("×0.5", named(sections.get(1).moves(), "Surf").value());
    }

    @Test
    void aMoveWhoseTypeHasNoIconKeepsItsRow() {
        ChartJson.Tiles tiles = new ChartJson.Tiles();
        tiles.cols = 4;
        tiles.rows = 1;
        tiles.tile_w = 12;
        tiles.tile_h = 12;
        IconSheet rockOnly = IconSheet.from(tiles, List.of("Rock")).sheet();

        List<HudModel.MoveRow> rows = model(List.of(ROCK_SLIDE, SURF), HudModel.Content.MOVES, rockOnly,
                new BattleView.Target("Charizard", CHARIZARD)).panels().get(0).sections().get(0).moves();

        assertEquals(2, rows.size());
        assertEquals(Type.ROCK, named(rows, "Rock Slide").type());
        assertNull(named(rows, "Surf").type(), "no icon on this sheet");
        assertEquals("×2", named(rows, "Surf").value(), "the multiplier is still worth showing");
    }

    // --- layout ---

    private static List<HudLayout.PlacedElement> moveRows(HudModel model) {
        List<HudLayout.PlacedElement> rows = new ArrayList<>();
        for (HudLayout.PlacedElement placed : HudLayout.measure(model, METRICS, SCREEN)
                .panels().get(0).elements()) {
            if (placed.element().kind() == HudLayout.Kind.MOVE_ROW) rows.add(placed);
        }
        return rows;
    }

    @Test
    void moveRowsFormAlignedColumns() {
        // Names of very different lengths must not stagger the icon or multiplier columns.
        List<HudLayout.PlacedElement> rows = moveRows(model(
                List.of(ROCK_SLIDE, SURF, EARTHQUAKE, GROWL), HudModel.Content.MOVES, sheet,
                new BattleView.Target("Charizard", CHARIZARD)));

        assertEquals(4, rows.size());
        int iconX = rows.get(0).iconX();
        int trailingX = rows.get(0).trailingX();

        for (HudLayout.PlacedElement row : rows) {
            assertEquals(iconX, row.iconX(), "icon column drifted");
            assertEquals(trailingX, row.trailingX(), "multiplier column drifted");
            assertTrue(row.trailingX() > row.iconX(), "the multiplier sits after the icon");
        }
    }

    @Test
    void aMoveWithNoIconStillLeavesItsMultiplierInTheColumn() {
        ChartJson.Tiles tiles = new ChartJson.Tiles();
        tiles.cols = 4;
        tiles.rows = 1;
        tiles.tile_w = 12;
        tiles.tile_h = 12;
        IconSheet rockOnly = IconSheet.from(tiles, List.of("Rock")).sheet();

        List<HudLayout.PlacedElement> rows = moveRows(model(List.of(ROCK_SLIDE, SURF), HudModel.Content.MOVES, rockOnly,
                new BattleView.Target("Charizard", CHARIZARD)));

        assertEquals(rows.get(0).trailingX(), rows.get(1).trailingX(),
                "a missing icon must still reserve its slot");
    }

    @Test
    void aVeryLongMoveNameIsTruncatedRatherThanWideningThePanel() {
        MoveInfo longName = new MoveInfo(
                "Extremely Long Fabricated Move Name That Should Not Fit", Type.ROCK, true, true);

        HudLayout.Panel panel = HudLayout.measure(
                model(List.of(longName), HudModel.Content.MOVES, sheet, new BattleView.Target("Charizard", CHARIZARD)),
                METRICS, SCREEN).panels().get(0);

        String drawn = panel.elements().get(0).element().text();
        assertTrue(drawn.endsWith("..."), () -> "expected an ellipsis, got: " + drawn);
        assertTrue(panel.width() <= Math.round(480 * 0.40F),
                () -> "panel took " + panel.width() + "px");
    }

    @Test
    void theTwoPanelsAreBuiltAsSeparateModels() {
        // Each panel is measured and placed on its own, so neither carries the other's rows.
        HudModel types = model(List.of(ROCK_SLIDE, SURF), HudModel.Content.TYPES, sheet,
                new BattleView.Target("Charizard", CHARIZARD));
        HudModel movesOnly = model(List.of(ROCK_SLIDE, SURF), HudModel.Content.MOVES, sheet,
                new BattleView.Target("Charizard", CHARIZARD));

        HudModel.Section typeSection = types.panels().get(0).sections().get(0);
        HudModel.Section moveSection = movesOnly.panels().get(0).sections().get(0);

        assertFalse(typeSection.rows().isEmpty());
        assertTrue(typeSection.moves().isEmpty(), "the effectiveness panel carries no move rows");
        assertFalse(moveSection.moves().isEmpty());
        assertTrue(moveSection.rows().isEmpty(), "the moves panel carries no type rows");
    }

    @Test
    void theMovesPanelNamesItsBlocksOnlyWhenThereIsMoreThanOne() {
        // In singles the name panel already says who it is; repeating it was the duplication.
        HudModel singles = model(List.of(ROCK_SLIDE), HudModel.Content.MOVES, sheet,
                new BattleView.Target("Charizard", CHARIZARD));
        HudModel doubles = model(List.of(ROCK_SLIDE), HudModel.Content.MOVES, sheet,
                new BattleView.Target("Charizard", CHARIZARD),
                new BattleView.Target("Blastoise", BLASTOISE));

        assertTrue(singles.panels().get(0).sections().get(0).types().isEmpty(),
                "only the name panel carries the type icons");
        assertEquals(2, doubles.panels().get(0).sections().size(),
                "each opponent still gets its own block of moves");
    }

    @Test
    void typesModeNeverShowsMovesEvenWhenTheyAreKnown() {
        HudModel.Section section = model(List.of(ROCK_SLIDE, SURF), HudModel.Content.TYPES, sheet,
                new BattleView.Target("Charizard", CHARIZARD)).panels().get(0).sections().get(0);

        assertTrue(section.moves().isEmpty());
        assertFalse(section.rows().isEmpty());
    }
}
