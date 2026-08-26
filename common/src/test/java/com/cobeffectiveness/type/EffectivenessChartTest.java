package com.cobeffectiveness.type;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectivenessChartTest {

    private static EffectivenessChart vanilla() throws Exception {
        EffectivenessChart.Parsed parsed = EffectivenessChart.from(Charts.load("vanilla"));
        assertTrue(parsed.warnings().isEmpty(), () -> "vanilla chart warnings: " + parsed.warnings());
        return parsed.chart();
    }

    @Test
    void multipliersCompoundAcrossBothTypes() throws Exception {
        Set<Type> charizard = EnumSet.of(Type.FIRE, Type.FLYING);
        EffectivenessChart chart = vanilla();

        assertEquals(4.0, chart.multiplier(Type.ROCK, charizard));
        assertEquals(2.0, chart.multiplier(Type.WATER, charizard));
        assertEquals(0.25, chart.multiplier(Type.GRASS, charizard));
    }

    @Test
    void immunityBeatsWeakness() throws Exception {
        // Ground is immune to Electric even though Water on the same Pokemon is weak to it.
        assertEquals(0.0, vanilla().multiplier(Type.ELECTRIC, EnumSet.of(Type.WATER, Type.GROUND)));
    }

    @Test
    void bucketsOnlyExactMultipliers() throws Exception {
        Map<Double, Set<Type>> buckets =
                vanilla().attackersByMultiplier(EnumSet.of(Type.FIRE, Type.FLYING), 4.0, 2.0);

        assertEquals(EnumSet.of(Type.ROCK), buckets.get(4.0));
        assertEquals(EnumSet.of(Type.WATER, Type.ELECTRIC), buckets.get(2.0));
        assertFalse(buckets.get(2.0).contains(Type.ROCK), "a x4 type must not also appear as x2");
    }

    @Test
    void bundledGebChartParsesWithoutWarnings() throws Exception {
        EffectivenessChart.Parsed parsed = EffectivenessChart.from(Charts.load("geb_default"));
        assertTrue(parsed.warnings().isEmpty(), () -> "geb chart warnings: " + parsed.warnings());
    }

    @Test
    void defaultMultiplierIsReadFromJson() {
        // Regression: the field was named default_ with no @SerializedName, so Gson never bound the
        // "default" key and a chart could not change the neutral multiplier.
        ChartJson json = new Gson().fromJson("{\"default\": 0.5, \"rules\": []}", ChartJson.class);
        assertEquals(0.5, json.defaultMultiplier);

        EffectivenessChart chart = EffectivenessChart.from(json).chart();
        assertEquals(0.5, chart.multiplier(Type.FIRE, EnumSet.of(Type.WATER)));
    }

    @Test
    void unknownTypesInRulesAreReportedNotThrown() {
        ChartJson json = new Gson().fromJson(
                "{\"rules\": [{\"attack\": \"Banana\", \"defend\": \"Fire\", \"mult\": 2.0}]}",
                ChartJson.class);

        EffectivenessChart.Parsed parsed = EffectivenessChart.from(json);

        assertEquals(1, parsed.warnings().size());
        assertEquals(1.0, parsed.chart().multiplier(Type.FIRE, EnumSet.of(Type.FIRE)));
    }
}
