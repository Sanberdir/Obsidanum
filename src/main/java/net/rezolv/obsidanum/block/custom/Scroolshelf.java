package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rezolv.obsidanum.item.ItemsObs;

public class Scroolshelf extends Block {

    // Определяем два состояния блока
    public static final BooleanProperty HAS_SCROLL = BooleanProperty.create("has_scroll");
    public static final BooleanProperty SCROLL_PRESENT = BooleanProperty.create("scroll_present");

    public Scroolshelf(Properties pProperties) {
        super(pProperties);
        // Устанавливаем начальные состояния
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_SCROLL, false)
                .setValue(SCROLL_PRESENT, false));
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && state.getValue(SCROLL_PRESENT)) {
            // Если игрок зажат Shift или рука не пустая - пропускаем
            if (!player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.PASS;
            }

            // Убираем свиток с полки
            level.setBlock(pos, state.setValue(SCROLL_PRESENT, false), 3);

            // Создаём случайный свиток (50/50)
            ItemStack scroll = new ItemStack(
                    level.random.nextBoolean()
                            ? ItemsObs.ANCIENT_SCROLL.get()
                            : ItemsObs.ENCHANTED_SCROLL.get()
            );

            // Пытаемся добавить в инвентарь
            if (!player.getInventory().add(scroll)) {
                // Если инвентарь полон - выпадаем на землю
                player.drop(scroll, false);
            }

            // Визуальные эффекты
            player.swing(hand);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        // При генерации мира pOldState будет air, поэтому это условие пропускает генерацию
        if (!pOldState.is(pState.getBlock())) {
            // Для ручного размещения (включая /place structure)
            if (!pLevel.isClientSide()) {
                // При размещении в выживании - всегда без свитка
                BlockState newState = pState.setValue(HAS_SCROLL, false)
                        .setValue(SCROLL_PRESENT, false);

                // Проверяем, есть ли игрок рядом (для творческого режима)
                Player player = pLevel.getNearestPlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 10, null);

                // Если размещение в творческом режиме - 10% шанс на свиток
                if (player != null && player.isCreative() && pLevel.random.nextFloat() < 0.1f) {
                    newState = newState.setValue(HAS_SCROLL, true)
                            .setValue(SCROLL_PRESENT, true);
                }

                if (!newState.equals(pState)) {
                    pLevel.setBlock(pPos, newState, 3);
                }
            }
        }
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SCROLL, SCROLL_PRESENT);
    }
    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return 2;
    }

}
