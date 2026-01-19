package com.cobeffectiveness.mixin;

import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.event.events.ReloadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    private ReloadableResourceManager resourceManager;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;registerReloadListener(Lnet/minecraft/server/packs/resources/PreparableReloadListener;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void registerReloadListener(GameConfig gameConfig, CallbackInfo ci) {
//        this.resourceManager.registerReloadListener(new CobEffectivenessChartReloadListener());
        CobEffectivenessMod.postEvent(new ReloadEvent(resourceManager));
    }
}

