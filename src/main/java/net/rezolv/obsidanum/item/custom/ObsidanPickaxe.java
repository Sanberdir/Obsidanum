package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.NetherrackBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ObsidanPickaxe extends PickaxeItem implements IUpgradeableItem {
    // NBT теги
    private static final String ACTIVATED_TAG = "Activated";
    private static final String LAST_ACTIVATION_TAG = "LastActivationTime";
    private static final String COOLDOWN_END_TAG = "CooldownEndTime";
    private static final String CUSTOM_MODEL_TAG = "CustomModelData";

    public static final long COOLDOWN_DURATION = 120 * 20; // 120 секунд
    private static final long ACTIVATION_DURATION = 5 * 20; // 5 секунд

    private static final Block[] INSTANT_BREAK_BLOCKS = {
            Blocks.STONE,
            Blocks.COBBLESTONE,
            Blocks.DIORITE,
            Blocks.GRANITE,
            Blocks.ANDESITE,
            Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE
    };

    // Список разрешенных улучшений для этого инструмента
    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.SHARPENING,
            ObsidanumToolUpgrades.HARVESTER,
            ObsidanumToolUpgrades.STONE_BREAKER,
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
            if (upgrade == ObsidanumToolUpgrades.HARVESTER
                    || upgrade == ObsidanumToolUpgrades.BALANCING
                    || upgrade == ObsidanumToolUpgrades.LONG_HANDLE
                    || upgrade == ObsidanumToolUpgrades.STONE_BREAKER) {
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

        if (upgrade == ObsidanumToolUpgrades.STONE_BREAKER) {
            return (int) (baseDurability * 0.2); // 20% от максимума
        }
        else if (upgrade == ObsidanumToolUpgrades.HARVESTER) {
            return baseDurability - 200; // -200 за уровень (уже учтено в getMaxDamageWithoutPenalties)
        }
        else if (upgrade == ObsidanumToolUpgrades.LONG_HANDLE) {
            return baseDurability - 50;
        }
        else if (upgrade == ObsidanumToolUpgrades.BALANCING) {
            return baseDurability - 25;
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

        // Если есть STONE_BREAKER, уменьшаем до 20%
        if (getUpgrades(stack).containsKey(ObsidanumToolUpgrades.STONE_BREAKER)) {
            return (int) (baseDurability * 0.2);
        }


        return baseDurability;
    }
    public ObsidanPickaxe(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
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
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("obsidan_pickaxe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obsidan_pickaxe.long_handle_attack_range");

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

        // Важно: работаем только на серверной стороне и для игроков
        if (!(entity instanceof Player player) || world.isClientSide) return;

        // Получаем ссылки на атрибуты
        var reachAttr = player.getAttribute(ForgeMod.BLOCK_REACH.get());
        var attackRangeAttr = player.getAttribute(ForgeMod.ENTITY_REACH.get());

        // Генерируем UUID для модификаторов
        UUID reachUUID = generateUUID("obsidan_pickaxe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("obsidan_pickaxe.long_handle_attack_range");

        // Удаляем старые модификаторы (если есть)
        if (reachAttr != null && reachAttr.getModifier(reachUUID) != null) {
            reachAttr.removeModifier(reachUUID);
        }
        if (attackRangeAttr != null && attackRangeAttr.getModifier(attackRangeUUID) != null) {
            attackRangeAttr.removeModifier(attackRangeUUID);
        }

        // Ключевое исправление: проверяем, что инструмент в руке
        boolean isInHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;

        if (isInHand) {
            int level = getUpgradeLevel(stack, ObsidanumToolUpgrades.LONG_HANDLE);
            if (level > 0) {
                double bonus = UpgradeLibrary.getLongHandleBonus(level);

                // Применяем модификатор к дальности взаимодействия
                if (reachAttr != null) {
                    AttributeModifier reachMod = new AttributeModifier(
                            reachUUID,
                            "obsidan_pickaxe.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем модификатор к дальности атаки
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            attackRangeUUID,
                            "obsidan_pickaxe.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }

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
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }
    // Вспомогательный метод для проверки каменных блоков
    private boolean isStoneBlock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) ||
                state.is(BlockTags.BASE_STONE_NETHER) ||
                state.getBlock() instanceof NetherrackBlock ||
                state.is(Blocks.BASALT) ||
                state.is(Blocks.COBBLESTONE) ||
                state.is(Blocks.BLACKSTONE);
    }
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getDestroySpeed(stack, state);

        // Получаем уровни улучшений
        int harvesterLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STRENGTH);
        int balancingLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.BALANCING);
        int stoneBreakerLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.STONE_BREAKER);
        if (stoneBreakerLevel > 0 && isStoneBlock(state)) {
            baseSpeed *= 10.0f;  // +50% за уровень
        }
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
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        // Обработка STONE_BREAKER - если блок каменный, не тратим прочность
        boolean isStoneBlock = getUpgrades(stack).containsKey(ObsidanumToolUpgrades.STONE_BREAKER) &&
                (state.is(BlockTags.BASE_STONE_OVERWORLD) ||
                        state.is(BlockTags.BASE_STONE_NETHER) ||
                        state.getBlock() instanceof NetherrackBlock ||
                        state.is(Blocks.BASALT) ||
                        state.is(Blocks.COBBLESTONE) ||
                        state.is(Blocks.BLACKSTONE));

        if (isStoneBlock) {
            // Пропускаем стандартное уменьшение прочности
            boolean result = true;

            // Проверяем активированный режим (даже для каменных блоков!)
            if (!level.isClientSide && isActivated(stack)) {
                Block block = state.getBlock();
                if (isInstantBreakBlock(block)) {
                    level.destroyBlock(pos, false);

                    if (level.random.nextFloat() < 0.2f) {
                        ItemStack diamond = new ItemStack(Items.DIAMOND);
                        Block.popResource(level, pos, diamond);
                    }

                    if (entity instanceof Player) {
                        deactivate(stack, (Player) entity, level.getGameTime());
                    }
                }
            }

            return result;
        }

        // Стандартная обработка для НЕ каменных блоков
        boolean result = super.mineBlock(stack, level, state, pos, entity);

        // Обработка HARVESTER
        int harvesterLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.HARVESTER);
        if (result && harvesterLevel > 0 && !level.isClientSide) {
            if (UpgradeLibrary.shouldDoubleDrops(harvesterLevel, level.random)) {
                List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, null, entity, stack);
                for (ItemStack drop : drops) {
                    Block.popResource(level, pos, drop.copy());
                }
            }
        }

        // Проверка активированного режима для НЕ каменных блоков
        if (!level.isClientSide && isActivated(stack)) {
            Block block = state.getBlock();
            if (isInstantBreakBlock(block)) {
                level.destroyBlock(pos, false);

                if (level.random.nextFloat() < 0.2f) {
                    ItemStack diamond = new ItemStack(Items.DIAMOND);
                    Block.popResource(level, pos, diamond);
                }

                if (entity instanceof Player) {
                    deactivate(stack, (Player) entity, level.getGameTime());
                }
            }
        }

        return result;
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

        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.obsidan.description.pickaxe").withStyle(ChatFormatting.DARK_GRAY));
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

    private boolean isInstantBreakBlock(Block block) {
        for (Block instantBreakBlock : INSTANT_BREAK_BLOCKS) {
            if (block == instantBreakBlock) {
                return true;
            }
        }
        return false;
    }
}