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
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.LeftCornerLevel;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

public class LeftCornerCompleteRecipe {

    public static void handleNeighborUpdate(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        Direction facing = state.getValue(ForgeCrucible.FACING);
        BlockPos expectedLeftPos = getLeftPos(pos, facing);

        if (expectedLeftPos != null && expectedLeftPos.equals(fromPos)) {
            BlockState leftBlockState = level.getBlockState(expectedLeftPos);
            BlockEntity crucibleEntity = level.getBlockEntity(pos);

            if (leftBlockState.getBlock() instanceof LeftCornerLevel &&
                    leftBlockState.getValue(LeftCornerLevel.IS_PRESSED)) {

                if (crucibleEntity instanceof ForgeCrucibleEntity crucible) {
                    if (checkAllIngredientsWithCount(crucible)) { // Изменили название метода для ясности
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

                if (slotStack.getCount() < requiredCount) {
                    return false;
                }

                if (!matchesIngredient(slotStack, json)) {
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
        // Повторная проверка — защита от изменения инвентаря между проверкой и крафтом
        if (!checkAllIngredientsWithCount(crucible)) {
            return;
        }

        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("RecipeResult", Tag.TAG_LIST)) return;

        ListTag resultList = data.getList("RecipeResult", Tag.TAG_COMPOUND);
        if (resultList.isEmpty()) return;

        ItemStack resultStack = ItemStack.of(resultList.getCompound(0));
        int resultCount = resultStack.getCount();

        // Проверка выходного слота (слот 6)
        ItemStack currentResult = crucible.itemHandler.getStackInSlot(6);
        if (!currentResult.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(currentResult, resultStack)) {
                return; // Разные предметы — не крафтим
            }
            if (currentResult.getCount() + resultCount > currentResult.getMaxStackSize()) {
                return; // Не помещается — не крафтим
            }
        }

        // Все проверки пройдены — удаляем ингредиенты
        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag ingredient = ingredients.getCompound(i);

            try {
                JsonObject json = JsonParser.parseString(ingredient.getString("IngredientJson")).getAsJsonObject();
                int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;

                crucible.itemHandler.extractItem(i, requiredCount, false);
            } catch (Exception e) {
                Obsidanum.LOGGER.error("Ошибка при извлечении предметов из слота {}: {}", i, e.getMessage());
            }
        }

        // Добавление результата
        if (currentResult.isEmpty()) {
            crucible.itemHandler.setStackInSlot(6, resultStack.copy());
        } else {
            currentResult.grow(resultCount);
            crucible.itemHandler.setStackInSlot(6, currentResult);
        }

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
