package com.cobeffectiveness.config;

import com.cobeffectiveness.CobEffectivenessMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves {@link CobConfig}.
 *
 * <p>The active config is published through a single volatile write, the same way the type chart is,
 * so a save from the config screen cannot be observed half-applied by the render thread.
 *
 * <p>The config directory differs per loader, so the platform entrypoint supplies it rather than
 * this class reaching for a loader API.
 */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "cobeffectiveness.json";

    private static volatile CobConfig active = new CobConfig();
    private static volatile Path file;

    private ConfigStore() {}

    public static CobConfig get() {
        return active;
    }

    /** Reads the config file if present, otherwise keeps the defaults and writes them out. */
    public static void init(Path configDir) {
        file = configDir.resolve(FILE_NAME);

        if (!Files.exists(file)) {
            CobEffectivenessMod.logInfo("No config found, writing defaults to '{}'", file);
            save(new CobConfig());
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            CobConfig loaded = GSON.fromJson(reader, CobConfig.class);
            active = loaded == null ? new CobConfig() : loaded.sanitized();
            CobEffectivenessMod.logInfo("Loaded config from '{}'", file);
        } catch (Exception ex) {
            // A broken file must not stop the mod loading; defaults are always usable.
            CobEffectivenessMod.logError("Could not read config '{}', using defaults", file, ex);
            active = new CobConfig();
        }
    }

    public static void save(CobConfig config) {
        active = config.sanitized();

        Path target = file;
        if (target == null) return;

        try {
            Files.createDirectories(target.getParent());
            try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                GSON.toJson(active, writer);
            }
        } catch (IOException ex) {
            CobEffectivenessMod.logError("Could not write config '{}'", target, ex);
        }
    }
}
