package com.cobeffectiveness.handler;

import com.cobeffectiveness.event.events.ReloadEvent;
import com.cobeffectiveness.hook.ChartLoader;
import net.neoforged.bus.api.SubscribeEvent;

public final class ReloadHandler {
    private boolean isRegistered = false;

    @SubscribeEvent
    public void onRegisterReloadListeners(ReloadEvent event) {
        if (isRegistered) return;
        ChartLoader.registerReload(event.getResourceManager());
        isRegistered = true;
    }
}