package com.cobeffectiveness.ui;

import net.minecraft.client.gui.DrawContext;

public final class PanelRenderer {
    private static final int BG = 0xE01E1E1E;
    private static final int BORDER = 0xFF8C8C8C;
    private static final int BORDER_IN = 0xFF2B2B2B;
    private static final int DIVIDER = 0xFF5A5A5A;

    private PanelRenderer() {}

    public static void drawPanel(DrawContext dc, int x, int y, int w, int h) {
        dc.fill(x, y, x + w, y + h, BG);
        dc.drawBorder(x, y, w, h, BORDER);
        dc.drawBorder(x + 1, y + 1, w - 2, h - 2, BORDER_IN);
    }

    public static void drawDivider(DrawContext dc, int x, int y, int w) {
        dc.fill(x, y, x + w, y + 1, DIVIDER);
    }
}
