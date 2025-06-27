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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;
import net.rezolv.obsidanum.particle.ParticlesObs;

import java.util.*;

public class SmolderingHoe extends HoeItem implements IUpgradeableItem{
    public SmolderingHoe(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    // Список разрешенных улучшений для этого инструмента
    private static final ObsidanumToolUpgrades[] ALLOWED_UPGRADES = {
            ObsidanumToolUpgrades.RICH_HARVEST,
            ObsidanumToolUpgrades.STRENGTH,
            ObsidanumToolUpgrades.BALANCING,
            ObsidanumToolUpgrades.SHARPENING,
            ObsidanumToolUpgrades.LONG_HANDLE
    };
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
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = world.getBlockState(pos);

        if (player == null) return InteractionResult.FAIL;

        // Новая логика для RICH_HARVEST
        if (state.getBlock() instanceof CropBlock cropBlock) {
            if (state.getValue(CropBlock.AGE) == cropBlock.getMaxAge()) {
                int harvestLevel = getUpgradeLevel(stack, ObsidanumToolUpgrades.RICH_HARVEST);
                if (harvestLevel > 0) {
                    harvestCrop(world, pos, state, player, harvestLevel);
                    return InteractionResult.sidedSuccess(world.isClientSide);
                }
            }
            return InteractionResult.PASS;
        }

        // Вызываем базовую реализацию для обработки земли
        return super.useOn(context);
    }
    private void harvestCrop(Level world, BlockPos pos, BlockState state, Player player, int harvestLevel) {
        if (!world.isClientSide && world instanceof ServerLevel serverWorld && state.getBlock() instanceof CropBlock cropBlock) {
            // Проверяем, достигло ли растение максимальной стадии
            if (state.getValue(CropBlock.AGE) == cropBlock.getMaxAge()) {
                // Получаем базовые дропы (1 предмет)
                List<ItemStack> drops = Block.getDrops(state, serverWorld, pos, world.getBlockEntity(pos), player, player.getMainHandItem());

                // Получаем случайное количество дополнительных предметов
                RandomSource random = world.getRandom();
                int bonusDrops = getRandomBonusCount(harvestLevel, random);

                // Сбрасываем стадию роста только если растение было полностью выросшим
                world.setBlock(pos, state.setValue(CropBlock.AGE, 0), 2);

                // Выдаем опыт (1-3 за базовый урожай + 1 за каждый бонусный предмет)
                int xp = 1 + random.nextInt(2) + bonusDrops;
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
    }
    private int getRandomBonusCount(int harvestLevel, RandomSource random) {
        return switch (harvestLevel) {
            case 1 -> random.nextInt(3) + 1; // 1-3 бонусных (итого 2-4)
            case 2 -> random.nextInt(4) + 2; // 2-5 бонусных (итого 3-6)
            case 3 -> random.nextInt(5) + 3; // 3-7 бонусных (итого 4-8)
            default -> 0;
        };
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

    }
    @Override
    public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
        boolean retval = super.mineBlock(itemstack, world, blockstate, pos, entity);

        ServerLevel serverLevel = (world instanceof ServerLevel) ? (ServerLevel) world : null;
        if (serverLevel == null) return retval;

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

            // Спавн предметов с правильным смещением и движением
            for (ItemStack result : results) {
                double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;
                double y = pos.getY() + 0.25 + world.random.nextDouble() * 0.5;
                double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 0.5;

                ItemEntity entityToSpawn = new ItemEntity(serverLevel, x, y, z, result);
                entityToSpawn.setPickUpDelay(10);

                // Добавляем естественное движение как при обычном дропе
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

        return retval;
    }
    @Override
    public boolean isEnchantable(ItemStack pStack) {
        return false;
    }
}