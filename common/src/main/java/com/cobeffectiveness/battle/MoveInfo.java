package com.cobeffectiveness.battle;

import com.cobeffectiveness.type.Type;

/**
 * One of the player's battle moves, reduced to what the HUD needs.
 *
 * <p>Free of Cobblemon and Minecraft types so the scoring that uses it can be unit tested.
 *
 * @param type     the move's effective type, or null when it could not be resolved
 * @param damaging false for status moves, which have no effectiveness to report
 * @param usable   false when the move is disabled or out of PP
 */
public record MoveInfo(String name, Type type, boolean damaging, boolean usable) {

    /** Whether a multiplier can meaningfully be shown for this move. */
    public boolean scoreable() {
        return damaging && type != null;
    }
}
