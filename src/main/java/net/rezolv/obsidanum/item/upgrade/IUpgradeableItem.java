package net.rezolv.obsidanum.item.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface IUpgradeableItem {

    String NBT_UPGRADE = "Upgrade";
    String NBT_NAME = "Name";
    String NBT_LEVEL = "Level";

    /**
     * Apply the given upgrade with a level to the ItemStack (store in NBT).
     */
    default void setUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }
        CompoundTag tag = stack.getTag();
        CompoundTag upgradeTag = new CompoundTag();
        upgradeTag.putString(NBT_NAME, upgrade.getName());
        upgradeTag.putInt(NBT_LEVEL, Math.max(1, level)); // минимум 1
        tag.put(NBT_UPGRADE, upgradeTag);
    }

    /**
     * Get the currently applied upgrade from the ItemStack, or null if none.
     */
    default ObsidanumToolUpgrades getUpgrade(ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag upgradeTag = stack.getTag().getCompound(NBT_UPGRADE);
        if (upgradeTag == null || !upgradeTag.contains(NBT_NAME)) return null;
        String name = upgradeTag.getString(NBT_NAME);
        for (ObsidanumToolUpgrades upg : ObsidanumToolUpgrades.values()) {
            if (upg.getName().equalsIgnoreCase(name)) {
                return upg;
            }
        }
        return null;
    }

    /**
     * Get the level of the currently applied upgrade, or 0 if none.
     */
    default int getUpgradeLevel(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        CompoundTag upgradeTag = stack.getTag().getCompound(NBT_UPGRADE);
        if (upgradeTag == null || !upgradeTag.contains(NBT_LEVEL)) return 0;
        return upgradeTag.getInt(NBT_LEVEL);
    }

    /**
     * Remove any applied upgrade from the ItemStack.
     */
    default void removeUpgrade(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove(NBT_UPGRADE);
        }
    }
}
