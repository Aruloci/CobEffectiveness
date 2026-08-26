package com.cobeffectiveness.handler;

import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.event.events.BattleScreenEvent;
import com.cobeffectiveness.ui.EffectivenessHud;
import com.cobeffectiveness.ui.HoverGate;
import com.cobeffectiveness.ui.HudState;
import net.neoforged.bus.api.SubscribeEvent;

public final class BattleScreenHandler {

    private final EffectivenessHud hud = new EffectivenessHud();

    /** Fires once per rendered frame while the battle screen is up. */
    @SubscribeEvent
    public void onBattleScreenRendered(BattleScreenEvent.Open event) {
        HudState.refresh();

        // Evaluated per frame rather than per sample: the pointer moves every frame.
        if (!HoverGate.shouldDraw(ConfigStore.get().hoverOnly,
                event.getMouseX(), event.getMouseY())) {
            return;
        }

        hud.render(event.getGraphics());
    }

    @SubscribeEvent
    public void onBattleScreenClosed(BattleScreenEvent.Close event) {
        HudState.clear();
    }
}
