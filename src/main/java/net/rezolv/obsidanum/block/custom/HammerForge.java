package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HammerForge extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public HammerForge(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(POWERED, false));
    }

    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return; // Только на сервере

        boolean hasPressedCorner = checkForPressedCorners(level, pos);
        boolean shouldBePowered = hasPressedCorner;

        // Если состояние изменилось с false → true
        if (!state.getValue(POWERED) && shouldBePowered) {
            // Ставим powered = true и планируем одиночный тик через 15 тиков
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, this, 15);
        }
        // Если упали обратно в false — просто обновляем состояние
        else if (state.getValue(POWERED) && !shouldBePowered) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1); // Запускаем тики
        }
    }
    private static final Map<BlockPos, Integer> tickCounters = new HashMap<>();

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean isPowered = checkForPressedCorners(level, pos);

        // Обновляем состояние на всякий случай, если соседние углы изменились вне neighborChanged
        if (state.getValue(POWERED) != isPowered) {
            level.setBlock(pos, state.setValue(POWERED, isPowered), Block.UPDATE_ALL);
            if (!isPowered) {
                tickCounters.remove(pos);
            } else {
                tickCounters.put(pos, 0);
            }
        }

        // Обработка звука — только один раз через 15 тиков после активации
        if (isPowered && tickCounters.containsKey(pos)) {
            int count = tickCounters.get(pos) + 1;
            if (count == 10) {
                level.playSound(null, pos,
                        net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        1.5F, 1.0F);
                // удаляем, чтобы звук не зацикливался
                tickCounters.remove(pos);
            } else {
                tickCounters.put(pos, count);
            }
        }

        // Перезапланируем следующий тик
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!oldState.is(newState.getBlock())) {
            tickCounters.remove(pos);
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }
    // Метод для проверки нажатых углов в радиусе 4 блоков
    public boolean checkForPressedCorners(Level level, BlockPos centerPos) {
        if (level.isClientSide) return false; // Работаем только на серверной стороне

        int radius = 4;
        int radiusSquared = radius * radius; // Квадрат радиуса для оптимизации
        Block cornerBlock = BlocksObs.LEFT_CORNER_LEVEL.get();

        // Перебираем куб 9x9x9, но проверяем расстояние
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Пропускаем блоки вне сферы
                    if (x * x + y * y + z * z > radiusSquared) {
                        continue;
                    }

                    BlockPos checkPos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.getBlock() == cornerBlock &&
                            state.hasProperty(LeftCornerLevel.IS_PRESSED) &&
                            state.getValue(LeftCornerLevel.IS_PRESSED)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction.getOpposite());
        return level.getBlockState(blockpos).isFaceSturdy(level, blockpos, direction);
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new HammerForgeEntity(blockPos, blockState);
    }
}
