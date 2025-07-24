package net.rezolv.obsidanum.item.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.rezolv.obsidanum.item.ItemsObs;

import javax.annotation.Nullable;
import java.util.List;

public class EnchantedScroll extends Item {
    public static final String TAG_STORED_ENCHANTMENTS = "StoredEnchantments";

    public EnchantedScroll(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    public static ListTag getEnchantments(ItemStack scrollStack) {
        CompoundTag tag = scrollStack.getTag();
        return tag != null ? tag.getList(TAG_STORED_ENCHANTMENTS, 10) : new ListTag();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ItemStack.appendEnchantmentNames(tooltip, getEnchantments(stack));
    }

    public static void addEnchantment(ItemStack scrollStack, EnchantmentInstance instance) {
        ListTag enchantments = getEnchantments(scrollStack);
        boolean shouldAdd = true;
        ResourceLocation newEnchantmentId = EnchantmentHelper.getEnchantmentId(instance.enchantment);

        for (int i = 0; i < enchantments.size(); ++i) {
            CompoundTag enchantmentTag = enchantments.getCompound(i);
            ResourceLocation existingEnchantmentId = EnchantmentHelper.getEnchantmentId(enchantmentTag);

            if (existingEnchantmentId != null && existingEnchantmentId.equals(newEnchantmentId)) {
                if (EnchantmentHelper.getEnchantmentLevel(enchantmentTag) < instance.level) {
                    EnchantmentHelper.setEnchantmentLevel(enchantmentTag, instance.level);
                }
                shouldAdd = false;
                break;
            }
        }

        if (shouldAdd) {
            enchantments.add(EnchantmentHelper.storeEnchantment(newEnchantmentId, instance.level));
        }

        scrollStack.getOrCreateTag().put(TAG_STORED_ENCHANTMENTS, enchantments);
    }
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        // Разрешаем использовать наш свиток как источник чар в наковальне
        return true;
    }

    public static ItemStack createForEnchantment(EnchantmentInstance instance) {
        ItemStack scroll = new ItemStack(ItemsObs.ENCHANTED_SCROLL.get()); // Замените на ваш Item
        addEnchantment(scroll, instance);
        return scroll;
    }
}