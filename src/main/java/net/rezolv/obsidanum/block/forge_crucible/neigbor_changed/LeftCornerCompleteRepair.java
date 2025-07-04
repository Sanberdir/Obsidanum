package net.rezolv.obsidanum.block.forge_crucible.neigbor_changed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.LeftCornerLevel;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

public class LeftCornerCompleteRepair {

    public static void handleNeighborUpdate(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        Direction facing = state.getValue(ForgeCrucible.FACING);
        BlockPos expectedLeftPos = getLeftPos(pos, facing);

        if (expectedLeftPos != null && expectedLeftPos.equals(fromPos)) {
            BlockState leftBlockState = level.getBlockState(expectedLeftPos);
            BlockEntity crucibleEntity = level.getBlockEntity(pos);

            if (leftBlockState.getBlock() instanceof LeftCornerLevel &&
                    leftBlockState.getValue(LeftCornerLevel.IS_PRESSED)) {

                if (crucibleEntity instanceof ForgeCrucibleEntity crucible) {
                    if (checkAllIngredientsWithCount(crucible)) {
                        createCraftingResult(crucible);
                    }
                }
            }
        }
    }

    private static BlockPos getLeftPos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> pos.east();
            case SOUTH -> pos.west();
            case EAST -> pos.south();
            case WEST -> pos.north();
            default -> null;
        };
    }

    private static boolean checkAllIngredientsWithCount(ForgeCrucibleEntity crucible) {
        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients", Tag.TAG_LIST)) return false;

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);

        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag ingredient = ingredients.getCompound(i);
            ItemStack slotStack = crucible.itemHandler.getStackInSlot(i);

            try {
                JsonObject json = JsonParser.parseString(ingredient.getString("IngredientJson")).getAsJsonObject();
                int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;

                if (slotStack.getCount() < requiredCount || !matchesIngredient(slotStack, json)) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    private static boolean matchesIngredient(ItemStack stack, JsonObject json) {
        if (stack.isEmpty()) return false;

        if (json.has("item")) {
            ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
            return ForgeRegistries.ITEMS.getValue(itemId) == stack.getItem();
        }

        if (json.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            return stack.is(tag);
        }

        return false;
    }

    private static void createCraftingResult(ForgeCrucibleEntity crucible) {
        // Предмет для ремонта
        ItemStack toRepair = crucible.itemHandler.getStackInSlot(1);

        // Если нечего чинить — выходим и ничего не тратим
        if (toRepair.isEmpty() || !toRepair.isDamaged()) {
            return;
        }

        // Проверка ингредиентов
        if (!checkAllIngredientsWithCount(crucible)) return;

        CompoundTag data = crucible.getReceivedData();
        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);

        // Изъятие ингредиентов — кроме слота 1 (ремонтируемый предмет)
        for (int i = 0; i < ingredients.size(); i++) {
            if (i == 1) continue; // слот 1 не тратится
            CompoundTag ingredient = ingredients.getCompound(i);
            try {
                JsonObject json = JsonParser.parseString(ingredient.getString("IngredientJson")).getAsJsonObject();
                int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;
                crucible.itemHandler.extractItem(i, requiredCount, false);
            } catch (Exception ignored) {}
        }

        // Ремонтируем предмет
        toRepair.setDamageValue(Math.max(toRepair.getDamageValue() - 20, 0));

        // Обновление блока
        crucible.getLevel().sendBlockUpdated(
                crucible.getBlockPos(),
                crucible.getBlockState(),
                crucible.getBlockState(),
                3
        );
        crucible.setChanged();
    }


}
