package com.cobeffectiveness.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattleActor;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattleSide;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobeffectiveness.CobEffectivenessMod;
import com.cobeffectiveness.type.Type;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Reads Cobblemon's client battle state into a {@link BattleView}. */
public final class BattleReader {
    private BattleReader() {}

    /** @return the current battle as a view, or null when there is nothing to show */
    public static BattleView read() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) return null;

        ClientBattleSide side1 = battle.getSide1();
        ClientBattleSide side2 = battle.getSide2();
        UUID self = mc.player.getUUID();

        boolean onSide1 = isActor(side1, self);
        boolean onSide2 = isActor(side2, self);

        if (onSide1 || onSide2) {
            List<BattleView.Target> targets = targetsOf(onSide1 ? side2 : side1);
            return targets.isEmpty()
                    ? null
                    : new BattleView(List.of(new BattleView.Side(null, targets)));
        }

        // Spectating: neither side is ours, so show both rather than guessing at one. Previously
        // the "am I on side 1?" test failed and side 1 was silently presented as the opponent.
        List<BattleView.Side> sides = new ArrayList<>(2);
        addSide(sides, side1);
        addSide(sides, side2);
        return sides.isEmpty() ? null : new BattleView(List.copyOf(sides));
    }

    private static void addSide(List<BattleView.Side> sides, ClientBattleSide side) {
        List<BattleView.Target> targets = targetsOf(side);
        if (!targets.isEmpty()) {
            sides.add(new BattleView.Side(nameOf(side), targets));
        }
    }

    private static boolean isActor(ClientBattleSide side, UUID self) {
        return side.getActors().stream().anyMatch(actor -> self.equals(actor.getUuid()));
    }

    private static String nameOf(ClientBattleSide side) {
        return side.getActors().stream()
                .map(ClientBattleActor::getDisplayName)
                .map(name -> name.getString())
                .collect(Collectors.joining(" & "));
    }

    private static List<BattleView.Target> targetsOf(ClientBattleSide side) {
        List<BattleView.Target> targets = new ArrayList<>();
        for (ActiveClientBattlePokemon active : side.getActiveClientBattlePokemon()) {
            ClientBattlePokemon pokemon = active == null ? null : active.getBattlePokemon();
            if (pokemon == null) continue;

            Set<Type> types = typesOf(pokemon);
            if (types.isEmpty()) continue;

            targets.add(new BattleView.Target(pokemon.getDisplayName().getString(), types));
        }
        return List.copyOf(targets);
    }

    private static Set<Type> typesOf(ClientBattlePokemon pokemon) {
        Set<Type> types = EnumSet.noneOf(Type.class);
        try {
            Species species = pokemon.getSpecies();
            if (species == null || pokemon.getProperties() == null) return types;

            FormData form = species.getForm(pokemon.getProperties().getAspects());
            if (form == null) return types;

            addType(types, form.getPrimaryType() == null ? null : form.getPrimaryType().getName());
            addType(types, form.getSecondaryType() == null ? null : form.getSecondaryType().getName());
        } catch (RuntimeException ex) {
            // Unusual or modded forms can throw on lookup. Skip the Pokemon rather than letting one
            // bad entry take the whole HUD down.
            CobEffectivenessMod.logDebug("Could not resolve types for a battle Pokemon", ex);
        }
        return types;
    }

    private static void addType(Set<Type> types, String name) {
        if (name == null) return;

        Type type = Type.fromName(name);
        if (type == null) {
            CobEffectivenessMod.logDebug("Unknown Cobblemon type '{}'", name);
            return;
        }
        types.add(type);
    }
}
