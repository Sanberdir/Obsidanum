package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.chests.block.entity.RunicObsidianChestBlockEntity;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.sound.SoundsObs;

public class LockedRunicChest extends Block {
    public LockedRunicChest(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        // Проверяем, использует ли игрок основной рукой и является ли предмет в руке ключом из обсаидиановых осколков
        if (pHand == InteractionHand.MAIN_HAND && pPlayer.getItemInHand(pHand).is(ItemsObs.OBSIDIAN_SHARD_KEY.get())) {
            // Получаем текущее направление блока
            Direction currentFacing = pState.getValue(LockedRunicChest.FACING);

            // Меняем блок на RUNIC с тем же направлением
            BlockState newState = BlocksObs.RUNIC_OBSIDIAN_CHEST.get().defaultBlockState()
                    .setValue(LockedRunicChest.FACING, currentFacing);

            // Устанавливаем новый блок на месте старого сундука
            pLevel.setBlock(pPos, newState, 3);

            // Добавляем лут, аналогичный сундуку крепости Незера
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof RunicObsidianChestBlockEntity) {
                RunicObsidianChestBlockEntity chestEntity = (RunicObsidianChestBlockEntity) blockEntity;

                // Получаем источник случайных чисел
                RandomSource randomSource = pLevel.getRandom();

                // Получаем случайное число в виде long для использования в таблице лута
                long lootSeed = randomSource.nextLong();

                // Устанавливаем таблицу лута для сундука крепости Незера
                chestEntity.setLootTable(new ResourceLocation("obsidanum", "chests/alchemists_branch/pools/ab_pool_hard"), lootSeed);

                // Синхронизируем состояние сундука
                chestEntity.setChanged();
            }

            // Воспроизводим звук активации маяка
            pLevel.playSound(null, pPos, SoundsObs.LOCK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

            // Уменьшаем количество ключей в инвентаре игрока
            if (!pPlayer.isCreative()) {
                pPlayer.getItemInHand(pHand).shrink(1); // Удаляем один ключ
            }

            return InteractionResult.SUCCESS;
        }

        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

}
