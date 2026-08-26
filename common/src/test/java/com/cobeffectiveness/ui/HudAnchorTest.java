package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.type.Type;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each anchor corner measures its offset from a different edge. */
class HudAnchorTest {

    private static final int SCREEN_W = 854;
    private static final int SCREEN_H = 480;
    private static final int OFFSET_X = 20;
    private static final int OFFSET_Y = 30;

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

    private static HudModel model() {
        return new HudModel(List.of(new HudModel.Panel(null, List.of(
                new HudModel.Section("Charizard", List.of(),
                        List.of(new HudModel.Row(2.0, "x2", 0xFFFFFFFF, List.of(Type.ROCK))))))));
    }

    private static HudLayout.Panel place(Anchor anchor) {
        HudLayout.Viewport viewport =
                new HudLayout.Viewport(SCREEN_W, SCREEN_H, anchor, OFFSET_X, OFFSET_Y);
        return HudLayout.measure(model(), METRICS, viewport).panels().get(0);
    }

    @Test
    void topLeftMeasuresFromTheTopLeft() {
        HudLayout.Panel panel = place(Anchor.TOP_LEFT);

        assertEquals(OFFSET_X, panel.x());
        assertEquals(OFFSET_Y, panel.y());
    }

    @Test
    void topRightMeasuresFromTheRightEdge() {
        HudLayout.Panel panel = place(Anchor.TOP_RIGHT);

        assertEquals(SCREEN_W - OFFSET_X, panel.x() + panel.width());
        assertEquals(OFFSET_Y, panel.y());
    }

    @Test
    void bottomLeftMeasuresFromTheBottomEdge() {
        HudLayout.Panel panel = place(Anchor.BOTTOM_LEFT);

        assertEquals(OFFSET_X, panel.x());
        assertEquals(SCREEN_H - OFFSET_Y, panel.y() + panel.height());
    }

    @Test
    void bottomRightMeasuresFromBothFarEdges() {
        HudLayout.Panel panel = place(Anchor.BOTTOM_RIGHT);

        assertEquals(SCREEN_W - OFFSET_X, panel.x() + panel.width());
        assertEquals(SCREEN_H - OFFSET_Y, panel.y() + panel.height());
    }

    @Test
    void everyAnchorKeepsThePanelOnScreen() {
        for (Anchor anchor : Anchor.values()) {
            // An offset far larger than the screen must still not push the panel out of view.
            HudLayout.Viewport viewport = new HudLayout.Viewport(SCREEN_W, SCREEN_H, anchor, 5000, 5000);
            HudLayout.Panel panel = HudLayout.measure(model(), METRICS, viewport).panels().get(0);

            assertTrue(panel.x() >= 0, anchor + " pushed the panel off the left");
            assertTrue(panel.y() >= 0, anchor + " pushed the panel off the top");
            assertTrue(panel.x() + panel.width() <= SCREEN_W, anchor + " pushed the panel off the right");
            assertTrue(panel.y() + panel.height() <= SCREEN_H, anchor + " pushed the panel off the bottom");
        }
    }
}
