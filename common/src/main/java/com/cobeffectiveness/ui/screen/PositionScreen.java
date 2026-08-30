package com.cobeffectiveness.ui.screen;

import com.cobeffectiveness.battle.BattleView;
import com.cobeffectiveness.battle.MoveInfo;
import com.cobeffectiveness.config.CobConfig;
import com.cobeffectiveness.config.ConfigStore;
import com.cobeffectiveness.config.Multiplier;
import com.cobeffectiveness.config.PanelId;
import com.cobeffectiveness.config.PanelPlacement;
import com.cobeffectiveness.type.EffectivenessChart;
import com.cobeffectiveness.type.IconSheet;
import com.cobeffectiveness.type.Type;
import com.cobeffectiveness.ui.BattleSprites;
import com.cobeffectiveness.ui.EffectivenessHud;
import com.cobeffectiveness.ui.HudArrangement;
import com.cobeffectiveness.ui.HudModel;
import com.cobeffectiveness.ui.PanelSnap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drag the three panels to where they should live.
 *
 * <p>Picking a panel up detaches it; dropping it against another attaches it. That is the difference
 * between "these looked lined up with the sample content" and an arrangement that holds in a real
 * battle, where every panel changes size with the matchup.
 */
public class PositionScreen extends Screen {
    /**
     * Distance from the bottom for the lowest line of text. The in-game HUD keeps rendering behind
     * an open screen, so this has to clear the hotbar and the experience bar above it.
     */
    private static final int BOTTOM_HUD_CLEARANCE = 50;
    private static final int LINE_HEIGHT = 12;
    /** Light enough to keep the panels crisp and true to how they will look in a battle. */
    private static final int DIM_COLOR = 0x40000000;

    private static final List<MoveInfo> SAMPLE_MOVES = List.of(
            new MoveInfo("Rock Slide", Type.ROCK, true, true),
            new MoveInfo("Surf", Type.WATER, true, true),
            new MoveInfo("Thunderbolt", Type.ELECTRIC, true, true),
            new MoveInfo("Growl", Type.NORMAL, false, true));

    private final Screen parent;
    private final CobConfig working;
    private final EffectivenessHud hud = new EffectivenessHud();

    private final Map<PanelId, HudModel> previews = new EnumMap<>(PanelId.class);

    private PanelId dragging;
    private int grabX;
    private int grabY;

    PositionScreen(Screen parent, CobConfig working) {
        super(Component.translatable("cobeffectiveness.config.position_title"));
        this.parent = parent;
        this.working = working;
    }

    @Override
    protected void init() {
        List<Multiplier> shown = working.shownInOrder();
        if (shown.isEmpty()) shown = List.of(Multiplier.values());

        BattleView view = new BattleView(List.of(new BattleView.Side(null, List.of(
                new BattleView.Target("Charizard", EnumSet.of(Type.FIRE, Type.FLYING))))));

        previews.clear();
        previews.put(PanelId.NAME, build(view, shown, HudModel.Content.NAME));
        previews.put(PanelId.TYPES, build(view, shown, HudModel.Content.TYPES));
        previews.put(PanelId.MOVES, build(view, shown, HudModel.Content.MOVES));
    }

    private static HudModel build(BattleView view, List<Multiplier> shown, HudModel.Content content) {
        return HudModel.from(view, SAMPLE_MOVES, EffectivenessChart.current(), IconSheet.current(),
                new HudModel.Options(shown, content));
    }

    private HudArrangement arrangement() {
        return HudArrangement.of(previews, EffectivenessHud.placementsOf(working),
                EffectivenessHud.metrics(), this.width, this.height);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, DIM_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render draws the background first, so the panels have to go on top of it afterwards.
        super.render(graphics, mouseX, mouseY, partialTick);

        hud.render(graphics, arrangement());

        int hintY = this.height - BOTTOM_HUD_CLEARANCE - LINE_HEIGHT;

        graphics.drawCenteredString(this.font, BattleSprites.styled(this.title),
                this.width / 2, 16, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font,
                BattleSprites.styled(Component.translatable("cobeffectiveness.config.position_hint")),
                this.width / 2, hintY, 0xFFA0A0A0);
        graphics.drawCenteredString(this.font,
                BattleSprites.styled(Component.translatable("cobeffectiveness.config.position_hint2")),
                this.width / 2, hintY + LINE_HEIGHT, 0xFFA0A0A0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        HudArrangement current = arrangement();

        // Later ids are drawn on top, so test in reverse.
        PanelId[] ids = PanelId.values();
        for (int i = ids.length - 1; i >= 0; i--) {
            HudArrangement.Placed placed = current.get(ids[i]);
            if (placed == null || !placed.contains(mouseX, mouseY)) continue;

            dragging = ids[i];
            grabX = (int) (mouseX - placed.x());
            grabY = (int) (mouseY - placed.y());

            // Picking a panel up frees it; where it lands decides whether it attaches again.
            placement(dragging).detach();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging == null) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        HudArrangement current = arrangement();
        HudArrangement.Placed placed = current.get(dragging);
        if (placed == null) return true;

        PanelSnap.Box moved = new PanelSnap.Box(
                (int) mouseX - grabX, (int) mouseY - grabY, placed.width(), placed.height());
        PanelSnap.Box snapped = PanelSnap.snap(moved, neighbours(current), PanelSnap.THRESHOLD);

        PanelPlacement placement = placement(dragging);
        placement.offsetX = placement.anchor.isRight()
                ? this.width - (snapped.x() + snapped.width())
                : snapped.x();
        placement.offsetY = placement.anchor.isBottom()
                ? this.height - (snapped.y() + snapped.height())
                : snapped.y();
        placement.offsetX = Math.max(0, placement.offsetX);
        placement.offsetY = Math.max(0, placement.offsetY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            HudArrangement current = arrangement();
            HudArrangement.Placed placed = current.get(dragging);

            if (placed != null) {
                PanelSnap.Box box = new PanelSnap.Box(placed.x(), placed.y(), placed.width(), placed.height());

                // Always record where it physically landed. That is what an axis with no link uses,
                // and what the panel falls back to if the one it rides on is switched off later.
                PanelPlacement placement = placement(dragging);
                placement.placeAt(box.x(), box.y(), box.width(), box.height(), this.width, this.height);

                // Whatever it snapped to is what it attaches to - the same scan, so the drop cannot
                // record something other than what the screen was showing. Each axis keeps its own
                // base: a panel can rest against one neighbour sideways and another vertically.
                PanelSnap.Attachment attachment =
                        PanelSnap.attach(box, neighbours(current), PanelSnap.THRESHOLD);
                placement.attachX(attachment.xBase(), attachment.x());
                placement.attachY(attachment.yBase(), attachment.y());
            }
            dragging = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The panels the dragged one may latch onto: drawn, not itself, and not already riding on it. */
    private Map<PanelId, PanelSnap.Box> neighbours(HudArrangement current) {
        Map<PanelId, PanelSnap.Box> boxes = new EnumMap<>(PanelId.class);
        for (PanelId id : PanelId.values()) {
            if (id == dragging || wouldCycle(id)) continue;

            HudArrangement.Placed placed = current.get(id);
            if (placed != null) {
                boxes.put(id, new PanelSnap.Box(placed.x(), placed.y(), placed.width(), placed.height()));
            }
        }
        return boxes;
    }

    /** Attaching to something that already rides on the dragged panel, by either axis, would loop. */
    private boolean wouldCycle(PanelId candidate) {
        Set<PanelId> seen = EnumSet.noneOf(PanelId.class);
        Deque<PanelId> pending = new ArrayDeque<>();
        pending.push(candidate);

        while (!pending.isEmpty()) {
            PanelId id = pending.pop();
            if (id == dragging) return true;
            if (!seen.add(id)) continue;

            PanelPlacement placement = placement(id);
            if (placement.attachXTo != null) pending.push(placement.attachXTo);
            if (placement.attachYTo != null) pending.push(placement.attachYTo);
        }
        return false;
    }

    private PanelPlacement placement(PanelId id) {
        return switch (id) {
            case NAME -> working.namePanel;
            case TYPES -> working.typesPanel;
            case MOVES -> working.movesPanel;
        };
    }

    @Override
    public void onClose() {
        // Leaving a screen whose entire purpose is choosing a position should keep that position.
        ConfigStore.save(working);
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
