package com.cobeffectiveness.type;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Loads the charts that actually ship with the mod, so tests run against real data. */
public final class Charts {
    private Charts() {}

    public static ChartJson load(String name) throws Exception {
        String path = "/assets/cobeffectiveness/charts/" + name + ".json";
        try (InputStream in = Charts.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing bundled chart: " + path);
            return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), ChartJson.class);
        }
    }
}
