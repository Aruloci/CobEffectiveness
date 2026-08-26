package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.type.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Geometry checks for the two layouts that are hard to reach in a running game: a doubles battle
 * and a spectated battle.
 */
class HudLayoutTest {

    private static final int ICON = 12;
    private static final int ICON_GAP = 2;
    private static final int FONT_H = 9;
    private static final int CHAR_W = 6;
    private static final int LABEL_GAP = 4;

    private static final HudLayout.Metrics METRICS = new HudLayout.Metrics() {
        @Override
        public int textWidth(String text) {
            return text.length() * CHAR_W;
        }

        @Override
        public int fontHeight() {
            return FONT_H;
        }

        @Override
        public int iconRowWidth(int count) {
            return count <= 0 ? 0 : count * ICON + (count - 1) * ICON_GAP;
        }

        @Override
        public int iconHeight() {
            return ICON;
        }
    };

    private static final HudLayout.Viewport SCREEN = new HudLayout.Viewport(854, 480, Anchor.TOP_RIGHT, 8, 30);

    private static HudModel.Section section(String name, Type... types) {
        return new HudModel.Section(name, List.of(), List.of(new HudModel.Row(2.0, "x2", 0xFFFFFFFF, List.of(types))));
    }

    private static HudModel singles() {
        return new HudModel(List.of(new HudModel.Panel(null, List.of(section("Charizard", Type.ROCK)))));
    }

    private static HudModel doubles() {
        return new HudModel(List.of(new HudModel.Panel(null, List.of(
                section("Charizard", Type.ROCK, Type.WATER),
                section("Blastoise", Type.GRASS, Type.ELECTRIC)))));
    }

    private static HudModel spectating() {
        return new HudModel(List.of(
                new HudModel.Panel("Alice", List.of(section("Charizard", Type.ROCK))),
                new HudModel.Panel("Bob", List.of(section("Blastoise", Type.GRASS)))));
    }

    private static void assertElementsStayInsidePanels(HudLayout.Layout layout) {
        for (HudLayout.Panel panel : layout.panels()) {
            for (HudLayout.PlacedElement placed : panel.elements()) {
                assertTrue(placed.x() >= panel.x(),
                        () -> "element starts left of its panel: " + placed);
                assertTrue(placed.x() + placed.width() <= panel.x() + panel.width(),
                        () -> "element runs past the panel right edge: " + placed);
                assertTrue(placed.y() >= panel.y(),
                        () -> "element starts above its panel: " + placed);
                assertTrue(placed.y() + placed.height() <= panel.y() + panel.height(),
                        () -> "element runs past the panel bottom: " + placed);
            }
        }
    }

    @Test
    void singlesPanelIsRightAligned() {
        HudLayout.Panel panel = HudLayout.measure(singles(), METRICS, SCREEN).panels().get(0);

        assertEquals(SCREEN.width() - SCREEN.offsetX(), panel.x() + panel.width());
        assertEquals(SCREEN.offsetY(), panel.y());
        assertElementsStayInsidePanels(HudLayout.measure(singles(), METRICS, SCREEN));
    }

    @Test
    void doublesGrowsDownwardsAndKeepsBothBlocks() {
        HudLayout.Layout layout = HudLayout.measure(doubles(), METRICS, SCREEN);
        HudLayout.Panel panel = layout.panels().get(0);

        assertEquals(1, layout.panels().size(), "one panel, two blocks inside it");
        assertTrue(panel.height() > HudLayout.measure(singles(), METRICS, SCREEN).panels().get(0).height(),
                "a second opponent must make the panel taller");

        long headings = panel.elements().stream()
                .filter(p -> p.element().kind() == HudLayout.Kind.HEADING)
                .count();
        long dividers = panel.elements().stream().filter(p -> p.element().isDivider()).count();

        assertEquals(2, headings, "each opponent is named in doubles");
        assertEquals(1, dividers, "the two opponents are separated");
        assertElementsStayInsidePanels(layout);
    }

    @Test
    void spectatorPanelsSitSideBySideWithoutOverlapping() {
        HudLayout.Layout layout = HudLayout.measure(spectating(), METRICS, SCREEN);
        assertEquals(2, layout.panels().size());

        HudLayout.Panel left = layout.panels().get(0);
        HudLayout.Panel right = layout.panels().get(1);

        assertTrue(left.x() + left.width() <= right.x(), "spectator panels must not overlap");
        assertEquals(left.y(), right.y(), "both sides sit on the same line");
        assertEquals(SCREEN.width() - SCREEN.offsetX(), right.x() + right.width());
        assertElementsStayInsidePanels(layout);
    }

    @Test
    void iconsFollowTheirLabel() {
        HudLayout.Panel panel = HudLayout.measure(singles(), METRICS, SCREEN).panels().get(0);
        HudLayout.PlacedElement row = panel.elements().stream()
                .filter(p -> p.element().kind() == HudLayout.Kind.ROW)
                .findFirst()
                .orElseThrow();

        assertEquals(row.x() + METRICS.textWidth(row.element().text()) + LABEL_GAP, row.iconX());
    }

    @Test
    void panelsNeverStartOffTheLeftEdge() {
        // A narrow window plus two spectator panels would otherwise place them at a negative x.
        HudLayout.Viewport narrow = new HudLayout.Viewport(120, 480, Anchor.TOP_RIGHT, 8, 30);
        HudLayout.Layout layout = HudLayout.measure(spectating(), METRICS, narrow);

        assertEquals(0, layout.panels().get(0).x());
        assertElementsStayInsidePanels(layout);
    }

    @Test
    void aTallPanelIsPulledUpToStayOnScreen() {
        HudLayout.Viewport shortScreen = new HudLayout.Viewport(854, 90, Anchor.TOP_RIGHT, 8, 30);
        HudLayout.Panel panel = HudLayout.measure(doubles(), METRICS, shortScreen).panels().get(0);

        assertTrue(panel.y() < shortScreen.offsetY(), "the panel should be lifted, not left hanging");
        assertTrue(panel.y() + panel.height() <= shortScreen.height(), "it must fit on screen");
    }

    @Test
    void aPanelTallerThanTheScreenIsPinnedToTheTop() {
        // Nothing sensible fits; keep the top visible so the strongest matchups can still be read.
        HudLayout.Viewport tiny = new HudLayout.Viewport(854, 30, Anchor.TOP_RIGHT, 8, 30);
        assertEquals(0, HudLayout.measure(doubles(), METRICS, tiny).panels().get(0).y());
    }

    @Test
    void anEmptyModelPlacesNothing() {
        assertTrue(HudLayout.measure(HudModel.EMPTY, METRICS, SCREEN).isEmpty());
    }
}
