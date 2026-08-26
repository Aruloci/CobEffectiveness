package com.cobeffectiveness.type;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IconSheetTest {

    private static ChartJson.Tiles tiles(int cols, int rows) {
        ChartJson.Tiles tiles = new ChartJson.Tiles();
        tiles.cols = cols;
        tiles.rows = rows;
        tiles.tile_w = 12;
        tiles.tile_h = 12;
        return tiles;
    }

    @Test
    void placeholderSlotsAreSkippedButStillTakeUpTheirTile() {
        IconSheet.Parsed parsed =
                IconSheet.from(tiles(4, 1), List.of("Normal", ChartJson.PLACEHOLDER, "Fire"));

        assertTrue(parsed.warnings().isEmpty(), "a known placeholder is not a parse problem");
        assertEquals(new IconSheet.Pos(0, 0), parsed.sheet().pos(Type.NORMAL));
        // Fire sits in slot 2, not slot 1: the placeholder still consumes its position.
        assertEquals(new IconSheet.Pos(24, 0), parsed.sheet().pos(Type.FIRE));
    }

    @Test
    void wrapsOntoLaterRows() {
        IconSheet.Parsed parsed = IconSheet.from(tiles(2, 2), List.of("Normal", "Fire", "Water"));

        assertEquals(new IconSheet.Pos(0, 12), parsed.sheet().pos(Type.WATER));
        assertEquals(24, parsed.sheet().sheetW());
        assertEquals(24, parsed.sheet().sheetH());
    }

    @Test
    void unknownNamesAreReportedAndLeftUnmapped() {
        IconSheet.Parsed parsed = IconSheet.from(tiles(4, 1), List.of("Normal", "Banana"));

        assertEquals(1, parsed.warnings().size());
        assertFalse(parsed.sheet().has(Type.DRAGON));
        assertNull(parsed.sheet().pos(Type.DRAGON));
    }

    @Test
    void nullEntriesDoNotAbortTheRestOfTheSheet() {
        // A hand edited pack can contain a null; it used to throw and leave the sheet half filled.
        IconSheet.Parsed parsed = IconSheet.from(tiles(4, 1), Arrays.asList("Normal", null, "Fire"));

        assertTrue(parsed.sheet().has(Type.NORMAL));
        assertTrue(parsed.sheet().has(Type.FIRE));
    }

    @Test
    void bundledChartsMapEveryIconTheyDeclare() throws Exception {
        for (String name : List.of("vanilla", "geb_default")) {
            ChartJson json = Charts.load(name);
            IconSheet.Parsed parsed = IconSheet.from(json.tiles, json.order);
            assertTrue(parsed.warnings().isEmpty(), () -> name + " sheet warnings: " + parsed.warnings());
        }
    }
}
