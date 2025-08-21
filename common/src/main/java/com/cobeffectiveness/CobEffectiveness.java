package com.cobeffectiveness;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CobEffectiveness {
    public static final String MOD_ID = "cobblemon_effectiveness";
    public static final Logger LOGGER = LogManager.getLogger("CobEffectiveness");

    private CobEffectiveness() {}

    public static void initCommon() {
        LOGGER.info("Mod initialized");
    }
}