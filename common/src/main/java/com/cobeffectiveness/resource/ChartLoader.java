package com.cobeffectiveness.resource;

import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.compat.Compat;
import com.cobeffectiveness.type.ChartParser;
import com.cobeffectiveness.ui.IconRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ChartLoader {

    private ChartLoader() {}

    private static ResourceLocation chartPath() {
        return ResourceLocation.fromNamespaceAndPath(CobEffectivenessMod.NAMESPACE, Compat.chartPath());
    }

    public static void registerReload(ReloadableResourceManager resourceManager) {
        resourceManager.registerReloadListener((synchronizer, manager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            IconRenderer.onResourcesReloaded();

            CompletableFuture<Void> parseTask = CompletableFuture.runAsync(() -> {
                ResourceLocation path = chartPath();
                try {
                    Optional<Resource> resource = manager.getResource(path);
                    if (resource.isEmpty()) {
                        CobEffectivenessMod.logWarn("Effectiveness chart not found '{}'", path);
                        return;
                    }
                    try (InputStream in = resource.get().open()) {
                        ChartParser.applyFromStream(in);
                    }
                    // Logged here, not at registration time, so success means the chart really parsed.
                    CobEffectivenessMod.logInfo("Loaded effectiveness chart from '{}'", path);
                } catch (Exception ex) {
                    CobEffectivenessMod.logError("Failed to load effectiveness chart '{}'", path, ex);
                }
            }, backgroundExecutor);

            return parseTask.thenCompose(synchronizer::wait);
        });
    }
}
