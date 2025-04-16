package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.block.entity.FlamePipeEntity;
import org.jetbrains.annotations.Nullable;

public class FlamePipe extends BaseEntityBlock {
    public static final BooleanProperty IS_ACTIVE = BooleanProperty.create("is_active");
    public FlamePipe(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(IS_ACTIVE, false));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IS_ACTIVE);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(IS_ACTIVE, false);

    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        // Если блок ещё не активирован, запланировать проверку через 10 тиков
        if (!state.getValue(IS_ACTIVE)) {
            level.scheduleTick(pos, this, 10); // Планируем активацию через 10 тиков
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Проверяем условие активации через 10 тиков
        boolean isActive = checkActivation(level, pos);

        // Если условия выполнены, активируем блок
        if (isActive && !state.getValue(IS_ACTIVE)) {
            level.setBlock(pos, state.setValue(IS_ACTIVE, true), 3);

            // Устанавливаем лаву сверху
            BlockPos abovePos = pos.above();
            if (level.isEmptyBlock(abovePos)) {
                level.setBlock(abovePos, BlocksObs.NETHER_FLAME_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    private boolean checkActivation(Level level, BlockPos pos) {
        // Проверяем соседние блоки
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            // Проверяем на наличие лавы или активного блока FlamePipe
            if (neighborState.is(BlocksObs.NETHER_FLAME_BLOCK.get()) ||
                    (neighborState.getBlock() instanceof FlamePipe && neighborState.getValue(IS_ACTIVE))) {
                return true;
            }
        }
        return false;
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new FlamePipeEntity(blockPos, blockState);
    }
}