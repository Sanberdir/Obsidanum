package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rezolv.obsidanum.block.BlocksObs;

public class LeftCornerLevel extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty IS_PRESSED = BooleanProperty.create("is_pressed");
    private static final int PRESSED_DURATION = 220; // 11 секунд (20 тиков/сек * 11)
    public LeftCornerLevel(Properties pProperties) {
        super(pProperties); // Включаем поддержку тиков
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(IS_PRESSED, false)
        );
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (!pLevel.isClientSide && pState.getValue(IS_PRESSED)) {
            pLevel.setBlock(pPos, pState.setValue(IS_PRESSED, false), 3);
            pLevel.playSound(null, pPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide && !pState.getValue(IS_PRESSED) && pLevel.getBlockState(pPos.below(2)).is(BlocksObs.NETHER_FLAME_BLOCK.get())) {
            pLevel.setBlock(pPos, pState.setValue(IS_PRESSED, true), 3);
            // Запланировать тик через 220 тиков (11 секунд)
            pLevel.scheduleTick(pPos, this, PRESSED_DURATION);
            pLevel.playSound(null, pPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, IS_PRESSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(IS_PRESSED, false);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
}