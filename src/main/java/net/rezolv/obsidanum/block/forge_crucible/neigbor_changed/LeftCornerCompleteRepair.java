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
                    if (canRepair(crucible)) {
                        startRepairProcess(crucible);
                    }
                }
            }
        }
    }
    private static boolean canRepair(ForgeCrucibleEntity crucible) {
        ItemStack toRepair = crucible.itemHandler.getStackInSlot(1);
        ItemStack ingredientsStack = crucible.itemHandler.getStackInSlot(0);

        return !toRepair.isEmpty() &&
                toRepair.isDamaged() &&
                !ingredientsStack.isEmpty() &&
                checkAllIngredientsWithCount(crucible);
    }

    private static void startRepairProcess(ForgeCrucibleEntity crucible) {
        CompoundTag data = crucible.getReceivedData();
        data.putBoolean("RepairMode", true); // Устанавливаем флаг починки
        crucible.receiveScrollData(data); // Обновляем данные

        int hammerStrikes = data.contains("HammerStrikes", Tag.TAG_INT) ?
                data.getInt("HammerStrikes") : 3;

        crucible.startCrafting(hammerStrikes);
    }

    public static void completeRepair(ForgeCrucibleEntity crucible) {
        ItemStack toRepair = crucible.itemHandler.getStackInSlot(1);
        ItemStack ingredientsStack = crucible.itemHandler.getStackInSlot(0);

        if (!canRepair(crucible)) return;

        int damageToRepair = toRepair.getDamageValue();
        int repairPerIngredient = 20;
        int availableIngredients = ingredientsStack.getCount();
        int neededIngredients = (int) Math.ceil((double)damageToRepair / repairPerIngredient);
        int usedIngredients = Math.min(availableIngredients, neededIngredients);

        // Применяем починку
        toRepair.setDamageValue(Math.max(damageToRepair - (usedIngredients * repairPerIngredient), 0));

        // Удаляем использованные ингредиенты
        crucible.itemHandler.extractItem(0, usedIngredients, false);

        // Сбрасываем флаг починки
        CompoundTag data = crucible.getReceivedData();
        data.remove("RepairMode");
        crucible.receiveScrollData(data);

        // Обновляем блок
        crucible.setChanged();
        if (crucible.getLevel() != null) {
            crucible.getLevel().sendBlockUpdated(
                    crucible.getBlockPos(),
                    crucible.getBlockState(),
                    crucible.getBlockState(),
                    3
            );
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


}