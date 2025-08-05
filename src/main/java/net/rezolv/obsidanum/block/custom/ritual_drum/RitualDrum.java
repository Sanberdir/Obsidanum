package net.rezolv.obsidanum.block.custom.ritual_drum;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rezolv.obsidanum.block.custom.multiblock_provider.MultiblockHandler;
import net.rezolv.obsidanum.block.custom.multiblock_provider.RotatingMultiblock;


public class RitualDrum extends RotatingMultiblock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RitualDrum(Properties pProperties) {
        super(MultiblockHandler.MULTIBLOCK_2X2X2, RotatingMultiblock.createMultiblockShapeBuilder(MultiblockHandler.MULTIBLOCK_2X2X2, SHAPE), pProperties);
        this.registerDefaultState(this.multiblockHandler.getCenterState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }
    public static final VoxelShape[][][] SHAPE = new VoxelShape[][][] {
            // height = 0
            {},
            // height = 1
            {
                    // width = 0
                    {},
                    // width = 1
                    {
                            Shapes.empty(),
                            Shapes.or(
                                    box(0, 0, 6, 11, 10, 16),
                                    box(0, 10, 9, 12, 16, 16)),

                            Shapes.or(
                                    box(0, 0, 0, 11, 10, 16),
                                    box(0, 10, 0, 12, 16, 13)),
                    },
                    // width = 2
                    {
                            Shapes.empty(),
                            Shapes.or(
                                    box(1, 0, 6, 16, 10, 16),
                                    box(0, 10, 9, 16, 16, 16)),

                            Shapes.or(
                                    box(1, 0, 0, 16, 10, 16),
                                    box(0, 10, 0, 16, 16, 13)),
                    }
            },
            // height = 2
            {
                    // width = 0
                    {},
                    // width = 1
                    {
                            Shapes.empty(),
                            Shapes.or(
                                    box(0, 0, 9, 12, 14, 16)),
                            Shapes.or(
                                    box(0, 0, 0, 12, 14, 13)),
                    },
                    // width = 2
                    {
                            Shapes.empty(),
                            Shapes.or(
                                    box(0, 0, 9, 16, 14, 16)),
                            Shapes.or(
                                    box(0, 0, 0, 16, 14, 13)),
                    }
            }
    };
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }


}