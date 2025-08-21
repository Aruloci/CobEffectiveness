package com.cobeffectiveness.fabric;

import com.cobeffectiveness.CobEffectiveness;
import com.cobeffectiveness.fabric.ui.EffectivenessHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class CobEffectivenessFabricClient implements ClientModInitializer {
    private final EffectivenessHud hud = new EffectivenessHud();

    @Override
    public void onInitializeClient() {
        CobEffectiveness.initCommon();

        HudRenderCallback.EVENT.register((DrawContext dc, RenderTickCounter t) -> hud.render(dc));
    }
}
