package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement.EdgeLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Edge snapping while a panel is being dragged, and reading the result back as an attachment.
 *
 * <p>Three panels that are meant to line up are fiddly to place by hand, so a dragged panel latches
 * onto the edges of the others when it comes close.
 *
 * <p>{@link #attach} is the whole of it: one scan of every edge of every other panel, keeping the
 * nearest match <b>per axis</b>. {@link #snap} is that same result expressed as a position, so what
 * the drag screen shows and what gets recorded cannot be two different things. Each time they were
 * computed separately they drifted apart - first by the attachment gap, then by which edges could be
 * expressed at all, then by which panel the edge belonged to.
 *
 * <p>Pure geometry, so the behaviour is unit tested rather than eyeballed.
 */
public final class PanelSnap {

    /** How close an edge has to come before it latches, in scaled pixels. */
    public static final int THRESHOLD = 6;

    private PanelSnap() {}

    public record Box(int x, int y, int width, int height) {}

    /**
     * What a panel is resting against, per axis.
     *
     * <p>The two axes carry their own base because a drop can line up against two different
     * neighbours at once - which is exactly what happened: a panel placed beside the moves panel
     * also had its top edge level with the bottom of the name panel above it.
     */
    public record Attachment(PanelId xBase, EdgeLink x, PanelId yBase, EdgeLink y) {
        public static final Attachment NONE = new Attachment(null, null, null, null);

        public boolean isEmpty() {
            return x == null && y == null;
        }
    }

    /**
     * Finds the nearest edge to latch onto on each axis, across every panel offered.
     *
     * @return the panel and link per axis; null on an axis with nothing within {@code threshold}
     */
    public static Attachment attach(Box moving, Map<PanelId, Box> bases, int threshold) {
        List<Candidate> candidates = new ArrayList<>(bases.size());
        for (Map.Entry<PanelId, Box> entry : bases.entrySet()) {
            Candidate candidate = nearestEdges(moving, entry.getKey(), entry.getValue(), threshold);
            if (candidate != null) candidates.add(candidate);
        }

        Candidate x = nearestOn(candidates, true);
        Candidate y = nearestOn(candidates, false);

        // Panels that are flush against each other make ties common - a panel tucked beside the moves
        // panel also has its top level with the bottom of the name panel above it. When one panel is
        // equally close on both axes it takes both, so the drop is recorded as riding on the panel it
        // visibly sits against instead of being split by whichever was looked at first.
        if (x != null && y != null && x.id() != y.id()) {
            for (Candidate candidate : candidates) {
                if (candidate.x() != null && candidate.dx() == x.dx()
                        && candidate.y() != null && candidate.dy() == y.dy()) {
                    x = candidate;
                    y = candidate;
                    break;
                }
            }
        }

        return new Attachment(x == null ? null : x.id(), x == null ? null : x.x(),
                y == null ? null : y.id(), y == null ? null : y.y());
    }

    /** The closest edge of one base on each axis, or null when neither axis is within reach. */
    private static Candidate nearestEdges(Box moving, PanelId id, Box base, int threshold) {
        EdgeLink x = null;
        EdgeLink y = null;
        int dx = threshold + 1;
        int dy = threshold + 1;

        for (EdgeLink link : EdgeLink.values()) {
            int distanceX = Math.abs(resolveX(link, base, moving) - moving.x());
            if (distanceX <= threshold && distanceX < dx) {
                dx = distanceX;
                x = link;
            }

            int distanceY = Math.abs(resolveY(link, base, moving) - moving.y());
            if (distanceY <= threshold && distanceY < dy) {
                dy = distanceY;
                y = link;
            }
        }
        return x == null && y == null ? null : new Candidate(id, x, dx, y, dy);
    }

    private static Candidate nearestOn(List<Candidate> candidates, boolean horizontal) {
        Candidate best = null;
        for (Candidate candidate : candidates) {
            if ((horizontal ? candidate.x() : candidate.y()) == null) continue;

            int distance = horizontal ? candidate.dx() : candidate.dy();
            if (best == null || distance < (horizontal ? best.dx() : best.dy())) best = candidate;
        }
        return best;
    }

    private record Candidate(PanelId id, EdgeLink x, int dx, EdgeLink y, int dy) {}

    /**
     * Nudges a dragged box onto whatever {@link #attach} found.
     *
     * @return the adjusted top-left, unchanged on an axis with nothing within the threshold
     */
    public static Box snap(Box moving, Map<PanelId, Box> bases, int threshold) {
        return position(moving, attach(moving, bases, threshold), bases);
    }

    /** Where an attachment puts a panel, given where its bases are. */
    public static Box position(Box moving, Attachment attachment, Map<PanelId, Box> bases) {
        int x = moving.x();
        int y = moving.y();

        Box xBase = attachment.x() == null ? null : bases.get(attachment.xBase());
        if (xBase != null) x = resolveX(attachment.x(), xBase, moving);

        Box yBase = attachment.y() == null ? null : bases.get(attachment.yBase());
        if (yBase != null) y = resolveY(attachment.y(), yBase, moving);

        return new Box(x, y, moving.width(), moving.height());
    }

    private static int resolveX(EdgeLink link, Box base, Box moving) {
        return link.resolve(base.x(), base.width(), moving.width(), PanelResolver.ATTACH_GAP);
    }

    private static int resolveY(EdgeLink link, Box base, Box moving) {
        return link.resolve(base.y(), base.height(), moving.height(), PanelResolver.ATTACH_GAP);
    }
}
