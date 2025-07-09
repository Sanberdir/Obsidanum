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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.LeftCornerLevel;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;

public class LeftCornerCompleteUp {

    public static void handleNeighborUpdate(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        Direction facing = state.getValue(ForgeCrucible.FACING);
        BlockPos expectedLeftPos = getLeftPos(pos, facing);

        if (expectedLeftPos != null && expectedLeftPos.equals(fromPos)) {
            BlockState leftState = level.getBlockState(expectedLeftPos);
            if (leftState.getBlock() instanceof LeftCornerLevel && leftState.getValue(LeftCornerLevel.IS_PRESSED)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ForgeCrucibleEntity crucible) {
                    // Проверяем условия перед запуском процесса
                    Player player = level.getNearestPlayer(crucible.getBlockPos().getX(), crucible.getBlockPos().getY(), crucible.getBlockPos().getZ(), 10, false);
                    String error = checkUpgradeConditions(crucible);

                    if (error != null) {
                        sendMessage(player, Component.translatable(error));
                    } else {
                        startUpgradeProcess(crucible);
                    }
                }
            }
        }
    }

    private static String checkUpgradeConditions(ForgeCrucibleEntity crucible) {
        ItemStack toolStack = crucible.itemHandler.getStackInSlot(3);
        if (toolStack.isEmpty() || !(toolStack.getItem() instanceof IUpgradeableItem)) {
            return "obsidanum.upgrade.error.no_tool";
        }

        if (!checkAllIngredientsWithCount(crucible)) {
            return "obsidanum.upgrade.error.missing_ingredients";
        }

        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Upgrade", Tag.TAG_STRING)) {
            return "obsidanum.upgrade.error.no_recipe";
        }

        String name = data.getString("Upgrade");
        ObsidanumToolUpgrades upgrade = ObsidanumToolUpgrades.byName(name);
        if (upgrade == null) {
            return "obsidanum.upgrade.error.invalid_upgrade";
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) toolStack.getItem();
        if (!upgradable.isUpgradeAllowed(upgrade)) {
            return "obsidanum.upgrade.error.not_allowed";
        }

        int current = upgradable.getUpgradeLevel(toolStack, upgrade);
        int max = UpgradeLibrary.getMaxLevel(upgrade);
        if (current >= max) {
            return "obsidanum.upgrade.error.max_level";
        }

        if (current == 0) {
            Set<ObsidanumToolUpgrades> ex = getExclusiveGroup(upgrade);
            if (ex != null) {
                for (ObsidanumToolUpgrades exist : upgradable.getUpgrades(toolStack).keySet()) {
                    if (ex.contains(exist) && exist != upgrade) {
                        return "obsidanum.upgrade.error.conflict";
                    }
                }
            }
        }

        if (upgradable.getUsedSlots(toolStack) + 1 > IUpgradeableItem.MAX_UPGRADE_SLOTS) {
            return "obsidanum.upgrade.error.no_slots";
        }

        return null;
    }

    private static void startUpgradeProcess(ForgeCrucibleEntity crucible) {
        CompoundTag data = crucible.getReceivedData();
        data.putBoolean("UpgradeMode", true); // Устанавливаем флаг улучшения
        crucible.receiveScrollData(data); // Обновляем данные

        // Получаем количество ударов из рецепта (по умолчанию 3)
        int hammerStrikes = 3;
        if (data.contains("HammerStrikes", Tag.TAG_INT)) {
            hammerStrikes = data.getInt("HammerStrikes");
        }

        crucible.startCrafting(hammerStrikes);
    }

    public static void completeUpgrade(ForgeCrucibleEntity crucible) {
        Level level = crucible.getLevel();
        Player player = level.getNearestPlayer(crucible.getBlockPos().getX(), crucible.getBlockPos().getY(), crucible.getBlockPos().getZ(), 10, false);

        ItemStack toolStack = crucible.itemHandler.getStackInSlot(3);
        IUpgradeableItem upgradable = (IUpgradeableItem) toolStack.getItem();

        CompoundTag data = crucible.getReceivedData();
        String name = data.getString("Upgrade");
        ObsidanumToolUpgrades upgrade = ObsidanumToolUpgrades.byName(name);

        // Вычитаем ингредиенты
        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag tag = ingredients.getCompound(i);
            JsonObject json = JsonParser.parseString(tag.getString("IngredientJson")).getAsJsonObject();
            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            ItemStack slot = crucible.itemHandler.getStackInSlot(i);
            slot.shrink(count);
        }

        // Применяем улучшение
        int current = upgradable.getUpgradeLevel(toolStack, upgrade);
        upgradable.addUpgrade(toolStack, upgrade, current + 1);
        crucible.itemHandler.setStackInSlot(3, toolStack);
        crucible.setChanged();

        sendMessage(player, Component.translatable("obsidanum.upgrade.success",
                Component.translatable("upgrade.obsidanum." + upgrade.getName()),
                current + 1));

        // Сбрасываем флаг улучшения
        data.remove("UpgradeMode");
        crucible.receiveScrollData(data);
    }

    private static void sendMessage(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, false);
        }
    }

    private static boolean checkAllIngredientsWithCount(ForgeCrucibleEntity crucible) {
        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients", Tag.TAG_LIST)) return false;
        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag ing = ingredients.getCompound(i);
            ItemStack stack = crucible.itemHandler.getStackInSlot(i);
            try {
                JsonObject json = JsonParser.parseString(ing.getString("IngredientJson")).getAsJsonObject();
                int req = json.has("count") ? json.get("count").getAsInt() : 1;
                if (stack.getCount() < req || !matchesIngredient(stack, json)) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private static Set<ObsidanumToolUpgrades> getExclusiveGroup(ObsidanumToolUpgrades upgrade) {
        if (upgrade == ObsidanumToolUpgrades.HARVESTER || upgrade == ObsidanumToolUpgrades.ARCHAEOLOGIST) {
            return Set.of(ObsidanumToolUpgrades.HARVESTER, ObsidanumToolUpgrades.ARCHAEOLOGIST);
        }
        return null;
    }

    private static boolean matchesIngredient(ItemStack stack, JsonObject json) {
        if (stack.isEmpty()) return false;
        if (json.has("item")) {
            ResourceLocation id = new ResourceLocation(json.get("item").getAsString());
            return ForgeRegistries.ITEMS.getValue(id) == stack.getItem();
        }
        if (json.has("tag")) {
            ResourceLocation tl = new ResourceLocation(json.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tl);
            return stack.is(tag);
        }
        return false;
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
}