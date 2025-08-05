package net.rezolv.obsidanum.block.custom.large_urn;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rezolv.obsidanum.block.custom.multiblock_provider.MultiblockHandler;
import net.rezolv.obsidanum.block.custom.multiblock_provider.RotatingMultiblock;

public class LargeUrnMultiBlock extends RotatingMultiblock {
    // Основной конструктор (принимает форму + свойства)
    public LargeUrnMultiBlock(Properties pProperties) {
        super(MultiblockHandler.MULTIBLOCK_2X2X2, RotatingMultiblock.createMultiblockShapeBuilder(MultiblockHandler.MULTIBLOCK_2X2X2, SHAPE), pProperties);
        this.registerDefaultState(this.multiblockHandler.getCenterState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)));
    }
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        // зеркалим через поворот, привязанный к mirror
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
                                    box(0, 0, 2, 14, 2, 16),
                                    box(0, 2, 0, 16, 16, 16)),

                            Shapes.or(
                                    box(0, 0, 0, 14, 2, 14),
                                    box(0, 2, 0, 16, 16, 16)),
                    },
                    // width = 2
                    {
                            Shapes.empty(),
                            Shapes.or(
                                    box(2, 0, 2, 16, 2, 16),
                                    box(0, 2, 0, 16, 16, 16)),

                            Shapes.or(
                                    box(2, 0, 0, 16, 2, 14),
                                    box(0, 2, 0, 16, 16, 16)),
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
                                    box(0, 0, 0, 16, 8, 16),
                                    box(0, 8, 4, 12, 10, 16),
                                    box(0, 10, 2, 14, 14, 16)),
                            Shapes.or(
                                    box(0, 0, 0, 16, 8, 16),
                                    box(0, 8, 0, 12, 10, 12),
                                    box(0, 10, 0, 14, 14, 14)),
                    },
                    // width = 2
                    {
                            Shapes.empty(),
                            Shapes.or(
                                    box(0, 0, 0, 16, 8, 16),
                                    box(4, 8, 4, 16, 10, 16),
                                    box(2, 10, 2, 16, 14, 16)),
                            Shapes.or(
                                    box(0, 0, 0, 16, 8, 16),
                                    box(4, 8, 0, 16, 10, 12),
                                    box(2, 10, 0, 16, 14, 14)),
                    }
            }
    };
}