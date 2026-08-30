package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement;
import com.cobeffectiveness.config.PanelPlacement.EdgeLink;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolving panel positions, including panels that ride on another.
 *
 * <p>The reported bug: panels lined up in the drag screen came apart in a real battle, because an
 * anchor plus an offset only preserves the anchored edge and every panel changes size with the
 * matchup. An attachment is resolved against the base's real size, so it holds.
 */
class PanelResolverTest {

    private static final int SCREEN_W = 480;
    private static final int SCREEN_H = 270;
    private static final int GAP = PanelResolver.ATTACH_GAP;

    private static PanelPlacement free(Anchor anchor, int offsetX, int offsetY) {
        return new PanelPlacement(anchor, offsetX, offsetY);
    }

    private static PanelPlacement riding(PanelId base, EdgeLink x, EdgeLink y) {
        PanelPlacement placement = new PanelPlacement(Anchor.TOP_RIGHT, 12, 100);
        placement.attach(base, x, y);
        return placement;
    }

    private record Setup(Map<PanelId, PanelResolver.Size> sizes,
                         Map<PanelId, PanelPlacement> placements) {}

    private static Setup setup() {
        return new Setup(new EnumMap<>(PanelId.class), new EnumMap<>(PanelId.class));
    }

    private static void add(Setup setup, PanelId id, int w, int h, PanelPlacement placement) {
        setup.sizes().put(id, new PanelResolver.Size(w, h));
        setup.placements().put(id, placement);
    }

    private static Map<PanelId, PanelResolver.Position> resolve(Setup setup) {
        return PanelResolver.resolve(setup.sizes(), setup.placements(), SCREEN_W, SCREEN_H);
    }

    /**
     * Cobblemon's collapsed battle log occupies {@code 30 + 55} pixels up from the bottom of the
     * screen, at the same right inset the panels use.
     */
    private static final int LOG_BAND = 85;

    private static Map<PanelId, PanelResolver.Position> resolve(Setup setup, int width, int height) {
        return PanelResolver.resolve(setup.sizes(), setup.placements(), width, height);
    }

    /** The three panels at a size a real singles matchup produces, measured from a running client. */
    private static Setup shippedDefaults(int typesHeight) {
        Setup setup = setup();
        add(setup, PanelId.NAME, 78, 23, PanelPlacement.name());
        add(setup, PanelId.TYPES, 66, typesHeight, PanelPlacement.types());
        add(setup, PanelId.MOVES, 100, 72, PanelPlacement.moves());
        return setup;
    }

    /**
     * The shipped arrangement must never overlap itself, whatever the matchup or the row count does
     * to the panels' heights.
     *
     * <p>The moves panel used to float against the bottom edge, which drew it straight through
     * Cobblemon's battle log. Raising its offset past the log only moved the collision onto the
     * effectiveness panel, because the right column and the log together want more height than a
     * 270 pixel screen has. Riding the stack is what removes both at once, so what is pinned here is
     * the relationship, not the number that happened to work.
     */
    @Test
    void theShippedDefaultsStackWithoutOverlapping() {
        for (int typesHeight : new int[] {39, 84}) {
            for (int height : new int[] {270, 360, 540}) {
                Map<PanelId, PanelResolver.Position> resolved = resolve(shippedDefaults(typesHeight), 640, height);

                PanelResolver.Position name = resolved.get(PanelId.NAME);
                PanelResolver.Position types = resolved.get(PanelId.TYPES);
                PanelResolver.Position moves = resolved.get(PanelId.MOVES);

                assertTrue(name.y() + 23 <= types.y(),
                        "effectiveness panel must clear the name panel at " + height);
                assertTrue(types.y() + typesHeight <= moves.y(),
                        "moves panel must clear the effectiveness panel at " + height);

                // Right edges flush, which is what FAR_TO_FAR records.
                assertEquals(name.x() + 78, types.x() + 66, "right edges must stay flush");
                assertEquals(types.x() + 66, moves.x() + 100, "right edges must stay flush");
            }
        }
    }

    /**
     * On a screen with room, the stack also clears Cobblemon's battle log, which is the collision
     * that made the old bottom-anchored default unusable. A 270 pixel screen - 1080p at auto GUI
     * scale - is genuinely oversubscribed and is left to dragging, so it is not asserted here.
     */
    @Test
    void theShippedDefaultsClearCobblemonsBattleLog() {
        for (int height : new int[] {360, 540}) {
            PanelResolver.Position moves = resolve(shippedDefaults(39), 640, height).get(PanelId.MOVES);

            assertTrue(moves.y() + 72 <= height - LOG_BAND,
                    "moves panel must sit above the battle log at " + height);
        }
    }

    @Test
    void aFreePanelResolvesFromItsAnchor() {
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20, free(Anchor.TOP_RIGHT, 12, 56));

        PanelResolver.Position position = resolve(setup).get(PanelId.NAME);

        assertEquals(SCREEN_W - 12 - 100, position.x());
        assertEquals(56, position.y());
    }

    @Test
    void anAttachedPanelSitsUnderItsBaseWithRightEdgesFlush() {
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20, free(Anchor.TOP_RIGHT, 12, 56));
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        Map<PanelId, PanelResolver.Position> resolved = resolve(setup);
        PanelResolver.Position base = resolved.get(PanelId.NAME);
        PanelResolver.Position rider = resolved.get(PanelId.TYPES);

        assertEquals(base.y() + 20 + GAP, rider.y());
        assertEquals(base.x() + 100, rider.x() + 140, "right edges flush");
    }

    @Test
    void leftAlignmentLinesUpTheOtherEdge() {
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20, free(Anchor.TOP_LEFT, 20, 30));
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.NEAR_TO_NEAR, EdgeLink.AFTER));

        Map<PanelId, PanelResolver.Position> resolved = resolve(setup);

        assertEquals(resolved.get(PanelId.NAME).x(), resolved.get(PanelId.TYPES).x());
    }

    @Test
    void attachingAboveStacksTheOtherWay() {
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20, free(Anchor.TOP_RIGHT, 12, 120));
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.BEFORE));

        Map<PanelId, PanelResolver.Position> resolved = resolve(setup);

        assertEquals(resolved.get(PanelId.NAME).y() - 60 - GAP, resolved.get(PanelId.TYPES).y());
    }

    @Test
    void theRiderFollowsWhenTheBaseChangesSize() {
        // This is the actual regression: in a real battle the base is a different size than it was
        // in the drag screen, and the rider has to move with it.
        Setup narrow = setup();
        add(narrow, PanelId.NAME, 100, 20, free(Anchor.TOP_RIGHT, 12, 56));
        add(narrow, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        Setup taller = setup();
        add(taller, PanelId.NAME, 100, 44, free(Anchor.TOP_RIGHT, 12, 56));
        add(taller, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        int before = resolve(narrow).get(PanelId.TYPES).y();
        int after = resolve(taller).get(PanelId.TYPES).y();

        assertEquals(before + 24, after, "a taller base pushes its rider down by exactly that much");
    }

    @Test
    void aPanelAttachedToOneThatIsNotDrawnFallsBackToItsOwnAnchor() {
        // The name panel is switched off, so the effectiveness panel is on its own again.
        Setup setup = setup();
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        PanelResolver.Position position = resolve(setup).get(PanelId.TYPES);

        assertNotNull(position);
        assertEquals(SCREEN_W - 12 - 140, position.x(), "back to its anchor and offset");
        assertEquals(100, position.y());
    }

    @Test
    void aCycleDoesNotHangAndBothPanelsStillGetPlaced() {
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20,
                riding(PanelId.TYPES, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        Map<PanelId, PanelResolver.Position> resolved = resolve(setup);

        assertEquals(2, resolved.size());
        assertNotNull(resolved.get(PanelId.NAME));
        assertNotNull(resolved.get(PanelId.TYPES));
    }

    @Test
    void aPanelAttachedToItselfFallsBackRatherThanLooping() {
        Setup setup = setup();
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.TYPES, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        assertNotNull(resolve(setup).get(PanelId.TYPES));
    }

    @Test
    void aRiderPushedPastTheEdgeIsClampedOnScreen() {
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 40, free(Anchor.BOTTOM_RIGHT, 12, 0));
        add(setup, PanelId.TYPES, 140, 60,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        PanelResolver.Position rider = resolve(setup).get(PanelId.TYPES);

        assertTrue(rider.y() >= 0);
        assertTrue(rider.y() + 60 <= SCREEN_H, "a rider must not be pushed off the bottom");
    }

    @Test
    void theTwoAxesResolveAgainstDifferentPanels() {
        // A drop can line up against two neighbours at once, so each axis carries its own base.
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20, free(Anchor.TOP_LEFT, 40, 30));
        add(setup, PanelId.MOVES, 80, 50, free(Anchor.TOP_LEFT, 200, 120));

        PanelPlacement split = new PanelPlacement(Anchor.TOP_LEFT, 0, 0);
        split.attachX(PanelId.MOVES, EdgeLink.NEAR_TO_NEAR);
        split.attachY(PanelId.NAME, EdgeLink.AFTER);
        add(setup, PanelId.TYPES, 60, 25, split);

        PanelResolver.Position position = resolve(setup).get(PanelId.TYPES);

        assertEquals(200, position.x(), "left edge from the moves panel");
        assertEquals(30 + 20 + GAP, position.y(), "top edge from the name panel");
    }

    @Test
    void anAxisWhoseBaseIsNotDrawnFallsBackOnThatAxisAlone() {
        Setup setup = setup();
        add(setup, PanelId.MOVES, 80, 50, free(Anchor.TOP_LEFT, 200, 120));

        PanelPlacement split = new PanelPlacement(Anchor.TOP_LEFT, 15, 45);
        split.attachX(PanelId.MOVES, EdgeLink.NEAR_TO_NEAR);
        split.attachY(PanelId.NAME, EdgeLink.AFTER);   // the name panel is switched off
        add(setup, PanelId.TYPES, 60, 25, split);

        PanelResolver.Position position = resolve(setup).get(PanelId.TYPES);

        assertEquals(200, position.x(), "still riding horizontally");
        assertEquals(45, position.y(), "but back on its own anchor vertically");
    }

    @Test
    void chainsResolveInOrder() {
        // moves rides on types, which rides on opponent.
        Setup setup = setup();
        add(setup, PanelId.NAME, 100, 20, free(Anchor.TOP_RIGHT, 12, 20));
        add(setup, PanelId.TYPES, 100, 30,
                riding(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));
        add(setup, PanelId.MOVES, 100, 40,
                riding(PanelId.TYPES, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER));

        Map<PanelId, PanelResolver.Position> resolved = resolve(setup);

        assertEquals(20, resolved.get(PanelId.NAME).y());
        assertEquals(20 + 20 + GAP, resolved.get(PanelId.TYPES).y());
        assertEquals(20 + 20 + GAP + 30 + GAP, resolved.get(PanelId.MOVES).y());
    }
}
