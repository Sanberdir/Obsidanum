package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;

public class ObsSword extends SwordItem implements IUpgradeableItem {

    public ObsSword(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.SHARPENING,
            ObsidanumToolUpgrades.LONG_HANDLE
    };

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);

        if (slot == EquipmentSlot.MAINHAND) {
            // Получаем текущий уровень улучшений
            int sharpeningLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.SHARPENING);
            int strengthLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
            int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);

            // Получаем базовые значения атрибутов
            float baseDamage = 0;
            float baseAttackSpeed = 0;

            for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
                if (modifier.getId().equals(Item.BASE_ATTACK_DAMAGE_UUID)) {
                    baseDamage = (float) modifier.getAmount();
                    break;
                }
            }

            for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_SPEED)) {
                if (modifier.getId().equals(Item.BASE_ATTACK_SPEED_UUID)) {
                    baseAttackSpeed = (float) modifier.getAmount();
                    break;
                }
            }

            // Создаем новый мультимап для изменений
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // 1. Обрабатываем SHARPENING (увеличение урона)
            double totalDamage = baseDamage;
            if (sharpeningLevel > 0) {
                totalDamage += sharpeningLevel; // +1 урона за уровень
            }


            // 3. Обрабатываем BALANCING (увеличение скорости атаки)
            float totalAttackSpeed = baseAttackSpeed;
            if (balancingLevel > 0) {
                totalAttackSpeed += 0.07f * balancingLevel; // +0.1 скорости атаки за уровень
            }

            // 4. Обрабатываем STRENGTH (уменьшение скорости атаки)
            if (strengthLevel > 0) {
                totalAttackSpeed -= 0.07f * strengthLevel; // -0.1 скорости атаки за уровень
            }

            // Добавляем модификаторы
            builder.put(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            Item.BASE_ATTACK_DAMAGE_UUID,
                            "Tool modifier",
                            totalDamage,
                            AttributeModifier.Operation.ADDITION
                    )
            );

            builder.put(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                            Item.BASE_ATTACK_SPEED_UUID,
                            "Tool modifier",
                            totalAttackSpeed,
                            AttributeModifier.Operation.ADDITION
                    )
            );

            // Копируем остальные модификаторы (если есть)
            for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
                if (entry.getKey() != Attributes.ATTACK_DAMAGE && entry.getKey() != Attributes.ATTACK_SPEED) {
                    builder.put(entry.getKey(), entry.getValue());
                }
            }

            modifiers = builder.build();
        }

        return modifiers;
    }

    @Override
    public Map<ObsidanumToolUpgrades, Integer> getUpgrades(ItemStack stack) {
        Map<ObsidanumToolUpgrades, Integer> upgrades = new HashMap<>();
        CompoundTag upgradesTag = stack.getTagElement(IUpgradeableItem.NBT_UPGRADES);
        if (upgradesTag != null) {
            for (String key : upgradesTag.getAllKeys()) {
                ObsidanumToolUpgrades upgrade = ObsidanumToolUpgrades.byName(key);
                if (upgrade != null && isUpgradeAllowed(upgrade)) {
                    upgrades.put(upgrade, upgradesTag.getInt(key));
                }
            }
        }
        return upgrades;
    }

    // Проверяем, разрешено ли улучшение для этого инструмента
    public boolean isUpgradeAllowed(ObsidanumToolUpgrades upgrade) {
        for (ObsidanumToolUpgrades allowed : ALLOWED_UPGRADES) {
            if (allowed == upgrade) {
                return true;
            }
        }
        return false;
    }

    // Переопределяем метод добавления улучшения с проверкой
    @Override
    public boolean addUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {
        if (isUpgradeAllowed(upgrade)) {
            // Если добавляем HARVESTER или STONE_BREAKER, пересчитываем урон
            if (upgrade == ObsidanumToolUpgrades.LONG_HANDLE
                    || upgrade == ObsidanumToolUpgrades.BALANCING
                    || upgrade == ObsidanumToolUpgrades.SHARPENING) {
                int currentDamage = stack.getDamageValue();
                int oldMaxDamage = getMaxDamage(stack); // Текущая макс. прочность (уже с учетом всех штрафов)
                int newMaxDamage = getMaxDamageAfterUpgrade(stack, upgrade); // Новая макс. прочность

                // Пересчитываем урон пропорционально
                double damageRatio = (double) currentDamage / oldMaxDamage;
                int scaledDamage = (int) (damageRatio * newMaxDamage);

                stack.setDamageValue(scaledDamage);
            }
            IUpgradeableItem.super.addUpgrade(stack, upgrade, level);
        }
        return true;
    }
    // Вспомогательный метод для расчета новой макс. прочности после апгрейда
    private int getMaxDamageAfterUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade) {
        int baseDurability = getMaxDamageWithoutPenalties(stack);

        if (upgrade == ObsidanumToolUpgrades.LONG_HANDLE) {
            return baseDurability - 50; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }
        else if (upgrade == ObsidanumToolUpgrades.BALANCING) {
            return baseDurability - 25;
        }
        else if (upgrade == ObsidanumToolUpgrades.SHARPENING) {
            return baseDurability - 50;
        }

        return baseDurability;
    }
    private int getMaxDamageWithoutPenalties(ItemStack stack) {
        int baseDurability = super.getMaxDamage(stack);
        int strengthLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);
        int sharpeningLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.SHARPENING);
        int longHandleLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);

        // Добавляем бонус от STRENGTH (+50 за уровень)
        int modifiedDurability = baseDurability + (50 * strengthLevel);

        // Вычитаем штраф от HARVESTER (-200 за уровень)
        modifiedDurability -= 200 * longHandleLevel;
        modifiedDurability -= 25 * balancingLevel;
        modifiedDurability -= 50 * sharpeningLevel;

        // Минимальная прочность = 1 (чтобы предмет не сломался полностью)
        return Math.max(1, modifiedDurability);
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        int baseDurability = getMaxDamageWithoutPenalties(stack);

        return baseDurability;
    }
    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);

        // Выводим все улучшения
        Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(itemstack);
        for (Map.Entry<ObsidanumToolUpgrades, Integer> entry : upgrades.entrySet()) {
            list.add(Component.translatable(entry.getKey().getTranslationKey())
                    .append(" " + entry.getValue())
                    .withStyle(ChatFormatting.GOLD));
        }

    }


    private static UUID generateUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("obs_sword.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obs_sword.long_handle_attack_range");

        // Снимаем модификаторы, если они остались
        var reachAttr = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reachAttr != null && reachAttr.getModifier(reachUUID) != null) {
            reachAttr.removeModifier(reachUUID);
        }
        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRangeAttr != null && attackRangeAttr.getModifier(attackRangeUUID) != null) {
            attackRangeAttr.removeModifier(attackRangeUUID);
        }

        return super.onDroppedByPlayer(stack, player);
    }
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!(entity instanceof Player player) || world.isClientSide)
            return;

        // Получаем текущие UUID модификаторов
        UUID reachUUID = generateUUID("obs_sword.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obs_sword.long_handle_attack_range");

        // Удаляем старые модификаторы (если есть)
        var reachAttr = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reachAttr != null) {
            reachAttr.removeModifier(reachUUID);
        }

        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRangeAttr != null) {
            attackRangeAttr.removeModifier(attackRangeUUID);
        }

        // Проверяем, держит ли игрок меч в руке (основной или дополнительной)
        boolean isHoldingSword = player.getMainHandItem() == stack || player.getOffhandItem() == stack;

        if (isHoldingSword) {
            int longHandleLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);

            if (longHandleLevel > 0) {
                double bonus = UpgradeLibrary.getLongHandleBonus(longHandleLevel);

                // Применяем бонус к дальности взаимодействия с блоками
                if (reachAttr != null) {
                    AttributeModifier reachMod = new AttributeModifier(
                            reachUUID,
                            "obs_sword.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем бонус к дальности атаки
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            attackRangeUUID,
                            "obs_sword.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }
        
    }
    
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        // Создаем экземпляр Random
        Random random = new Random();
        // Генерируем случайное число от 0 до 10
        int damage = random.nextInt(6); // Верхний предел 11, чтобы получить число от 0 до 10

        pStack.hurtAndBreak(damage, pAttacker, (p_43296_) -> {
            p_43296_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
        });
        return true;
    }
    public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {
        if (!pLevel.isClientSide && pState.getDestroySpeed(pLevel, pPos) != 0.0F) {
            // Создаем экземпляр Random
            Random random = new Random();
            // Генерируем случайное число от 0 до 10
            int damage = random.nextInt(6); // Верхний предел 11, чтобы получить число от 0 до 10

            pStack.hurtAndBreak(damage, pEntityLiving, (p_40992_) -> {
                p_40992_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        }

        return true;
    }
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }
}
