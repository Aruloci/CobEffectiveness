package com.cobeffectiveness.event.events;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.Event;

public abstract class BattleScreenEvent extends Event {

    protected BattleScreenEvent() {}

    public static class Open extends BattleScreenEvent{
        private final GuiGraphics graphics;

        public Open(GuiGraphics graphics) {
            super();
            this.graphics = graphics;
        }

        public GuiGraphics getGraphics() {
            return this.graphics;
        }
    }

    public static class Close extends BattleScreenEvent{
        public Close() {
            super();
        }
    }

}