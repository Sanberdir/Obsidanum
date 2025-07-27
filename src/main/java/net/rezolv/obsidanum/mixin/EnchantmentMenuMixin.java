package net.rezolv.obsidanum.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.rezolv.obsidanum.block.custom.AncientScroll;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.item.custom.EnchantedScroll;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow
    private Container enchantSlots;

    @Shadow @Final private DataSlot enchantmentSeed;

    @Shadow protected abstract List<EnchantmentInstance> getEnchantmentList(ItemStack pStack, int pEnchantSlot, int pLevel);

    @Inject(
            method = "lambda$clickMenuButton$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;enchant(Lnet/minecraft/world/item/enchantment/Enchantment;I)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            locals = LocalCapture.NO_CAPTURE
    )
    private void handleScrollTransfer(ItemStack itemstack, int enchantSlot, Player player, int cost, ItemStack lapisStack, Level level, BlockPos pos, CallbackInfo ci) {
        ItemStack scroll = this.enchantSlots.getItem(0);

        if (scroll.getItem() instanceof AncientScroll) {
            // Получаем список чар, которые были бы применены к предмету (как в оригинальном коде)
            List<EnchantmentInstance> enchantmentsToApply = this.getEnchantmentList(itemstack, enchantSlot, cost);

            if (!enchantmentsToApply.isEmpty()) {
                // Создаем зачарованный свиток (аналогично созданию зачарованной книги)
                ItemStack enchantedScroll = new ItemStack(ItemsObs.ENCHANTED_SCROLL.get());
                CompoundTag compoundtag = scroll.getTag();
                if (compoundtag != null) {
                    enchantedScroll.setTag(compoundtag.copy());
                }

                // Применяем только те чары, которые были бы наложены на предмет
                for (EnchantmentInstance enchantment : enchantmentsToApply) {
                    EnchantedScroll.addEnchantment(enchantedScroll, enchantment);
                }

                // Уменьшаем количество лазурита (если не творческий режим)
                if (!player.getAbilities().instabuild) {
                    lapisStack.shrink(enchantSlot + 1);
                    if (lapisStack.isEmpty()) {
                        this.enchantSlots.setItem(1, ItemStack.EMPTY);
                    }
                }

                // Обновляем опыт игрока
                player.onEnchantmentPerformed(itemstack, enchantSlot + 1);

                // Воспроизводим звук
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F);

                // Заменяем свиток в слоте
                this.enchantSlots.setItem(0, enchantedScroll);
                this.enchantSlots.setChanged();
                this.enchantmentSeed.set(player.getEnchantmentSeed());

                ci.cancel();
            }
        }
    }
}