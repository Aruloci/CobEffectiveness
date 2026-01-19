package com.cobeffectiveness.ui;

import com.cobeffectiveness.compat.Compat;
import com.cobeffectiveness.type.Type;
import com.cobeffectiveness.type.TypeIcon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public final class IconRenderer {
    private static ResourceLocation TYPES_SHEET = resolveTypesSheet();
    public static final int GAP = 2;

    private IconRenderer() {}

    public static void drawTypes(GuiGraphics graphics, int x, int y, Collection<Type> types) {
        int i = 0;
        for (Type t : types) {
            var uv = TypeIcon.pos(t);
            if (uv == null) continue;
            graphics.blit(
                    TYPES_SHEET,
                    x + i * (TypeIcon.TILE_W + GAP), y,
                    uv.u(), uv.v(),
                    TypeIcon.TILE_W, TypeIcon.TILE_H,
                    TypeIcon.SHEET_W, TypeIcon.SHEET_H
            );
            i++;
        }
    }

    private static ResourceLocation resolveTypesSheet() {
        return ResourceLocation.fromNamespaceAndPath(Compat.getNameSpace(), Compat.TEXTURE_PATH);
    }

    public static void onResourcesReloaded() {
        TYPES_SHEET = resolveTypesSheet();
    }

    public static int rowWidth(int count) {
        if (count <= 0) return 0;
        return count * TypeIcon.TILE_W + (count - 1) * GAP;
    }

    public static int rowHeight() {
        return TypeIcon.TILE_H;
    }
}
