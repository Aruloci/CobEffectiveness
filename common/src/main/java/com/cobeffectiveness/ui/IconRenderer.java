package com.cobeffectiveness.ui;

import com.cobeffectiveness.compat.Compat;
import com.cobeffectiveness.type.IconSheet;
import com.cobeffectiveness.type.Type;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class IconRenderer {
    public static final int GAP = 2;

    /** Owned by Cobblemon or Gravelmon, so it is re-resolved whenever resources reload. */
    private static volatile ResourceLocation typesSheet = resolve();

    private IconRenderer() {}

    public static void draw(GuiGraphics graphics, IconSheet sheet, int x, int y, List<Type> types) {
        ResourceLocation texture = typesSheet;
        int drawn = 0;

        for (Type type : types) {
            IconSheet.Pos uv = sheet.pos(type);
            if (uv == null) continue;

            graphics.blit(
                    texture,
                    x + drawn * (sheet.tileW() + GAP), y,
                    uv.u(), uv.v(),
                    sheet.tileW(), sheet.tileH(),
                    sheet.sheetW(), sheet.sheetH()
            );
            drawn++;
        }
    }

    public static int rowWidth(IconSheet sheet, int count) {
        if (count <= 0) return 0;
        return count * sheet.tileW() + (count - 1) * GAP;
    }

    public static void onResourcesReloaded() {
        typesSheet = resolve();
    }

    private static ResourceLocation resolve() {
        return ResourceLocation.fromNamespaceAndPath(Compat.namespace(), Compat.TEXTURE_PATH);
    }
}
