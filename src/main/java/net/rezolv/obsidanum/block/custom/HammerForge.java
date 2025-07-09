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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;
import net.rezolv.obsidanum.sound.SoundsObs;
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
                .setValue(POWERED, false)
                .setValue(ANIMATION_TIME, 0));
    }

    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ANIMATION_TIME);
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
            level.scheduleTick(pos, this, 2);
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
    public static final IntegerProperty ANIMATION_TIME = IntegerProperty.create("animation_time", 0, 208);
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean isPowered = state.getValue(POWERED);
        boolean shouldBePowered = checkForPressedCorners(level, pos);

        if (isPowered) {
            // Уменьшаем счетчик времени анимации
            int animationTime = state.getValue(ANIMATION_TIME);
            if (animationTime > 0) {
                level.setBlock(pos, state.setValue(ANIMATION_TIME, animationTime - 1), 3);
            } else {
                level.setBlock(pos, state.setValue(POWERED, false), 3);
            }
        }

        level.scheduleTick(pos, this, 1);
    }

    private void processForgeStrike(ServerLevel level, BlockPos pos) {
        // Ищем ближайший тигель в радиусе 4 блоков
        int radius = 4;
        for (BlockPos checkPos : BlockPos.withinManhattan(pos, radius, radius, radius)) {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof ForgeCrucibleEntity crucible && crucible.isCrafting) {
                // Передаем удар в тигель
                crucible.processStrike();
                return;
            }
        }
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