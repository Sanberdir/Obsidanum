package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ConnectedPillar extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty CONNECTED_TOP = BooleanProperty.create("connected_top");
    public static final BooleanProperty CONNECTED_BOTTOM = BooleanProperty.create("connected_bottom");
    public ConnectedPillar(BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(CONNECTED_TOP, false)
                .setValue(CONNECTED_BOTTOM, false));
    }
    @Override
    public BlockState rotate(BlockState pState, Rotation pRot) {
        return rotatePillar(pState, pRot);
    }
    public static BlockState rotatePillar(BlockState pState, Rotation pRotation) {
        switch (pRotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (pState.getValue(AXIS)) {
                    case X -> {
                        return pState.setValue(AXIS, Direction.Axis.Z);
                    }
                    case Z -> {
                        return pState.setValue(AXIS, Direction.Axis.X);
                    }
                    default -> {
                        return pState;
                    }
                }
            default:
                return pState;
        }
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AXIS, CONNECTED_TOP, CONNECTED_BOTTOM);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction.Axis axis = pContext.getClickedFace().getAxis();
        return this.defaultBlockState().setValue(AXIS, axis);
    }
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);

        // Проверяем, если направление совпадает с осью блока
        if (direction.getAxis() == axis) {
            boolean isConnected = neighborState.is(this) && neighborState.getValue(AXIS) == axis;

            switch (axis) {
                case Y -> {
                    if (direction == Direction.UP) {
                        state = state.setValue(CONNECTED_BOTTOM, isConnected);
                    } else if (direction == Direction.DOWN) {
                        state = state.setValue(CONNECTED_TOP, isConnected);
                    }
                }
                case X -> {
                    if (direction == Direction.EAST) {
                        state = state.setValue(CONNECTED_BOTTOM, isConnected);
                    } else if (direction == Direction.WEST) {
                        state = state.setValue(CONNECTED_TOP, isConnected);
                    }
                }
                case Z -> {
                    if (direction == Direction.NORTH) {
                        state = state.setValue(CONNECTED_BOTTOM, isConnected);
                    } else if (direction == Direction.SOUTH) {
                        state = state.setValue(CONNECTED_TOP, isConnected);
                    }
                }
            }
        }
        return state;
    }
    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return true;
    }
}