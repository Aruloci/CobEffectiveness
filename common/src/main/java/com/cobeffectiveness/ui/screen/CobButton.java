package com.cobeffectiveness.ui.screen;

import com.cobeffectiveness.ui.BattleSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * A button wearing Cobblemon's battle chrome, so the settings look like the thing they configure.
 *
 * <p>The plate is painted from Cobblemon's action-tile palette rather than blitted, so there is no
 * texture to be missing and no fallback to get stuck in.
 */
public class CobButton extends Button {

    /** Kept clear of the plate's bevelled ends. */
    private static final int PADDING = 6;
    private static final String ELLIPSIS = "...";

    public CobButton(int x, int y, int width, Component message, OnPress onPress) {
        super(x, y, width, BattleSprites.BUTTON_H, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        BattleSprites.drawButton(graphics, getX(), getY(), getWidth(), isHovered || isFocused());

        // With the shadow, like Cobblemon's own move buttons: the label is white on a light plate,
        // so the shadow is what holds it off the face rather than something to be rid of.
        // renderScrollingString is still avoided, because it cannot carry the font and weight.
        Font font = Minecraft.getInstance().font;
        FormattedCharSequence label = Language.getInstance()
                .getVisualOrder(fit(font, BattleSprites.styled(getMessage()), getWidth() - PADDING * 2));

        graphics.drawString(font, label,
                getX() + (getWidth() - font.width(label)) / 2,
                getY() + (getHeight() - 8) / 2,
                active ? BattleSprites.TEXT : BattleSprites.TEXT_DISABLED,
                true);
    }

    /** Labels cannot wrap on a fixed plate, so an overlong one is cut rather than spilling over it. */
    private static FormattedText fit(Font font, Component label, int available) {
        if (font.width(label) <= available) return label;

        Component ellipsis = BattleSprites.text(ELLIPSIS);
        return FormattedText.composite(
                font.substrByWidth(label, Math.max(0, available - font.width(ellipsis))), ellipsis);
    }
}
