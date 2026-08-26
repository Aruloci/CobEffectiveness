package com.cobeffectiveness.neoforge;

import com.cobeffectiveness.compat.Compat;
import com.cobeffectiveness.ui.screen.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.cobeffectiveness.CobEffectivenessMod;

/**
 * Client only: this mod does nothing but draw a HUD, and constructing it pulls in client classes
 * that do not exist on a dedicated server.
 */
@Mod(value = CobEffectivenessMod.MOD_ID, dist = Dist.CLIENT)
public final class CobEffectivenessNeoForge {

    public CobEffectivenessNeoForge(ModContainer container) {
        //Check for GEB
        Compat.init(ModList.get().isLoaded(Compat.MODID_GEB));

        CobEffectivenessMod.init(FMLPaths.CONFIGDIR.get());

        // NeoForge shows a config button on the mod list entry for this.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (owner, parent) -> new ConfigScreen(parent));
    }
}
