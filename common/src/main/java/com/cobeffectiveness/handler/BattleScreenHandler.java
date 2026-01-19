package com.cobeffectiveness.handler;

import com.cobeffectiveness.battle.OpponentSnapshot;
import com.cobeffectiveness.battle.PokemonTypeFetcher;
import com.cobeffectiveness.event.events.BattleScreenEvent;
import com.cobeffectiveness.ui.EffectivenessHud;
import net.neoforged.bus.api.SubscribeEvent;

public final class BattleScreenHandler {

    private static final EffectivenessHud HUD = new EffectivenessHud();

    @SubscribeEvent
    public void onBattleScreenOpened(BattleScreenEvent.Open event) {
        PokemonTypeFetcher.refreshFromCurrentBattle();
        HUD.render(event.getGraphics());
    }

    @SubscribeEvent
    public void onBattleScreenClosed(BattleScreenEvent.Close event) {
        OpponentSnapshot.clear();
    }
}