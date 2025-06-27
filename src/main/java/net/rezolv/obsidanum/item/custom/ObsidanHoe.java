package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;

public class ObsidanHoe extends HoeItem implements IUpgradeableItem {
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }

    public static final long COOLDOWN_DURATION = 10 * 20; // 10 seconds in ticks
    private static final long ACTIVATION_DURATION = 5 * 20; // 5 seconds in ticks
    private static final String TAG_ACTIVATED = "Activated";
    private static final String TAG_LAST_ACTIVATION_TIME = "LastActivationTime";
    private static final String TAG_COOLDOWN_END = "CooldownEndTime";

    // Список разрешенных улучшений для этого инструмента
    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.RICH_HARVEST,
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.SHARPENING,
            ObsidanumToolUpgrades.LONG_HANDLE
    };
    private int getMaxDamageAfterUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade) {
        int baseDurability = getMaxDamageWithoutPenalties(stack);

        if (upgrade == ObsidanumToolUpgrades.LONG_HANDLE) {
            return baseDurability - 50; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }
        else if (upgrade == ObsidanumToolUpgrades.BALANCING) {
            return baseDurability - 25; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }
        else if (upgrade == ObsidanumToolUpgrades.SHARPENING) {
            return baseDurability - 50; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }

        return baseDurability;
    }
    private int getMaxDamageWithoutPenalties(ItemStack stack) {
        int baseDurability = super.getMaxDamage(stack);
        int strengthLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
        int sharpeningLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.SHARPENING);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);
        int longHandleLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);

        // Добавляем бонус от STRENGTH (+50 за уровень)
        int modifiedDurability = baseDurability + (50 * strengthLevel);

        // Вычитаем штраф от HARVESTER (-200 за уровень)
        modifiedDurability -= 50 * longHandleLevel;
        modifiedDurability -= 50 * balancingLevel;
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


    private static final BlockState[] TARGET_BLOCKS = {
            Blocks.GRASS.defaultBlockState(),
            Blocks.TALL_GRASS.defaultBlockState(),
            Blocks.FERN.defaultBlockState(),
            Blocks.DEAD_BUSH.defaultBlockState(),
            Blocks.CRIMSON_ROOTS.defaultBlockState(),
            Blocks.WARPED_ROOTS.defaultBlockState(),
            Blocks.FIRE.defaultBlockState(),
            Blocks.LARGE_FERN.defaultBlockState(),
            Blocks.NETHER_SPROUTS.defaultBlockState(),
            Blocks.SCULK.defaultBlockState(),
            Blocks.SCULK_VEIN.defaultBlockState(),
            Blocks.SCULK_SENSOR.defaultBlockState(),
            Blocks.SCULK_SHRIEKER.defaultBlockState(),
            Blocks.KELP.defaultBlockState(),
            Blocks.KELP_PLANT.defaultBlockState(),
            Blocks.LILY_PAD.defaultBlockState(),
            Blocks.MOSS_BLOCK.defaultBlockState(),
            Blocks.CAVE_VINES.defaultBlockState(),
            Blocks.CAVE_VINES_PLANT.defaultBlockState(),
            Blocks.MOSS_CARPET.defaultBlockState(),
            Blocks.SPORE_BLOSSOM.defaultBlockState(),
            Blocks.GLOW_LICHEN.defaultBlockState(),
            BlocksObs.CRIMSON_GRASS.get().defaultBlockState()
    };

    public ObsidanHoe(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    public boolean isActivated(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_ACTIVATED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        long currentTime = worldIn.getGameTime();

        if (!isActivated(stack) && currentTime >= getCooldownEnd(stack)) {
            if (!worldIn.isClientSide) {
                activate(stack, currentTime);
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        // Получаем все улучшения
        Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(stack);
        int multiplier = 1;
        // Обрабатываем каждое улучшение
        for (Map.Entry<ObsidanumToolUpgrades, Integer> entry : upgrades.entrySet()) {
            ObsidanumToolUpgrades upg = entry.getKey();


        }

        if (!level.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) level;
            Block block = state.getBlock();

            // Применяем множитель дропа для BushBlock или блоков из списка TARGET_BLOCKS


            // Обработка активированного режима
            if (isActivated(stack) &&  isTargetBlock(state)) {
                BlockPos playerPos = entity.blockPosition();
                for (BlockPos blockPos : BlockPos.betweenClosed(playerPos.offset(-20, -20, -20),
                        playerPos.offset(20, 20, 20))) {
                    BlockState targetState = level.getBlockState(blockPos);
                    Block targetBlock = targetState.getBlock();

                    if (isTargetBlock(targetState)) {
                            level.destroyBlock(blockPos, true);

                    }
                }
                deactivate(stack, (Player) entity, level.getGameTime());
                return false;
            }
        }

        // Вызываем базовую логику копания
        boolean result = super.mineBlock(stack, level, state, pos, entity);

        return result;
    }

    // Вспомогательный метод для проверки, является ли блок целевым блоком
    private boolean isTargetBlock(BlockState state) {
        for (BlockState targetBlock : TARGET_BLOCKS) {
            if (state.is(targetBlock.getBlock())) {
                return true;
            }
        }
        return false;
    }



    public void activate(ItemStack stack, long currentTime) {
        stack.getOrCreateTag().putBoolean(TAG_ACTIVATED, true);
        stack.getOrCreateTag().putLong(TAG_LAST_ACTIVATION_TIME, currentTime);
        stack.getOrCreateTag().putInt("CustomModelData", 1);
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
        int strengthLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);

        // Уменьшаем скорость от HARVESTER (например, на 10% за уровень)
        if (strengthLevel > 0) {
            float speedReduction = 0.1f * strengthLevel;
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

    private static UUID generateUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("obsidan_hoe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obsidan_hoe.long_handle_attack_range");

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
            reachAttr.removeModifier(generateUUID("obsidan_hoe.long_handle_reach")); // Удаляем по сгенерированному UUID
        }

// Обработка дальности атаки через Forge-специфичный атрибут
        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRangeAttr != null) {
            attackRangeAttr.removeModifier(generateUUID("obsidan_hoe.long_handle_attack_range")); // Удаляем по сгенерированному UUID
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
                            generateUUID("obsidan_hoe.long_handle_reach"), // Динамически генерируемый UUID
                            "obsidan_hoe.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем бонус к дальности атаки мобов
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            generateUUID("obsidan_hoe.long_handle_attack_range"), // Динамически генерируемый UUID
                            "obsidan_hoe.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }

        if (!world.isClientSide && isActivated(stack)) {
            long currentTime = world.getGameTime();
            long lastActivationTime = stack.getOrCreateTag().getLong(TAG_LAST_ACTIVATION_TIME);

            if (currentTime - lastActivationTime >= ACTIVATION_DURATION) {
                deactivate(stack, player, currentTime);
            }
        }
    }

    public void deactivate(ItemStack stack, Player player, long currentTime) {
        stack.getOrCreateTag().putBoolean(TAG_ACTIVATED, false);
        stack.getOrCreateTag().putInt("CustomModelData", 0);
        setCooldownEnd(stack, currentTime + COOLDOWN_DURATION);
    }

    private long getCooldownEnd(ItemStack stack) {
        return stack.getOrCreateTag().getLong(TAG_COOLDOWN_END);
    }

    private void setCooldownEnd(ItemStack stack, long time) {
        stack.getOrCreateTag().putLong(TAG_COOLDOWN_END, time);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = world.getBlockState(pos);

        if (player == null) return InteractionResult.FAIL;

        // Новая логика для RICH_HARVEST (без проверки на isSecondaryUseActive)
        if (state.getBlock() instanceof CropBlock && isMaxAge(state)) {
            int harvestLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.RICH_HARVEST);
            if (harvestLevel > 0) {
                harvestCrop(world, pos, state, player, harvestLevel);
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // Старая логика обработки земли
        if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT)) {
            world.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!world.isClientSide) {
                for (BlockPos targetPos : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
                    BlockState targetState = world.getBlockState(targetPos);
                    if (targetState.is(BlockTags.DIRT) || targetState.is(Blocks.GRASS_BLOCK) || targetState.is(Blocks.DIRT_PATH) || targetState.is(Blocks.COARSE_DIRT)) {
                        world.setBlock(targetPos, Blocks.FARMLAND.defaultBlockState(), 11);
                        world.levelEvent(2001, targetPos, Block.getId(targetState));
                    }
                }
                stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(context.getHand()));
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private boolean isMaxAge(BlockState state) {
        if (state.getBlock() instanceof CropBlock) {
            return state.getValue(CropBlock.AGE) >= ((CropBlock)state.getBlock()).getMaxAge();
        }
        return false;
    }

    private void harvestCrop(Level world, BlockPos pos, BlockState state, Player player, int harvestLevel) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            // Получаем базовые дропы (1 предмет)
            List<ItemStack> drops = Block.getDrops(state, serverWorld, pos, world.getBlockEntity(pos), player, player.getMainHandItem());

            // Получаем случайное количество дополнительных предметов
            RandomSource random = world.getRandom();
            int bonusDrops = getRandomBonusCount(harvestLevel, random);

            // Сбрасываем стадию роста
            if (state.getBlock() instanceof CropBlock) {
                world.setBlock(pos, state.setValue(CropBlock.AGE, 0), 2);
            }

            // Выдаем опыт (1-3 за базовый урожай + 1 за каждый бонусный предмет)
            int xp = 1 + random.nextInt(3) + bonusDrops;
            if (xp > 0) {
                ExperienceOrb.award(serverWorld, Vec3.atCenterOf(pos), xp);
            }

            // Выдаем дропы (базовый + бонусные)
            if (!drops.isEmpty()) {
                ItemStack baseDrop = drops.get(0);
                int totalDrops = 1 + bonusDrops; // Базовый 1 + бонусные

                // Создаем один ItemStack с суммарным количеством
                ItemStack resultDrop = baseDrop.copy();
                resultDrop.setCount(totalDrops);
                Block.popResource(world, pos, resultDrop);
            }
        }
    }
    private int getRandomBonusCount(int harvestLevel, RandomSource random) {
        return switch (harvestLevel) {
            case 1 -> random.nextInt(3) + 1; // 1-3 бонусных (итого 2-4)
            case 2 -> random.nextInt(4) + 2; // 2-5 бонусных (итого 3-6)
            case 3 -> random.nextInt(5) + 3; // 3-7 бонусных (итого 4-8)
            default -> 0;
        };
    }

}