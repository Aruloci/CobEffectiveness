package com.cobeffectiveness.handler;

import com.cobeffectiveness.event.events.ReloadEvent;
import com.cobeffectiveness.resource.ChartLoader;
import net.neoforged.bus.api.SubscribeEvent;

public final class ReloadHandler {
    // The mixin now targets only the first registerReloadListener call in the Minecraft
    // constructor, so this normally fires once. The guard stays as cheap insurance: mod
    // construction ordering differs between Fabric and NeoForge, and registering the chart
    // listener twice would parse the chart twice on every reload.
    private boolean registered = false;

    @SubscribeEvent
    public void onRegisterReloadListeners(ReloadEvent event) {
        if (registered) return;
        ChartLoader.registerReload(event.getResourceManager());
        registered = true;
    }
}
