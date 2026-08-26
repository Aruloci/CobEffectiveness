package com.cobeffectiveness;


import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.handler.BattleScreenHandler;
import com.cobeffectiveness.handler.ReloadHandler;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobEffectivenessMod  {
    public static final String  MOD_ID = "cobeffectiveness";
    public static final String NAMESPACE = "cobeffectiveness";

    /** Battle screen events are posted per frame, so a broken handler must not spam the log. */
    private static final long ERROR_LOG_INTERVAL_MS = 5_000;

    private static final IEventBus eventBus = BusBuilder.builder().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile long lastErrorLoggedAt = 0L;


    private CobEffectivenessMod() {}

    public static void init(java.nio.file.Path configDir) {
        ConfigStore.init(configDir);

        eventBus.register(new BattleScreenHandler());
        eventBus.register(new ReloadHandler());

        CobEffectivenessMod.logInfo("CobEffectiveness initialized!");
    }

    public static <T extends Event> void postEvent(T event) {
        try {
            eventBus.post(event);
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            if (now - lastErrorLoggedAt < ERROR_LOG_INTERVAL_MS) return;

            lastErrorLoggedAt = now;
            logError("Error while posting " + event.getClass().getSimpleName()
                    + " (further errors suppressed for a few seconds)", e);
        }
    }

    public static void logInfo(String msg) {
        LOGGER.info(msg);
    }

    public static void logInfo(String msg, Object... args) {
        LOGGER.info(msg, args);
    }

    public static void logWarn(String msg) {
        LOGGER.warn(msg);
    }

    public static void logWarn(String msg, Object... args) {
        LOGGER.warn(msg, args);
    }

    public static void logDebug(String msg) { LOGGER.debug(msg); }

    public static void logDebug(String msg, Object... args) { LOGGER.debug(msg, args); }

    public static void logError(String msg) {
        LOGGER.error(msg);
    }

    public static void logError(String msg, Object... args) {
        LOGGER.error(msg, args);
    }

    public static void logError(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }
}
