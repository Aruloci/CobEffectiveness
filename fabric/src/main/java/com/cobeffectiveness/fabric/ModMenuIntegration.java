package com.cobeffectiveness.fabric;

import com.cobeffectiveness.ui.screen.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Puts a config button on the mod list entry when ModMenu is installed.
 *
 * <p>ModMenu is compile-only and this entrypoint is only invoked by ModMenu itself, so the class is
 * never loaded when it is absent.
 */
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
