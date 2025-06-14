package net.rezolv.obsidanum.gui.forge_crucible.recipes_render.render_types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiMenu;

public class RecipeIngredientsRenderer {
    private static final ResourceLocation[] INGREDIENT_TEXTURES = {
            new ResourceLocation("obsidanum:textures/gui/forge_crucible_ingredients_no.png"),
            new ResourceLocation("obsidanum:textures/gui/forge_crucible_ingredients_yes.png")
    };

    private static final int SLOT_SIZE = 18;
    private static final int MAX_SLOTS = 6;
    private static final int START_X_OFFSET = 74;
    private static final int START_Y_OFFSET = 73;

    public static void render(GuiGraphics guiGraphics, Font font, ForgeCrucibleGuiMenu menu,
                              Level level, BlockPos pos, int leftPos, int topPos) {
        ForgeCrucibleEntity crucible = getCrucible(level, pos);
        if (crucible == null) return;

        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients")) return;

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        if (ingredients.isEmpty()) return;

        renderIngredients(guiGraphics, font, menu, crucible, ingredients, leftPos, topPos);
    }

    private static ForgeCrucibleEntity getCrucible(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ForgeCrucibleEntity crucible ? crucible : null;
    }

    private static void renderIngredients(GuiGraphics guiGraphics, Font font, ForgeCrucibleGuiMenu menu,
                                          ForgeCrucibleEntity crucible, ListTag ingredients,
                                          int leftPos, int topPos) {
        int startX = leftPos + START_X_OFFSET;
        int startY = topPos + START_Y_OFFSET;

        for (int i = 0; i < Math.min(MAX_SLOTS, ingredients.size()); i++) {
            CompoundTag entry = ingredients.getCompound(i);
            try {
                renderIngredientSlot(guiGraphics, font, menu, crucible, entry, i, startX, startY);
            } catch (Exception e) {
                Obsidanum.LOGGER.error("Failed to render ingredient: {}", e.getMessage());
            }
        }
    }

    private static void renderIngredientSlot(GuiGraphics guiGraphics, Font font, ForgeCrucibleGuiMenu menu,
                                             ForgeCrucibleEntity crucible, CompoundTag entry, int slotIndex,
                                             int startX, int startY) throws Exception {
        JsonObject json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
        int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;

        ItemStack displayStack = getDisplayStackForIngredient(json, slotIndex);
        if (displayStack.isEmpty()) return;
        Ingredient ingredient = getIngredientFromJson(json);

        int x = startX + slotIndex * SLOT_SIZE;
        int y = startY;
        ItemStack slotStack = menu.internal.getStackInSlot(slotIndex);
        boolean hasEnough = ingredient.test(slotStack) && slotStack.getCount() >= requiredCount;
        boolean satisfied = hasEnough;

        renderSlotBackground(guiGraphics, x, y, slotStack, hasEnough);
        renderIngredientItem(guiGraphics, font, displayStack, slotStack, satisfied, x, y, requiredCount);
    }

    private static void renderSlotBackground(GuiGraphics guiGraphics, int x, int y,
                                             ItemStack slotStack, boolean hasEnough) {
        if (!slotStack.isEmpty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 100);

            int frame = getAnimationFrame();
            int textureIndex = hasEnough ? 1 : 0;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.blit(INGREDIENT_TEXTURES[textureIndex], x, y, 0, frame * 16, 16, 16, 16, 128);
            RenderSystem.disableBlend();

            guiGraphics.pose().popPose();
        }
    }

    private static void renderIngredientItem(GuiGraphics guiGraphics, Font font, ItemStack displayStack,
                                             ItemStack slotStack, boolean satisfied,
                                             int x, int y, int requiredCount) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);

        if (!satisfied) {
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.7f);
            guiGraphics.renderItem(displayStack, x, y);
            renderCountText(guiGraphics, font, x, y, requiredCount);
        }

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.pose().popPose();
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

        // Render outline
        guiGraphics.drawString(font, countText, textX + 1, textY, 0x000000, false);
        guiGraphics.drawString(font, countText, textX - 1, textY, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY + 1, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY - 1, 0x000000, false);
        // Render main text
        guiGraphics.drawString(font, countText, textX, textY, 0xFFFFFF, false);

        guiGraphics.pose().popPose();
    }

    /**
     * Возвращает отображаемый стак для ингредиента. Если указан тег, цикляет предметы из тега.
     */
    public static ItemStack getDisplayStackForIngredient(JsonObject json, int slotIndex) {
        if (json.has("item")) {
            ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        }

        if (json.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            var items = ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList();
            if (items.isEmpty()) return ItemStack.EMPTY;
            // Циклим предметы каждые 500 мс
            int idx = (int)((System.currentTimeMillis() / 500) % items.size());
            return new ItemStack(items.get(idx));
        }

        return ItemStack.EMPTY;
    }
    public static Ingredient getIngredientFromJson(JsonObject json) {
        return Ingredient.fromJson(json);
    }
}
