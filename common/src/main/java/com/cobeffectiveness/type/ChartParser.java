package com.cobeffectiveness.type;

import com.cobeffectiveness.CobEffectivenessMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ChartParser {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ChartParser() {}

    public static void applyFromStream(InputStream in) {
        ChartJson json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), ChartJson.class);
        if (json == null) {
            CobEffectivenessMod.logWarn("Chart JSON is empty");
            return;
        }
        apply(json);
    }

    public static void apply(ChartJson json) {
        EffectivenessChart.Parsed chart = EffectivenessChart.from(json);
        chart.warnings().forEach(CobEffectivenessMod::logWarn);
        EffectivenessChart.publish(chart.chart());

        IconSheet.Parsed sheet = IconSheet.from(json.tiles, json.order);
        sheet.warnings().forEach(CobEffectivenessMod::logWarn);
        IconSheet.publish(sheet.sheet());
    }
}
