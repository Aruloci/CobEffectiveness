package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement;
import com.cobeffectiveness.config.PanelPlacement.EdgeLink;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Edge snapping between the three panels while one is dragged, and what a drop records. */
class PanelSnapTest {

    private static final int THRESHOLD = 6;
    private static final int GAP = PanelResolver.ATTACH_GAP;
    private static final int SCREEN_W = 480;
    private static final int SCREEN_H = 270;

    private static PanelSnap.Box box(int x, int y, int w, int h) {
        return new PanelSnap.Box(x, y, w, h);
    }

    /** Names boxes in enum order; the ids only matter to the tests that check which one won. */
    private static Map<PanelId, PanelSnap.Box> bases(PanelSnap.Box... boxes) {
        Map<PanelId, PanelSnap.Box> map = new EnumMap<>(PanelId.class);
        PanelId[] ids = PanelId.values();
        for (int i = 0; i < boxes.length; i++) {
            map.put(ids[i], boxes[i]);
        }
        return map;
    }

    private static PanelSnap.Box snap(PanelSnap.Box moving, PanelSnap.Box... others) {
        return PanelSnap.snap(moving, bases(others), THRESHOLD);
    }

    @Test
    void leftEdgesLatchTogether() {
        PanelSnap.Box result = snap(box(104, 300, 60, 30), box(100, 200, 80, 40));

        assertEquals(100, result.x(), "4px away should latch");
        assertEquals(300, result.y(), "nothing near on this axis");
    }

    @Test
    void rightEdgesLatchTogether() {
        // Moving box is 60 wide; flush right against an 80-wide box at x=100 means x=120.
        PanelSnap.Box result = snap(box(117, 300, 60, 30), box(100, 200, 80, 40));

        assertEquals(120, result.x());
    }

    @Test
    void aPanelStacksDirectlyUnderneath() {
        // Other spans y 200..240, so stacked underneath is y=240 plus whatever gap an attachment
        // will leave.
        PanelSnap.Box result = snap(box(100, 243, 80, 30), box(100, 200, 80, 40));

        assertEquals(240 + GAP, result.y());
    }

    @Test
    void aPanelStacksDirectlyAbove() {
        // Moving box is 30 tall, so sitting on top of a box starting at y=200 means y=170.
        PanelSnap.Box result = snap(box(100, 174, 80, 30), box(100, 200, 80, 40));

        assertEquals(170 - GAP, result.y());
    }

    @Test
    void nothingHappensBeyondTheThreshold() {
        PanelSnap.Box moving = box(120, 300, 60, 30);
        PanelSnap.Box result = snap(moving, box(100, 200, 80, 40));

        assertEquals(moving.x(), result.x());
        assertEquals(moving.y(), result.y());
    }

    @Test
    void theNearestCandidateWins() {
        // x=103 is 3 from the left edge of one box and 5 from the left edge of another.
        PanelSnap.Box result = snap(box(103, 500, 40, 20), box(100, 200, 40, 20), box(108, 250, 40, 20));

        assertEquals(100, result.x());
    }

    @Test
    void theTwoAxesSnapIndependently() {
        // Close enough horizontally, far vertically: only x should move.
        PanelSnap.Box result = snap(box(102, 400, 60, 30), box(100, 200, 80, 40));

        assertEquals(100, result.x());
        assertEquals(400, result.y());
    }

    @Test
    void withNothingToSnapToThePositionIsUntouched() {
        PanelSnap.Box moving = box(37, 91, 60, 30);

        assertEquals(moving, snap(moving));
    }

    @Test
    void sizeIsNeverChangedBySnapping() {
        PanelSnap.Box result = snap(box(104, 243, 60, 30), box(100, 200, 80, 40));

        assertEquals(60, result.width());
        assertEquals(30, result.height());
    }

    @Test
    void anAxisWithNothingNearIsLeftUnattached() {
        PanelSnap.Attachment attachment =
                PanelSnap.attach(box(102, 400, 60, 30), bases(box(100, 200, 80, 40)), THRESHOLD);

        assertEquals(EdgeLink.NEAR_TO_NEAR, attachment.x());
        assertNull(attachment.y(), "far away vertically, so that axis stays on its anchor");
        assertNull(attachment.yBase());
    }

    /**
     * The whole drop, end to end: snap where the pointer let go, read the attachment back out of the
     * result, resolve it the way the HUD does. The panel must not move.
     *
     * <p>It used to. Snapping latched flush while the resolver added two pixels, so a panel shifted
     * the moment it was let go - lined up in the drag screen, slightly apart in a battle.
     */
    @Test
    void aDroppedPanelResolvesExactlyWhereItSnapped() {
        PanelSnap.Box base = box(300, 60, 80, 40);
        PanelSnap.Box dropped = snap(box(297, 104, 60, 30), base);

        PanelResolver.Position resolved = resolveRider(dropped, base, base);

        assertEquals(dropped.x(), resolved.x(), "it must not slide sideways when dropped");
        assertEquals(dropped.y(), resolved.y(), "nor open a gap it did not have while dragging");
    }

    /**
     * A panel snapped against the <em>left</em> edge of another, hanging off its bottom corner.
     *
     * <p>An attachment could once only say "below" or "above" aligned on a vertical edge, so a panel
     * resting sideways matched neither and was saved as a free panel - and in a battle the base came
     * out a different width, its left edge moved, and the gap the user had closed opened up.
     */
    @Test
    void aPanelDroppedBesideAnotherStaysAgainstItWhenTheBaseResizes() {
        PanelSnap.Box base = box(300, 84, 105, 57);
        PanelSnap.Box dropped = snap(box(213, 138, 90, 40), base);

        PanelSnap.Attachment attachment = PanelSnap.attach(dropped, bases(base), THRESHOLD);
        assertEquals(EdgeLink.BEFORE, attachment.x(), "resting against the base's left edge");
        assertEquals(EdgeLink.AFTER, attachment.y(), "and hanging below it");

        // Same panel in a real battle: longer move names, so it is 25px wider. It is pinned to the
        // right of the screen, so widening it pushes its left edge left.
        PanelSnap.Box wider = box(base.x() - 25, base.y(), base.width() + 25, base.height());
        PanelResolver.Position resolved = resolveRider(dropped, base, wider);

        assertEquals(wider.x() - dropped.width(), resolved.x(), "still flush against the left edge");
        assertEquals(wider.y() + wider.height(), resolved.y(), "and still hanging off the bottom");
    }

    /**
     * The reported bug, exactly as the saved config showed it. The effectiveness panel was tucked
     * against the left edge of the moves panel with their tops level. The moves panel sits flush
     * under the name panel, so that same top edge was also level with the name panel's bottom - and
     * the old drop took the first panel that matched <em>anything</em>, attached vertically to the
     * name panel, and threw the horizontal edge away. Only the top alignment survived.
     */
    @Test
    void aDropLinesUpWithTheRightPanelWhenTwoAreFlushAgainstEachOther() {
        // Both pinned to the same right edge at 405, but the name panel is much narrower - which is
        // why only the moves panel has an edge anywhere near the dropped panel horizontally.
        PanelSnap.Box name = box(340, 60, 65, 24);
        PanelSnap.Box moves = box(300, 84, 105, 57);   // directly under the name panel

        Map<PanelId, PanelSnap.Box> bases = new EnumMap<>(PanelId.class);
        bases.put(PanelId.NAME, name);
        bases.put(PanelId.MOVES, moves);

        // Tucked against the moves panel's left edge, tops level.
        PanelSnap.Box dropped = PanelSnap.snap(box(207, 86, 90, 40), bases, THRESHOLD);
        PanelSnap.Attachment attachment = PanelSnap.attach(dropped, bases, THRESHOLD);

        assertFalse(attachment.isEmpty());
        assertEquals(PanelId.MOVES, attachment.xBase(), "the edge it is tucked against");
        assertEquals(EdgeLink.BEFORE, attachment.x());
        assertEquals(PanelId.MOVES, attachment.yBase(),
                "the name panel's bottom is level too, but the moves panel is the one it sits against");
        assertEquals(EdgeLink.NEAR_TO_NEAR, attachment.y());
    }

    @Test
    void theTwoAxesMayRideOnDifferentPanels() {
        // Left edge against one panel, top edge against another that is nowhere near it horizontally.
        PanelSnap.Box name = box(300, 60, 105, 24);
        PanelSnap.Box moves = box(60, 200, 105, 57);

        Map<PanelId, PanelSnap.Box> bases = new EnumMap<>(PanelId.class);
        bases.put(PanelId.NAME, name);
        bases.put(PanelId.MOVES, moves);

        PanelSnap.Attachment attachment =
                PanelSnap.attach(box(60, 84, 90, 40), bases, THRESHOLD);

        assertEquals(PanelId.MOVES, attachment.xBase(), "left edges flush with the far panel");
        assertEquals(EdgeLink.NEAR_TO_NEAR, attachment.x());
        assertEquals(PanelId.NAME, attachment.yBase(), "but stacked under the near one");
        assertEquals(EdgeLink.AFTER, attachment.y());
    }

    /**
     * Reads a drop back as an attachment against {@code base}, then resolves it the way the HUD
     * does against {@code actual} - the same panel at whatever size a real battle gave it.
     */
    private static PanelResolver.Position resolveRider(PanelSnap.Box dropped, PanelSnap.Box base,
                                                       PanelSnap.Box actual) {
        PanelSnap.Attachment attachment = PanelSnap.attach(dropped, bases(base), THRESHOLD);
        assertFalse(attachment.isEmpty(), "the panel is resting against the base, so it should attach");

        Map<PanelId, PanelResolver.Size> sizes = new EnumMap<>(PanelId.class);
        sizes.put(PanelId.MOVES, new PanelResolver.Size(actual.width(), actual.height()));
        sizes.put(PanelId.TYPES, new PanelResolver.Size(dropped.width(), dropped.height()));

        // Pinned to the right of the screen, the way the moves panel sits, so a wider panel reaches
        // further left rather than further right.
        PanelPlacement basePlacement = new PanelPlacement(
                Anchor.TOP_RIGHT, SCREEN_W - (base.x() + base.width()), actual.y());
        PanelPlacement rider = new PanelPlacement(Anchor.TOP_RIGHT, 0, 0);
        rider.attachX(PanelId.MOVES, attachment.x());
        rider.attachY(PanelId.MOVES, attachment.y());

        Map<PanelId, PanelPlacement> placements = new EnumMap<>(PanelId.class);
        placements.put(PanelId.MOVES, basePlacement);
        placements.put(PanelId.TYPES, rider);

        return PanelResolver.resolve(sizes, placements, SCREEN_W, SCREEN_H).get(PanelId.TYPES);
    }
}
