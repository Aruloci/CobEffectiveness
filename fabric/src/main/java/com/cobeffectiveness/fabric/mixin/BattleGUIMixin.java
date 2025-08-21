package com.cobeffectiveness.fabric.mixin;

import com.cobeffectiveness.battle.OpponentSnapshot;
import com.cobeffectiveness.fabric.battle.PokemonTypeFetcher;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(targets = "com.cobblemon.mod.common.client.gui.battle.BattleGUI")
public abstract class BattleGUIMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void render(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PokemonTypeFetcher.refreshFromCurrentBattle();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void onClose(CallbackInfo ci) {
        OpponentSnapshot.clear();
    }
}
