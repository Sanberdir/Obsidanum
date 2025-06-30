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
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;

public class ObsShovel extends ShovelItem implements IUpgradeableItem {


    // Список разрешенных улучшений для этого инструмента
    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.HARVESTER,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.ARCHAEOLOGIST,
            ObsidanumToolUpgrades.LONG_HANDLE
    };

    public ObsShovel(Tier pTier, float pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
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
        if (!isUpgradeAllowed(upgrade)) {
            return false;
        }

        // Если добавляем HARVESTER или другие улучшения, влияющие на прочность
        if (upgrade == ObsidanumToolUpgrades.HARVESTER
                || upgrade == ObsidanumToolUpgrades.BALANCING
                || upgrade == ObsidanumToolUpgrades.LONG_HANDLE) {
            int currentDamage = stack.getDamageValue();
            int oldMaxDamage = getMaxDamage(stack);
            int newMaxDamage = getMaxDamageAfterUpgrade(stack, upgrade);

            double damageRatio = (double) currentDamage / oldMaxDamage;
            int scaledDamage = (int) (damageRatio * newMaxDamage);

            stack.setDamageValue(scaledDamage);
        }

        // Добавляем улучшение без проверки конфликтов (они уже проверены в команде)
        CompoundTag upgradesTag = stack.getOrCreateTagElement(NBT_UPGRADES);
        upgradesTag.putInt(upgrade.getName(), level);
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
        UUID reachUUID = generateUUID("obs_shovel.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obs_shovel.long_handle_attack_range");

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
        UUID reachUUID = generateUUID("obs_shovel.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obs_shovel.long_handle_attack_range");

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
                            "obs_shovel.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем бонус к дальности атаки
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            attackRangeUUID,
                            "obs_shovel.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }
    }


    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {

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

        if (!world.isClientSide && state.getDestroySpeed(world, pos) != 0.0F) {
            // Создаем экземпляр Random
            Random random = new Random();
            // Генерируем случайное число от 0 до 10
            int damage = random.nextInt(6); // Верхний предел 11, чтобы получить число от 0 до 10

            stack.hurtAndBreak(damage, miner, (p_40992_) -> {
                p_40992_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        }

        return true;
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
    }

    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }
}
