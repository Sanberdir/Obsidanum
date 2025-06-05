package net.rezolv.obsidanum.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.RightForgeScroll;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.entity.RightForgeScrollEntity;
import net.rezolv.obsidanum.block.enum_blocks.ScrollType;
import net.rezolv.obsidanum.item.ItemsObs;

public class HammerForgeGuiRenderer {
    public static void renderBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight, ResourceLocation texture) {
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    public static void renderRecipeResult(GuiGraphics guiGraphics, Font font, ForgeCrucibleEntity blockEntity, int leftPos, int topPos) {
        if (blockEntity == null) return;

        CompoundTag data = blockEntity.getReceivedData();
        if (!data.contains("RecipeResult", Tag.TAG_LIST)) return;

        ListTag resultList = data.getList("RecipeResult", Tag.TAG_COMPOUND);
        if (resultList.isEmpty()) return;

        ItemStack resultStack = ItemStack.of(resultList.getCompound(0));
        if (resultStack.isEmpty()) return;

        // Координаты слота
        int xPos = leftPos + 79;
        int yPos = topPos + 26;

        guiGraphics.pose().pushPose();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.7F);

        guiGraphics.renderItem(resultStack, xPos, yPos);
        guiGraphics.renderItemDecorations(font, resultStack, xPos, yPos); // Теперь font передаётся явно

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.pose().popPose();
    }
    public static void renderScrollItem(GuiGraphics guiGraphics, Font font, Level world, int x, int y, int z, int leftPos, int topPos) {
        BlockEntity blockEntity = world.getBlockEntity(new BlockPos(x, y, z));
        if (!(blockEntity instanceof ForgeCrucibleEntity crucible)) {
            return;
        }

        Direction facing = crucible.getBlockState().getValue(ForgeCrucible.FACING);
        BlockPos scrollPos = switch (facing) {
            case NORTH -> crucible.getBlockPos().west();
            case SOUTH -> crucible.getBlockPos().east();
            case EAST -> crucible.getBlockPos().north();
            case WEST -> crucible.getBlockPos().south();
            default -> null;
        };

        if (scrollPos == null) return;

        BlockEntity scrollEntity = world.getBlockEntity(scrollPos);
        if (!(scrollEntity instanceof RightForgeScrollEntity scroll)) {
            return;
        }

        ScrollType scrollType = world.getBlockState(scrollPos).getValue(RightForgeScroll.TYPE_SCROLL);
        if (scrollType == ScrollType.NONE) {
            return;
        }

        ItemStack scrollStack = getScrollItemStack(scrollType);
        CompoundTag nbt = scroll.getScrollNBT();
        if (!nbt.isEmpty()) {
            scrollStack.setTag(nbt.copy());
        }

        int scrollX = leftPos + 80;
        int scrollY = topPos + 105;

        guiGraphics.renderItem(scrollStack, scrollX, scrollY);
        guiGraphics.renderItemDecorations(font, scrollStack, scrollX, scrollY);
    }

    public static void renderRecipeIngredients(GuiGraphics guiGraphics, Font font, HammerForgeGuiMenu menu, Level level, BlockPos pos, int leftPos, int topPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ForgeCrucibleEntity crucible)) return;

        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients")) return;

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        if (ingredients.isEmpty()) return;

        final int startX = leftPos + 35;
        final int startY = topPos + 73;
        final int slotSize = 18;
        final int maxSlots = 6;

        ResourceLocation[] TEXTURES = {
                new ResourceLocation("obsidanum:textures/gui/hammer_forge_ingredients_no.png"),
                new ResourceLocation("obsidanum:textures/gui/hammer_forge_ingredients_yes.png")
        };

        for (int i = 0; i < Math.min(maxSlots, ingredients.size()); i++) {
            CompoundTag entry = ingredients.getCompound(i);

            try {
                JsonObject json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
                int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;
                ItemStack stack = getDisplayStackForIngredient(json);
                if (stack.isEmpty()) continue;

                int x = startX + i * slotSize;
                int y = startY;
                ItemStack slotStack = menu.internal.getStackInSlot(i);
                boolean hasEnough = slotStack.getCount() >= requiredCount;
                boolean satisfied = crucible.isIngredientSatisfied(i);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 100);

                // Анимированная текстура
                int frame = (int)((System.currentTimeMillis() % 1000) / 1000f * 8);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(TEXTURES[hasEnough ? 1 : 0], x, y, 0, frame * 16, 16, 16, 16, 128);
                RenderSystem.disableBlend();

                guiGraphics.setColor(1.0f, 1.0f, 1.0f, satisfied ? 1.0f : 0.7f);
                guiGraphics.renderItem(stack, x, y);

                if (!satisfied && slotStack.isEmpty()) {
                    renderCountText(guiGraphics, font, x, y, requiredCount);
                }

                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                guiGraphics.pose().popPose();
            } catch (Exception e) {
                Obsidanum.LOGGER.error("Failed to render ingredient: {}", e.getMessage());
            }
        }
    }

    private static void renderCountText(GuiGraphics guiGraphics, Font font, int x, int y, int count) {
        String countText = String.valueOf(count);
        int textWidth = font.width(countText);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);

        int textX = (int)((x + 16 - textWidth * 0.75f) / 0.75f);
        int textY = (int)((y + 11) / 0.75f);

        // Рендерим контур
        guiGraphics.drawString(font, countText, textX + 1, textY, 0x000000, false);
        guiGraphics.drawString(font, countText, textX - 1, textY, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY + 1, 0x000000, false);
        guiGraphics.drawString(font, countText, textX, textY - 1, 0x000000, false);
        // Рендерим основной текст
        guiGraphics.drawString(font, countText, textX, textY, 0xFFFFFF, false);

        guiGraphics.pose().popPose();
    }

    public static ItemStack getDisplayStackForIngredient(JsonObject json) {
        if (json.has("item")) {
            ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        }

        if (json.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);

            return ForgeRegistries.ITEMS.tags()
                    .getTag(tag)
                    .stream()
                    .findFirst()
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getScrollItemStack(ScrollType type) {
        return switch (type) {
            case NETHER -> ItemsObs.NETHER_PLAN.get().getDefaultInstance();
            case ORDER -> ItemsObs.ORDER_PLAN.get().getDefaultInstance();
            case CATACOMBS -> ItemsObs.CATACOMBS_PLAN.get().getDefaultInstance();
            case UPDATE -> ItemsObs.UPGRADE_PLAN.get().getDefaultInstance();
            default -> ItemStack.EMPTY;
        };
    }
}