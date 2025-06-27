package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;

public class ObsAxe extends AxeItem implements IUpgradeableItem {
    public ObsAxe(Tier pTier, float pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    // Список разрешенных улучшений для этого инструмента
    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.WOODCUTTER,
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.SHARPENING,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.HARVESTER,
            ObsidanumToolUpgrades.LONG_HANDLE
    };

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);

        if (slot == EquipmentSlot.MAINHAND) {
            // Получаем текущий уровень SHARPENING
            int sharpeningLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.SHARPENING);

            if (sharpeningLevel > 0) {
                // Создаем новый мультимап для изменений
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

                // Копируем существующие модификаторы
                for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
                    if (entry.getKey() != Attributes.ATTACK_DAMAGE) {
                        builder.put(entry);
                    }
                }

                // Добавляем новый модификатор урона
                double baseDamage = super.getAttackDamage();
                double bonusDamage = sharpeningLevel; // +1 за уровень
                builder.put(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                BASE_ATTACK_DAMAGE_UUID,
                                "Sharpening modifier",
                                baseDamage + bonusDamage,
                                AttributeModifier.Operation.ADDITION
                        )
                );

                modifiers = builder.build();
            }
        }

        return modifiers;
    }
    private static final UUID BASE_ATTACK_DAMAGE_UUID = Item.BASE_ATTACK_DAMAGE_UUID;

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

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getDestroySpeed(stack, state);

        // Получаем уровни улучшений
        int harvesterLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);

        // Уменьшаем скорость от HARVESTER (например, на 10% за уровень)
        if (harvesterLevel > 0) {
            float speedReduction = 0.1f * harvesterLevel;
            float minSpeed = 2.0f; // Минимальная скорость
            baseSpeed = Math.max(minSpeed, baseSpeed * (1.0f - speedReduction));
        }

        // Увеличиваем скорость от BALANCING (например, на 15% за уровень)
        if (balancingLevel > 0) {
            float speedBoost = 1.0f + 0.15f * balancingLevel;
            baseSpeed *= speedBoost;
        }

        return baseSpeed;
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
    // Переопределяем метод добавления улучшения с проверкой
    @Override
    public boolean addUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {
        if (isUpgradeAllowed(upgrade)) {
            // Если добавляем HARVESTER или STONE_BREAKER, пересчитываем урон
            if (upgrade == ObsidanumToolUpgrades.HARVESTER
                    || upgrade == ObsidanumToolUpgrades.WOODCUTTER
                    || upgrade == ObsidanumToolUpgrades.BALANCING
                    || upgrade == ObsidanumToolUpgrades.LONG_HANDLE) {
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

        if (upgrade == ObsidanumToolUpgrades.WOODCUTTER) {
            return (int) (baseDurability * 0.4); // 20% от максимума
        }
        else if (upgrade == ObsidanumToolUpgrades.HARVESTER) {
            return baseDurability - 200; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }
        else if (upgrade == ObsidanumToolUpgrades.LONG_HANDLE) {
            return baseDurability - 50; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }
        else if (upgrade == ObsidanumToolUpgrades.BALANCING) {
            return baseDurability - 25; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }

        return baseDurability;
    }
    private int getMaxDamageWithoutPenalties(ItemStack stack) {
        int baseDurability = super.getMaxDamage(stack);
        int strengthLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);
        int harvesterLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.HARVESTER);
        int longHandleLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);

        // Добавляем бонус от STRENGTH (+50 за уровень)
        int modifiedDurability = baseDurability + (50 * strengthLevel);

        // Вычитаем штраф от HARVESTER (-200 за уровень)
        modifiedDurability -= 200 * harvesterLevel;
        modifiedDurability -= 50 * longHandleLevel;
        modifiedDurability -= 25 * balancingLevel;

        // Минимальная прочность = 1 (чтобы предмет не сломался полностью)
        return Math.max(1, modifiedDurability);
    }
    @Override
    public int getMaxDamage(ItemStack stack) {
        int baseDurability = getMaxDamageWithoutPenalties(stack);

        // Если есть WOODCUTTER, уменьшаем до 20%
        if (getUpgrades(stack).containsKey(ObsidanumToolUpgrades.WOODCUTTER)) {
            return (int) (baseDurability * 0.4);
        }

        return baseDurability;
    }

    private static UUID generateUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }
    // Вместо LONG_HANDLE_REACH_UUID используйте:
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("obs_axe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obs_axe.long_handle_attack_range");

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

        if (!(entity instanceof Player player) || world.isClientSide) return;

        // Обработка дальности взаимодействия с блоками
        var reachAttr = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reachAttr != null) {
            reachAttr.removeModifier(generateUUID("obs_axe.long_handle_reach")); // Удаляем по сгенерированному UUID
        }

// Обработка дальности атаки через Forge-специфичный атрибут
        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRangeAttr != null) {
            attackRangeAttr.removeModifier(generateUUID("obs_axe.long_handle_attack_range")); // Удаляем по сгенерированному UUID
        }

        // Проверяем, держит ли игрок именно этот инструмент в одной из рук
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean holdingThis = mainHand == stack || offHand == stack;

        if (holdingThis) {
            int level = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);
            if (level > 0) {
                double bonus = UpgradeLibrary.getLongHandleBonus(level);

                // Применяем бонус к дальности взаимодействия с блоками
                if (reachAttr != null) {
                    AttributeModifier reachMod = new AttributeModifier(
                            generateUUID("obs_axe.long_handle_reach"), // Динамически генерируемый UUID
                            "obs_axe.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем бонус к дальности атаки мобов
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            generateUUID("obs_axe.long_handle_attack_range"), // Динамически генерируемый UUID
                            "obs_axe.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }
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

    public boolean mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving) {

        if (pLevel.isClientSide || !(pEntityLiving instanceof Player player)) {
            return super.mineBlock(pStack, pLevel, pState, pPos, pEntityLiving);
        }

        Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(pStack);
        int harvesterLevel = upgrades.getOrDefault(ObsidanumToolUpgrades.HARVESTER, 0);
        boolean hasWoodcutter = upgrades.containsKey(ObsidanumToolUpgrades.WOODCUTTER);

        if (hasWoodcutter) {
            BlockPos furthest = findFurthestConnectedLog(pLevel, pPos, pState.getBlock());

            if (!furthest.equals(pPos)) {
                BlockState furthestState = pLevel.getBlockState(furthest);

                // Проверяем HARVESTER для удвоения дропа
                if (harvesterLevel > 0 && UpgradeLibrary.shouldDoubleDrops(harvesterLevel, pLevel.random)) {
                    List<ItemStack> drops = Block.getDrops(furthestState, (ServerLevel) pLevel, furthest, null, pEntityLiving, pStack);
                    for (ItemStack drop : drops) {
                        Block.popResource(pLevel, furthest, drop.copy());
                    }
                }

                pStack.hurtAndBreak(1, player, (p) -> {
                    p.broadcastBreakEvent(EquipmentSlot.MAINHAND);
                });

                pLevel.playSound(null, pPos, pState.getSoundType().getBreakSound(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);

                return true;
            }
        }
        // Обычное разрушение с проверкой HARVESTER
        boolean result = super.mineBlock(pStack, pLevel, pState, pPos, pEntityLiving);

        // Проверяем HARVESTER для обычного разрушения
        if (result && harvesterLevel > 0) {
            if (UpgradeLibrary.shouldDoubleDrops(harvesterLevel, pLevel.random)) {
                List<ItemStack> drops = Block.getDrops(pState, (ServerLevel) pLevel, pPos, null, pEntityLiving, pStack);
                for (ItemStack drop : drops) {
                    Block.popResource(pLevel, pPos, drop.copy());
                }
            }
        }

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
    public BlockPos findFurthestConnectedLog(Level level, BlockPos startPos, Block targetBlock) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);
        visited.add(startPos);

        BlockPos furthest = startPos;
        double maxDistance = 0;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            double dist = current.distSqr(startPos); // квадрат евклидовой дистанции
            if (dist > maxDistance) {
                maxDistance = dist;
                furthest = current;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && level.getBlockState(neighbor).is(targetBlock)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return furthest;
    }
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }
}
