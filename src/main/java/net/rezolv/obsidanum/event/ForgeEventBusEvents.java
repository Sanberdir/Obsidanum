package net.rezolv.obsidanum.event;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.effect.EffectsObs;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.item.custom.EnchantedScroll;
import net.rezolv.obsidanum.item.custom.ObsAxe;
import net.rezolv.obsidanum.item.custom.ObsidanAxe;
import net.rezolv.obsidanum.item.custom.SmolderingAxe;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeCommand;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;
import net.rezolv.obsidanum.particle.ParticlesObs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventBusEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        UpgradeCommand.register(event.getDispatcher());
    }
    @SubscribeEvent
    public static void onBlockBreakObsidanAxe(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();

        // Работает только на сервере
        if (level.isClientSide) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof ObsidanAxe axe)) return;

        Map<ObsidanumToolUpgrades, Integer> upgrades = axe.getUpgrades(heldItem);
        if (!upgrades.containsKey(ObsidanumToolUpgrades.WOODCUTTER)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Только для бревен
        if (!state.is(BlockTags.LOGS)) return;

        // Если топор активирован - разрешаем стандартное разрушение
        if (axe.isActivated(heldItem)) {
            return; // Не отменяем событие
        }

        // Отменяем стандартное разрушение
        event.setCanceled(true);

        // Находим самый дальний блок в цепочке
        BlockPos furthest = axe.findFurthestConnectedLog(level, pos, state.getBlock());

        // Если это тот же блок (значит он один в цепочке)
        if (furthest.equals(pos)) {
            // Разрешаем стандартное разрушение
            event.setCanceled(false);
            return;
        }

        // Проверяем наличие и уровень HARVESTER
        int harvesterLevel = upgrades.getOrDefault(ObsidanumToolUpgrades.HARVESTER, 0);
        boolean shouldDouble = harvesterLevel > 0 && UpgradeLibrary.shouldDoubleDrops(harvesterLevel, level.random);

        // Разрушаем дальний блок
        BlockState furthestState = level.getBlockState(furthest);
        BlockEntity blockEntity = level.getBlockEntity(furthest);
        List<ItemStack> drops = Block.getDrops(furthestState, (ServerLevel)level, furthest, blockEntity, player, heldItem);
        level.destroyBlock(furthest, false);

        // Выбрасываем предметы (с возможным удвоением)
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
            // Добавляем дополнительный дроп, если HARVESTER сработал
            if (shouldDouble) {
                Block.popResource(level, pos, drop.copy());
            }
        }

        // Тратим прочность инструмента (если Strength не сработал)
        heldItem.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));

        // Воспроизводим звук разрушения блока
        level.playSound(null, pos, state.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onBlockBreakObsidianAxe(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();

        // Работает только на сервере
        if (level.isClientSide) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof ObsAxe axe)) return;

        Map<ObsidanumToolUpgrades, Integer> upgrades = axe.getUpgrades(heldItem);
        if (!upgrades.containsKey(ObsidanumToolUpgrades.WOODCUTTER)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Только для бревен
        if (!state.is(BlockTags.LOGS)) return;

        // Отменяем стандартное разрушение
        event.setCanceled(true);

        // Находим самый дальний блок в цепочке
        BlockPos furthest = axe.findFurthestConnectedLog(level, pos, state.getBlock());

        // Если это тот же блок (значит он один в цепочке)
        if (furthest.equals(pos)) {
            // Разрешаем стандартное разрушение
            event.setCanceled(false);
            return;
        }

        // Проверяем наличие и уровень HARVESTER
        int harvesterLevel = upgrades.getOrDefault(ObsidanumToolUpgrades.HARVESTER, 0);
        boolean shouldDouble = harvesterLevel > 0 && UpgradeLibrary.shouldDoubleDrops(harvesterLevel, level.random);

        // Разрушаем дальний блок
        BlockState furthestState = level.getBlockState(furthest);
        BlockEntity blockEntity = level.getBlockEntity(furthest);
        List<ItemStack> drops = Block.getDrops(furthestState, (ServerLevel)level, furthest, blockEntity, player, heldItem);
        level.destroyBlock(furthest, false);

        // Выбрасываем предметы (с возможным удвоением)
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
            // Добавляем дополнительный дроп, если HARVESTER сработал
            if (shouldDouble) {
                Block.popResource(level, pos, drop.copy());
            }
        }

        // Тратим прочность инструмента (если Strength не сработал)
        heldItem.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));

        // Воспроизводим звук разрушения блока
        level.playSound(null, pos, state.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }
    @SubscribeEvent
    public static void onBlockBreakSmolderingAxe(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        Level level = (Level) event.getLevel();

        if (level.isClientSide) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof SmolderingAxe axe)) return;

        Map<ObsidanumToolUpgrades, Integer> upgrades = axe.getUpgrades(heldItem);
        if (!upgrades.containsKey(ObsidanumToolUpgrades.WOODCUTTER)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(BlockTags.LOGS)) return;

        event.setCanceled(true);

        BlockPos furthest = axe.findFurthestConnectedLog(level, pos, state.getBlock());

        if (furthest.equals(pos)) {
            event.setCanceled(false);
            return;
        }

        int harvesterLevel = upgrades.getOrDefault(ObsidanumToolUpgrades.HARVESTER, 0);
        boolean shouldDouble = harvesterLevel > 0 && UpgradeLibrary.shouldDoubleDrops(harvesterLevel, level.random);

        BlockState furthestState = level.getBlockState(furthest);
        BlockEntity blockEntity = level.getBlockEntity(furthest);
        List<ItemStack> drops = Block.getDrops(furthestState, (ServerLevel)level, furthest, blockEntity, player, heldItem);
        level.destroyBlock(furthest, false);

        ServerLevel serverLevel = (ServerLevel) level;

        // Сначала обрабатываем все дропы и находим переплавленные версии
        List<ItemStack> processedDrops = new ArrayList<>();
        int totalExp = 0;

        for (ItemStack drop : drops) {
            Optional<SmeltingRecipe> recipeOpt = serverLevel.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drop), serverLevel);

            ItemStack result;
            if (recipeOpt.isPresent()) {
                result = recipeOpt.get().getResultItem(serverLevel.registryAccess()).copy();
                result.setCount(drop.getCount());
                totalExp += recipeOpt.get().getExperience() * drop.getCount();

                // Добавляем частицы для каждого переплавленного предмета
                for (int i = 0; i < drop.getCount(); i++) {
                    double offsetX = level.random.nextDouble() * 0.5 - 0.25;
                    double offsetY = level.random.nextDouble() * 0.5 - 0.25;
                    double offsetZ = level.random.nextDouble() * 0.5 - 0.25;
                    serverLevel.sendParticles(ParticlesObs.NETHER_FLAME2_PARTICLES.get(),
                            furthest.getX() + 0.5 + offsetX,
                            furthest.getY() + 0.5 + offsetY,
                            furthest.getZ() + 0.5 + offsetZ,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            } else {
                result = drop.copy();
            }

            processedDrops.add(result);

            // Если HARVESTER сработал, добавляем копию переплавленного предмета
            if (shouldDouble) {
                processedDrops.add(result.copy());
            }
        }

        // Выбрасываем все обработанные предметы
        for (ItemStack result : processedDrops) {
            Block.popResource(level, pos, result);
        }

        // Начисляем опыт за переплавку
        if (totalExp > 0) {
            serverLevel.addFreshEntity(new ExperienceOrb(serverLevel,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    totalExp));
        }

        heldItem.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));

        level.playSound(null, pos, state.getSoundType().getBreakSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onLivingVisibilityCheck(LivingEvent.LivingVisibilityEvent event) {
        // Проверяем, является ли сущность игроком
        if (event.getEntity() instanceof ServerPlayer player) {
            // Проверяем, есть ли у игрока эффект Inviolability
            MobEffectInstance inviolabilityEffect = player.getEffect(EffectsObs.INVIOLABILITY.get());

            // Если эффект есть, то меняем видимость
            if (inviolabilityEffect != null) {
                // Убедимся, что не происходит ошибка при изменении видимости
                try {
                    // Если эффект есть, ставим видимость 0.0 (невидимость)
                    event.modifyVisibility(0.0);
                } catch (Exception e) {
                    // Логируем ошибку, если что-то пошло не так
                    Obsidanum.LOGGER.error("Ошибка при изменении видимости: " + e.getMessage(), e);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // Только наш EnchantedScroll должен срабатывать
        if (right.getItem() != ItemsObs.ENCHANTED_SCROLL.get()) return;

        // Получаем список чар из свитка
        ListTag stored = EnchantedScroll.getEnchantments(right);
        if (stored.isEmpty()) return;
        System.out.println("Scroll enchants: " + stored);

        // Копируем левый предмет
        ItemStack result = left.copy();

        // Получаем текущие чары предмета
        ListTag existingEnchants = result.getEnchantmentTags();
        if (existingEnchants == null) {
            existingEnchants = new ListTag();
        }

        // Собираем текущие Enchantment-объекты
        List<Enchantment> appliedEnchantments = new ArrayList<>();
        for (int i = 0; i < existingEnchants.size(); i++) {
            CompoundTag tag = existingEnchants.getCompound(i);
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(tag.getString("id")));
            if (enchantment != null) {
                appliedEnchantments.add(enchantment);
            }
        }

        boolean enchantmentsAdded = false;

        // Обрабатываем чары из свитка
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag enchantmentTag = stored.getCompound(i);
            String enchantId = enchantmentTag.getString("id");
            int level = enchantmentTag.getInt("lvl");

            Enchantment newEnchant = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantId));
            if (newEnchant == null) continue;

            // Проверка: применимость к предмету
            if (!newEnchant.canApplyAtEnchantingTable(left) && !left.getItem().canApplyAtEnchantingTable(left, newEnchant)) {
                continue;
            }

            boolean found = false;
            boolean updated = false;

            // Проверяем наличие и уровень существующего чара
            for (int j = 0; j < existingEnchants.size(); j++) {
                CompoundTag existingTag = existingEnchants.getCompound(j);
                String existingId = existingTag.getString("id");

                if (existingId.equals(enchantId)) {
                    found = true;
                    int existingLevel = existingTag.getInt("lvl");
                    if (level > existingLevel) {
                        existingTag.putInt("lvl", level);
                        enchantmentsAdded = true;
                        updated = true;
                    }
                    break;
                }
            }

            // Если чар новый, проверяем на конфликты
            if (!found) {
                boolean hasConflict = false;
                for (Enchantment existing : appliedEnchantments) {
                    if (!newEnchant.isCompatibleWith(existing)) {
                        hasConflict = true;
                        break;
                    }
                }

                if (hasConflict) continue;

                CompoundTag newEnchantTag = new CompoundTag();
                newEnchantTag.putString("id", enchantId);
                newEnchantTag.putInt("lvl", level);
                existingEnchants.add(newEnchantTag);
                enchantmentsAdded = true;
            }

            // Если добавлен новый чар или улучшен — добавляем в список для проверки конфликтов следующих
            if (!found || updated) {
                appliedEnchantments.add(newEnchant);
            }
        }

        // Только если из свитка были применены или обновлены чары — показываем результат
        if (enchantmentsAdded) {
            result.getOrCreateTag().put("Enchantments", existingEnchants);
            event.setOutput(result);
            event.setCost(1);
            event.setMaterialCost(1);
        }
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(EffectsObs.MOROK.get())) {
            int amplifier = entity.getEffect(EffectsObs.MOROK.get()).getAmplifier();

            float originalDamage = event.getAmount();
            float multiplier = 1.0f;

            if (amplifier == 0) {
                multiplier = 1.20f; // +15%
            } else if (amplifier == 1) {
                multiplier = 1.40f; // +40%
            }

            event.setAmount(originalDamage * multiplier);
        }
    }

}