package com.cobeffectiveness.neoforge;

import com.cobeffectiveness.CobEffectiveness;
import com.cobeffectiveness.neoforge.ui.EffectivenessHud;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CobEffectiveness.MOD_ID)
public final class ClientEvents {

    private static final EffectivenessHud HUD = new EffectivenessHud();

    private ClientEvents() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post e) {
        HUD.render(e.getGuiGraphics());
    }
}
