package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SnowyAlchemicalDirtBlock extends Block {
    public static final BooleanProperty SNOWY;
    public SnowyAlchemicalDirtBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Устанавливаем состояние по умолчанию: SNOWY = false
        this.registerDefaultState(this.stateDefinition.any().setValue(SNOWY, false));
    }
    // Обновление состояния при изменении соседних блоков
    @Override
    public BlockState updateShape(BlockState currentState, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        // Если изменение произошло сверху - обновляем свойство SNOWY
        if (direction == Direction.UP) {
            return currentState.setValue(SNOWY, isSnowySetting(neighborState));
        }
        return super.updateShape(currentState, direction, neighborState, level, currentPos, neighborPos);
    }

    // Установка состояния при размещении блока
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Проверяем блок сверху при установке
        BlockState aboveState = context.getLevel().getBlockState(context.getClickedPos().above());
        return this.defaultBlockState().setValue(SNOWY, isSnowySetting(aboveState));
    }

    // Проверка, считается ли блок "снежным" (использует теги для гибкости)
    private static boolean isSnowySetting(BlockState state) {
        return state.is(BlockTags.SNOW); // Проверка по тегу, а не конкретному блоку
    }

    // Регистрация свойства блока
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SNOWY);
    }

    static {
        SNOWY = BlockStateProperties.SNOWY; // Связываем свойство с константой
    }
}
