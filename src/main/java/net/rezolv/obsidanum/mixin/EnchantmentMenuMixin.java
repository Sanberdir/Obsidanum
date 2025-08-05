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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private DataSlot enchantmentSeed;
    @Shadow @Final private net.minecraft.world.inventory.ContainerLevelAccess access;
    @Shadow @Final private int[] costs;
    @Shadow protected abstract List<EnchantmentInstance> getEnchantmentList(ItemStack stack, int slot, int level);

    @Inject(
            method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void handleScrollTransfer(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 1) Гарантируем, что ид кнопки валиден
            if (buttonId < 0 || this.costs == null || buttonId >= this.costs.length) {
                return;
            }

            ItemStack scroll     = enchantSlots.getItem(0);
            ItemStack lapisStack = enchantSlots.getItem(1);

            if (!(scroll.getItem() instanceof AncientScroll)) {
                return; // не наш случай — отдать стандартную логику дальше
            }

            int cost = this.costs[buttonId];

            // 2) Список чар
            List<EnchantmentInstance> enchToApply =
                    this.getEnchantmentList(scroll, buttonId, cost);
            if (enchToApply.isEmpty()) {
                return;
            }

            // 3) Собираем зачарованный свиток
            ItemStack enchantedScroll = new ItemStack(ItemsObs.ENCHANTED_SCROLL.get());
            CompoundTag tag = scroll.getTag();
            if (tag != null) enchantedScroll.setTag(tag.copy());
            enchToApply.forEach(e -> EnchantedScroll.addEnchantment(enchantedScroll, e));

            // 4) Тратим лазурит
            if (!player.getAbilities().instabuild) {
                lapisStack.shrink(cost);
                if (lapisStack.isEmpty()) {
                    enchantSlots.setItem(1, ItemStack.EMPTY);
                }
            }

            // 5) Опыт и звук
            player.onEnchantmentPerformed(scroll, cost);
            access.execute((lvl, pos) -> {
                lvl.playSound(
                        null, pos,
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS,
                        1.0F,
                        lvl.random.nextFloat() * 0.1F + 0.9F
                );
            });

            // 6) Вставляем результат, синхронизируем и отменяем стандарт
            enchantSlots.setItem(0, enchantedScroll);
            enchantSlots.setChanged();
            enchantmentSeed.set(player.getEnchantmentSeed());
            cir.setReturnValue(true);

        } catch (Throwable t) {
            // Пробросим реальное исключение в лог, чтобы не терять стек
            t.printStackTrace();
            // и позволим Миксину продолжить штатно (или упасть после этого)
        }
    }
}