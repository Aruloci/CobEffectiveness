package com.cobeffectiveness.ui;

import com.cobeffectiveness.battle.TileBounds;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rule that decides when "only while hovering" gives up and shows the HUD anyway.
 *
 * <p>TileBounds reproduces Cobblemon's tile geometry, so if Cobblemon changes its battle UI the
 * rectangles stop making sense. A strict hover test would then hide the HUD forever with no clue
 * why, so the gate has to fail open - and that rule is worth testing rather than trusting.
 */
class HoverGateTest {

    private static final int SCREEN_W = 480;
    private static final int SCREEN_H = 270;

    private static TileBounds.Rect rect(int x, int y, int w, int h) {
        return new TileBounds.Rect(x, y, w, h);
    }

    @Test
    void noTilesAtAllCountsAsUnusable() {
        assertFalse(HoverGate.anyUsable(List.of(), SCREEN_W, SCREEN_H));
    }

    @Test
    void zeroSizedTilesCountAsUnusable() {
        assertFalse(HoverGate.anyUsable(List.of(rect(10, 10, 0, 28)), SCREEN_W, SCREEN_H));
        assertFalse(HoverGate.anyUsable(List.of(rect(10, 10, 128, 0)), SCREEN_W, SCREEN_H));
    }

    @Test
    void tilesEntirelyOffScreenCountAsUnusable() {
        assertFalse(HoverGate.anyUsable(List.of(
                rect(-300, 10, 128, 28),
                rect(600, 10, 128, 28),
                rect(10, 900, 128, 28)), SCREEN_W, SCREEN_H));
    }

    @Test
    void anOrdinaryTileIsUsable() {
        assertTrue(HoverGate.anyUsable(List.of(rect(12, 10, 128, 28)), SCREEN_W, SCREEN_H));
    }

    @Test
    void aPartlyVisibleTileStillCounts() {
        // Tiles slide in from off-screen, so a partly visible one is normal, not broken.
        assertTrue(HoverGate.anyUsable(List.of(rect(-20, 10, 128, 28)), SCREEN_W, SCREEN_H));
    }

    @Test
    void oneGoodTileAmongBadOnesIsEnough() {
        assertTrue(HoverGate.anyUsable(List.of(
                rect(600, 10, 128, 28),
                rect(12, 40, 128, 28)), SCREEN_W, SCREEN_H));
    }

    @Test
    void hoveringIsTrueOnlyInsideATile() {
        List<TileBounds.Rect> tiles = List.of(rect(12, 10, 128, 28));

        assertTrue(HoverGate.hovering(tiles, 20, 20));
        assertFalse(HoverGate.hovering(tiles, 200, 20));
        assertFalse(HoverGate.hovering(tiles, 20, 200));
    }

    @Test
    void tileEdgesBehaveLikeOtherWidgets() {
        List<TileBounds.Rect> tiles = List.of(rect(10, 10, 10, 10));

        assertTrue(HoverGate.hovering(tiles, 10, 10), "top left corner is inside");
        assertFalse(HoverGate.hovering(tiles, 20, 15), "right edge is exclusive");
        assertFalse(HoverGate.hovering(tiles, 15, 20), "bottom edge is exclusive");
    }
}
