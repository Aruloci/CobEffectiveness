package com.cobeffectiveness.config;

/**
 * A damage multiplier the HUD can show a row for.
 *
 * <p>Colour runs green to red from the player's point of view: green is a move worth pressing, red is
 * one the opponent shrugs off. The label is always drawn as well, so colour reinforces the meaning
 * rather than carrying it - which is what keeps a red/green ramp usable for colourblind players.
 * Nothing darker than ×0 stays legible on the panel background.
 *
 * <p>Label and colour live here rather than in the config file: the set of interesting multipliers
 * is fixed by the type chart, and letting users pick colours is a larger feature than anyone asked
 * for. The config only records which of these are switched on.
 */
public enum Multiplier {
    X4(4.0, "×4", 0xFF5FD97A),
    X2(2.0, "×2", 0xFFA5D6A5),
    X_HALF(0.5, "×0.5", 0xFFE6A9A0),
    X_QUARTER(0.25, "×0.25", 0xFFD4796E),
    X0(0.0, "×0", 0xFFC05A52);

    private final double value;
    private final String label;
    private final int color;

    Multiplier(double value, String label, int color) {
        this.value = value;
        this.label = label;
        this.color = color;
    }

    public double value() {
        return value;
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }
}
