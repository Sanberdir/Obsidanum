package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;


public class ObsidanShovel extends ShovelItem implements IUpgradeableItem {
    // Константы NBT-тегов
    private static final String ACTIVATED_TAG = "Activated";
    private static final String LAST_ACTIVATION_TAG = "LastActivationTime";
    private static final String COOLDOWN_END_TAG = "CooldownEndTime";
    private static final String CUSTOM_MODEL_TAG = "CustomModelData";

    public static final long COOLDOWN_DURATION = 50 * 20; // 60 секунд
    private static final long ACTIVATION_DURATION = 5 * 20; // 5 секунд

    // Список разрешенных улучшений для этого инструмента
    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.HARVESTER,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.ARCHAEOLOGIST,
            ObsidanumToolUpgrades.LONG_HANDLE
    };

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
    private static Set<ObsidanumToolUpgrades> getExclusiveGroup(ObsidanumToolUpgrades upgrade) {
        return EXCLUSIVE_GROUPS.get(upgrade);
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

    // Список взаимоисключающих групп улучшений
    private static final Map<ObsidanumToolUpgrades, Set<ObsidanumToolUpgrades>> EXCLUSIVE_GROUPS = new HashMap<>();
    static {
        // Группа взаимоисключающих улучшений
        Set<ObsidanumToolUpgrades> exclusiveGroup = new HashSet<>();
        exclusiveGroup.add(ObsidanumToolUpgrades.HARVESTER);
        exclusiveGroup.add(ObsidanumToolUpgrades.ARCHAEOLOGIST);

        // Добавляем в карту для каждого улучшения его группу
        for (ObsidanumToolUpgrades upgrade : exclusiveGroup) {
            EXCLUSIVE_GROUPS.put(upgrade, exclusiveGroup);
        }
    }

    @Override
    public boolean addUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {


        if (isUpgradeAllowed(upgrade)) {
            // Если добавляем HARVESTER или STONE_BREAKER, пересчитываем урон
            if (upgrade == ObsidanumToolUpgrades.HARVESTER
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

        // Проверяем на конфликты с уже установленными улучшениями
        Map<ObsidanumToolUpgrades, Integer> currentUpgrades = getUpgrades(stack);
        Set<ObsidanumToolUpgrades> exclusiveGroup = EXCLUSIVE_GROUPS.get(upgrade);

        if (exclusiveGroup != null) {
            for (ObsidanumToolUpgrades existingUpgrade : currentUpgrades.keySet()) {
                if (exclusiveGroup.contains(existingUpgrade)) {
                    // Нашли конфликтующее улучшение - отменяем добавление
                    return false;
                }
            }
        }
        return true;
    }
    private int getMaxDamageAfterUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade) {
        int baseDurability = getMaxDamageWithoutPenalties(stack);

        if (upgrade == ObsidanumToolUpgrades.HARVESTER) {
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
        int harvesterLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.HARVESTER);
        int longHandleLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);

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

        return baseDurability;
    }
    public ObsidanShovel(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }



    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }


    private static UUID generateUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }
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
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("obsidan_shovel.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obsidan_shovel.long_handle_attack_range");

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

        // Получаем UUID для модификаторов
        UUID reachUUID = generateUUID("obsidan_shovel.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obsidan_shovel.long_handle_attack_range");

        // Удаляем старые модификаторы (если есть)
        var reachAttr = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (reachAttr != null) {
            reachAttr.removeModifier(reachUUID);
        }

        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRangeAttr != null) {
            attackRangeAttr.removeModifier(attackRangeUUID);
        }

        // Проверяем, держит ли игрок лопату в руке (основной или дополнительной)
        boolean isHoldingShovel = player.getMainHandItem() == stack || player.getOffhandItem() == stack;

        if (isHoldingShovel) {
            int longHandleLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);

            if (longHandleLevel > 0) {
                double bonus = UpgradeLibrary.getLongHandleBonus(longHandleLevel);

                // Применяем бонус к дальности взаимодействия с блоками
                if (reachAttr != null) {
                    AttributeModifier reachMod = new AttributeModifier(
                            reachUUID,
                            "obsidan_shovel.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем бонус к дальности атаки
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            attackRangeUUID,
                            "obsidan_shovel.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }

        // Обработка активации/деактивации (без изменений)
        if (!world.isClientSide && isActivated(stack)) {
            long currentTime = world.getGameTime();
            long lastActivationTime = getLastActivationTime(stack);

            if (currentTime - lastActivationTime >= ACTIVATION_DURATION) {
                deactivate(stack, player, currentTime);
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
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
       // Обработка HARVESTER
       boolean result = super.mineBlock(stack, world, state, pos, miner);
       int harvesterLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.HARVESTER);
       if (result && harvesterLevel > 0 && !world.isClientSide) {
           if (UpgradeLibrary.shouldDoubleDrops(harvesterLevel, world.random)) {
               List<ItemStack> drops = Block.getDrops(state, (ServerLevel) world, pos, null, miner, stack);
               for (ItemStack drop : drops) {
                   Block.popResource(world, pos, drop.copy());
               }
           }
       }

        if (miner instanceof Player player) {
            Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(stack);
            int archaeologistLevel = upgrades.getOrDefault(ObsidanumToolUpgrades.ARCHAEOLOGIST, 0);

            if (archaeologistLevel > 0 && isDiggableBlock(state)) {
                tryFindArchaeologistLoot(player, world, pos, state.getBlock(), archaeologistLevel);
            }
        }

        return super.mineBlock(stack, world, state, pos, miner);
    }

    private boolean isDiggableBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.SAND || block == Blocks.GRAVEL ||
                block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL;
    }

    private void tryFindArchaeologistLoot(Player player, Level world, BlockPos pos, Block block, int archaeologistLevel) {
        if (!world.isClientSide && UpgradeLibrary.tryFindArchaeologistLoot(archaeologistLevel, world.random)) {
            Item lootItem = UpgradeLibrary.getRandomArchaeologistLoot(block, world.random);

            if (lootItem != Items.AIR) {
                // Создаем предмет с 1-3 единицами (для ресурсов)
                int count = 1;
                if (lootItem.getMaxStackSize() > 1 && world.random.nextFloat() < 0.3f) {
                    count += world.random.nextInt(2); // 1-3 предмета
                }

                ItemEntity itemEntity = new ItemEntity(
                        world,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        new ItemStack(lootItem, count)
                );

                world.addFreshEntity(itemEntity);

                // Специальные звуки для разных типов лута
                SoundEvent sound = lootItem.getDefaultInstance().getRarity() == Rarity.RARE
                        ? SoundEvents.EXPERIENCE_ORB_PICKUP
                        : SoundEvents.ITEM_PICKUP;

                world.playSound(
                        null,
                        pos,
                        sound,
                        SoundSource.PLAYERS,
                        0.2F,
                        ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F
                );

                // Частицы при находке
                world.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        0, 0.1, 0);
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isActivated(stack)) {
            // Реализация knockback без изменений
            double knockbackY = 4.0;
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, knockbackY, motion.z);

            if (target instanceof Player) {
                ((Player) target).hurtMarked = true;
            }

            if (attacker instanceof Player) {
                deactivate(stack, (Player) attacker, attacker.level().getGameTime());
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        // Без изменений
        super.appendHoverText(itemstack, world, list, flag);

        // Выводим все улучшения
        Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(itemstack);
        for (Map.Entry<ObsidanumToolUpgrades, Integer> entry : upgrades.entrySet()) {
            list.add(Component.translatable(entry.getKey().getTranslationKey())
                    .append(" " + entry.getValue())
                    .withStyle(ChatFormatting.GOLD));
        }

        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.obsidan.description.shovel").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
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