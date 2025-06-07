package net.rezolv.obsidanum.gui.hammer_forge.recipes_render.render_types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

public class RecipeResultRenderer {
    private static final ResourceLocation[] RESULT_TEXTURES = {
            new ResourceLocation("obsidanum:textures/gui/hammer_forge_res_no.png"),
            new ResourceLocation("obsidanum:textures/gui/hammer_forge_res_yes.png")
    };

    public static void render(GuiGraphics guiGraphics, Font font, ForgeCrucibleEntity blockEntity, int leftPos, int topPos) {
        if (blockEntity == null) return;

        int xPos = leftPos + 79;
        int yPos = topPos + 26;
        ItemStack currentResult = blockEntity.itemHandler.getStackInSlot(6);
        CompoundTag data = blockEntity.getReceivedData();

        if (!data.contains("Ingredients")) {
            renderSimpleResult(guiGraphics, font, currentResult, xPos, yPos);
            return;
        }

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        if (ingredients.isEmpty()) {
            renderSimpleResult(guiGraphics, font, currentResult, xPos, yPos);
            return;
        }

        IngredientCheckResult checkResult = checkIngredients(blockEntity, ingredients);
        if (checkResult.allEmpty()) {
            renderEmptyState(guiGraphics, font, currentResult, data, xPos, yPos);
            return;
        }

        renderAnimatedResult(guiGraphics, font, currentResult, data, xPos, yPos, checkResult.allSatisfied());
    }

    private static IngredientCheckResult checkIngredients(ForgeCrucibleEntity blockEntity, ListTag ingredients) {
        boolean allEmpty = true;
        boolean allSatisfied = true;

        for (int i = 0; i < Math.min(6, ingredients.size()); i++) {
            CompoundTag entry = ingredients.getCompound(i);
            JsonObject json;
            try {
                json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
            } catch (Exception e) {
                Obsidanum.LOGGER.error("Error parsing ingredient JSON: {}", e.getMessage());
                continue;
            }

            int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;
            ItemStack stackInSlot = blockEntity.itemHandler.getStackInSlot(i);

            if (!stackInSlot.isEmpty()) {
                allEmpty = false;
            }

            if (stackInSlot.getCount() < requiredCount) {
                allSatisfied = false;
            }
        }

        return new IngredientCheckResult(allEmpty, allSatisfied);
    }

    private static void renderSimpleResult(GuiGraphics guiGraphics, Font font, ItemStack currentResult, int xPos, int yPos) {
        if (!currentResult.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 100);

            int frame = getAnimationFrame();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(RESULT_TEXTURES[1], xPos - 4, yPos - 4, 0, frame * 24, 24, 24, 24, 192);
            RenderSystem.disableBlend();

            guiGraphics.renderItem(currentResult, xPos, yPos);
            guiGraphics.renderItemDecorations(font, currentResult, xPos, yPos);

            guiGraphics.pose().popPose();
        }
    }

    private static void renderEmptyState(GuiGraphics guiGraphics, Font font, ItemStack currentResult, CompoundTag data, int xPos, int yPos) {
        if (!currentResult.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 100);
            guiGraphics.renderItem(currentResult, xPos, yPos);
            guiGraphics.renderItemDecorations(font, currentResult, xPos, yPos);
            guiGraphics.pose().popPose();
        } else if (data.contains("RecipeResult", Tag.TAG_LIST)) {
            renderResultPreview(guiGraphics, font, data, xPos, yPos);
        }
    }

    private static void renderAnimatedResult(GuiGraphics guiGraphics, Font font,
                                             ItemStack currentResult, CompoundTag data,
                                             int xPos, int yPos, boolean allSatisfied) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);

        int textureIndex = allSatisfied ? 1 : 0;
        int frame = getAnimationFrame();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(RESULT_TEXTURES[textureIndex], xPos - 4, yPos - 4, 0, frame * 24, 24, 24, 24, 192);
        RenderSystem.disableBlend();

        if (!currentResult.isEmpty()) {
            guiGraphics.renderItem(currentResult, xPos, yPos);
            guiGraphics.renderItemDecorations(font, currentResult, xPos, yPos);
        } else {
            renderResultPreview(guiGraphics, font, data, xPos, yPos);
        }

        guiGraphics.pose().popPose();
    }

    private static void renderResultPreview(GuiGraphics guiGraphics, Font font, CompoundTag data, int xPos, int yPos) {
        ListTag resultList = data.getList("RecipeResult", Tag.TAG_COMPOUND);
        if (!resultList.isEmpty()) {
            ItemStack resultStack = ItemStack.of(resultList.getCompound(0));
            if (!resultStack.isEmpty()) {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.7F);
                guiGraphics.renderItem(resultStack, xPos, yPos);
                if (resultStack.getCount() > 1) {
                    renderCountText(guiGraphics, font, xPos, yPos, resultStack.getCount());
                }
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private static int getAnimationFrame() {
        return (int)((System.currentTimeMillis() % 1000) / 1000f * 8);
    }

    private static void renderCountText(GuiGraphics guiGraphics, Font font, int x, int y, int count) {
        String countText = String.valueOf(count);
        int textWidth = font.width(countText);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);

        int textX = (int)((x + 16 - textWidth * 0.75f) / 0.75f);
        int textY = (int)((y + 11) / 0.75f);

        guiGraphics.drawString(font, countText, textX + 1, textY, 0x000000, false);
        guiGraphics.drawString(font, countText, textX - 1, textY, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY + 1, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY - 1, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY, 0xFFFFFF, false);

        guiGraphics.pose().popPose();
    }

    private record IngredientCheckResult(boolean allEmpty, boolean allSatisfied) {}
}
