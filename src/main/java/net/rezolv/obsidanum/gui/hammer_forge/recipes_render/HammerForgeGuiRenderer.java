package net.rezolv.obsidanum.gui.hammer_forge.recipes_render;

import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.gui.hammer_forge.recipes_render.render_types.RecipeIngredientsRenderer;
import net.rezolv.obsidanum.gui.hammer_forge.recipes_render.render_types.RecipeResultRenderer;
import net.rezolv.obsidanum.gui.hammer_forge.recipes_render.render_types.ScrollItemRenderer;

public class HammerForgeGuiRenderer {
    public static void renderBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight, ResourceLocation texture) {
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    public static void renderRecipeResult(GuiGraphics guiGraphics, Font font, ForgeCrucibleEntity blockEntity, int leftPos, int topPos) {
        RecipeResultRenderer.render(guiGraphics, font, blockEntity, leftPos, topPos);
    }

    public static void renderScrollItem(GuiGraphics guiGraphics, Font font, Level world, int x, int y, int z, int leftPos, int topPos) {
        ScrollItemRenderer.render(guiGraphics, font, world, x, y, z, leftPos, topPos);
    }

    public static void renderRecipeIngredients(GuiGraphics guiGraphics, Font font,
                                               HammerForgeGuiMenu menu, Level level,
                                               BlockPos pos, int leftPos, int topPos) {
        RecipeIngredientsRenderer.render(guiGraphics, font, menu, level, pos, leftPos, topPos);
    }

}