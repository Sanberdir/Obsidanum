package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class ObsidanAxe extends AxeItem implements IUpgradeableItem{
    // Константы для NBT-тегов
    private static final String ACTIVATED_TAG = "Activated";
    private static final String LAST_ACTIVATION_TAG = "LastActivationTime";
    private static final String COOLDOWN_END_TAG = "CooldownEndTime";
    private static final String DURABILITY_LOST_TAG = "DurabilityLost";
    private static final String CUSTOM_MODEL_DATA_TAG = "CustomModelData";

    private static final TagKey<Block> MINEABLE_LOGS_TAG = BlockTags.create(new ResourceLocation("minecraft", "logs"));
    private static final TagKey<Block> MINEABLE_LEAVES_TAG = BlockTags.create(new ResourceLocation("minecraft", "leaves"));

    public static final long COOLDOWN_DURATION = 25 * 20; // 25 секунд в тиках
    private static final long ACTIVATION_DURATION = 5 * 20; // 5 секунд в тиках


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
    public ObsidanAxe(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            public float getCooldownPercent(ItemStack stack, Player player, float partialTicks) {
                long currentTime = player.level().getGameTime();
                long cooldownEnd = getCooldownEndTime(stack);

                if (currentTime >= cooldownEnd) return 0.0f;

                long cooldownDuration = COOLDOWN_DURATION;
                long remaining = cooldownEnd - currentTime;
                return (float) remaining / (float) cooldownDuration;
            }
        });
    }






    private static UUID generateUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }
    // Вместо LONG_HANDLE_REACH_UUID используйте:
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("obsidan_axe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obsidan_axe.long_handle_attack_range");

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
            reachAttr.removeModifier(generateUUID("obsidan_axe.long_handle_reach")); // Удаляем по сгенерированному UUID
        }

// Обработка дальности атаки через Forge-специфичный атрибут
        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (attackRangeAttr != null) {
            attackRangeAttr.removeModifier(generateUUID("obsidan_axe.long_handle_attack_range")); // Удаляем по сгенерированному UUID
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
                            generateUUID("obsidan_axe.long_handle_reach"), // Динамически генерируемый UUID
                            "obsidan_axe.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем бонус к дальности атаки мобов
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            generateUUID("obsidan_axe.long_handle_attack_range"), // Динамически генерируемый UUID
                            "obsidan_axe.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }

        // УДАЛЯЕМ запись времени в NBT
        if (!world.isClientSide && isActivated(stack)) {
            long lastActivationTime = getLastActivationTime(stack);
            if (world.getGameTime() - lastActivationTime >= ACTIVATION_DURATION) {
                if (entity instanceof Player) {
                    deactivate(stack, (Player) entity, world.getGameTime());
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
        } else {
            return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
        }
    }

    // Активация предмета
    public void activate(ItemStack stack, long currentTime) {
        stack.getOrCreateTag().putBoolean(ACTIVATED_TAG, true);
        stack.getOrCreateTag().putInt(CUSTOM_MODEL_DATA_TAG, 1);
        stack.getOrCreateTag().putBoolean(DURABILITY_LOST_TAG, false);
        setLastActivationTime(stack, currentTime);
    }

    // Деактивация с установкой кулдауна
    public void deactivate(ItemStack stack, Player player, long currentTime) {
        stack.getOrCreateTag().putBoolean(ACTIVATED_TAG, false);
        stack.getOrCreateTag().putInt(CUSTOM_MODEL_DATA_TAG, 0);
        stack.getOrCreateTag().putBoolean(DURABILITY_LOST_TAG, false);
        setCooldownEndTime(stack, currentTime + COOLDOWN_DURATION);
    }

    // Проверка активации
    public boolean isActivated(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(ACTIVATED_TAG);
    }

    // Методы для работы с NBT-тегами
    private long getLastActivationTime(ItemStack stack) {
        return stack.getOrCreateTag().getLong(LAST_ACTIVATION_TAG);
    }

    private void setLastActivationTime(ItemStack stack, long time) {
        stack.getOrCreateTag().putLong(LAST_ACTIVATION_TAG, time);
    }

    private long getCooldownEndTime(ItemStack stack) {
        return stack.getOrCreateTag().getLong(COOLDOWN_END_TAG);
    }

    private void setCooldownEndTime(ItemStack stack, long time) {
        stack.getOrCreateTag().putLong(COOLDOWN_END_TAG, time);
    }

    // Остальные методы остаются без изменений
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

        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.obsidan.description.axe").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entity) {
        if (world.isClientSide || !(entity instanceof Player player)) {
            return super.mineBlock(stack, world, state, pos, entity);
        }

        Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(stack);
        int harvesterLevel = upgrades.getOrDefault(ObsidanumToolUpgrades.HARVESTER, 0);
        boolean hasWoodcutter = upgrades.containsKey(ObsidanumToolUpgrades.WOODCUTTER);

        // 1) Цепное разрушение
        if (isActivated(stack)) {
            if (state.is(MINEABLE_LOGS_TAG)) {
                chainBreak(world, pos, player, stack, state);
                deactivate(stack, player, world.getGameTime());
                return true;
            } else if (state.is(MINEABLE_LEAVES_TAG)) {
                breakPlants(world, pos, player, stack);
                deactivate(stack, player, world.getGameTime());
                return true;
            } else if (isNetherLog(state) || isNetherFungus(state)) {
                breakNetherTree(world, pos, player, stack);
                deactivate(stack, player, world.getGameTime());
                return true;
            }
        }

        // 2) Улучшения
        // 2.1) WOODCUTTER + лог
        if (hasWoodcutter && state.is(MINEABLE_LOGS_TAG)) {
            BlockPos furthest = findFurthestConnectedLog(world, pos, state.getBlock());

            if (!furthest.equals(pos)) {
                BlockState furthestState = world.getBlockState(furthest);

                // Проверяем HARVESTER для удвоения дропа
                if (harvesterLevel > 0 && UpgradeLibrary.shouldDoubleDrops(harvesterLevel, world.random)) {
                    List<ItemStack> drops = Block.getDrops(furthestState, (ServerLevel) world, furthest, null, entity, stack);
                    for (ItemStack drop : drops) {
                        Block.popResource(world, furthest, drop.copy());
                    }
                }

                stack.hurtAndBreak(1, player, (p) -> {
                    p.broadcastBreakEvent(EquipmentSlot.MAINHAND);
                });

                world.playSound(null, pos, state.getSoundType().getBreakSound(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);

                return true;
            }
        }

        // Обычное разрушение с проверкой HARVESTER
        boolean result = super.mineBlock(stack, world, state, pos, entity);

        // Проверяем HARVESTER для обычного разрушения
        if (result && harvesterLevel > 0 && (state.is(MINEABLE_LOGS_TAG) || state.is(MINEABLE_LEAVES_TAG))) {
            if (UpgradeLibrary.shouldDoubleDrops(harvesterLevel, world.random)) {
                List<ItemStack> drops = Block.getDrops(state, (ServerLevel) world, pos, null, entity, stack);
                for (ItemStack drop : drops) {
                    Block.popResource(world, pos, drop.copy());
                }
            }
        }

        return result;
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

    private boolean isNetherLog(BlockState state) {
        return state.getBlock() == Blocks.CRIMSON_STEM || state.getBlock() == Blocks.WARPED_STEM;
    }

    private boolean isNetherFungus(BlockState state) {
        return state.getBlock() == Blocks.NETHER_WART_BLOCK ||
                state.getBlock() == Blocks.WARPED_WART_BLOCK ||
                state.getBlock() == Blocks.SHROOMLIGHT;
    }

    private void chainBreak(Level world, BlockPos pos, Player player, ItemStack stack, BlockState state) {
        if (isNetherLog(state) || isNetherFungus(state)) {
            breakNetherTree(world, pos, player, stack);
        } else if (state.is(MINEABLE_LOGS_TAG)) {
            breakTree(world, pos, player, stack);
        } else if (state.is(MINEABLE_LEAVES_TAG)) {
            breakPlants(world, pos, player, stack);
        }
    }

    private void breakTree(Level world, BlockPos startPos, Player player, ItemStack stack) {
        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        Block startBlock = world.getBlockState(startPos).getBlock();

        findConnectedBlocks(world, startPos, logs, startBlock, 1024);

        for (BlockPos logPos : logs) {
            findLeaves(world, logPos, leaves, 2048);
        }

        destroyBlocks(world, logs, player, stack);
        destroyBlocks(world, leaves, player, stack);
    }

    private void breakNetherTree(Level world, BlockPos startPos, Player player, ItemStack stack) {
        Set<BlockPos> netherBlocks = new HashSet<>();
        findConnectedNetherTreeBlocks(world, startPos, netherBlocks, 2048);
        destroyBlocks(world, netherBlocks, player, stack);
    }

    private void findConnectedBlocks(Level world, BlockPos pos, Set<BlockPos> result, Block targetBlock, int max) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);

        while (!queue.isEmpty() && result.size() < max) {
            BlockPos current = queue.poll();
            if (result.contains(current)) continue;
            BlockState state = world.getBlockState(current);
            if (state.getBlock() == targetBlock) {
                result.add(current);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            queue.add(current.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }
    }

    private void findConnectedNetherTreeBlocks(Level world, BlockPos pos, Set<BlockPos> result, int max) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);

        while (!queue.isEmpty() && result.size() < max) {
            BlockPos current = queue.poll();
            if (result.contains(current)) continue;
            BlockState state = world.getBlockState(current);
            if (isNetherLog(state) || isNetherFungus(state)) {
                result.add(current);
                queue.add(current.offset(1, 0, 0));
                queue.add(current.offset(-1, 0, 0));
                queue.add(current.offset(0, 0, 1));
                queue.add(current.offset(0, 0, -1));
                queue.add(current.offset(0, 1, 0));
                queue.add(current.offset(0, -1, 0));
            }
        }
    }

    private void findLeaves(Level world, BlockPos pos, Set<BlockPos> result, int max) {
        BlockPos.betweenClosedStream(pos.offset(-5, -3, -5), pos.offset(5, 3, 5))
                .filter(p -> world.getBlockState(p).is(MINEABLE_LEAVES_TAG))
                .limit(max)
                .forEach(p -> result.add(p.immutable()));
    }

    private void breakPlants(Level world, BlockPos pos, Player player, ItemStack stack) {
        AtomicInteger counter = new AtomicInteger(0);
        BlockPos.betweenClosedStream(pos.offset(-7, -3, -7), pos.offset(7, 3, 7))
                .filter(p -> world.getBlockState(p).is(MINEABLE_LEAVES_TAG))
                .limit(300)
                .forEach(p -> {
                    if (counter.getAndIncrement() < 300) {
                        world.destroyBlock(p, true);
                    }
                });
    }

    private void destroyBlocks(Level world, Collection<BlockPos> positions, Player player, ItemStack stack) {
        boolean durabilityLost = stack.getOrCreateTag().getBoolean(DURABILITY_LOST_TAG);

        positions.forEach(p -> {
            if (p.distSqr(player.blockPosition()) < 4096) {
                world.destroyBlock(p, true);
                if (!durabilityLost) {
                    stack.getOrCreateTag().putBoolean(DURABILITY_LOST_TAG, true);
                }
            }
        });
    }
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }
}