package net.rezolv.obsidanum.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


public class ObsidanAxe extends AxeItem {
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





    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

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
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            list.add(Component.translatable("item.obsidan.description.axe").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!world.isClientSide && isActivated(stack) && entity instanceof Player) {
            if (state.is(MINEABLE_LOGS_TAG)) {
                chainBreak(world, pos, (Player) entity, stack, state);
                deactivate(stack, (Player) entity, world.getGameTime());
                return true;
            } else if (state.is(MINEABLE_LEAVES_TAG)) {
                breakPlants(world, pos, (Player) entity, stack);
                deactivate(stack, (Player) entity, world.getGameTime());
                return true;
            } else if (isNetherLog(state) || isNetherFungus(state)) {
                breakNetherTree(world, pos, (Player) entity, stack);
                deactivate(stack, (Player) entity, world.getGameTime());
                return true;
            }
        }
        return super.mineBlock(stack, world, state, pos, entity);
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
}