package net.rezolv.obsidanum.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public class ObsidanSword extends SwordItem {
    // NBT теги
    private static final String ACTIVATED_TAG = "Activated";
    private static final String LAST_ACTIVATION_TAG = "LastActivationTime";
    private static final String COOLDOWN_END_TAG = "CooldownEndTime";
    private static final String CUSTOM_MODEL_TAG = "CustomModelData";

    public static final long COOLDOWN_DURATION = 80 * 20; // 80 секунд
    private static final long ACTIVATION_DURATION = 5 * 20; // 5 секунд

    public ObsidanSword(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.obsidan.description.sword").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        super.onCraftedBy(stack, world, player);
        stack.getOrCreateTag().putBoolean(ACTIVATED_TAG, false);
        stack.getOrCreateTag().putLong(LAST_ACTIVATION_TAG, 0);
        stack.getOrCreateTag().putLong(COOLDOWN_END_TAG, 0);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!world.isClientSide && isActivated(stack)) {
            long currentTime = world.getGameTime();
            long lastActivationTime = getLastActivationTime(stack);

            if (currentTime - lastActivationTime >= ACTIVATION_DURATION) {
                if (entity instanceof Player) {
                    deactivate(stack, (Player) entity, currentTime);
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        long currentTime = worldIn.getGameTime();

        if (!isActivated(stack) && currentTime >= getCooldownEndTime(stack)) {
            if (!worldIn.isClientSide) {
                activate(stack, currentTime);
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.isLocalPlayer() && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (isActivated(stack)) {
                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float additionalDamage = 4f;

                livingEntity.hurt(new DamageSource(player.getCommandSenderWorld().registryAccess()
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(DamageTypes.PLAYER_ATTACK)),
                        baseDamage * additionalDamage);

                deactivate(stack, player, player.level().getGameTime());
                return true;
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    private void activate(ItemStack stack, long currentTime) {
        stack.getOrCreateTag().putBoolean(ACTIVATED_TAG, true);
        stack.getOrCreateTag().putLong(LAST_ACTIVATION_TAG, currentTime);
        stack.getOrCreateTag().putInt(CUSTOM_MODEL_TAG, 1);
    }

    private void deactivate(ItemStack stack, Player player, long currentTime) {
        stack.getOrCreateTag().putBoolean(ACTIVATED_TAG, false);
        stack.getOrCreateTag().putInt(CUSTOM_MODEL_TAG, 0);
        setCooldownEndTime(stack, currentTime + COOLDOWN_DURATION);
    }

    public boolean isActivated(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(ACTIVATED_TAG);
    }

    private long getLastActivationTime(ItemStack stack) {
        return stack.getOrCreateTag().getLong(LAST_ACTIVATION_TAG);
    }

    private long getCooldownEndTime(ItemStack stack) {
        return stack.getOrCreateTag().getLong(COOLDOWN_END_TAG);
    }

    private void setCooldownEndTime(ItemStack stack, long time) {
        stack.getOrCreateTag().putLong(COOLDOWN_END_TAG, time);
    }
}