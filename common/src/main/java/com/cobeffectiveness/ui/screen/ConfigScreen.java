package com.cobeffectiveness.ui.screen;

import com.cobeffectiveness.config.CobConfig;
import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.config.Multiplier;
import com.cobeffectiveness.ui.BattleSprites;
import com.cobeffectiveness.ui.PanelRenderer;
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
 *
 * <p>Each captioned group sits on a card drawn in the same chrome as the HUD panels, so the settings
 * and the thing they configure read as one mod rather than two.
 */
public class ConfigScreen extends Screen {
    private static final int ROW_HEIGHT = BattleSprites.BUTTON_H + 2;
    private static final int COLUMN_WIDTH = 158;
    private static final int GROUP_GAP = 18;
    private static final int HEADING_GAP = 16;
    private static final int TOP = 44;
    /** Space between a card's edge and the controls inside it. */
    private static final int CARD_PAD = 6;
    /** Half the space between the two columns. */
    private static final int COLUMN_GAP = 9;
    /** Distance from the bottom of the screen to the top of the Reset and Done row. */
    private static final int BOTTOM_ROW = BattleSprites.BUTTON_H + 10;
    /** Clearance kept below the last group when the screen is too short to honour BOTTOM_ROW. */
    private static final int BOTTOM_GAP = 4;
    /** Below this scaled height the full rhythm does not fit, so the margins are trimmed. */
    private static final int TIGHT_BELOW = 264;
    private static final int TIGHT_TOP = 26;
    private static final int TIGHT_GROUP_GAP = 8;

    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int HEADING_COLOR = 0xFFA9B0C0;

    private final Screen parent;
    private final CobConfig working;

    /** Section captions, positioned during {@link #init()} and drawn in {@link #render}. */
    private record Caption(String key, int x, int y) {}

    /** A card behind one captioned group, drawn under the widgets. */
    private record Card(int x, int y, int width, int height) {}

    private final List<Caption> captions = new ArrayList<>();
    private final List<Card> cards = new ArrayList<>();

    /** Resolved in {@link #init()}, because it moves up on a short screen. */
    private int titleY = 16;

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
        cards.clear();

        int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP;
        int rightX = this.width / 2 + COLUMN_GAP;

        // A small window or a large GUI scale leaves less height than the full rhythm wants. The
        // margins give way before the controls do, so nothing is pushed off the bottom edge.
        boolean tight = this.height < TIGHT_BELOW;
        int startY = tight ? TIGHT_TOP : TOP;
        int groupGap = tight ? TIGHT_GROUP_GAP : GROUP_GAP;
        titleY = tight ? 8 : 16;

        // Left: the three panels, then behaviour.
        int top = startY;
        int left = heading("cobeffectiveness.config.group_panels", leftX, top);
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
        card(leftX, top, left);

        left += groupGap;
        top = left;
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
                leftX, left, button -> {
                    if (minecraft != null) minecraft.setScreen(new PositionScreen(this, working));
                }));
        left += ROW_HEIGHT;
        card(leftX, top, left);

        // Right: which multiplier rows the effectiveness panel lists.
        int right = heading("cobeffectiveness.config.group_rows", rightX, startY);
        for (Multiplier multiplier : Multiplier.values()) {
            Button toggle = new CobButton(rightX, right, COLUMN_WIDTH, multiplierLabel(multiplier), b -> {
                working.setShown(multiplier, !working.isShown(multiplier));
                b.setMessage(multiplierLabel(multiplier));
            });

            toggle.setTooltip(Tooltip.create(Component.translatable(
                    "cobeffectiveness.config.row.tip", multiplier.label())));
            addRenderableWidget(toggle);
            right += ROW_HEIGHT;
        }
        card(rightX, startY, right);

        // Anchored to the bottom, but never on top of the last group: on a short screen - a small
        // window, or a large GUI scale - the columns reach further down than the anchor, and Reset
        // used to be drawn straight over "Move panels...", leaving two buttons sharing one spot.
        int bottomY = Math.max(this.height - BOTTOM_ROW, Math.max(left, right) + BOTTOM_GAP);

        // No tooltip: "Reset to defaults" says what it does, and a hover card over the bottom row
        // covers the controls it would be explaining.
        addRenderableWidget(new CobButton(leftX, bottomY, COLUMN_WIDTH,
                Component.translatable("cobeffectiveness.config.reset"), button -> {
                    working.resetToDefaults();
                    rebuildWidgets();
                }));

        addRenderableWidget(new CobButton(rightX, bottomY, COLUMN_WIDTH,
                CommonComponents.GUI_DONE, button -> onClose()));
    }

    private int heading(String key, int x, int y) {
        captions.add(new Caption(key, x, y));
        return y + HEADING_GAP;
    }

    /**
     * @param top    the y the caption started at
     * @param bottom the y the next row would have started at
     */
    private void card(int x, int top, int bottom) {
        // The running y is where the *next* row would go, which is one inter-row gap past the last
        // control. Trim it so the card hugs the group instead of trailing a gap.
        int height = bottom - (ROW_HEIGHT - BattleSprites.BUTTON_H) - top;
        cards.add(new Card(x - CARD_PAD, top - CARD_PAD,
                COLUMN_WIDTH + CARD_PAD * 2, height + CARD_PAD * 2));
    }

    /** @return the y coordinate for the next row */
    private int toggleRow(int x, int y, String key, Flag flag) {
        Button toggle = new CobButton(x, y, COLUMN_WIDTH, toggle(Component.translatable(key), flag.get()),
                button -> {
                    flag.set(!flag.get());
                    button.setMessage(toggle(Component.translatable(key), flag.get()));
                });

        toggle.setTooltip(Tooltip.create(Component.translatable(key + ".tip")));
        addRenderableWidget(toggle);
        return y + ROW_HEIGHT;
    }

    private Button button(String key, String tipKey, int x, int y, Button.OnPress onPress) {
        Button button = new CobButton(x, y, COLUMN_WIDTH, Component.translatable(key), onPress);
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Between the background and the widgets: anywhere else and the cards would either be
        // painted over by the background or drawn on top of the buttons they sit behind.
        for (Card card : cards) {
            PanelRenderer.drawCard(graphics, card.x(), card.y(), card.width(), card.height());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, BattleSprites.styled(this.title),
                this.width / 2, titleY, TITLE_COLOR);

        for (Caption caption : captions) {
            graphics.drawString(this.font, BattleSprites.styled(Component.translatable(caption.key())),
                    caption.x(), caption.y(), HEADING_COLOR);
            PanelRenderer.drawDivider(graphics, caption.x(),
                    caption.y() + this.font.lineHeight + 2, COLUMN_WIDTH);
        }
    }

    @Override
    public void onClose() {
        ConfigStore.save(working);
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
