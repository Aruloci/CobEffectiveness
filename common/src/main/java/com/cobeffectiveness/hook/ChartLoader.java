package com.cobeffectiveness.hook;

import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.compat.Compat;
import com.cobeffectiveness.type.ChartParser;
import com.cobeffectiveness.ui.IconRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.SubscribeEvent;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ChartLoader {

    private ChartLoader() {}

    private static ResourceLocation chartPath() {
        return ResourceLocation.fromNamespaceAndPath(CobEffectivenessMod.NAMESPACE, Compat.getJsonPath());
    }

    @SubscribeEvent
    public static void registerReload(ReloadableResourceManager resourceManager) {
        resourceManager.registerReloadListener((synchronizer, manager, prepProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            IconRenderer.onResourcesReloaded(); // reload sheet
            CompletableFuture<Void> parseTask = CompletableFuture.runAsync(() -> {
                ResourceLocation path = chartPath();
                try {
                    Optional<Resource> res = manager.getResource(path);
                    if (res.isEmpty()) {
                        CobEffectivenessMod.logWarn("Effectiveness chart not found '{}'", path);
                        return;
                    }
                    try (InputStream in = res.get().open()) {
                        ChartParser.applyFromStream(in);
                    }
                } catch (Exception ex) {
                    CobEffectivenessMod.logError("Failed to load effectiveness chart '{}'", path, ex);
                }
            }, backgroundExecutor);
            CobEffectivenessMod.logInfo("Successfully loaded effectiveness chart from '{}'", chartPath());
            return parseTask.thenCompose(synchronizer::wait);
        });
    }
}
