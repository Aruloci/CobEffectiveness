package com.cobeffectiveness.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {

    private static final String FILE = "cobeffectiveness.json";

    @Test
    void writesDefaultsWhenThereIsNoFileYet(@TempDir Path dir) {
        ConfigStore.init(dir);

        assertTrue(Files.exists(dir.resolve(FILE)));
        assertEquals(Anchor.TOP_RIGHT, ConfigStore.get().typesPanel.anchor);
    }

    @Test
    void aSavedPositionSurvivesAReload(@TempDir Path dir) {
        ConfigStore.init(dir);

        CobConfig edited = ConfigStore.get().copy();
        edited.typesPanel.anchor = Anchor.BOTTOM_LEFT;
        edited.typesPanel.offsetX = 137;
        edited.typesPanel.offsetY = 42;
        edited.movesPanel.enabled = false;
        edited.hoverOnly = true;
        edited.setShown(Multiplier.X0, true);
        ConfigStore.save(edited);

        // Re-read from disk the way the next launch would.
        ConfigStore.init(dir);
        CobConfig loaded = ConfigStore.get();

        assertEquals(Anchor.BOTTOM_LEFT, loaded.typesPanel.anchor);
        assertEquals(137, loaded.typesPanel.offsetX);
        assertEquals(42, loaded.typesPanel.offsetY);
        assertFalse(loaded.movesPanel.enabled, "each panel round-trips independently");
        assertTrue(loaded.namePanel.enabled);
        assertTrue(loaded.hoverOnly);
        assertTrue(loaded.isShown(Multiplier.X0));
    }

    @Test
    void savePublishesANewInstanceSoTheHudRebuilds(@TempDir Path dir) {
        ConfigStore.init(dir);
        CobConfig before = ConfigStore.get();

        CobConfig edited = before.copy();
        edited.typesPanel.offsetX = 99;
        ConfigStore.save(edited);

        // HudState spots a config change by identity, so save must not mutate in place.
        assertNotSame(before, ConfigStore.get());
        assertEquals(99, ConfigStore.get().typesPanel.offsetX);
    }

    @Test
    void aCorruptFileFallsBackToDefaultsInsteadOfFailingToLoad(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(FILE), "{ this is not json", StandardCharsets.UTF_8);

        ConfigStore.init(dir);

        assertEquals(Anchor.TOP_RIGHT, ConfigStore.get().typesPanel.anchor);
        assertEquals(12, ConfigStore.get().typesPanel.offsetX);
    }

    @Test
    void unknownRowNamesInTheFileAreDiscardedOnLoad(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(FILE),
                "{\"typesPanel\":{\"anchor\":\"TOP_LEFT\",\"offsetX\":5},\"shown\":[\"X4\",\"NOT_A_ROW\"]}",
                StandardCharsets.UTF_8);

        ConfigStore.init(dir);

        assertEquals(Anchor.TOP_LEFT, ConfigStore.get().typesPanel.anchor);
        assertEquals(java.util.List.of(Multiplier.X4), ConfigStore.get().shownInOrder());
    }

    @Test
    void anAttachmentSurvivesAReload(@TempDir Path dir) {
        ConfigStore.init(dir);

        CobConfig edited = ConfigStore.get().copy();
        edited.movesPanel.attach(PanelId.TYPES, PanelPlacement.EdgeLink.NEAR_TO_NEAR, PanelPlacement.EdgeLink.BEFORE);
        ConfigStore.save(edited);

        ConfigStore.init(dir);
        PanelPlacement loaded = ConfigStore.get().movesPanel;

        assertEquals(PanelId.TYPES, loaded.attachXTo);
        assertEquals(PanelId.TYPES, loaded.attachYTo);
        assertEquals(PanelPlacement.EdgeLink.NEAR_TO_NEAR, loaded.attachX);
        assertEquals(PanelPlacement.EdgeLink.BEFORE, loaded.attachY);
    }

    @Test
    void theDefaultStackIsRecordedAsAnAttachmentNotJustNumbers(@TempDir Path dir) {
        // Otherwise it would come apart the moment a real battle resized the name panel.
        ConfigStore.init(dir);

        assertEquals(PanelId.NAME, ConfigStore.get().typesPanel.attachYTo);
    }
}
