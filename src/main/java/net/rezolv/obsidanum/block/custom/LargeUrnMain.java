// LargeUrnMain.java
package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rezolv.obsidanum.block.BlocksObs;

import javax.annotation.Nullable;

public class LargeUrnMain extends HorizontalDirectionalBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public LargeUrnMain(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.PART_000));
    }
    private static final VoxelShape BASE_SHAPE = Block.box(2, 0, 2, 16, 2, 16);
    private static final VoxelShape TOP_SHAPE = Block.box(0, 2, 0, 16, 16, 16);
    private static final VoxelShape COMBINED_SHAPE = Shapes.or(BASE_SHAPE,TOP_SHAPE);
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return COMBINED_SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos mainPos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();

        // Проверка — можно ли поставить все части?
        for (int dy = 0; dy < 2; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 2; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue; // mainPos
                    BlockPos pos = UrnUtils.calculatePosition(mainPos, facing, dx, dy, dz);
                    if (!level.getBlockState(pos).canBeReplaced()) {
                        level.destroyBlock(mainPos, true);
                        return;
                    }
                }
            }
        }

        // Установка всех частей
        for (int dy = 0; dy < 2; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 2; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos partPos = UrnUtils.calculatePosition(mainPos, facing, dx, dy, dz);
                    Part part = Part.fromCoords(dx, dy, dz);
                    BlockState partState = BlocksObs.LARGE_URN_PART.get().defaultBlockState()
                            .setValue(LargeUrnPart.FACING, facing)
                            .setValue(LargeUrnPart.PART, part);
                    level.setBlock(partPos, partState, 3);
                }
            }
        }
    }
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            for (BlockPos part : UrnUtils.getAllConnectedParts(level, pos, state)) {
                if (!part.equals(pos)) level.destroyBlock(part, false);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (BlockPos part : UrnUtils.getAllConnectedParts(level, pos, state)) {
                if (!part.equals(pos)) level.destroyBlock(part, false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
