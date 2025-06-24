package net.rezolv.obsidanum.item.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IUpgradeableItem {
    String NBT_UPGRADES = "Upgrades";

    /**
     * Добавить или обновить улучшение
     */
    default void addUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {
        CompoundTag upgradesTag = stack.getOrCreateTagElement(NBT_UPGRADES);
        upgradesTag.putInt(upgrade.getName(), level);
    }

    default void removeAllUpgrades(ItemStack stack) {
        stack.removeTagKey(NBT_UPGRADES);
    }

    /**
     * Удалить конкретное улучшение
     */
    default void removeUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade) {
        CompoundTag upgradesTag = stack.getTagElement(NBT_UPGRADES);
        if (upgradesTag != null) {
            upgradesTag.remove(upgrade.getName());
            if (upgradesTag.isEmpty()) {
                stack.removeTagKey(NBT_UPGRADES);
            }
        }
    }

    /**
     * Получить уровень конкретного улучшения
     */
    default int getUpgradeLevel(ItemStack stack, ObsidanumToolUpgrades upgrade) {
        CompoundTag upgradesTag = stack.getTagElement(NBT_UPGRADES);
        if (upgradesTag == null || !upgradesTag.contains(upgrade.getName())) {
            return 0;
        }
        return upgradesTag.getInt(upgrade.getName());
    }

    /**
     * Получить все улучшения
     */
    Map<ObsidanumToolUpgrades, Integer> getUpgrades(ItemStack stack);

    /**
     * Проверить, разрешено ли улучшение для этого предмета
     */
    default boolean isUpgradeAllowed(ObsidanumToolUpgrades upgrade) {
        return true; // По умолчанию разрешены все улучшения
    }
}