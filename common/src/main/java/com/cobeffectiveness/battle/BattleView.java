package com.cobeffectiveness.battle;

import com.cobeffectiveness.type.Type;

import java.util.List;
import java.util.Set;

/**
 * What the HUD needs to know about the current battle, expressed without Cobblemon or Minecraft
 * types. Being a plain value it compares cheaply between frames, which is what lets the HUD skip
 * recomputing effectiveness while nothing has changed.
 */
public record BattleView(List<Side> sides) {

    /**
     * One set of Pokemon to work out effectiveness against.
     *
     * @param label heading for the side, or null when the viewer is a battler and the side is
     *              simply "the opponent"
     */
    public record Side(String label, List<Target> targets) {}

    /**
     * A single active opposing Pokemon. Targets stay separate on purpose: merging the types of
     * every active opponent describes a Pokemon that does not exist, which is what made doubles
     * report nonsense.
     */
    public record Target(String label, Set<Type> types) {}

    public boolean isEmpty() {
        return sides.isEmpty();
    }
}
