package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ObsidianLectern extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty THERE_BOOK = BooleanProperty.create("there_book");
    public ObsidianLectern(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING,Direction.NORTH)
                .setValue(THERE_BOOK,true)
        );
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(THERE_BOOK,true);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING,THERE_BOOK);
    }


    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide) { // Проверяем, что код выполняется на сервере
            if (pState.getValue(THERE_BOOK)) { // Проверяем, есть ли книга
                pLevel.setBlock(pPos, pState.setValue(THERE_BOOK, false), 3); // Устанавливаем значение THERE_BOOK в false

                // Генерируем 500 очков опыта в виде сфер
                int totalExperience = 500;
                while (totalExperience > 0) {
                    int orbValue = Math.min(totalExperience, 50); // Определяем размер одной сферы (максимум 10 опыта за одну сферу)
                    totalExperience -= orbValue;

                    // Создаём сферу опыта в позиции блока с небольшим смещением
                    ExperienceOrb experienceOrb = new ExperienceOrb(pLevel,
                            pPos.getX() + 0.5 + (pLevel.random.nextDouble() - 0.5),
                            pPos.getY() + 1.0,
                            pPos.getZ() + 0.5 + (pLevel.random.nextDouble() - 0.5),
                            orbValue);

                    pLevel.addFreshEntity(experienceOrb); // Добавляем сферу опыта в мир
                }

                return InteractionResult.SUCCESS; // Указываем успешное взаимодействие
            }
        }
        return InteractionResult.PASS; // Если нет книги, ничего не делаем
    }

}
