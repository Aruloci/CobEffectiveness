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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudModelTest {

    private static final Set<Type> CHARIZARD = EnumSet.of(Type.FIRE, Type.FLYING);
    private static final Set<Type> BLASTOISE = EnumSet.of(Type.WATER);

    private static final List<Multiplier> SHOWN = List.of(Multiplier.X4, Multiplier.X2);

    private static EffectivenessChart chart;
    private static IconSheet sheet;

    @BeforeAll
    static void loadVanillaChart() throws Exception {
        ChartJson json = Charts.load("vanilla");
        chart = EffectivenessChart.from(json).chart();
        sheet = IconSheet.from(json.tiles, json.order).sheet();
    }

    private static BattleView battlerAgainst(BattleView.Target... targets) {
        return new BattleView(List.of(new BattleView.Side(null, List.of(targets))));
    }

    private static HudModel.Options opts() {
        return new HudModel.Options(SHOWN, HudModel.Content.TYPES);
    }

    private static HudModel.Options opts(List<Multiplier> shown) {
        return new HudModel.Options(shown, HudModel.Content.TYPES);
    }

    private static HudModel.Row row(HudModel.Section section, double multiplier) {
        return section.rows().stream()
                .filter(r -> r.multiplier() == multiplier)
                .findFirst()
                .orElse(null);
    }

    @Test
    void eachActiveOpponentGetsItsOwnSection() {
        HudModel model = HudModel.from(battlerAgainst(
                new BattleView.Target("Charizard", CHARIZARD),
                new BattleView.Target("Blastoise", BLASTOISE)), List.of(), chart, sheet, opts());

        assertEquals(1, model.panels().size());

        List<HudModel.Section> sections = model.panels().get(0).sections();
        assertEquals(2, sections.size(), "doubles must not collapse into a single block");
        assertEquals("Charizard", sections.get(0).label());
        assertEquals("Blastoise", sections.get(1).label());
    }

    @Test
    void opponentsAreScoredSeparatelyRatherThanAsOneMergedTypeSet() {
        // Merging every active opponent describes a Pokemon that does not exist. Against the union
        // of Charizard and Blastoise, Electric reads x4 (x2 from Flying, x2 from Water) even though
        // it is only x2 against either of them.
        Set<Type> merged = EnumSet.copyOf(CHARIZARD);
        merged.addAll(BLASTOISE);
        assertEquals(4.0, chart.multiplier(Type.ELECTRIC, merged), "guard for the merged behaviour");

        HudModel model = HudModel.from(battlerAgainst(
                new BattleView.Target("Charizard", CHARIZARD),
                new BattleView.Target("Blastoise", BLASTOISE)), List.of(), chart, sheet, opts());

        List<HudModel.Section> sections = model.panels().get(0).sections();

        HudModel.Row charizardFourX = row(sections.get(0), 4.0);
        assertNotNull(charizardFourX);
        assertEquals(List.of(Type.ROCK), charizardFourX.types());
        assertTrue(row(sections.get(0), 2.0).types().contains(Type.ELECTRIC));

        HudModel.Row blastoiseTwoX = row(sections.get(1), 2.0);
        assertNotNull(blastoiseTwoX);
        assertTrue(blastoiseTwoX.types().containsAll(List.of(Type.GRASS, Type.ELECTRIC)));
        assertFalse(blastoiseTwoX.types().contains(Type.ROCK), "Rock is neutral against Water");
        assertNull(row(sections.get(1), 4.0), "nothing hits a lone Water type for x4");
    }

    @Test
    void aBattlerSeesNoSideHeading() {
        HudModel model = HudModel.from(
                battlerAgainst(new BattleView.Target("Charizard", CHARIZARD)), List.of(), chart, sheet, opts());

        assertNull(model.panels().get(0).label());
    }

    @Test
    void aSpectatorGetsOneLabelledPanelPerSide() {
        BattleView view = new BattleView(List.of(
                new BattleView.Side("Alice", List.of(new BattleView.Target("Charizard", CHARIZARD))),
                new BattleView.Side("Bob", List.of(new BattleView.Target("Blastoise", BLASTOISE)))));

        HudModel model = HudModel.from(view, List.of(), chart, sheet, opts());

        assertEquals(2, model.panels().size());
        assertEquals("Alice", model.panels().get(0).label());
        assertEquals("Bob", model.panels().get(1).label());
        assertEquals(List.of(Type.ROCK), row(model.panels().get(0).sections().get(0), 4.0).types());
    }

    @Test
    void typesTheSheetCannotDrawAreLeftOut() {
        // Measuring types that the sheet has no icon for used to reserve width for icons that were
        // then skipped at draw time.
        ChartJson.Tiles tiles = new ChartJson.Tiles();
        tiles.cols = 4;
        tiles.rows = 1;
        tiles.tile_w = 12;
        tiles.tile_h = 12;
        IconSheet rockOnly = IconSheet.from(tiles, List.of("Rock")).sheet();

        HudModel model = HudModel.from(
                battlerAgainst(new BattleView.Target("Charizard", CHARIZARD)), List.of(), chart, rockOnly, opts());

        List<HudModel.Row> rows = model.panels().get(0).sections().get(0).rows();
        assertEquals(1, rows.size(), "only the x4 Rock row can be drawn");
        assertEquals(List.of(Type.ROCK), rows.get(0).types());
    }

    @Test
    void anEmptyBattleProducesNothingToDraw() {
        assertTrue(HudModel.from(new BattleView(List.of()), List.of(), chart, sheet, opts()).isEmpty());
    }

    @Test
    void immunitiesAppearOnlyWhenTheirRowIsEnabled() {
        // Ground is x2 into Fire but x0 into Flying, so it does nothing at all to Charizard.
        List<Multiplier> withZero = List.of(Multiplier.X4, Multiplier.X2, Multiplier.X0);

        HudModel without = HudModel.from(
                battlerAgainst(new BattleView.Target("Charizard", CHARIZARD)), List.of(), chart, sheet, opts());
        assertNull(row(without.panels().get(0).sections().get(0), 0.0));

        HudModel with = HudModel.from(
                battlerAgainst(new BattleView.Target("Charizard", CHARIZARD)), List.of(), chart, sheet, opts(withZero));
        HudModel.Row zero = row(with.panels().get(0).sections().get(0), 0.0);

        assertNotNull(zero, "x0 was switched on, so the row must exist");
        assertTrue(zero.types().contains(Type.GROUND));
    }

    @Test
    void onlyTheEnabledRowsAreBuilt() {
        HudModel model = HudModel.from(
                battlerAgainst(new BattleView.Target("Charizard", CHARIZARD)),
                List.of(), chart, sheet, opts(List.of(Multiplier.X0)));

        List<HudModel.Row> rows = model.panels().get(0).sections().get(0).rows();
        assertEquals(1, rows.size());
        assertEquals(0.0, rows.get(0).multiplier());
    }

    @Test
    void switchingEveryRowOffDrawsNothing() {
        HudModel model = HudModel.from(
                battlerAgainst(new BattleView.Target("Charizard", CHARIZARD)), List.of(), chart, sheet, opts(List.of()));

        assertTrue(model.isEmpty());
    }
}
