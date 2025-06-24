package net.rezolv.obsidanum.item.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IUpgradeableItem {

    String NBT_UPGRADES = "Upgrades"; // Изменено на множественное число

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
            // Если тег пуст - удаляем его полностью
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
     * Получить все улучшения (должен быть реализован в классе предмета)
     */
    Map<ObsidanumToolUpgrades, Integer> getUpgrades(ItemStack stack);
}