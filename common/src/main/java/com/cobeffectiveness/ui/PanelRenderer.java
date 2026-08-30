package com.cobeffectiveness.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The chrome the panels and the settings cards are drawn in.
 *
 * <p>The palette is Cobblemon's: a {@code #2f2f2f} outline and its greys for the bevel inside it.
 * Drawn rather than blitted, and not as a shortcut - Cobblemon's own dark surface,
 * {@code battle_info_underlay.png}, is a {@code #2f2f2f} body with a corner cut and nothing else, so
 * blitting it would put the same pixels on screen at the cost of a texture any Cobblemon update can
 * reshape. Its <em>tiles</em> are the part worth borrowing, and those are in {@link BattleSprites}.
 */
public final class PanelRenderer {

    /** Cobblemon's battle outline. */
    private static final int OUTLINE = 0xFF2F2F2F;
    /** Its dark body, let through a little so a battle is not hidden behind the HUD. */
    private static final int FILL = 0xE62F2F2F;
    /**
     * A config card sits on Minecraft's own blurred, already dark screen background, so it needs to
     * be darker than the HUD panel to read against it at all. At the HUD's value it was invisible.
     */
    private static final int CARD_FILL = 0xD4121212;
    /**
     * The bevel Cobblemon runs inside its outline, at its weight rather than a muted one. Muted, the
     * panel loses the only edge that told it apart from the scene behind it.
     */
    private static final int RIM_TOP = 0xFF8D8D8D;
    private static final int RIM = 0xFF676767;

    private static final int DIVIDER = 0xFF676767;
    /** Alternate move rows, barely there - enough to follow a row across three columns. */
    private static final int STRIPE = 0x14FFFFFF;

    /**
     * How far the rows nearest an edge are pulled in, which is what rounds a corner in a medium
     * where a curve is four pixels. Cobblemon cuts one pixel; two reads as round at the GUI scales
     * anyone plays at, and is the one place this deliberately goes further than the reference.
     */
    private static final int[] CORNER = {2, 1};

    private PanelRenderer() {}

    /** The inset of one row from either side, given how close it is to the top or bottom edge. */
    static int inset(int row, int height) {
        int fromEdge = Math.min(row, height - 1 - row);
        return fromEdge < CORNER.length ? CORNER[fromEdge] : 0;
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
        draw(graphics, x, y, w, h, FILL);
    }

    public static void drawCard(GuiGraphics graphics, int x, int y, int w, int h) {
        draw(graphics, x, y, w, h, CARD_FILL);
    }

    /**
     * Painted a row at a time rather than as four rectangles, because the corner inset varies by row
     * and the outline and the rim both have to follow it. A row is: outline, rim, body, rim,
     * outline.
     */
    private static void draw(GuiGraphics graphics, int x, int y, int w, int h, int fill) {
        if (w < 6 || h < 6) return;

        int right = x + w;
        int last = h - 1;

        for (int row = 0; row < h; row++) {
            int in = inset(row, h);
            int left = x + in;
            int end = right - in;
            int top = y + row;

            if (row == 0 || row == last) {
                graphics.fill(left, top, end, top + 1, OUTLINE);
                continue;
            }

            graphics.fill(left, top, left + 1, top + 1, OUTLINE);
            graphics.fill(end - 1, top, end, top + 1, OUTLINE);

            int innerLeft = left + 1;
            int innerRight = end - 1;
            if (innerRight - innerLeft < 2) continue;

            if (row == 1) {
                graphics.fill(innerLeft, top, innerRight, top + 1, RIM_TOP);
            } else if (row == last - 1) {
                graphics.fill(innerLeft, top, innerRight, top + 1, RIM);
            } else {
                graphics.fill(innerLeft, top, innerLeft + 1, top + 1, RIM);
                graphics.fill(innerRight - 1, top, innerRight, top + 1, RIM);
                graphics.fill(innerLeft + 1, top, innerRight - 1, top + 1, fill);
            }
        }
    }

    public static void drawDivider(GuiGraphics graphics, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + 1, DIVIDER);
    }

    /** Shades one move row so the eye can carry a name across to its multiplier. */
    public static void drawRowStripe(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, STRIPE);
    }
}
