package com.cobeffectiveness.neoforge.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattleSide;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobeffectiveness.CobEffectiveness;
import com.cobeffectiveness.battle.OpponentSnapshot;
import com.cobeffectiveness.type.Type;
import net.minecraft.client.MinecraftClient;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class PokemonTypeFetcher {
    private PokemonTypeFetcher() {
    }

    public static void refreshFromCurrentBattle() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            OpponentSnapshot.clear();
            return;
        }

        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) {
            OpponentSnapshot.clear();
            return;
        }

        ClientBattleSide side1 = battle.getSide1();
        ClientBattleSide side2 = battle.getSide2();

        UUID me = mc.player.getUuid();
        boolean iAmSide1 = side1.getActors().stream().anyMatch(a -> me.equals(a.getUuid()));
        ClientBattleSide enemy = iAmSide1 ? side2 : side1;

        // Collect types across all enemy actives (1v1 = one entry; doubles = two)
        Set<Type> out = EnumSet.noneOf(Type.class);
        for (ActiveClientBattlePokemon active : enemy.getActiveClientBattlePokemon()) {
            resolveTypes(active, out);
        }

        if (out.isEmpty()) {
            OpponentSnapshot.clear();
        } else {
            OpponentSnapshot.update(out);
        }
    }

    private static void resolveTypes(ActiveClientBattlePokemon active, Set<Type> into) {
        ClientBattlePokemon bp = active.getBattlePokemon();
        if (bp == null) return;

        Species species = bp.getSpecies();

        Set<String> aspects = bp.getProperties().getAspects();
        FormData form = species.getForm(aspects);

        form.getPrimaryType();
        try {
            into.add(Type.fromCobblemon(form.getPrimaryType().getName()));
        } catch (IllegalArgumentException ex) {
            CobEffectiveness.LOGGER.warn(
                    "Unknown primary type '{}'", form.getPrimaryType().getName()
            );
        }
        if (form.getSecondaryType() != null) {
            try {
                into.add(Type.fromCobblemon(form.getSecondaryType().getName()));
            } catch (IllegalArgumentException ex) {
                com.cobeffectiveness.CobEffectiveness.LOGGER.warn(
                        "Unknown secondary type '{}'", form.getSecondaryType().getName()
                );
            }
        }
    }

}
