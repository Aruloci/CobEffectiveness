package com.cobeffectiveness.mixin;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.event.events.BattleScreenEvent;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BattleGUI.class)
public abstract class BattleGUIMixin {

    /** Posted once per frame, not once per screen open. */
    @Inject(method = "render", at = @At("TAIL"))
    private void render(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        CobEffectivenessMod.postEvent(new BattleScreenEvent.Open(graphics, mouseX, mouseY));
    }

    @Inject(method = "onClose", at = @At("TAIL"))
    private void onClose(CallbackInfo ci) {
        CobEffectivenessMod.postEvent(new BattleScreenEvent.Close());
    }
}
