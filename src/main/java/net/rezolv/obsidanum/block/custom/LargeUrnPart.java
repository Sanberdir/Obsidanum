// LargeUrnPart.java
package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class LargeUrnPart extends HorizontalDirectionalBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }
    // Общие базовые части (все BASE одинаковы)
    private static final VoxelShape BASE = Block.box(0, 0, 0, 16, 8, 16);

    // Разные MIDDLE и TOP
    private static final VoxelShape MIDDLE_1 = Block.box(0, 8, 0, 12, 10, 12);
    private static final VoxelShape MIDDLE_2 = Block.box(4, 8, 0, 16, 10, 12);
    private static final VoxelShape MIDDLE_3 = Block.box(4, 8, 4, 16, 10, 16);
    private static final VoxelShape MIDDLE_4 = Block.box(0, 8, 4, 12, 10, 16);

    private static final VoxelShape TOP_1 = Block.box(0, 10, 0, 14, 14, 14);
    private static final VoxelShape TOP_2 = Block.box(2, 10, 0, 16, 14, 14);
    private static final VoxelShape TOP_3 = Block.box(2, 10, 2, 16, 14, 16);
    private static final VoxelShape TOP_4 = Block.box(0, 10, 2, 14, 14, 16);

    // DOWN_BASE (разные) и DOWN_TOP (все одинаковые)
    private static final VoxelShape DOWN_TOP = Block.box(0, 2, 0, 16, 16, 16);

    private static final VoxelShape DOWN_BASE_1 = Block.box(0, 0, 0, 14, 2, 14);
    private static final VoxelShape DOWN_BASE_2 = Block.box(0, 0, 2, 14, 2, 16);
    private static final VoxelShape DOWN_BASE_3 = Block.box(2, 0, 2, 16, 2, 16);
    private static final VoxelShape DOWN_BASE_4 = Block.box(2, 0, 0, 16, 2, 14);

    // Оригинальные SHAPE_XXX (без изменений)
    private static final VoxelShape SHAPE_000 = Shapes.or(DOWN_BASE_3, DOWN_TOP);
    private static final VoxelShape SHAPE_001 = Shapes.or(DOWN_BASE_4, DOWN_TOP);
    private static final VoxelShape SHAPE_010 = Shapes.or(BASE, MIDDLE_3, TOP_3);
    private static final VoxelShape SHAPE_011 = Shapes.or(BASE, MIDDLE_2, TOP_2);
    private static final VoxelShape SHAPE_100 = Shapes.or(DOWN_BASE_2, DOWN_TOP);
    private static final VoxelShape SHAPE_101 = Shapes.or(DOWN_BASE_1, DOWN_TOP);
    private static final VoxelShape SHAPE_110 = Shapes.or(BASE, MIDDLE_4, TOP_4);
    private static final VoxelShape SHAPE_111 = Shapes.or(BASE, MIDDLE_1, TOP_1);

    public LargeUrnPart(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.PART_000));
    }
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos main = UrnUtils.findMainPart(pos, state, level);
            if (!main.equals(pos)) {
                // не ломаем эту часть, а ломаем главный
                level.setBlock(pos, state, Block.UPDATE_ALL);
                level.destroyBlock(main, true);
                return;
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            BlockPos main = UrnUtils.findMainPart(pos, state, level);
            if (!main.equals(pos)) {
                BlockState mainState = level.getBlockState(main);
                if (mainState.getBlock() instanceof LargeUrnMain) {
                    level.destroyBlock(main, true);
                    return;
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case PART_000 -> SHAPE_000;
            case PART_001 -> SHAPE_001;
            case PART_010 -> SHAPE_010;
            case PART_011 -> SHAPE_011;
            case PART_100 -> SHAPE_100;
            case PART_101 -> SHAPE_101;
            case PART_110 -> SHAPE_110;
            case PART_111 -> SHAPE_111;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}