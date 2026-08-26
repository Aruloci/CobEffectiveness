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

    /**
     * Announces the resource manager so the chart reload listener can be attached.
     *
     * <p>The Minecraft constructor calls registerReloadListener 23 times; without an ordinal this
     * injection matched every one of them and posted 23 events per launch. Ordinal 0 is the right
     * one to take: it runs after the resourceManager field is assigned and after mod entrypoints
     * have registered their handlers, but still before the initial resource reload is kicked off,
     * so the chart is loaded on that first reload rather than only on later ones.
     */
    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ReloadableResourceManager;registerReloadListener(Lnet/minecraft/server/packs/resources/PreparableReloadListener;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void registerReloadListener(GameConfig gameConfig, CallbackInfo ci) {
        CobEffectivenessMod.postEvent(new ReloadEvent(resourceManager));
    }
}
