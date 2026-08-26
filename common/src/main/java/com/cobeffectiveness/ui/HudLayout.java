package com.cobeffectiveness.ui;

import com.cobeffectiveness.config.Anchor;
import com.cobeffectiveness.type.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Measures and places the HUD panels.
 *
 * <p>Deliberately free of Minecraft types. A doubles battle and a spectated battle are both hard to
 * reach on demand in a running game, so the geometry for them is pinned by unit tests instead of by
 * looking at the screen.
 *
 * <p>Placement is resolved down to absolute coordinates here, so {@link EffectivenessHud} does no
 * arithmetic of its own. Measuring and drawing cannot disagree if only one of them can measure.
 */
public final class HudLayout {
    private static final int PAD_X = 6;
    private static final int PAD_Y = 6;
    private static final int LABEL_GAP = 4;
    private static final int ELEMENT_GAP = 4;
    /** Extra breathing room before the next opponent's header. */
    private static final int SECTION_GAP = 8;
    private static final int PANEL_GAP = 4;
    private static final int DIVIDER_H = 1;

    /** Breathing room kept either side of the panels when deciding how wide a row may be. */
    private static final int SCREEN_MARGIN = 8;
    /** A panel never squeezes below this, even on a tiny screen; clamping handles the rest. */
    private static final int MIN_INNER_WIDTH = 60;
    /**
     * Share of the screen a single panel may occupy before rows wrap. Chosen so the widest vanilla
     * row (10 icons, 138px) still fits on one line at normal scaled widths, keeping the HUD the
     * shape users already know, while the much longer Gravelmon rows fold instead of taking over
     * the screen. At Minecraft's 320px minimum scaled width even a vanilla row folds, which is the
     * only thing that can happen there short of running off the edge.
     */
    private static final float MAX_PANEL_WIDTH_FRACTION = 0.38F;

    private static final String ELLIPSIS = "...";

    private static final int HEADING_COLOR = 0xFFFFFFFF;
    private static final int SECTION_COLOR = 0xFFD0D0D0;

    private HudLayout() {}

    /** Text and icon sizes, supplied by the renderer so this class stays testable. */
    public interface Metrics {
        int textWidth(String text);

        int fontHeight();

        int iconRowWidth(int count);

        int iconHeight();
    }

    /** Screen size plus where the user has anchored the HUD. */
    public record Viewport(int width, int height, Anchor anchor, int offsetX, int offsetY) {}

    /**
     * What an element is. All three share a geometry path - text, then optional icons, then optional
     * trailing text - but they are not interchangeable.
     */
    public enum Kind { HEADING, ROW, MOVE_ROW, DIVIDER }

    /**
     * @param types         icons drawn after the text; empty for text-only elements
     * @param trailing      right hand text, used by move rows for the multiplier; empty otherwise
     * @param showLabel     false for the continuation lines of a wrapped row, which keep the label
     *                      for measurement so their icons line up but must not draw it twice
     * @param labelWidth    fixed width for the text column, so move rows line up as a table; 0 means
     *                      use the text's natural width
     * @param groupWidth    width the element is centred as: shared by the lines of a wrapped row and
     *                      by every move row in a block, so neither drifts against the other
     */
    public record Element(Kind kind, String text, int color, List<Type> types,
                          String trailing, int trailingColor,
                          boolean showLabel, int labelWidth, int groupWidth) {

        static Element heading(String text, int color, List<Type> types) {
            return new Element(Kind.HEADING, text, color, types, "", 0, true, 0, 0);
        }

        static Element row(String label, int color, List<Type> types, boolean showLabel, int groupWidth) {
            return new Element(Kind.ROW, label, color, types, "", 0, showLabel, 0, groupWidth);
        }

        static Element moveRow(String name, int nameColor, Type icon, String value, int valueColor,
                               int labelWidth, int groupWidth) {
            List<Type> types = icon == null ? List.of() : List.of(icon);
            return new Element(Kind.MOVE_ROW, name, nameColor, types, value, valueColor,
                    true, labelWidth, groupWidth);
        }

        static Element separator() {
            return new Element(Kind.DIVIDER, "", 0, List.of(), "", 0, true, 0, 0);
        }

        public boolean isDivider() {
            return kind == Kind.DIVIDER;
        }
    }

    public record PlacedElement(Element element, int x, int y, int width, int height,
                                int textY, int iconX, int iconY, int trailingX) {}

    public record Panel(int x, int y, int width, int height, List<PlacedElement> elements) {}

    public record Layout(List<Panel> panels) {
        public boolean isEmpty() {
            return panels.isEmpty();
        }
    }

    public static Layout measure(HudModel model, Metrics metrics, Viewport viewport) {
        if (model.isEmpty()) return new Layout(List.of());

        int fontH = metrics.fontHeight();
        int iconH = metrics.iconHeight();
        int panelCount = model.panels().size();
        int maxInnerWidth = maxInnerWidth(viewport, panelCount);

        List<List<Element>> contents = new ArrayList<>(panelCount);
        List<int[]> sizes = new ArrayList<>(panelCount);
        int totalW = PANEL_GAP * (panelCount - 1);
        int tallest = 0;

        for (HudModel.Panel panel : model.panels()) {
            List<Element> elements = elementsOf(panel, metrics, maxInnerWidth);
            int innerW = 0;
            int innerH = 0;

            for (int i = 0; i < elements.size(); i++) {
                innerW = Math.max(innerW, elementWidth(elements.get(i), metrics));
                innerH += elementHeight(elements.get(i), fontH, iconH);
                if (i > 0) innerH += gapBefore(elements, i);
            }

            contents.add(elements);
            sizes.add(new int[]{innerW + PAD_X * 2, innerH + PAD_Y * 2, innerW});
            totalW += innerW + PAD_X * 2;
            tallest = Math.max(tallest, innerH + PAD_Y * 2);
        }

        // Resolve the anchored offset, then clamp into the viewport. Doubles and triples stack
        // downwards, so a tall panel would otherwise hang off the bottom at a large GUI scale;
        // clamping keeps the strongest rows on screen wherever the user has put the HUD.
        int x = viewport.anchor().isRight()
                ? viewport.width() - viewport.offsetX() - totalW
                : viewport.offsetX();
        int y = viewport.anchor().isBottom()
                ? viewport.height() - viewport.offsetY() - tallest
                : viewport.offsetY();

        x = clamp(x, viewport.width() - totalW);
        y = clamp(y, viewport.height() - tallest);

        List<Panel> panels = new ArrayList<>(panelCount);
        for (int i = 0; i < contents.size(); i++) {
            int[] size = sizes.get(i);
            panels.add(place(contents.get(i), x, y, size[0], size[1], size[2], metrics, fontH, iconH));
            x += size[0] + PANEL_GAP;
        }
        return new Layout(List.copyOf(panels));
    }

    /** A following opponent's header gets more room than a plain line break. */
    private static int gapBefore(List<Element> elements, int index) {
        return elements.get(index).kind() == Kind.HEADING && index > 0 ? SECTION_GAP : ELEMENT_GAP;
    }

    /**
     * How wide a panel's content may get before rows wrap.
     *
     * <p>Without this a long row runs off the screen: with Gravel's Extended Battles loaded the x2
     * row against a Shadow type is 24 icons, which is wider than the whole screen at a large GUI
     * scale, and a spectated battle draws two of those side by side.
     */
    private static int maxInnerWidth(Viewport viewport, int panelCount) {
        int usable = viewport.width() - SCREEN_MARGIN * 2 - PANEL_GAP * (panelCount - 1);
        int fitAll = usable / Math.max(1, panelCount);
        int share = Math.round(viewport.width() * MAX_PANEL_WIDTH_FRACTION);

        return Math.max(MIN_INNER_WIDTH, Math.min(fitAll, share) - PAD_X * 2);
    }

    private static Panel place(List<Element> elements, int panelX, int panelY, int panelW, int panelH,
                               int innerW, Metrics metrics, int fontH, int iconH) {
        List<PlacedElement> placed = new ArrayList<>(elements.size());
        int innerX = panelX + PAD_X;
        int cursorY = panelY + PAD_Y;

        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (i > 0) cursorY += gapBefore(elements, i);

            int height = elementHeight(element, fontH, iconH);

            if (element.isDivider()) {
                placed.add(new PlacedElement(element, innerX, cursorY, innerW, height, cursorY, 0, 0, 0));
            } else {
                int labelW = element.labelWidth() > 0
                        ? element.labelWidth()
                        : metrics.textWidth(element.text());
                int width = elementWidth(element, metrics);
                // Centre every line of a wrapped row, and every move row, as the same block so their
                // columns line up instead of each row centring on its own content.
                int block = element.groupWidth() > 0 ? element.groupWidth() : width;
                int x = innerX + (innerW - block) / 2;

                int iconX = x + labelW + (labelW > 0 ? LABEL_GAP : 0);
                // A move row always reserves one icon slot, so a move whose type has no icon still
                // leaves its multiplier in the same column as every other row.
                int iconSpan = element.kind() == Kind.MOVE_ROW
                        ? metrics.iconRowWidth(1)
                        : metrics.iconRowWidth(element.types().size());

                placed.add(new PlacedElement(element, x, cursorY, width, height,
                        cursorY + (height - fontH) / 2 + 1,
                        iconX,
                        cursorY + (height - iconH) / 2,
                        iconX + iconSpan + LABEL_GAP));
            }

            cursorY += height;
        }
        return new Panel(panelX, panelY, panelW, panelH, List.copyOf(placed));
    }

    private static List<Element> elementsOf(HudModel.Panel panel, Metrics metrics, int maxInnerWidth) {
        List<Element> elements = new ArrayList<>();

        if (panel.label() != null) {
            elements.add(Element.heading("vs " + panel.label(), HEADING_COLOR, List.of()));
        }

        for (int i = 0; i < panel.sections().size(); i++) {
            HudModel.Section section = panel.sections().get(i);
            boolean header = !section.types().isEmpty();

            if (header) {
                // Name and types. A rule follows only when something is drawn under it - the opponent
                // panel is just the names, so a trailing rule there would be a line to nowhere.
                int iconsW = metrics.iconRowWidth(section.types().size());
                String name = fit(section.label(), metrics, maxInnerWidth - iconsW - LABEL_GAP);
                elements.add(Element.heading(name, SECTION_COLOR, section.types()));
                if (!section.rows().isEmpty() || !section.moves().isEmpty()) {
                    elements.add(Element.separator());
                }
            } else {
                // Header switched off: fall back to the old shape, which only names an opponent
                // when there is more than one to tell apart.
                if (i > 0) elements.add(Element.separator());
                if (panel.sections().size() > 1) {
                    elements.add(Element.heading(fit(section.label(), metrics, maxInnerWidth),
                            SECTION_COLOR, List.of()));
                }
            }

            for (HudModel.Row row : section.rows()) {
                addWrapped(elements, row, metrics, maxInnerWidth);
            }
            addMoveRows(elements, section, metrics, maxInnerWidth);
        }
        return elements;
    }

    /** Lays the move list out as a table: name column, icon column, multiplier column. */
    private static void addMoveRows(List<Element> out, HudModel.Section section, Metrics metrics,
                                    int maxInnerWidth) {
        if (section.moves().isEmpty()) return;

        int iconW = metrics.iconRowWidth(1);
        int valueW = 0;
        for (HudModel.MoveRow move : section.moves()) {
            valueW = Math.max(valueW, metrics.textWidth(move.value()));
        }

        int nameBudget = maxInnerWidth - LABEL_GAP - iconW - LABEL_GAP - valueW;
        List<String> names = new ArrayList<>(section.moves().size());
        int nameW = 0;
        for (HudModel.MoveRow move : section.moves()) {
            String name = fit(move.name(), metrics, nameBudget);
            names.add(name);
            nameW = Math.max(nameW, metrics.textWidth(name));
        }

        int rowWidth = nameW + LABEL_GAP + iconW + LABEL_GAP + valueW;
        for (int i = 0; i < section.moves().size(); i++) {
            HudModel.MoveRow move = section.moves().get(i);
            out.add(Element.moveRow(names.get(i), move.nameColor(), move.type(), move.value(),
                    move.valueColor(), nameW, rowWidth));
        }
    }

    /** Splits one row across as many lines as it takes to stay within the width budget. */
    private static void addWrapped(List<Element> out, HudModel.Row row, Metrics metrics, int maxInnerWidth) {
        List<Type> types = row.types();
        int labelW = metrics.textWidth(row.label());
        int available = maxInnerWidth - labelW - LABEL_GAP;
        int perLine = iconsThatFit(metrics, available, types.size());

        int groupWidth = labelW + LABEL_GAP + metrics.iconRowWidth(Math.min(perLine, types.size()));

        for (int start = 0; start < types.size(); start += perLine) {
            List<Type> line = types.subList(start, Math.min(start + perLine, types.size()));
            out.add(Element.row(row.label(), row.color(), List.copyOf(line), start == 0, groupWidth));
        }
    }

    /** Largest icon count whose row still fits the budget; never zero, or wrapping would not end. */
    private static int iconsThatFit(Metrics metrics, int available, int total) {
        int fits = 0;
        while (fits < total && metrics.iconRowWidth(fits + 1) <= available) {
            fits++;
        }
        return Math.max(1, fits);
    }

    /**
     * Shortens text to fit. Rows wrap, but a heading or a move name cannot, so without this a long
     * name would push the panel straight past the width budget.
     */
    private static String fit(String text, Metrics metrics, int available) {
        if (text == null) return "";
        if (metrics.textWidth(text) <= available) return text;

        int room = available - metrics.textWidth(ELLIPSIS);
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (metrics.textWidth(text.substring(0, i + 1)) > room) break;
            kept.append(text.charAt(i));
        }
        return kept + ELLIPSIS;
    }

    /** Keeps a coordinate on screen; if the content is larger than the screen, pins it to 0. */
    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, Math.max(0, max)));
    }

    private static int elementWidth(Element element, Metrics metrics) {
        if (element.isDivider()) return 0;
        // Move rows are uniform by construction, so their block width is their width.
        if (element.kind() == Kind.MOVE_ROW) return element.groupWidth();

        int width = metrics.textWidth(element.text());
        if (!element.types().isEmpty()) {
            if (width > 0) width += LABEL_GAP;
            width += metrics.iconRowWidth(element.types().size());
        }
        return width;
    }

    private static int elementHeight(Element element, int fontH, int iconH) {
        if (element.isDivider()) return DIVIDER_H;
        if (element.kind() == Kind.MOVE_ROW) return Math.max(fontH, iconH);
        if (element.types().isEmpty()) return fontH;
        return Math.max(fontH, iconH);
    }
}
