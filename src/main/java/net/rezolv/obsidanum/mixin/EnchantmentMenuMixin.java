package net.rezolv.obsidanum.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.rezolv.obsidanum.block.custom.AncientScroll;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.item.custom.EnchantedScroll;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow
    private Container enchantSlots;

    @Inject(
            method = "lambda$clickMenuButton$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;enchant(Lnet/minecraft/world/item/enchantment/Enchantment;I)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void handleScrollTransfer(ItemStack p_39476_, int p_39477_, Player p_39478_, int p_39479_, ItemStack p_39480_, Level p_39481_, BlockPos p_39482_, CallbackInfo ci) {
        ItemStack scroll = this.enchantSlots.getItem(0);

        if (scroll.getItem() instanceof AncientScroll) {
            // Получаем все чары с предмета
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(p_39476_);

            // Создаем новый зачарованный свиток
            ItemStack enchantedScroll = new ItemStack(ItemsObs.ENCHANTED_SCROLL.get());

            // Переносим все чары на свиток
            enchantments.forEach((enchantment, level) -> {
                EnchantedScroll.addEnchantment(enchantedScroll, new EnchantmentInstance(enchantment, level));
            });

            // Очищаем чары с исходного предмета
            p_39476_.removeTagKey("Enchantments");
            p_39476_.removeTagKey("StoredEnchantments");
            p_39481_.playSound((Player)null, p_39482_, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, p_39481_.random.nextFloat() * 0.1F + 0.9F);

            // Заменяем свиток в слоте
            this.enchantSlots.setItem(0, enchantedScroll);
            ci.cancel();
        }
    }
}