package com.cobeffectiveness.battle;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.moves.categories.DamageCategories;
import com.cobblemon.mod.common.battles.InBattleMove;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads the player's current battle moves.
 *
 * <p>Cobblemon only sends the moveset while a choice is pending, so reading it directly would make
 * the HUD change shape every turn. The last moveset is therefore cached against the Pokemon it
 * belongs to and reused until that Pokemon leaves the field.
 *
 * <p>Kept deliberately thin: everything that can be decided without Cobblemon lives in the model.
 */
public final class MoveReader {

    private static List<MoveInfo> cached = List.of();
    private static UUID cachedFor = null;

    private MoveReader() {}

    /** @return the player's moves, or an empty list when they are not known */
    public static List<MoveInfo> currentMoves() {
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) {
            clear();
            return List.of();
        }

        try {
            SingleActionRequest request = battle.getFirstUnansweredRequest();
            if (request != null && request.getMoveSet() != null) {
                ClientBattlePokemon pokemon = request.getActivePokemon().getBattlePokemon();
                if (pokemon != null) {
                    cached = read(request, pokemon);
                    cachedFor = pokemon.getUuid();
                    return cached;
                }
            }

            // No request pending: keep showing the last moveset while the same Pokemon is out.
            return stillOut(battle) ? cached : List.of();
        } catch (RuntimeException ex) {
            CobEffectivenessMod.logDebug("Could not read the player's battle moves", ex);
            return List.of();
        }
    }

    public static void clear() {
        cached = List.of();
        cachedFor = null;
    }

    /** True while the Pokemon the cache belongs to is still an active battler. */
    private static boolean stillOut(ClientBattle battle) {
        if (cachedFor == null) return false;

        for (var side : List.of(battle.getSide1(), battle.getSide2())) {
            for (var active : side.getActiveClientBattlePokemon()) {
                ClientBattlePokemon pokemon = active == null ? null : active.getBattlePokemon();
                if (pokemon != null && cachedFor.equals(pokemon.getUuid())) return true;
            }
        }
        return false;
    }

    private static List<MoveInfo> read(SingleActionRequest request, ClientBattlePokemon battlePokemon) {
        Pokemon owner = request.getActivePokemon().getActor().getPokemon().stream()
                .filter(p -> p.getUuid().equals(battlePokemon.getUuid()))
                .findFirst()
                .orElse(null);

        List<MoveInfo> moves = new ArrayList<>();
        for (InBattleMove move : request.getMoveSet().getMoves()) {
            moves.add(describe(move, owner));
        }
        return List.copyOf(moves);
    }

    private static MoveInfo describe(InBattleMove move, Pokemon owner) {
        // An unrecognised id yields a dummy template rather than throwing; it reports as a status
        // move, so it degrades to a dashed row.
        var template = Moves.getByNameOrDummy(move.getId());

        // The same call Cobblemon's own move buttons use, so the type matches the button colour.
        Type type = Type.fromName(template.getEffectiveElementalType(owner).getName());
        boolean damaging = !template.getDamageCategory().equals(DamageCategories.INSTANCE.getSTATUS());
        boolean usable = !move.getDisabled() && move.getPp() > 0;

        return new MoveInfo(template.getDisplayName().getString(), type, damaging, usable);
    }
}
