package net.rezolv.obsidanum.gui.forge_crucible.repair_render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ForgeCrucibleRepairRenderer {
    public static void renderBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight, ResourceLocation texture) {
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }
}