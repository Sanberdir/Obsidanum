package net.rezolv.obsidanum.item.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public interface IUpgradeableItem {
    String NBT_UPGRADES = "Upgrades";
    int MAX_UPGRADE_SLOTS = 10; // Максимальное количество слотов

    /**
     * Добавить или обновить улучшение
     */
    default boolean addUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {
        // Получаем текущие улучшения
        Map<ObsidanumToolUpgrades, Integer> currentUpgrades = getUpgrades(stack);

        // Рассчитываем текущее количество слотов
        int usedSlots = currentUpgrades.values().stream().mapToInt(Integer::intValue).sum();
        int currentLevel = currentUpgrades.getOrDefault(upgrade, 0);
        int newSlots = usedSlots - currentLevel + level;

        // Проверяем ограничение
        if (newSlots > MAX_UPGRADE_SLOTS) {
            return false;
        }

        // Сохраняем улучшение
        CompoundTag upgradesTag = stack.getOrCreateTagElement(NBT_UPGRADES);
        upgradesTag.putInt(upgrade.getName(), level);
        return true;
    }

    default void removeAllUpgrades(ItemStack stack) {
        stack.removeTagKey(NBT_UPGRADES);
    }

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
    default Map<ObsidanumToolUpgrades, Integer> getUpgrades(ItemStack stack) {
        Map<ObsidanumToolUpgrades, Integer> upgrades = new HashMap<>();
        CompoundTag upgradesTag = stack.getTagElement(NBT_UPGRADES);

        if (upgradesTag != null) {
            for (String key : upgradesTag.getAllKeys()) {
                ObsidanumToolUpgrades upgrade = ObsidanumToolUpgrades.byName(key);
                if (upgrade != null) {
                    upgrades.put(upgrade, upgradesTag.getInt(key));
                }
            }
        }
        return upgrades;
    }
    default int getUsedSlots(ItemStack stack) {
        return getUpgrades(stack).values().stream().mapToInt(Integer::intValue).sum();
    }
    /**
     * Проверить, разрешено ли улучшение для этого предмета
     */
    default boolean isUpgradeAllowed(ObsidanumToolUpgrades upgrade) {
        return true; // По умолчанию разрешены все улучшения
    }
}