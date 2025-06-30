package net.rezolv.obsidanum.gui.forge_crucible.upgrade_render.render_types;

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
import net.rezolv.obsidanum.gui.forge_crucible.upgrade_render.ForgeCrucibleUpgradeMenu;

public class RecipeIngredientsRendererUpgrade {
    private static final ResourceLocation[] INGREDIENT_TEXTURES = {
            new ResourceLocation("obsidanum:textures/gui/forge_crucible_ingredients_no.png"),
            new ResourceLocation("obsidanum:textures/gui/forge_crucible_ingredients_yes.png")
    };
    private static final int SLOT_SIZE = 16;
    private static final int[][] SLOT_POSITIONS = {
            {119, 19},  // Слот 0 - основной предмет
            {69, 78},   // Слот 1 - первый материал
            {169, 78}   // Слот 2 - второй материал
    };

    public static void render(GuiGraphics guiGraphics, Font font,
                              ForgeCrucibleUpgradeMenu menu,
                              Level level, BlockPos pos,
                              int leftPos, int topPos) {
        ForgeCrucibleEntity crucible = getCrucible(level, pos);
        if (crucible == null) return;

        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients")) return;

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        if (ingredients.isEmpty()) return;

        for (int i = 0; i < Math.min(ingredients.size(), SLOT_POSITIONS.length); i++) {
            CompoundTag entry = ingredients.getCompound(i);
            try {
                int x = leftPos + SLOT_POSITIONS[i][0];
                int y = topPos + SLOT_POSITIONS[i][1];
                renderIngredientSlot(guiGraphics, font, menu, crucible, entry, i, x, y);
            } catch (Exception e) {
                Obsidanum.LOGGER.error("Failed to render upgrade ingredient {}: {}", i, e.getMessage());
            }
        }
    }

    private static ForgeCrucibleEntity getCrucible(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ForgeCrucibleEntity crucible ? crucible : null;
    }

    private static void renderIngredientSlot(GuiGraphics guiGraphics, Font font,
                                             ForgeCrucibleUpgradeMenu menu,
                                             ForgeCrucibleEntity crucible,
                                             CompoundTag entry,
                                             int slotIndex, int x, int y) throws Exception {
        JsonObject json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
        int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;

        ItemStack displayStack = getDisplayStackForIngredient(json, slotIndex);
        if (displayStack.isEmpty()) return;

        ItemStack slotStack = menu.getSlot(slotIndex).getItem();
        Ingredient ingredient = Ingredient.fromJson(json);
        boolean hasEnough = ingredient.test(slotStack) && slotStack.getCount() >= requiredCount;

        renderSlotBackground(guiGraphics, x, y, hasEnough);
        renderIngredientItem(guiGraphics, font, displayStack, slotStack, hasEnough, x, y, requiredCount);
    }

    private static void renderSlotBackground(GuiGraphics guiGraphics,
                                             int x, int y, boolean hasEnough) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int frame = getAnimationFrame();
        int idx = hasEnough ? 1 : 0;
        guiGraphics.blit(INGREDIENT_TEXTURES[idx], x, y, 0, frame * 16, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, 128);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    private static void renderIngredientItem(GuiGraphics guiGraphics, Font font, ItemStack displayStack,
                                             ItemStack slotStack, boolean satisfied,
                                             int x, int y, int count) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100);

        // Показываем иконку только если слот пуст
        if (slotStack.isEmpty()) {
            if (!satisfied) {
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.7f);
                guiGraphics.renderItem(displayStack, x, y);

                // Показываем требуемое количество если слот пуст
                if (count > 1) {
                    renderCountText(guiGraphics, font, x, y, count);
                }
            } else {
                guiGraphics.renderItem(slotStack, x, y);
            }
        } else {
            // Если слот не пуст, просто рисуем сам предмет из слота
            guiGraphics.renderItem(slotStack, x, y);
        }

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.pose().popPose();
    }


    private static void renderCountText(GuiGraphics guiGraphics, Font font,
                                        int x, int y, int count) {
        String text = String.valueOf(count);
        int w = font.width(text);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.pose().scale(0.75f, 0.75f, 1f);

        int tx = (int)((x + SLOT_SIZE - w * 0.75f) / 0.75f);
        int ty = (int)((y + 11) / 0.75f);

        // Outline
        guiGraphics.drawString(font, text, tx + 1, ty, 0x000000, false);
        guiGraphics.drawString(font, text, tx - 1, ty, 0x000000, false);
        guiGraphics.drawString(font, text, tx, ty + 1, 0x000000, false);
        guiGraphics.drawString(font, text, tx, ty - 1, 0x000000, false);
        // Main
        guiGraphics.drawString(font, text, tx, ty, 0xFFFFFF, false);

        guiGraphics.pose().popPose();
    }

    private static int getAnimationFrame() {
        return (int)((System.currentTimeMillis() % 1000) / 1000f * 8);
    }

    public static ItemStack getDisplayStackForIngredient(JsonObject json, int slotIndex) {
        if (json.has("item")) {
            ResourceLocation id = new ResourceLocation(json.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(id);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        }
        if (json.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            var list = ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList();
            if (list.isEmpty()) return ItemStack.EMPTY;
            int idx = (int)((System.currentTimeMillis() / 500) % list.size());
            return new ItemStack(list.get(idx));
        }
        return ItemStack.EMPTY;
    }
}
