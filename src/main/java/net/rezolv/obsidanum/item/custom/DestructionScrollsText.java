package net.rezolv.obsidanum.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DestructionScrollsText extends Item {
    public DestructionScrollsText(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // Main header
        tooltip.add(Component.translatable("tooltip.recipe_information").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        MutableComponent typeScrollIcon = Component.literal("\uE035")
                .withStyle(style -> style.withFont(new ResourceLocation("obsidanum", "tool_icons")));
// Combine icons inline (after results)
        MutableComponent iconsLine = Component.literal("")
                .append(typeScrollIcon)
                .append(" ");
        tooltip.add(iconsLine);
        tooltip.add(Component.translatable("tooltip.recipe_end").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }


}