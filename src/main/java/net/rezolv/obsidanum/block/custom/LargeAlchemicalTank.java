package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rezolv.obsidanum.block.BlocksObs;

import javax.annotation.Nullable;

public class LargeAlchemicalTank extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty VOID_TANK = BooleanProperty.create("void_tank");
    public LargeAlchemicalTank(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VOID_TANK, false)
        );
    }
    @Override
    public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        super.onProjectileHit(pLevel, pState, pHit, pProjectile);
        pLevel.destroyBlock(pHit.getBlockPos(), true);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING,VOID_TANK);
    }
    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static final VoxelShape TOP = Shapes.or(

                     box(2, 0, 2, 14, 11, 14));

    private static final VoxelShape BOTTOM = Shapes.or(
            box(2.3, 5.1, 2.3, 13.7, 16, 13.7),
            box(1, 0, 1, 15, 5, 15),
                    box(2, 5, 2, 14, 16, 14));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // Определяем коллизионные формы для трех частей и добавляем второй невидимый блок для верхней части.
        if (state.getValue(VOID_TANK)) {
            return TOP; // Заданный хитбокс
        }else {
            return BOTTOM;
        }
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Проверяем, можно ли поставить блок только один раз выше.
        BlockPos upperPos = pos.offset(0, 1, 0); // Позиция сверху блока.

        if (level.isEmptyBlock(upperPos)) {
            // Устанавливаем новый блок на одну позицию выше.
            BlockState upperBlockState = this.defaultBlockState()
                    .setValue(FACING, state.getValue(FACING))
                    .setValue(VOID_TANK, true);
            level.setBlockAndUpdate(upperPos, upperBlockState);
        }
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Удаляем блоки вокруг, независимо от их состояния
        BlockPos[] positionsToRemove = new BlockPos[] {
                 // Второй ряд сверху
                pos.offset(0, 1, 0),
                pos.offset(0, -1, 0)
        };

        for (BlockPos position : positionsToRemove) {
            // Удаляем блок и получаем результаты
            BlockState blockState = level.getBlockState(position);
            if (blockState.getBlock() == BlocksObs.LARGE_ALCHEMICAL_TANK.get().defaultBlockState().setValue(VOID_TANK, true).getBlock()) {
                level.destroyBlock(position, false); // true - означает, что ресурсы блока будут выброшены
            } else if (blockState.getBlock() == BlocksObs.LARGE_ALCHEMICAL_TANK.get().defaultBlockState().setValue(VOID_TANK, false).getBlock()) {
                level.destroyBlock(position, true);
            }
        }
        // Спавним "сломанный" блок
        if (!state.getValue(VOID_TANK)) {

            // Спавним "сломанный" блок только для нижней части
            BlockState brokenTankState = BlocksObs.LARGE_ALCHEMICAL_TANK_BROKEN.get().defaultBlockState();
            level.setBlockAndUpdate(pos, brokenTankState);
        }

        // Вызываем стандартный метод удаления
        super.onRemove(state, level, pos, newState, isMoving);
    }
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Проверяем все необходимые позиции
        BlockPos[] positionsToCheck = new BlockPos[] {
                pos.above()
        };

        for (BlockPos checkPos : positionsToCheck) {
            if (!level.isEmptyBlock(checkPos)) {
                return false; // Если хотя бы одна позиция занята, блок не может быть установлен
            }
        }
        return true; // Все позиции свободны, блок может быть установлен
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();

        BlockState partState = this.defaultBlockState().setValue(FACING, facing);
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(VOID_TANK, false);
    }
}
