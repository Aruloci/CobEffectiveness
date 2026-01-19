package com.cobeffectiveness.event.events;

import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.bus.api.Event;

public class ReloadEvent extends Event {
    private final ReloadableResourceManager resourceManager;

    public ReloadEvent(ReloadableResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public ReloadableResourceManager getResourceManager() { return resourceManager; }

}