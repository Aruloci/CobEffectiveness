package com.cobeffectiveness.fabric.ui;

import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.cobeffectiveness.battle.OpponentSnapshot;
import com.cobeffectiveness.type.Type;
import com.cobeffectiveness.type.EffectivenessChart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Set;

public final class EffectivenessHud {
    private static final int RIGHT_MARGIN = BattleOverlay.HORIZONTAL_INSET;
    private static final int TOP_OFFSET = BattleOverlay.COMPACT_TILE_TEXTURE_HEIGHT;

    private static final int PAD_X = 6;
    private static final int PAD_Y = 6;
    private static final int LABEL_GAP = 4;
    private static final int ROW_GAP = 4;
    private static final int DIV_H = 1;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void render(DrawContext dc) {
        OpponentSnapshot.current().ifPresent(opponentTypes -> {
            if (opponentTypes.isEmpty()) return; // just in case

            Map<Double, Set<Type>> groups = EffectivenessChart.attackersByMultipliers(opponentTypes, 4.0, 2.0);
            var fourX = groups.get(4.0);
            var twoX = groups.get(2.0);
            if ((fourX == null || fourX.isEmpty()) && (twoX == null || twoX.isEmpty())) return;

            final int fontH = mc.textRenderer.fontHeight;
            final int iconH = IconRenderer.rowHeight();

            final String L4 = "×4";
            final String L2 = "×2";
            final int L4w = mc.textRenderer.getWidth(L4);
            final int L2w = mc.textRenderer.getWidth(L2);

            final int fourW = IconRenderer.rowWidth(fourX != null ? fourX.size() : 0);
            final int twoW = IconRenderer.rowWidth(twoX != null ? twoX.size() : 0);

            final int row4W = (fourW > 0) ? (L4w + LABEL_GAP + fourW) : 0;
            final int row2W = (twoW > 0) ? (L2w + LABEL_GAP + twoW) : 0;

            final int innerW = Math.max(row4W, row2W);
            if (innerW == 0) return;

            int panelH = PAD_Y;
            if (row4W > 0) panelH += iconH;
            if (row4W > 0 && row2W > 0) panelH += ROW_GAP + DIV_H + ROW_GAP;
            if (row2W > 0) panelH += iconH;
            panelH += PAD_Y;

            final int panelW = innerW + PAD_X * 2;

            int sw = mc.getWindow().getScaledWidth();
            int x = sw - RIGHT_MARGIN - panelW;
            int y = TOP_OFFSET;

            // panel
            PanelRenderer.drawPanel(dc, x, y, panelW, panelH);

            // content
            int cursorY = y + PAD_Y;
            int labelYOffset = (iconH - fontH) / 2 + 1;

            // ×4 row
            if (row4W > 0) {
                int rowX = x + PAD_X + (innerW - row4W) / 2;
                dc.drawTextWithShadow(mc.textRenderer, Text.literal(L4), rowX, cursorY + labelYOffset, 0xFFFFE066);
                IconRenderer.drawTypes(dc, rowX + L4w + LABEL_GAP, cursorY, fourX);
                cursorY += iconH;
            }

            // divider
            if (row4W > 0 && row2W > 0) {
                cursorY += ROW_GAP;
                PanelRenderer.drawDivider(dc, x + PAD_X, cursorY, innerW);
                cursorY += DIV_H + ROW_GAP;
            }

            // ×2 row
            if (row2W > 0) {
                int rowX = x + PAD_X + (innerW - row2W) / 2;
                dc.drawTextWithShadow(mc.textRenderer, Text.literal(L2), rowX, cursorY + labelYOffset, 0xFFB7C0FF);
                IconRenderer.drawTypes(dc, rowX + L2w + LABEL_GAP, cursorY, twoX);
            }
        });
    }
}
