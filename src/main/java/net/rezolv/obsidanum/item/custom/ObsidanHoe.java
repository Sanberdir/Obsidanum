package net.rezolv.obsidanum.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.item.upgrade.IUpgradeableItem;
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;
import net.rezolv.obsidanum.item.upgrade.UpgradeLibrary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    // Проверяем, разрешено ли улучшение для этого инструмента
    private boolean isUpgradeAllowed(ObsidanumToolUpgrades upgrade) {
        for (ObsidanumToolUpgrades allowed : ALLOWED_UPGRADES) {
            if (allowed == upgrade) {
                return true;
            }
        }
        return false;
    }

    // Переопределяем метод добавления улучшения с проверкой
    @Override
    public void addUpgrade(ItemStack stack, ObsidanumToolUpgrades upgrade, int level) {
        if (isUpgradeAllowed(upgrade)) {
            IUpgradeableItem.super.addUpgrade(stack, upgrade, level);
        }
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
            int levelUpg = entry.getValue();
            if (upg == ObsidanumToolUpgrades.RICH_HARVEST) {
                multiplier *= UpgradeLibrary.getRichHarvestMultiplier(levelUpg);
            }
            // Другие типы улучшений можно добавить здесь
        }

        if (!level.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) level;

            // Применяем множитель дропа
            if (multiplier > 1) {
                BlockEntity be = level.getBlockEntity(pos);
                List<ItemStack> originalDrops = Block.getDrops(state, serverWorld, pos, be, entity, stack);
                level.destroyBlock(pos, false);

                for (ItemStack drop : originalDrops) {
                    for (int i = 0; i < multiplier; i++) {
                        ItemEntity itemEntity = new ItemEntity(level,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                drop.copy());
                        level.addFreshEntity(itemEntity);
                    }
                }
            }

            // Обработка активированного режима
            if (isActivated(stack)) {
                for (BlockState targetBlock : TARGET_BLOCKS) {
                    if (state.is(targetBlock.getBlock())) {
                        BlockPos playerPos = entity.blockPosition();
                        for (BlockPos blockPos : BlockPos.betweenClosed(playerPos.offset(-20, -20, -20),
                                playerPos.offset(20, 20, 20))) {
                            BlockState targetState = level.getBlockState(blockPos);
                            for (BlockState targetBlockInner : TARGET_BLOCKS) {
                                if (targetState.is(targetBlockInner.getBlock())) {
                                    if (multiplier > 1) {
                                        BlockEntity be = level.getBlockEntity(blockPos);
                                        List<ItemStack> drops = Block.getDrops(targetState, serverWorld, blockPos, be, entity, stack);
                                        level.destroyBlock(blockPos, false);
                                        for (ItemStack drop : drops) {
                                            for (int i = 0; i < multiplier; i++) {
                                                ItemEntity e = new ItemEntity(level,
                                                        blockPos.getX() + 0.5,
                                                        blockPos.getY() + 0.5,
                                                        blockPos.getZ() + 0.5,
                                                        drop.copy());
                                                level.addFreshEntity(e);
                                            }
                                        }
                                    } else {
                                        level.destroyBlock(blockPos, true);
                                    }
                                    break;
                                }
                            }
                        }
                        deactivate(stack, (Player) entity, level.getGameTime());
                        return false;
                    }
                }
            }
        }

        return super.mineBlock(stack, level, state, pos, entity);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);

        // Выводим все улучшения
        Map<ObsidanumToolUpgrades, Integer> upgrades = getUpgrades(itemstack);
        for (Map.Entry<ObsidanumToolUpgrades, Integer> entry : upgrades.entrySet()) {
            list.add(Component.literal("Улучшение: " + entry.getKey().getName() + " " + entry.getValue())
                    .withStyle(ChatFormatting.GOLD));
        }

        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.obsidan.description.hoe").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public void activate(ItemStack stack, long currentTime) {
        stack.getOrCreateTag().putBoolean(TAG_ACTIVATED, true);
        stack.getOrCreateTag().putLong(TAG_LAST_ACTIVATION_TIME, currentTime);
        stack.getOrCreateTag().putInt("CustomModelData", 1);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!world.isClientSide && isActivated(stack)) {
            long currentTime = world.getGameTime();
            long lastActivationTime = stack.getOrCreateTag().getLong(TAG_LAST_ACTIVATION_TIME);

            if (currentTime - lastActivationTime >= ACTIVATION_DURATION) {
                if (entity instanceof Player) {
                    deactivate(stack, (Player) entity, currentTime);
                }
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
        // Без изменений из оригинального кода
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();
        BlockState state = world.getBlockState(pos);

        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (!player.mayUseItemAt(pos.relative(context.getClickedFace()), context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }

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
                stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}