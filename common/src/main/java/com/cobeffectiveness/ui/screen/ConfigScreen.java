package com.cobeffectiveness.ui.screen;

import com.cobeffectiveness.config.CobConfig;
import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.config.Multiplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for the effectiveness HUD.
 *
 * <p>Edits a copy and publishes it on close, so the renderer never sees a half-applied change.
 * Done and Escape both route through {@link #onClose()}: there is one way out and it always saves.
 * Reset is the way back from a bad edit.
 *
 * <p>Position is not edited here. Three independently placed panels would be six numbers and three
 * corners; dragging them where you can see them beats typing at them.
 */
public class ConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int COLUMN_WIDTH = 158;
    private static final int GROUP_GAP = 14;
    private static final int HEADING_GAP = 13;
    private static final int TOP = 40;

    private static final int HEADING_COLOR = 0xFFA9B0C0;

    private final Screen parent;
    private final CobConfig working;

    /** Section captions, positioned during {@link #init()} and drawn in {@link #render}. */
    private record Caption(String key, int x, int y) {}

    private final List<Caption> captions = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        this(parent, ConfigStore.get().copy());
    }

    ConfigScreen(Screen parent, CobConfig working) {
        super(Component.translatable("cobeffectiveness.config.title"));
        this.parent = parent;
        this.working = working;
    }

    private interface Flag {
        boolean get();

        void set(boolean value);
    }

    @Override
    protected void init() {
        captions.clear();

        int leftX = this.width / 2 - COLUMN_WIDTH - 5;
        int rightX = this.width / 2 + 5;

        // Left: the three panels, then behaviour.
        int left = heading("cobeffectiveness.config.group_panels", leftX, TOP);
        left = toggleRow(leftX, left, "cobeffectiveness.config.panel_name", new Flag() {
            public boolean get() {
                return working.namePanel.enabled;
            }

            public void set(boolean value) {
                working.namePanel.enabled = value;
            }
        });
        left = toggleRow(leftX, left, "cobeffectiveness.config.panel_types", new Flag() {
            public boolean get() {
                return working.typesPanel.enabled;
            }

            public void set(boolean value) {
                working.typesPanel.enabled = value;
            }
        });
        left = toggleRow(leftX, left, "cobeffectiveness.config.panel_moves", new Flag() {
            public boolean get() {
                return working.movesPanel.enabled;
            }

            public void set(boolean value) {
                working.movesPanel.enabled = value;
            }
        });

        left += GROUP_GAP;
        left = heading("cobeffectiveness.config.group_behaviour", leftX, left);
        left = toggleRow(leftX, left, "cobeffectiveness.config.hover_only", new Flag() {
            public boolean get() {
                return working.hoverOnly;
            }

            public void set(boolean value) {
                working.hoverOnly = value;
            }
        });

        addRenderableWidget(button("cobeffectiveness.config.move", "cobeffectiveness.config.move.tip",
                leftX, left + 4, button -> {
                    if (minecraft != null) minecraft.setScreen(new PositionScreen(this, working));
                }));

        // Right: which multiplier rows the effectiveness panel lists.
        int right = heading("cobeffectiveness.config.group_rows", rightX, TOP);
        for (Multiplier multiplier : Multiplier.values()) {
            Button toggle = Button.builder(multiplierLabel(multiplier), b -> {
                working.setShown(multiplier, !working.isShown(multiplier));
                b.setMessage(multiplierLabel(multiplier));
            }).bounds(rightX, right, COLUMN_WIDTH, 20).build();

            toggle.setTooltip(Tooltip.create(Component.translatable(
                    "cobeffectiveness.config.row.tip", multiplier.label())));
            addRenderableWidget(toggle);
            right += ROW_HEIGHT;
        }

        addRenderableWidget(button("cobeffectiveness.config.reset", "cobeffectiveness.config.reset.tip",
                leftX, this.height - 30, button -> {
                    working.resetToDefaults();
                    rebuildWidgets();
                }));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(rightX, this.height - 30, COLUMN_WIDTH, 20).build());
    }

    private int heading(String key, int x, int y) {
        captions.add(new Caption(key, x, y));
        return y + HEADING_GAP;
    }

    /** @return the y coordinate for the next row */
    private int toggleRow(int x, int y, String key, Flag flag) {
        Button toggle = Button.builder(toggle(Component.translatable(key), flag.get()), button -> {
            flag.set(!flag.get());
            button.setMessage(toggle(Component.translatable(key), flag.get()));
        }).bounds(x, y, COLUMN_WIDTH, 20).build();

        toggle.setTooltip(Tooltip.create(Component.translatable(key + ".tip")));
        addRenderableWidget(toggle);
        return y + ROW_HEIGHT;
    }

    private Button button(String key, String tipKey, int x, int y, Button.OnPress onPress) {
        Button button = Button.builder(Component.translatable(key), onPress)
                .bounds(x, y, COLUMN_WIDTH, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable(tipKey)));
        return button;
    }

    private Component multiplierLabel(Multiplier multiplier) {
        return toggle(Component.literal(multiplier.label()), working.isShown(multiplier));
    }

    private static Component toggle(Component label, boolean on) {
        return Component.translatable("cobeffectiveness.config.toggle", label,
                Component.translatable(on ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
        for (Caption caption : captions) {
            graphics.drawString(this.font, Component.translatable(caption.key()),
                    caption.x(), caption.y(), HEADING_COLOR);
        }
    }

    @Override
    public void onClose() {
        ConfigStore.save(working);
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
