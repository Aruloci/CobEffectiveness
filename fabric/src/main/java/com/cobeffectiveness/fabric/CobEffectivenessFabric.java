package com.cobeffectiveness.fabric;

import com.cobeffectiveness.compat.Compat;
import net.fabricmc.api.ClientModInitializer;

import com.cobeffectiveness.CobEffectivenessMod;
import net.fabricmc.loader.api.FabricLoader;

public final class CobEffectivenessFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //Check for GEB
        Compat.init(FabricLoader.getInstance().isModLoaded(Compat.MODID_GEB));

        CobEffectivenessMod.init();
    }
}
