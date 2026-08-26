package com.cobeffectiveness.ui;

import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.battle.TileBounds;

import java.util.List;

/**
 * Decides whether the HUD should be drawn when "only while hovering" is switched on.
 *
 * <p>{@link TileBounds} reproduces Cobblemon's battle tile geometry, which is the most version
 * fragile code in the mod. If that ever stops producing sensible rectangles, a strict hover test
 * would hide the HUD forever with no clue why. So the gate fails **open**: unusable geometry means
 * draw anyway and say so once in the log, rather than silently showing nothing.
 */
public final class HoverGate {
    private static boolean warned = false;

    private HoverGate() {}

    /** Reads the live battle and applies the gate. */
    public static boolean shouldDraw(boolean hoverOnly, double mouseX, double mouseY) {
        if (!hoverOnly) return true;

        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        List<TileBounds.Rect> tiles = TileBounds.opponentTiles();

        if (!anyUsable(tiles, window.getGuiScaledWidth(), window.getGuiScaledHeight())) {
            warnOnce();
            return true;
        }
        return hovering(tiles, mouseX, mouseY);
    }

    /**
     * Whether the computed rectangles look like real on-screen tiles.
     *
     * <p>Pure, so the fallback rule is unit tested rather than trusted.
     */
    public static boolean anyUsable(List<TileBounds.Rect> tiles, int screenWidth, int screenHeight) {
        for (TileBounds.Rect tile : tiles) {
            boolean sized = tile.width() > 0 && tile.height() > 0;
            boolean onScreen = tile.x() < screenWidth && tile.y() < screenHeight
                    && tile.x() + tile.width() > 0 && tile.y() + tile.height() > 0;
            if (sized && onScreen) return true;
        }
        return false;
    }

    public static boolean hovering(List<TileBounds.Rect> tiles, double mouseX, double mouseY) {
        for (TileBounds.Rect tile : tiles) {
            if (tile.contains(mouseX, mouseY)) return true;
        }
        return false;
    }

    private static void warnOnce() {
        if (warned) return;
        warned = true;
        CobEffectivenessMod.logWarn(
                "Could not work out where Cobblemon draws its battle tiles, so 'only while hovering'"
                        + " cannot be applied. Showing the HUD instead of hiding it."
                        + " This usually means Cobblemon changed its battle UI.");
    }
}
