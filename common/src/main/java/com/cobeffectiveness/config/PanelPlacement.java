package com.cobeffectiveness.config;

/**
 * Whether one HUD panel is drawn, and where.
 *
 * <p>The offset is scaled pixels from the anchored corner, which is stable across GUI scale changes
 * because every other HUD element is positioned the same way - Cobblemon's battle tiles sit at a
 * fixed inset too. It only appears to drift when the offset is large, so each panel defaults next to
 * the thing it describes and the drag screen snaps to the nearest corner.
 */
public final class PanelPlacement {

    /**
     * How this panel is tied to the one it rides on, along a single axis.
     *
     * <p>These are exactly the four positions the drag screen snaps to, so every snap it can make is
     * a relationship that can be recorded. Recording only some of them is what left a panel dropped
     * beside another - rather than squarely under it - saved as a free panel that then drifted.
     *
     * <p>Snapping and resolving both position a panel through {@link #resolve}, so what the drag
     * screen shows and what a battle draws cannot disagree.
     */
    public enum EdgeLink {
        /** Left edges flush, or top edges flush. */
        NEAR_TO_NEAR,
        /** Right edges flush, or bottom edges flush. */
        FAR_TO_FAR,
        /** Sitting past the base: to its right, or below it. */
        AFTER,
        /** Sitting before the base: to its left, or above it. */
        BEFORE;

        /** @return where this panel's near edge goes on that axis */
        public int resolve(int basePos, int baseSize, int size, int gap) {
            return switch (this) {
                case NEAR_TO_NEAR -> basePos;
                case FAR_TO_FAR -> basePos + baseSize - size;
                case AFTER -> basePos + baseSize + gap;
                case BEFORE -> basePos - size - gap;
            };
        }
    }

    public boolean enabled = true;
    public Anchor anchor = Anchor.TOP_RIGHT;
    public int offsetX = 12;
    public int offsetY = 56;

    /**
     * What this panel rides along with, held **per axis**: the panel to measure against and how its
     * edge is tied to it. Null on an axis means that axis keeps the anchor and offset.
     *
     * <p>An anchor and an offset only preserve the anchored edge, so two panels lined up in the drag
     * screen come apart as soon as a real battle changes their size. An attachment records the
     * relationship instead of the numbers it happened to produce, and is resolved against wherever
     * the other panel actually ends up.
     *
     * <p>The two axes are independent, and may name **different** panels, because that is what the
     * drag screen can produce: it snaps each axis to the nearest edge among all the panels. One base
     * for the whole panel meant a drop that lined up against two different neighbours kept only one
     * of them, and the other edge silently went back to being free.
     */
    public PanelId attachXTo = null;
    public EdgeLink attachX = null;
    public PanelId attachYTo = null;
    public EdgeLink attachY = null;

    public PanelPlacement() {}

    public PanelPlacement(Anchor anchor, int offsetX, int offsetY) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /** Directly under the opponent's battle tiles, which is what it names. */
    public static PanelPlacement name() {
        return new PanelPlacement(Anchor.TOP_RIGHT, 12, 56);
    }

    /**
     * Rides under the name panel by default. The anchor and offset stay as the fallback for when
     * the name panel is switched off.
     */
    public static PanelPlacement types() {
        PanelPlacement placement = new PanelPlacement(Anchor.TOP_RIGHT, 12, 84);
        placement.attach(PanelId.NAME, EdgeLink.FAR_TO_FAR, EdgeLink.AFTER);
        return placement;
    }

    /** Above your move buttons, which is what it annotates. */
    public static PanelPlacement moves() {
        return new PanelPlacement(Anchor.BOTTOM_RIGHT, 12, 46);
    }

    public boolean isAttached() {
        return attachX != null || attachY != null;
    }

    public void detach() {
        attachXTo = null;
        attachX = null;
        attachYTo = null;
        attachY = null;
    }

    /** Ties both axes to the same panel. */
    public void attach(PanelId base, EdgeLink x, EdgeLink y) {
        attachX(base, x);
        attachY(base, y);
    }

    /** @param link null to leave the horizontal axis on its anchor */
    public void attachX(PanelId base, EdgeLink link) {
        attachXTo = link == null ? null : base;
        attachX = link;
    }

    /** @param link null to leave the vertical axis on its anchor */
    public void attachY(PanelId base, EdgeLink link) {
        attachYTo = link == null ? null : base;
        attachY = link;
    }

    public PanelPlacement sanitized() {
        if (anchor == null) anchor = Anchor.TOP_RIGHT;
        // A base without a link, or a link without a base, positions nothing. Drop the half that is
        // left rather than carrying a reference that does no work.
        if (attachX == null || attachXTo == null) attachX(null, null);
        if (attachY == null || attachYTo == null) attachY(null, null);
        offsetX = Math.max(0, offsetX);
        offsetY = Math.max(0, offsetY);
        return this;
    }

    public PanelPlacement copy() {
        PanelPlacement copy = new PanelPlacement(anchor, offsetX, offsetY);
        copy.enabled = enabled;
        copy.attachXTo = attachXTo;
        copy.attachX = attachX;
        copy.attachYTo = attachYTo;
        copy.attachY = attachY;
        return copy;
    }

    /**
     * Re-anchors to the nearest corner without moving the panel, keeping the offset small. Only
     * meaningful for a free panel; an attached one takes its position from its base.
     */
    public void placeAt(int left, int top, int width, int height, int screenWidth, int screenHeight) {
        Anchor nearest = Anchor.nearest(left + width / 2, top + height / 2, screenWidth, screenHeight);
        offsetX = nearest.offsetXFor(left, width, screenWidth);
        offsetY = nearest.offsetYFor(top, height, screenHeight);
        anchor = nearest;
    }
}
