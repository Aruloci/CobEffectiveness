package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Turns panel sizes and placements into final screen positions.
 *
 * <p>A free panel resolves from its anchor and offset. An attached one resolves against wherever its
 * base actually landed, which is the whole point: an anchor plus an offset only preserves the
 * anchored edge, so panels lined up in the drag screen came apart as soon as a real battle changed
 * their size.
 *
 * <p>Pure arithmetic, so the arrangement is unit tested without needing a battle.
 */
public final class PanelResolver {

    /**
     * Space left between two attached panels.
     *
     * <p>Zero, so attached panels touch: each draws its own border, so a stack reads as one block,
     * and that is what the drag screen shows the moment a panel latches on. {@link PanelSnap} snaps
     * to this same constant - it used to snap flush while this added two pixels, so a panel dropped
     * against another moved as soon as it was let go.
     */
    public static final int ATTACH_GAP = 0;

    private PanelResolver() {}

    public record Size(int width, int height) {}

    public record Position(int x, int y) {}

    /**
     * @param sizes      measured size per panel; a panel absent here is not drawn
     * @param placements placement per panel
     * @return the top-left of each panel that has a size
     */
    public static Map<PanelId, Position> resolve(Map<PanelId, Size> sizes,
                                                 Map<PanelId, PanelPlacement> placements,
                                                 int screenWidth, int screenHeight) {
        Map<PanelId, Position> resolved = new EnumMap<>(PanelId.class);
        for (PanelId id : sizes.keySet()) {
            resolve(id, sizes, placements, screenWidth, screenHeight, resolved, EnumSet.noneOf(PanelId.class));
        }
        return resolved;
    }

    private static Position resolve(PanelId id, Map<PanelId, Size> sizes,
                                    Map<PanelId, PanelPlacement> placements,
                                    int screenWidth, int screenHeight,
                                    Map<PanelId, Position> resolved, Set<PanelId> visiting) {
        Position done = resolved.get(id);
        if (done != null) return done;

        Size size = sizes.get(id);
        PanelPlacement placement = placements.get(id);
        if (size == null || placement == null) return null;

        // The anchor is the fallback for either axis: for one with no link, and for one whose base
        // turns out not to be drawn or to lead back here.
        Position anchored = anchored(placement, size, screenWidth, screenHeight);
        int x = anchored.x();
        int y = anchored.y();

        visiting.add(id);

        Position xBase = base(placement.attachXTo, placement.attachX, id, sizes, placements,
                screenWidth, screenHeight, resolved, visiting);
        if (xBase != null) {
            x = placement.attachX.resolve(xBase.x(), sizes.get(placement.attachXTo).width(),
                    size.width(), ATTACH_GAP);
        }

        Position yBase = base(placement.attachYTo, placement.attachY, id, sizes, placements,
                screenWidth, screenHeight, resolved, visiting);
        if (yBase != null) {
            y = placement.attachY.resolve(yBase.y(), sizes.get(placement.attachYTo).height(),
                    size.height(), ATTACH_GAP);
        }

        visiting.remove(id);

        Position position = clamp(new Position(x, y), size, screenWidth, screenHeight);
        resolved.put(id, position);
        return position;
    }

    /**
     * Where one axis's base panel sits, or null when this axis has to fall back to the anchor.
     *
     * <p>A panel attached to something that is not drawn, to itself, or round a cycle falls back
     * rather than disappearing or looping forever.
     */
    private static Position base(PanelId baseId, PanelPlacement.EdgeLink link, PanelId self,
                                 Map<PanelId, Size> sizes, Map<PanelId, PanelPlacement> placements,
                                 int screenWidth, int screenHeight,
                                 Map<PanelId, Position> resolved, Set<PanelId> visiting) {
        if (baseId == null || link == null || baseId == self) return null;
        if (!sizes.containsKey(baseId) || visiting.contains(baseId)) return null;

        return resolve(baseId, sizes, placements, screenWidth, screenHeight, resolved, visiting);
    }

    private static Position anchored(PanelPlacement placement, Size size, int screenWidth, int screenHeight) {
        int x = placement.anchor.isRight()
                ? screenWidth - placement.offsetX - size.width()
                : placement.offsetX;
        int y = placement.anchor.isBottom()
                ? screenHeight - placement.offsetY - size.height()
                : placement.offsetY;
        return new Position(x, y);
    }

    private static Position clamp(Position position, Size size, int screenWidth, int screenHeight) {
        int x = Math.max(0, Math.min(position.x(), Math.max(0, screenWidth - size.width())));
        int y = Math.max(0, Math.min(position.y(), Math.max(0, screenHeight - size.height())));
        return new Position(x, y);
    }
}
