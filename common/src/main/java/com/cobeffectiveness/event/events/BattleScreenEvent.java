package com.cobeffectiveness.event.events;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.Event;

public abstract class BattleScreenEvent extends Event {

    protected BattleScreenEvent() {}

    public static class Open extends BattleScreenEvent{
        private final GuiGraphics graphics;
        private final int mouseX;
        private final int mouseY;

        public Open(GuiGraphics graphics, int mouseX, int mouseY) {
            super();
            this.graphics = graphics;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public GuiGraphics getGraphics() {
            return this.graphics;
        }

        public int getMouseX() {
            return this.mouseX;
        }

        public int getMouseY() {
            return this.mouseY;
        }
    }

    public static class Close extends BattleScreenEvent{
        public Close() {
            super();
        }
    }

}