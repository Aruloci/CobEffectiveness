package com.cobeffectiveness.battle;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattle;
import com.cobblemon.mod.common.client.battle.ClientBattleSide;
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Works out where Cobblemon draws its battle tiles, so the HUD can tell whether the pointer is over
 * one.
 *
 * <p>Cobblemon does not expose this: {@code BattleOverlay.drawTile} takes an {@code isHovered} flag
 * but every call site passes false, so the geometry has to be reproduced here. The x coordinate is
 * read back from {@link ActiveClientBattlePokemon#getXDisplacement()}, which Cobblemon writes each
 * frame with the slide-in animation already applied; only the y needs recomputing.
 *
 * <p>That makes this the part most likely to drift if Cobblemon changes its battle UI. Callers are
 * expected to treat an empty or nonsensical result as "cannot tell" rather than "not hovering" -
 * see {@code HoverGate}.
 */
public final class TileBounds {
    private TileBounds() {}

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    /**
     * Tiles of whichever side the HUD is describing: the enemy side for a battler, or both sides
     * for a spectator, matching what the HUD itself shows.
     *
     * @return an empty list when there is no battle or the geometry cannot be worked out
     */
    public static List<Rect> opponentTiles() {
        ClientBattle battle = CobblemonClient.INSTANCE.getBattle();
        if (battle == null) return List.of();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return List.of();

        ClientBattleSide side1 = battle.getSide1();
        ClientBattleSide side2 = battle.getSide2();
        UUID self = mc.player.getUUID();
        boolean onSide1 = isActor(side1, self);
        boolean onSide2 = isActor(side2, self);

        List<Rect> tiles = new ArrayList<>();
        if (!onSide1 && !onSide2) {
            // Spectating shows both sides, so either side's tiles should reveal the HUD.
            addSide(tiles, battle, side1, true);
            addSide(tiles, battle, side2, false);
        } else if (onSide1) {
            addSide(tiles, battle, side2, false);
        } else {
            addSide(tiles, battle, side1, true);
        }
        return List.copyOf(tiles);
    }

    private static boolean isActor(ClientBattleSide side, UUID self) {
        return side.getActors().stream().anyMatch(actor -> self.equals(actor.getUuid()));
    }

    private static void addSide(List<Rect> tiles, ClientBattle battle, ClientBattleSide side, boolean left) {
        // Cobblemon exposes this as a plain Iterable, but tile rank depends on position within it.
        List<ActiveClientBattlePokemon> active = new ArrayList<>();
        side.getActiveClientBattlePokemon().forEach(active::add);

        for (int index = 0; index < active.size(); index++) {
            int rank = left ? index : active.size() - index - 1;
            Rect rect = boundsOf(battle, active.get(index), left, rank);
            if (rect != null) tiles.add(rect);
        }
    }

    private static Rect boundsOf(ClientBattle battle, ActiveClientBattlePokemon active, boolean left, int rank) {
        if (active == null || active.getBattlePokemon() == null) return null;

        var battleType = battle.getBattleFormat().getBattleType();
        int actorsPerSide = battleType.getActorsPerSide();

        // pokemonPerSide is a Kotlin interface default getter; multiplying the two abstract
        // properties ourselves avoids depending on how that compiles.
        boolean compact = actorsPerSide * battleType.getSlotsPerActor() > 1;
        int width = compact ? BattleOverlay.COMPACT_TILE_WIDTH : BattleOverlay.TILE_WIDTH;
        int height = compact ? BattleOverlay.COMPACT_TILE_HEIGHT : BattleOverlay.TILE_HEIGHT;

        int playerNumberOffset = playerNumberOffset(active);
        int spacing = compact ? BattleOverlay.COMPACT_VERTICAL_SPACING : BattleOverlay.VERTICAL_SPACING;

        int y = BattleOverlay.VERTICAL_INSET
                + rank * spacing
                + (left ? playerNumberOffset : (actorsPerSide - 1) * 10 - playerNumberOffset);

        // Cobblemon already applied the slide-in animation to this value.
        int x = Math.round(active.getXDisplacement());

        return new Rect(x, y, width, height);
    }

    private static int playerNumberOffset(ActiveClientBattlePokemon active) {
        String showdownId = active.getActorShowdownId();
        if (showdownId == null || showdownId.length() < 2) return 0;

        char digit = showdownId.charAt(1);
        if (digit < '0' || digit > '9') return 0;
        return (Character.digit(digit, 10) - 1) / 2 * 10;
    }
}
