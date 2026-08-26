package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.CobConfig;
import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement;
import com.cobeffectiveness.type.IconSheet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.EnumMap;
import java.util.Map;

/**
 * Paints the panels wherever {@link HudArrangement} has placed them.
 *
 * <p>All geometry comes from {@link HudLayout} and {@link PanelResolver}; this class only supplies
 * text and icon sizes and then draws what it is handed.
 */
public final class EffectivenessHud {

    /** Draws every enabled panel at its resolved position. */
    public void render(GuiGraphics graphics) {
        CobConfig config = ConfigStore.get();

        Map<PanelId, HudModel> models = new EnumMap<>(PanelId.class);
        models.put(PanelId.NAME, HudState.nameModel());
        models.put(PanelId.TYPES, HudState.typesModel());
        models.put(PanelId.MOVES, HudState.movesModel());

        render(graphics, HudArrangement.of(models, placementsOf(config), metrics(),
                screenWidth(), screenHeight()));
    }

    /** Draws a prepared arrangement, shared with the drag screen. */
    public void render(GuiGraphics graphics, HudArrangement arrangement) {
        Font font = font();
        IconSheet sheet = IconSheet.current();

        for (HudArrangement.Placed placed : arrangement.panels().values()) {
            draw(graphics, placed, font, sheet);
        }
    }

    private void draw(GuiGraphics graphics, HudArrangement.Placed placed, Font font, IconSheet sheet) {
        // The layout was measured at the origin, so everything shifts by the resolved position.
        int dx = placed.x();
        int dy = placed.y();

        for (HudLayout.Panel panel : placed.layout().panels()) {
            PanelRenderer.drawPanel(graphics, panel.x() + dx, panel.y() + dy, panel.width(), panel.height());

            for (HudLayout.PlacedElement element : panel.elements()) {
                HudLayout.Element content = element.element();

                if (content.isDivider()) {
                    PanelRenderer.drawDivider(graphics, element.x() + dx, element.y() + dy, element.width());
                    continue;
                }

                // Continuation lines of a wrapped row keep the label for alignment but must not
                // print it again.
                if (content.showLabel() && !content.text().isEmpty()) {
                    graphics.drawString(font, content.text(),
                            element.x() + dx, element.textY() + dy, content.color());
                }

                if (!content.types().isEmpty()) {
                    IconRenderer.draw(graphics, sheet, element.iconX() + dx, element.iconY() + dy,
                            content.types());
                }

                if (!content.trailing().isEmpty()) {
                    graphics.drawString(font, content.trailing(),
                            element.trailingX() + dx, element.textY() + dy, content.trailingColor());
                }
            }
        }
    }

    public static Map<PanelId, PanelPlacement> placementsOf(CobConfig config) {
        Map<PanelId, PanelPlacement> placements = new EnumMap<>(PanelId.class);
        placements.put(PanelId.NAME, config.namePanel);
        placements.put(PanelId.TYPES, config.typesPanel);
        placements.put(PanelId.MOVES, config.movesPanel);
        return placements;
    }

    public static int screenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int screenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static Font font() {
        return Minecraft.getInstance().fontFilterFishy;
    }

    /** Shared by the HUD and the drag screen so both measure identically. */
    public static HudLayout.Metrics metrics() {
        Font font = font();
        IconSheet sheet = IconSheet.current();

        return new HudLayout.Metrics() {
            @Override
            public int textWidth(String text) {
                return font.width(text);
            }

            @Override
            public int fontHeight() {
                return font.lineHeight;
            }

            @Override
            public int iconRowWidth(int count) {
                return IconRenderer.rowWidth(sheet, count);
            }

            @Override
            public int iconHeight() {
                return sheet.tileH();
            }
        };
    }
}
