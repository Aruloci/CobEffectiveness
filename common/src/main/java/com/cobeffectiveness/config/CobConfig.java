package com.cobeffectiveness.config;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * User settings. Plain mutable fields so Gson can read and write it directly.
 *
 * <p>Treated as immutable once published: the config screen edits a {@link #copy()} and commits the
 * whole thing, so the render thread never sees a half-applied change.
 */
public final class CobConfig {

    /** Who you are fighting: name and their own types. */
    public PanelPlacement namePanel = PanelPlacement.name();

    /** Type effectiveness, placed under the name panel. */
    public PanelPlacement typesPanel = PanelPlacement.types();

    /** Your own moves, placed above your move buttons. */
    public PanelPlacement movesPanel = PanelPlacement.moves();

    /** Draw only while the pointer is over one of the opponent tiles. */
    public boolean hoverOnly = false;

    /** Defaults match the behaviour before this setting existed. */
    public List<Multiplier> shown = new ArrayList<>(List.of(Multiplier.X4, Multiplier.X2));

    public boolean isShown(Multiplier multiplier) {
        return shown.contains(multiplier);
    }

    public void setShown(Multiplier multiplier, boolean on) {
        shown.remove(multiplier);
        if (on) shown.add(multiplier);
        sortShown();
    }

    /** Rows in the order they should be drawn: strongest multiplier first. */
    public List<Multiplier> shownInOrder() {
        sortShown();
        return List.copyOf(shown);
    }

    private void sortShown() {
        shown.sort((a, b) -> Double.compare(b.value(), a.value()));
    }

    /**
     * Repairs anything a hand-edited or older config file might contain: unknown enum names arrive
     * as nulls from Gson, duplicates would draw a row twice, and a null list would NPE the renderer.
     */
    public CobConfig sanitized() {
        namePanel = namePanel == null ? PanelPlacement.name() : namePanel.sanitized();
        typesPanel = typesPanel == null ? PanelPlacement.types() : typesPanel.sanitized();
        movesPanel = movesPanel == null ? PanelPlacement.moves() : movesPanel.sanitized();

        if (shown == null) {
            shown = new ArrayList<>(List.of(Multiplier.X4, Multiplier.X2));
        } else {
            Set<Multiplier> unique = EnumSet.noneOf(Multiplier.class);
            for (Multiplier multiplier : shown) {
                if (multiplier != null) unique.add(multiplier);
            }
            shown = new ArrayList<>(unique);
        }

        sortShown();
        return this;
    }

    /** Restores every setting. There is no Cancel, so this is the way back from a bad edit. */
    public void resetToDefaults() {
        CobConfig defaults = new CobConfig();
        namePanel = defaults.namePanel;
        typesPanel = defaults.typesPanel;
        movesPanel = defaults.movesPanel;
        hoverOnly = defaults.hoverOnly;
        shown = new ArrayList<>(defaults.shown);
    }

    public CobConfig copy() {
        CobConfig copy = new CobConfig();
        copy.namePanel = namePanel.copy();
        copy.typesPanel = typesPanel.copy();
        copy.movesPanel = movesPanel.copy();
        copy.hoverOnly = hoverOnly;
        copy.shown = new ArrayList<>(shown);
        return copy;
    }
}
