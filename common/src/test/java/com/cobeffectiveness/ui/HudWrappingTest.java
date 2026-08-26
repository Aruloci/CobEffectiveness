package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.type.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Long rows must fold rather than run off the screen.
 *
 * <p>With Gravel's Extended Battles loaded the x2 row against a Shadow type is 24 icons, 334px on a
 * single line - wider than the whole screen at a large GUI scale, and drawn twice side by side when
 * spectating.
 */
class HudWrappingTest {

    private static final int SCREEN_W = 480;
    private static final int SCREEN_H = 270;

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
            new HudLayout.Viewport(SCREEN_W, SCREEN_H, Anchor.TOP_RIGHT, 12, 30);

    private static List<Type> types(int count) {
        return List.of(Type.values()).subList(0, count);
    }

    private static HudModel modelWith(int iconCount) {
        return new HudModel(List.of(new HudModel.Panel(null, List.of(
                new HudModel.Section("Opponent", List.of(),
                        List.of(new HudModel.Row(2.0, "x2", 0xFFFFFFFF, types(iconCount))))))));
    }

    private static List<HudLayout.PlacedElement> iconRows(HudLayout.Panel panel) {
        List<HudLayout.PlacedElement> rows = new ArrayList<>();
        for (HudLayout.PlacedElement placed : panel.elements()) {
            if (placed.element().kind() == HudLayout.Kind.ROW) rows.add(placed);
        }
        return rows;
    }

    @Test
    void aVanillaWidthRowStillFitsOnOneLine() {
        // The widest vanilla row is 10 icons; the shape users already know must not change.
        HudLayout.Panel panel = HudLayout.measure(modelWith(10), METRICS, SCREEN).panels().get(0);

        assertEquals(1, iconRows(panel).size(), "a vanilla row should not wrap at this scale");
    }

    @Test
    void aLongRowWrapsRatherThanOverflowing() {
        HudLayout.Panel panel = HudLayout.measure(modelWith(24), METRICS, SCREEN).panels().get(0);

        assertTrue(iconRows(panel).size() > 1, "24 icons must not stay on one line");
        assertTrue(panel.width() <= Math.round(SCREEN_W * 0.40F),
                () -> "panel took " + panel.width() + "px of a " + SCREEN_W + "px screen");
        assertTrue(panel.x() >= 0 && panel.x() + panel.width() <= SCREEN_W, "panel must stay on screen");
    }

    @Test
    void wrappingKeepsEveryIconExactlyOnceAndInOrder() {
        HudLayout.Panel panel = HudLayout.measure(modelWith(24), METRICS, SCREEN).panels().get(0);

        List<Type> drawn = new ArrayList<>();
        for (HudLayout.PlacedElement row : iconRows(panel)) {
            drawn.addAll(row.element().types());
        }
        assertEquals(types(24), drawn, "wrapping must not drop, duplicate or reorder icons");
    }

    @Test
    void onlyTheFirstLineOfAWrappedRowPrintsTheLabel() {
        HudLayout.Panel panel = HudLayout.measure(modelWith(24), METRICS, SCREEN).panels().get(0);
        List<HudLayout.PlacedElement> rows = iconRows(panel);

        assertTrue(rows.get(0).element().showLabel());
        for (int i = 1; i < rows.size(); i++) {
            assertTrue(!rows.get(i).element().showLabel(), "line " + i + " repeated the label");
        }
    }

    @Test
    void wrappedLinesLineUpWithEachOther() {
        HudLayout.Panel panel = HudLayout.measure(modelWith(24), METRICS, SCREEN).panels().get(0);
        List<HudLayout.PlacedElement> rows = iconRows(panel);

        int iconX = rows.get(0).iconX();
        for (HudLayout.PlacedElement row : rows) {
            assertEquals(iconX, row.iconX(), "wrapped lines must share an icon column");
        }
    }

    @Test
    void twoWidePanelsStillFitSideBySideWhenSpectating() {
        HudModel model = new HudModel(List.of(
                new HudModel.Panel("Alice", List.of(new HudModel.Section("A", List.of(),
                        List.of(new HudModel.Row(2.0, "x2", 0xFFFFFFFF, types(24)))))),
                new HudModel.Panel("Bob", List.of(new HudModel.Section("B", List.of(),
                        List.of(new HudModel.Row(2.0, "x2", 0xFFFFFFFF, types(24))))))));

        HudLayout.Layout layout = HudLayout.measure(model, METRICS, SCREEN);
        HudLayout.Panel left = layout.panels().get(0);
        HudLayout.Panel right = layout.panels().get(1);

        assertTrue(left.x() >= 0, "left panel ran off the screen");
        assertTrue(right.x() + right.width() <= SCREEN_W, "right panel ran off the screen");
        assertTrue(left.x() + left.width() <= right.x(), "panels overlapped");
    }

    @Test
    void aTinyScreenStillProducesSomethingDrawable() {
        HudLayout.Viewport tiny = new HudLayout.Viewport(320, 240, Anchor.TOP_RIGHT, 12, 30);
        HudLayout.Panel panel = HudLayout.measure(modelWith(24), METRICS, tiny).panels().get(0);

        assertTrue(panel.width() <= 320, "panel must never exceed the screen");
        for (HudLayout.PlacedElement row : iconRows(panel)) {
            assertTrue(!row.element().types().isEmpty(), "a line with no icons would loop forever");
        }
    }
}
