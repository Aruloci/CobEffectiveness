package com.cobeffectiveness.neoforge;

import com.cobeffectiveness.compat.Compat;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

import com.cobeffectiveness.CobEffectivenessMod;

@Mod(CobEffectivenessMod.MOD_ID)
public final class CobEffectivenessNeoForge {

    public CobEffectivenessNeoForge() {
        //Check for GEB
        Compat.init(ModList.get().isLoaded(Compat.MODID_GEB));

        CobEffectivenessMod.init();
    }
}
