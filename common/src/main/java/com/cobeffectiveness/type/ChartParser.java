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

        EffectivenessChart.configure(json, name -> {
            try {
                return Type.fromCobblemon(name);
            } catch (Exception e) {
                return null;
            }
        });

        if (json.tiles != null) {
            TypeIcon.configure(json.tiles.cols, json.tiles.rows, json.tiles.tile_w, json.tiles.tile_h, json.order);
        }
    }
}
