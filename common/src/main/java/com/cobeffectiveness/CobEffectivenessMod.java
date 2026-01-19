package com.cobeffectiveness;


import com.cobeffectiveness.event.EventBusWrapper;
import com.cobeffectiveness.handler.BattleScreenHandler;
import com.cobeffectiveness.handler.ReloadHandler;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobEffectivenessMod  {
    public static final String  MOD_ID = "cobeffectiveness";
    public static final String NAMESPACE = "cobeffectiveness";

    private static final IEventBus eventBus = EventBusWrapper.createEventBus();
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    private CobEffectivenessMod() {}

    public static void init() {
        eventBus.register(new BattleScreenHandler());
        eventBus.register(new ReloadHandler());

        CobEffectivenessMod.logInfo("CobEffectiveness initialized!");
    }

    public static <T extends Event> void postEvent(T event) {
        try {
            eventBus.post(event);
            if (event instanceof ICancellableEvent cancellableEvent) {
                cancellableEvent.isCanceled();
            }
        } catch (Exception e) {
            logError("Error while posting event...");
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