package com.cobeffectiveness.config;

/**
 * Screen corner the HUD offset is measured from.
 *
 * <p>Anchoring rather than storing absolute coordinates is what keeps a position where the user put
 * it when the window is resized or the GUI scale changes.
 */
public enum Anchor {
    TOP_LEFT(false, false),
    TOP_RIGHT(true, false),
    BOTTOM_LEFT(false, true),
    BOTTOM_RIGHT(true, true);

    private final boolean right;
    private final boolean bottom;

    Anchor(boolean right, boolean bottom) {
        this.right = right;
        this.bottom = bottom;
    }

    public boolean isRight() {
        return right;
    }

    public boolean isBottom() {
        return bottom;
    }

    public static Anchor of(boolean right, boolean bottom) {
        for (Anchor anchor : values()) {
            if (anchor.right == right && anchor.bottom == bottom) return anchor;
        }
        return TOP_RIGHT;
    }

    /**
     * The corner a panel should anchor to, given where it ended up.
     *
     * <p>Offsets are stored in scaled pixels from the anchored corner, so they only hold across GUI
     * scale changes while they stay small. Snapping to the nearest corner after a drag is what keeps
     * them small: a panel dragged toward the middle used to store a huge offset and then get clamped
     * to the opposite edge on a smaller screen.
     */
    public static Anchor nearest(int centerX, int centerY, int screenWidth, int screenHeight) {
        return of(centerX >= screenWidth / 2, centerY >= screenHeight / 2);
    }

    /** Distance from this anchor's vertical edge to the panel. */
    public int offsetXFor(int left, int width, int screenWidth) {
        return Math.max(0, right ? screenWidth - (left + width) : left);
    }

    /** Distance from this anchor's horizontal edge to the panel. */
    public int offsetYFor(int top, int height, int screenHeight) {
        return Math.max(0, bottom ? screenHeight - (top + height) : top);
    }

    public Anchor next() {
        Anchor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** Translation key for this corner, e.g. {@code cobeffectiveness.config.anchor.top_right}. */
    public String translationKey() {
        return "cobeffectiveness.config.anchor." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
