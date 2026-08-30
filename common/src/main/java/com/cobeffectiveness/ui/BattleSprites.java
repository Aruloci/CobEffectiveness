package com.cobeffectiveness.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Cobblemon's battle look, in the two parts of it that are worth borrowing: the palette of its
 * action tiles, and the font it writes everything in.
 *
 * <p>The tiles are drawn rather than blitted. Cobblemon ships no neutral one - {@code
 * battle_menu_fight.png} and its siblings each bake in their own icon and coloured bar, and their
 * hovered state is tinted per action (red for Fight, green for Switch) - so there is nothing to
 * borrow but the colours. Those are flat bands, which {@code fill} reproduces exactly, at any width,
 * without a texture that a Cobblemon update can reshape underneath us. {@link PanelRenderer} draws
 * the panels on the same reasoning.
 */
public final class BattleSprites {

    /** Cobblemon's own {@code OPTION_HEIGHT} is 26; 24 keeps the settings rows as they were. */
    public static final int BUTTON_H = 24;

    /**
     * Cobblemon's greys run in a ramp - {@code 2f2f2f, 4b4b4b, 676767, 8d8d8d, c6c6c6} - and the
     * action tile sits on the upper half of it. This is that tile shifted one step down the same
     * ramp, because white text on its {@code 676767} body was still a squint. Nothing here is
     * invented: every value is one Cobblemon already uses.
     */
    private static final int OUTLINE = 0xFF2F2F2F;
    private static final int HIGHLIGHT = 0xFF8D8D8D;
    private static final int UPPER = 0xFF676767;
    private static final int BODY = 0xFF4B4B4B;

    /** Hovered is the tile's own resting palette, so the lift is a step back up the same ramp. */
    private static final int HOVER_HIGHLIGHT = 0xFFC6C6C6;
    private static final int HOVER_UPPER = 0xFF8D8D8D;
    private static final int HOVER_BODY = 0xFF676767;

    /**
     * White, like Cobblemon: {@code drawScaledText} defaults to {@code 0x00FFFFFF} plus the opacity
     * and passes {@code shadow = true}, so the shadow is what holds the label off the face. A
     * disabled label halves the alpha rather than changing colour, which is how Cobblemon dims an
     * unselectable option.
     */
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DISABLED = 0x80FFFFFF;

    /**
     * Cobblemon writes its battle text - move names, PP, the tile status - in vanilla's {@code
     * uniform} font and in bold, which together are most of why its UI reads as its own. Nothing is
     * shipped for this: it is a vanilla font, so glyph coverage is a given.
     */
    private static final Style FONT = Style.EMPTY
            .withFont(ResourceLocation.withDefaultNamespace("uniform"))
            .withBold(true);

    private BattleSprites() {}

    /**
     * Puts text in Cobblemon's font.
     *
     * <p>Everything that measures text has to go through here too, not just everything that draws
     * it - a width taken in one font and painted in another is the same disagreement the layout is
     * built to avoid, and bold costs a pixel per glyph.
     */
    public static Component text(String text) {
        return Component.literal(text).setStyle(FONT);
    }

    /** As {@link #text}, for a component that already exists - a translated label, say. */
    public static Component styled(Component component) {
        return component.copy().withStyle(FONT);
    }

    /**
     * One tile at any width: an outline with rounded corners, then the face in flat bands.
     *
     * <p>Painted a row at a time so the outline can follow the corner inset, which varies by row.
     * The rounding is shared with {@link PanelRenderer} so a button and the card behind it are cut
     * to the same shape.
     */
    public static void drawButton(GuiGraphics graphics, int x, int y, int width, boolean highlighted) {
        if (width < 6) return;

        int right = x + width;
        int last = BUTTON_H - 1;

        for (int row = 0; row < BUTTON_H; row++) {
            int in = PanelRenderer.inset(row, BUTTON_H);
            int left = x + in;
            int end = right - in;
            int top = y + row;

            if (row == 0 || row == last) {
                graphics.fill(left, top, end, top + 1, OUTLINE);
                continue;
            }

            graphics.fill(left, top, left + 1, top + 1, OUTLINE);
            graphics.fill(end - 1, top, end, top + 1, OUTLINE);

            if (end - left > 2) {
                graphics.fill(left + 1, top, end - 1, top + 1, band(row, last, highlighted));
            }
        }
    }

    /** The face is a light rule under the top edge, a lighter band, the body, then a closing band. */
    private static int band(int row, int last, boolean highlighted) {
        if (row == 1) return highlighted ? HOVER_HIGHLIGHT : HIGHLIGHT;
        if (row < 5 || row >= last - 1) return highlighted ? HOVER_UPPER : UPPER;
        return highlighted ? HOVER_BODY : BODY;
    }
}
