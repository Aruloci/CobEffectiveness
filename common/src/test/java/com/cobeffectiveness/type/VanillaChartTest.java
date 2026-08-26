package com.cobeffectiveness.type;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the whole 18x18 vanilla type chart against both bundled files.
 *
 * <p>Written after the shipped charts turned out to be missing every Normal attacker rule, so Ghost
 * never showed as immune to Normal and Normal read as neutral into Rock and Steel. Spot checks did
 * not catch it; only checking every cell does.
 */
class VanillaChartTest {

    /** Non-neutral cells only, "DEFENDER:multiplier"; everything unlisted is 1.0. */
    private static final Map<Type, String> CANONICAL = new HashMap<>();

    static {
        CANONICAL.put(Type.NORMAL, "ROCK:0.5 GHOST:0 STEEL:0.5");
        CANONICAL.put(Type.FIRE, "FIRE:0.5 WATER:0.5 GRASS:2 ICE:2 BUG:2 ROCK:0.5 DRAGON:0.5 STEEL:2");
        CANONICAL.put(Type.WATER, "FIRE:2 WATER:0.5 GRASS:0.5 GROUND:2 ROCK:2 DRAGON:0.5");
        CANONICAL.put(Type.ELECTRIC, "WATER:2 ELECTRIC:0.5 GRASS:0.5 GROUND:0 FLYING:2 DRAGON:0.5");
        CANONICAL.put(Type.GRASS, "FIRE:0.5 WATER:2 GRASS:0.5 POISON:0.5 GROUND:2 FLYING:0.5 BUG:0.5 ROCK:2 DRAGON:0.5 STEEL:0.5");
        CANONICAL.put(Type.ICE, "FIRE:0.5 WATER:0.5 GRASS:2 ICE:0.5 GROUND:2 FLYING:2 DRAGON:2 STEEL:0.5");
        CANONICAL.put(Type.FIGHTING, "NORMAL:2 ICE:2 POISON:0.5 FLYING:0.5 PSYCHIC:0.5 BUG:0.5 ROCK:2 GHOST:0 DARK:2 STEEL:2 FAIRY:0.5");
        CANONICAL.put(Type.POISON, "GRASS:2 POISON:0.5 GROUND:0.5 ROCK:0.5 GHOST:0.5 STEEL:0 FAIRY:2");
        CANONICAL.put(Type.GROUND, "FIRE:2 ELECTRIC:2 GRASS:0.5 POISON:2 FLYING:0 BUG:0.5 ROCK:2 STEEL:2");
        CANONICAL.put(Type.FLYING, "ELECTRIC:0.5 GRASS:2 FIGHTING:2 BUG:2 ROCK:0.5 STEEL:0.5");
        CANONICAL.put(Type.PSYCHIC, "FIGHTING:2 POISON:2 PSYCHIC:0.5 DARK:0 STEEL:0.5");
        CANONICAL.put(Type.BUG, "FIRE:0.5 GRASS:2 FIGHTING:0.5 POISON:0.5 FLYING:0.5 PSYCHIC:2 GHOST:0.5 DARK:2 STEEL:0.5 FAIRY:0.5");
        CANONICAL.put(Type.ROCK, "FIRE:2 ICE:2 FIGHTING:0.5 GROUND:0.5 FLYING:2 BUG:2 STEEL:0.5");
        CANONICAL.put(Type.GHOST, "NORMAL:0 PSYCHIC:2 GHOST:2 DARK:0.5");
        CANONICAL.put(Type.DRAGON, "DRAGON:2 STEEL:0.5 FAIRY:0");
        CANONICAL.put(Type.DARK, "FIGHTING:0.5 PSYCHIC:2 GHOST:2 DARK:0.5 FAIRY:0.5");
        CANONICAL.put(Type.STEEL, "FIRE:0.5 WATER:0.5 ELECTRIC:0.5 ICE:2 ROCK:2 STEEL:0.5 FAIRY:2");
        CANONICAL.put(Type.FAIRY, "FIRE:0.5 FIGHTING:2 POISON:0.5 DRAGON:2 DARK:2 STEEL:0.5");
    }

    /** The 18 types a vanilla chart must cover. */
    private static final List<Type> VANILLA_TYPES = List.copyOf(CANONICAL.keySet());

    private static Map<Type, Double> expectedRow(Type attacker) {
        Map<Type, Double> row = new HashMap<>();
        for (Type defender : VANILLA_TYPES) {
            row.put(defender, 1.0);
        }
        for (String cell : CANONICAL.get(attacker).split(" ")) {
            String[] parts = cell.split(":");
            row.put(Type.valueOf(parts[0]), Double.parseDouble(parts[1]));
        }
        return row;
    }

    private static void assertChartMatches(String chartName) throws Exception {
        EffectivenessChart.Parsed parsed = EffectivenessChart.from(Charts.load(chartName));
        assertTrue(parsed.warnings().isEmpty(), () -> chartName + " warnings: " + parsed.warnings());

        EffectivenessChart chart = parsed.chart();
        List<String> wrong = new ArrayList<>();

        for (Type attacker : VANILLA_TYPES) {
            Map<Type, Double> expected = expectedRow(attacker);
            for (Type defender : VANILLA_TYPES) {
                double actual = chart.multiplier(attacker, EnumSet.of(defender));
                if (Math.abs(actual - expected.get(defender)) > 1e-9) {
                    wrong.add(attacker + " -> " + defender + " is " + actual
                            + ", should be " + expected.get(defender));
                }
            }
        }

        assertEquals(List.of(), wrong, () -> chartName + " differs from the vanilla type chart");
    }

    @Test
    void bundledVanillaChartMatchesTheRealTypeChart() throws Exception {
        assertChartMatches("vanilla");
    }

    @Test
    void gebChartKeepsTheVanillaTypesCorrectToo() throws Exception {
        // Gravelmon adds types; the original 18 must still behave exactly as vanilla.
        assertChartMatches("geb_default");
    }

    @Test
    void ghostIsImmuneToNormal() throws Exception {
        // The specific cell that was missing from both shipped charts.
        EffectivenessChart chart = EffectivenessChart.from(Charts.load("vanilla")).chart();

        assertEquals(0.0, chart.multiplier(Type.NORMAL, EnumSet.of(Type.GHOST)));
        assertEquals(0.5, chart.multiplier(Type.NORMAL, EnumSet.of(Type.ROCK)));
        assertEquals(0.5, chart.multiplier(Type.NORMAL, EnumSet.of(Type.STEEL)));
    }
}
