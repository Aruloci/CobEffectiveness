package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement;

import java.util.EnumMap;
import java.util.Map;

/**
 * The three panels measured and positioned, ready to draw or hit test.
 *
 * <p>Each panel is laid out once at the origin - its size does not depend on where it ends up - and
 * then {@link PanelResolver} decides the positions, so an attached panel can be placed against the
 * real size of the one it rides on. The renderer and the drag screen share this so a panel cannot be
 * previewed in one place and drawn in another.
 */
public record HudArrangement(Map<PanelId, Placed> panels) {

    /** @param layout laid out at the origin; {@code x} and {@code y} are where to draw it */
    public record Placed(HudLayout.Layout layout, int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    public static HudArrangement of(Map<PanelId, HudModel> models,
                                    Map<PanelId, PanelPlacement> placements,
                                    HudLayout.Metrics metrics,
                                    int screenWidth, int screenHeight) {

        // Size first. Laying out at the origin keeps size independent of placement, which is what
        // lets an attached panel be positioned from its base's real dimensions.
        HudLayout.Viewport origin = new HudLayout.Viewport(screenWidth, screenHeight, Anchor.TOP_LEFT, 0, 0);

        Map<PanelId, HudLayout.Layout> layouts = new EnumMap<>(PanelId.class);
        Map<PanelId, PanelResolver.Size> sizes = new EnumMap<>(PanelId.class);
        Map<PanelId, PanelPlacement> used = new EnumMap<>(PanelId.class);

        for (Map.Entry<PanelId, HudModel> entry : models.entrySet()) {
            PanelPlacement placement = placements.get(entry.getKey());
            if (placement == null || !placement.enabled) continue;

            HudModel model = entry.getValue();
            if (model == null || model.isEmpty()) continue;

            HudLayout.Layout layout = HudLayout.measure(model, metrics, origin);
            if (layout.isEmpty()) continue;

            int width = 0;
            int height = 0;
            for (HudLayout.Panel panel : layout.panels()) {
                width = Math.max(width, panel.x() + panel.width());
                height = Math.max(height, panel.y() + panel.height());
            }

            layouts.put(entry.getKey(), layout);
            sizes.put(entry.getKey(), new PanelResolver.Size(width, height));
            used.put(entry.getKey(), placement);
        }

        Map<PanelId, PanelResolver.Position> positions =
                PanelResolver.resolve(sizes, used, screenWidth, screenHeight);

        Map<PanelId, Placed> placed = new EnumMap<>(PanelId.class);
        for (Map.Entry<PanelId, HudLayout.Layout> entry : layouts.entrySet()) {
            PanelResolver.Position position = positions.get(entry.getKey());
            PanelResolver.Size size = sizes.get(entry.getKey());
            if (position == null || size == null) continue;

            placed.put(entry.getKey(), new Placed(entry.getValue(),
                    position.x(), position.y(), size.width(), size.height()));
        }
        return new HudArrangement(placed);
    }

    public Placed get(PanelId id) {
        return panels.get(id);
    }
}
