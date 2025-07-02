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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherrackBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;
import net.rezolv.obsidanum.particle.ParticlesObs;

import java.util.*;

public class SmolderingPickaxe extends PickaxeItem implements IUpgradeableItem {
    public SmolderingPickaxe(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

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


    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        boolean retval = super.onLeftClickEntity(stack, player, target);

        // Определяем 20% вероятность поджечь цель
        double chanceToIgnite = 0.20; // 20% вероятность

        // Генерируем случайное число от 0 до 1
        Random random = new Random();
        double roll = random.nextDouble();

        // Если выпало значение меньше или равно 20%, поджигаем цель
        if (roll <= chanceToIgnite && target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity) target;
            livingTarget.setSecondsOnFire(6); // Поджигаем цель на 5 секунд
        }

        return retval;
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

        if(Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.smoldering_obsidian.description.instrument").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }

    }

    @Override
    public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
        // Проверка STONE_BREAKER
        boolean isStoneBlockWithUpgrade = getUpgrades(itemstack).containsKey(ObsidanumToolUpgrades.STONE_BREAKER) &&
                isStoneBlock(blockstate);

        if (!isStoneBlockWithUpgrade) {
            boolean retval = super.mineBlock(itemstack, world, blockstate, pos, entity);
            if (!retval) return false;
        }

        // Основная обработка дропа и переплавки
        processBlockDrops(itemstack, world, blockstate, pos, entity);

        // Обработка HARVESTER
        int harvesterLevel = getUpgradeLevel(itemstack, ObsidanumToolUpgrades.HARVESTER);
        if (harvesterLevel > 0 && !world.isClientSide) {
            // Получаем базовые дропы (без учета переплавки)
            List<ItemStack> originalDrops = Block.getDrops(blockstate, (ServerLevel) world, pos, null, entity, itemstack);

            for (ItemStack originalDrop : originalDrops) {
                // Проверяем шанс удвоения для каждого предмета
                if (UpgradeLibrary.shouldDoubleDrops(harvesterLevel, world.random)) {
                    // Определяем результат переплавки (если есть)
                    Optional<SmeltingRecipe> recipeOpt = world.getRecipeManager()
                            .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(originalDrop), (ServerLevel) world);

                    ItemStack dropToAdd = recipeOpt.isPresent()
                            ? recipeOpt.get().getResultItem(world.registryAccess()).copy()
                            : originalDrop.copy();

                    // Добавляем только 1 дополнительный предмет
                    dropToAdd.setCount(1);
                    Block.popResource(world, pos, dropToAdd);

                }
            }
        }

        return true;
    }

    // Вынесенный метод для обработки дропа и переплавки
    private void processBlockDrops(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
        if (world.isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) world;
        List<ItemStack> drops = Block.getDrops(blockstate, serverLevel, pos, world.getBlockEntity(pos), entity, itemstack);

        if (!drops.isEmpty()) {
            List<ItemStack> results = new ArrayList<>();
            int totalExp = 0;

            for (ItemStack drop : drops) {
                Optional<SmeltingRecipe> recipeOpt = serverLevel.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drop), serverLevel);

                if (recipeOpt.isPresent()) {
                    // Добавление частиц
                    for (int i = 0; i < 5; i++) {
                        double offsetX = world.random.nextDouble() * 0.5 - 0.25;
                        double offsetY = world.random.nextDouble() * 0.5 - 0.25;
                        double offsetZ = world.random.nextDouble() * 0.5 - 0.25;
                        serverLevel.sendParticles(ParticlesObs.NETHER_FLAME2_PARTICLES.get(),
                                pos.getX() + 0.5 + offsetX,
                                pos.getY() + 0.5 + offsetY,
                                pos.getZ() + 0.5 + offsetZ,
                                1, 0.0, 0.0, 0.0, 0.0);
                    }

                    ItemStack smeltedResult = recipeOpt.get().getResultItem(serverLevel.registryAccess()).copy();
                    smeltedResult.setCount(drop.getCount());
                    results.add(smeltedResult);
                    totalExp += recipeOpt.get().getExperience() * 2;
                } else {
                    results.add(drop);
                }
            }

            // Спавн предметов
            for (ItemStack result : results) {
                double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
                double y = pos.getY() + 0.25 + world.random.nextDouble() * 0.5;
                double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;

                ItemEntity entityToSpawn = new ItemEntity(serverLevel, x, y, z, result);
                entityToSpawn.setPickUpDelay(10);
                entityToSpawn.setDeltaMovement(
                        world.random.nextGaussian() * 0.05,
                        world.random.nextGaussian() * 0.05 + 0.2,
                        world.random.nextGaussian() * 0.05
                );
                serverLevel.addFreshEntity(entityToSpawn);
            }

            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

            // Опыт от блока
            int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, itemstack);
            int silkTouchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemstack);
            int exp = blockstate.getBlock().getExpDrop(blockstate, serverLevel, serverLevel.getRandom(), pos, fortuneLevel, silkTouchLevel);

            if (exp > 0) {
                blockstate.getBlock().popExperience(serverLevel, pos, exp);
            }

            // Опыт за переплавку
            if (totalExp > 0) {
                while (totalExp > 0) {
                    int expToDrop = ExperienceOrb.getExperienceValue(totalExp);
                    totalExp -= expToDrop;
                    serverLevel.addFreshEntity(new ExperienceOrb(serverLevel,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            expToDrop));
                }
            }
        }
    }

    private static UUID generateUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes());
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // UUID-ы те же, что в inventoryTick
        UUID reachUUID = generateUUID("smoldering_pickaxe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("smoldering_pickaxe.long_handle_attack_range");

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
        UUID reachUUID = generateUUID("smoldering_pickaxe.long_handle_reach");
        UUID attackRangeUUID = generateUUID("smoldering_pickaxe.long_handle_attack_range");

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
                            "smoldering_pickaxe.long_handle_reach",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    reachAttr.addTransientModifier(reachMod);
                }

                // Применяем модификатор к дальности атаки
                if (attackRangeAttr != null) {
                    AttributeModifier attackRangeMod = new AttributeModifier(
                            attackRangeUUID,
                            "smoldering_pickaxe.long_handle_attack_range",
                            bonus,
                            AttributeModifier.Operation.ADDITION
                    );
                    attackRangeAttr.addTransientModifier(attackRangeMod);
                }
            }
        }
    }
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
            baseSpeed *= 15.0f;  // +50% за уровень
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
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }
}