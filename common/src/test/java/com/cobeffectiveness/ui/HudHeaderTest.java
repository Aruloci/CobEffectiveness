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

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the opponent's name is printed.
 *
 * <p>It used to appear on every panel, which read as duplication once there was more than one. The
 * name now lives on the name panel; the other two only name a block when there is more than one
 * opponent and the blocks would otherwise be ambiguous.
 */
class HudHeaderTest {

    private static final int SCREEN_W = 480;
    private static final List<Multiplier> SHOWN = List.of(Multiplier.X4, Multiplier.X2);

    private static final List<MoveInfo> MOVES = List.of(
            new MoveInfo("Rock Slide", Type.ROCK, true, true),
            new MoveInfo("Surf", Type.WATER, true, true));

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
            new HudLayout.Viewport(SCREEN_W, 270, Anchor.TOP_RIGHT, 12, 30);

    private static EffectivenessChart chart;
    private static IconSheet sheet;

    @BeforeAll
    static void loadVanillaChart() throws Exception {
        ChartJson json = Charts.load("vanilla");
        chart = EffectivenessChart.from(json).chart();
        sheet = IconSheet.from(json.tiles, json.order).sheet();
    }

    private static BattleView singles(String name) {
        return new BattleView(List.of(new BattleView.Side(null, List.of(
                new BattleView.Target(name, EnumSet.of(Type.FIRE, Type.FLYING))))));
    }

    private static BattleView doubles() {
        return new BattleView(List.of(new BattleView.Side(null, List.of(
                new BattleView.Target("Charizard", EnumSet.of(Type.FIRE, Type.FLYING)),
                new BattleView.Target("Blastoise", EnumSet.of(Type.WATER))))));
    }

    private static HudModel model(BattleView view, HudModel.Content content, IconSheet withSheet) {
        return HudModel.from(view, MOVES, chart, withSheet, new HudModel.Options(SHOWN, content));
    }

    private static List<HudLayout.PlacedElement> place(HudModel model) {
        return HudLayout.measure(model, METRICS, SCREEN).panels().get(0).elements();
    }

    private static List<String> headings(HudModel model) {
        return place(model).stream()
                .filter(p -> p.element().kind() == HudLayout.Kind.HEADING)
                .map(p -> p.element().text())
                .toList();
    }

    @Test
    void theOpponentPanelCarriesTheNameAndTheTypes() {
        HudModel.Section section = model(singles("Charizard"), HudModel.Content.NAME, sheet)
                .panels().get(0).sections().get(0);

        assertEquals("Charizard", section.label());
        assertEquals(List.of(Type.FIRE, Type.FLYING), section.types());
        assertTrue(section.rows().isEmpty(), "the name panel lists nothing else");
    }

    @Test
    void theOpponentPanelHasNoTrailingRule() {
        // A rule under the name would be a line to nowhere: there is nothing beneath it.
        List<HudLayout.PlacedElement> elements = place(model(singles("Charizard"),
                HudModel.Content.NAME, sheet));

        assertEquals(1, elements.size());
        assertEquals(HudLayout.Kind.HEADING, elements.get(0).element().kind());
    }

    @Test
    void inSinglesTheOtherPanelsDoNotRepeatTheName() {
        // This is the duplication that prompted the split.
        assertEquals(List.of(), headings(model(singles("Charizard"), HudModel.Content.TYPES, sheet)));
        assertEquals(List.of(), headings(model(singles("Charizard"), HudModel.Content.MOVES, sheet)));
    }

    @Test
    void inDoublesTheOtherPanelsStillNameEachBlock() {
        // Two blocks with no names would be unreadable, so the name comes back where it earns its place.
        assertEquals(List.of("Charizard", "Blastoise"),
                headings(model(doubles(), HudModel.Content.TYPES, sheet)));
        assertEquals(List.of("Charizard", "Blastoise"),
                headings(model(doubles(), HudModel.Content.MOVES, sheet)));
    }

    @Test
    void theOpponentPanelNamesEveryOpponentInDoubles() {
        assertEquals(List.of("Charizard", "Blastoise"),
                headings(model(doubles(), HudModel.Content.NAME, sheet)));
    }

    @Test
    void headerIconsAreDrawnAfterTheName() {
        HudLayout.PlacedElement header =
                place(model(singles("Charizard"), HudModel.Content.NAME, sheet)).get(0);

        assertTrue(header.iconX() > header.x(), "icons follow the name");
        assertEquals(header.x() + METRICS.textWidth("Charizard") + 4, header.iconX());
    }

    @Test
    void typesTheSheetCannotDrawAreLeftOutOfTheHeader() {
        ChartJson.Tiles tiles = new ChartJson.Tiles();
        tiles.cols = 4;
        tiles.rows = 1;
        tiles.tile_w = 12;
        tiles.tile_h = 12;
        IconSheet rockOnly = IconSheet.from(tiles, List.of("Rock")).sheet();

        assertTrue(model(singles("Charizard"), HudModel.Content.NAME, rockOnly).isEmpty(),
                "neither Fire nor Flying is on this sheet, so there is nothing to draw");
    }

    @Test
    void aLongNicknameIsTruncatedRatherThanWideningThePanel() {
        // Rows wrap, headings cannot, so an unbounded name would blow the width budget.
        String nickname = "Bartholomew The Absolutely Enormous Fire Lizard";
        HudLayout.Panel panel = HudLayout.measure(
                model(singles(nickname), HudModel.Content.NAME, sheet), METRICS, SCREEN)
                .panels().get(0);

        String drawn = panel.elements().get(0).element().text();
        assertTrue(drawn.endsWith("..."), () -> "expected an ellipsis, got: " + drawn);
        assertFalse(drawn.equals(nickname));
        assertTrue(panel.width() <= Math.round(SCREEN_W * 0.40F),
                () -> "panel took " + panel.width() + "px of a " + SCREEN_W + "px screen");
    }
}
