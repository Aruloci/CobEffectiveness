package com.cobeffectiveness.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snapping a dragged panel to its nearest corner.
 *
 * <p>Offsets are stored in scaled pixels from the anchored corner, so they survive a GUI scale change
 * only while they stay small. Reported symptom: a panel dragged away from its anchor moved when the
 * GUI scale changed, though the clamp kept it on screen.
 */
class AnchorSnapTest {

    private static final int WIDE = 960;
    private static final int TALL = 540;
    private static final int PANEL_W = 180;
    private static final int PANEL_H = 60;

    /** The same resolution HudLayout performs, so these tests measure the real behaviour. */
    private static int resolveX(Anchor anchor, int offsetX, int panelW, int screenW) {
        int x = anchor.isRight() ? screenW - offsetX - panelW : offsetX;
        return Math.max(0, Math.min(x, Math.max(0, screenW - panelW)));
    }

    @Test
    void eachQuadrantSnapsToItsOwnCorner() {
        assertEquals(Anchor.TOP_LEFT, Anchor.nearest(100, 80, WIDE, TALL));
        assertEquals(Anchor.TOP_RIGHT, Anchor.nearest(860, 80, WIDE, TALL));
        assertEquals(Anchor.BOTTOM_LEFT, Anchor.nearest(100, 460, WIDE, TALL));
        assertEquals(Anchor.BOTTOM_RIGHT, Anchor.nearest(860, 460, WIDE, TALL));
    }

    @Test
    void offsetsAreMeasuredFromTheAnchoredEdges() {
        int left = WIDE - PANEL_W - 12;
        int top = TALL - PANEL_H - 20;

        assertEquals(12, Anchor.BOTTOM_RIGHT.offsetXFor(left, PANEL_W, WIDE));
        assertEquals(20, Anchor.BOTTOM_RIGHT.offsetYFor(top, PANEL_H, TALL));
        assertEquals(left, Anchor.TOP_LEFT.offsetXFor(left, PANEL_W, WIDE));
        assertEquals(top, Anchor.TOP_LEFT.offsetYFor(top, PANEL_H, TALL));
    }

    @Test
    void snappingAlwaysKeepsTheSmallerOfTheTwoPossibleOffsets() {
        for (int left = 0; left <= WIDE - PANEL_W; left += 60) {
            Anchor anchor = Anchor.nearest(left + PANEL_W / 2, 40, WIDE, TALL);
            int offset = anchor.offsetXFor(left, PANEL_W, WIDE);

            int finalLeft = left;
            assertTrue(offset <= WIDE / 2,
                    () -> "left=" + finalLeft + " stored an offset of " + anchor.offsetXFor(finalLeft, PANEL_W, WIDE));
        }
    }

    @Test
    void aCornerPlacedPanelHoldsItsPositionWhenTheGuiScaleChanges() {
        // Dropped 12px from the right edge at one scale.
        int left = WIDE - PANEL_W - 12;
        Anchor anchor = Anchor.nearest(left + PANEL_W / 2, 30, WIDE, TALL);
        int offsetX = anchor.offsetXFor(left, PANEL_W, WIDE);

        // The same config resolved on a half-width screen: still 12px from the right edge.
        int narrow = WIDE / 2;
        assertEquals(narrow - PANEL_W - 12, resolveX(anchor, offsetX, PANEL_W, narrow));
    }

    @Test
    void withoutSnappingTheSamePanelWouldHaveJumpedToTheOppositeEdge() {
        // The reported bug: dragged left of centre while still anchored TOP_RIGHT, the stored offset
        // is wider than a smaller screen, so the clamp pins it to x=0.
        int left = 300;
        int strandedOffset = Anchor.TOP_RIGHT.offsetXFor(left, PANEL_W, WIDE);

        int narrow = WIDE / 2;
        assertEquals(0, resolveX(Anchor.TOP_RIGHT, strandedOffset, PANEL_W, narrow));

        // Snapping picks the near edge instead, so the offset stays usable.
        Anchor snapped = Anchor.nearest(left + PANEL_W / 2, 30, WIDE, TALL);
        assertEquals(Anchor.TOP_LEFT, snapped);
        assertNotEquals(0, resolveX(snapped, snapped.offsetXFor(left, PANEL_W, WIDE), PANEL_W, narrow));
    }

    @Test
    void aPanelParkedInTheMiddleStillMovesAndThatIsKnown() {
        // Snapping bounds the offset, it cannot make a centre-parked panel scale invariant: half the
        // screen is still half the screen. Fractional offsets would fix it, at the cost of the X/Y
        // boxes becoming percentages. Documented rather than fixed.
        int left = WIDE / 2 - PANEL_W / 2;
        Anchor anchor = Anchor.nearest(left + PANEL_W / 2, 30, WIDE, TALL);
        int offsetX = anchor.offsetXFor(left, PANEL_W, WIDE);

        int narrow = WIDE / 2;
        assertNotEquals(narrow / 2 - PANEL_W / 2, resolveX(anchor, offsetX, PANEL_W, narrow),
                "if this ever passes, someone made offsets scale relative and the comment above is stale");
    }
}
