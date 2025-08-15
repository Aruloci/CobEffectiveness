package com.cobeffectiveness.fabric;

import net.fabricmc.api.ModInitializer;

import com.cobeffectiveness.CobEffectiveness;

public final class CobEffectivenessFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CobEffectiveness.initCommon();
    }
}
