package com.cobeffectiveness.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobConfigTest {

    @Test
    void defaultsMatchTheBehaviourBeforeTheSettingExisted() {
        CobConfig config = new CobConfig();

        assertEquals(List.of(Multiplier.X4, Multiplier.X2), config.shownInOrder());
        assertEquals(Anchor.TOP_RIGHT, config.namePanel.anchor, "the name sits under the tiles");
        assertEquals(Anchor.TOP_RIGHT, config.typesPanel.anchor, "effectiveness stacks under it");
        // Moves used to be anchored bottom-right on the theory that it sat above the move buttons.
        // Those are bottom-*left* (Cobblemon puts them at x 20..217); what actually shares the
        // bottom-right corner is the battle log, which the panel was drawn straight through. It
        // rides the stack now, and the anchor is only the fallback for when effectiveness is off.
        assertEquals(Anchor.TOP_RIGHT, config.movesPanel.anchor, "falls back into the effectiveness slot");
        assertEquals(PanelId.TYPES, config.movesPanel.attachYTo, "moves rides under effectiveness");
        assertEquals(PanelPlacement.EdgeLink.AFTER, config.movesPanel.attachY);
        assertTrue(config.namePanel.enabled);
        assertTrue(config.typesPanel.enabled);
        assertTrue(config.movesPanel.enabled);
        assertFalse(config.hoverOnly);
    }

    @Test
    void rowsAreOrderedStrongestFirst() {
        CobConfig config = new CobConfig();
        config.setShown(Multiplier.X0, true);
        config.setShown(Multiplier.X_HALF, true);

        assertEquals(
                List.of(Multiplier.X4, Multiplier.X2, Multiplier.X_HALF, Multiplier.X0),
                config.shownInOrder());
    }

    @Test
    void togglingOffRemovesTheRow() {
        CobConfig config = new CobConfig();
        config.setShown(Multiplier.X4, false);

        assertFalse(config.isShown(Multiplier.X4));
        assertEquals(List.of(Multiplier.X2), config.shownInOrder());
    }

    @Test
    void everyRowCanBeSwitchedOff() {
        CobConfig config = new CobConfig();
        for (Multiplier multiplier : Multiplier.values()) {
            config.setShown(multiplier, false);
        }
        assertTrue(config.shownInOrder().isEmpty());
    }

    @Test
    void sanitizeDropsUnknownEntriesAndDuplicates() {
        // Gson turns an unrecognised enum name in a hand-edited file into a null.
        CobConfig config = new CobConfig();
        config.shown = new ArrayList<>(Arrays.asList(Multiplier.X2, null, Multiplier.X2, Multiplier.X4));

        config.sanitized();

        assertEquals(List.of(Multiplier.X4, Multiplier.X2), config.shownInOrder());
    }

    @Test
    void sanitizeRepairsMissingFields() {
        CobConfig config = new CobConfig();
        config.typesPanel = null;
        config.shown = null;

        config.sanitized();

        assertNotNull(config.typesPanel);
        assertNotNull(config.typesPanel.anchor);
        assertEquals(List.of(Multiplier.X4, Multiplier.X2), config.shownInOrder());
    }

    @Test
    void copyDoesNotShareItsRowList() {
        CobConfig original = new CobConfig();
        CobConfig copy = original.copy();

        copy.setShown(Multiplier.X0, true);
        copy.typesPanel.anchor = Anchor.BOTTOM_LEFT;
        copy.namePanel.enabled = false;

        assertFalse(original.isShown(Multiplier.X0), "editing a copy must not touch the live config");
        assertEquals(Anchor.TOP_RIGHT, original.typesPanel.anchor,
                "the panel placements must be deep copied too");
        assertTrue(original.namePanel.enabled);
    }

    @Test
    void everyAnchorIsReachableByCycling() {
        Anchor anchor = Anchor.TOP_LEFT;
        for (int i = 0; i < Anchor.values().length; i++) {
            anchor = anchor.next();
        }
        assertEquals(Anchor.TOP_LEFT, anchor, "cycling through them all returns to the start");
    }

    @Test
    void resetRestoresEverySetting() {
        // Save-on-close leaves no Cancel, so this is the only way back from a bad edit.
        CobConfig config = new CobConfig();
        config.typesPanel.anchor = Anchor.BOTTOM_LEFT;
        config.typesPanel.offsetX = 300;
        config.movesPanel.enabled = false;
        config.namePanel.enabled = false;
        config.hoverOnly = true;
        config.setShown(Multiplier.X4, false);
        config.setShown(Multiplier.X0, true);

        config.resetToDefaults();

        CobConfig fresh = new CobConfig();
        assertEquals(fresh.typesPanel.anchor, config.typesPanel.anchor);
        assertEquals(fresh.typesPanel.offsetX, config.typesPanel.offsetX);
        assertEquals(fresh.movesPanel.anchor, config.movesPanel.anchor);
        assertTrue(config.movesPanel.enabled);
        assertTrue(config.namePanel.enabled);
        assertFalse(config.hoverOnly);
        assertEquals(fresh.shownInOrder(), config.shownInOrder());
    }

    @Test
    void resetDoesNotShareItsRowListWithTheNextConfig() {
        CobConfig config = new CobConfig();
        config.resetToDefaults();
        config.setShown(Multiplier.X0, true);

        assertFalse(new CobConfig().isShown(Multiplier.X0), "reset must deep copy the defaults");
    }
}
