/*
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package net.rezolv.obsidanum.block.custom.multiblock_provider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Базовый класс для мультиблоков с возможностью водозаполнения, горизонтальным направлением
 * и изменяемым размером. Обрабатывает логику размещения, проверки целостности и отображения мультиблоков.
 */
public class RotatingMultiblock extends Block implements SimpleWaterloggedBlock, IDelegateProvider {

    // Свойства блока
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Обработчик мультиблока
    protected final MultiblockHandler multiblockHandler;

    // Кэшированные формы для отрисовки и коллизий
    protected final Map<BlockState, VoxelShape> blockShapes = new HashMap<>();
    protected final Map<BlockState, VoxelShape> multiblockShapes = new HashMap<>();

    // Построитель форм для блоков
    private final ShapeBuilder shapeBuilder;

    /**
     * Конструктор мультиблока
     * @param multiblockHandler обработчик мультиблока
     * @param shapeBuilder построитель форм
     * @param pProperties свойства блока
     */
    protected RotatingMultiblock(MultiblockHandler multiblockHandler,
                                 ShapeBuilder shapeBuilder,
                                 Properties pProperties) {
        super(pProperties.dynamicShape());
        this.multiblockHandler = multiblockHandler;
        this.shapeBuilder = shapeBuilder;
        // Создание определения состояний
        this.stateDefinition = createStateDefinition();
        // Установка состояния по умолчанию
        this.registerDefaultState(this.multiblockHandler.getCenterState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(FACING, Direction.NORTH)));
        // Предварительный расчет форм
        this.precalculateShapes();
    }

    /**
     * @return обработчик мультиблока
     */
    public MultiblockHandler getMultiblockHandler() {
        return multiblockHandler;
    }

    /**
     * Создает определение состояний блока
     */
    protected StateDefinition<Block, BlockState> createStateDefinition() {
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        this.createMultiblockStateDefinition(builder);
        return builder.create(Block::defaultBlockState, BlockState::new);
    }

    //// ИНТЕРФЕЙС IDelegateProvider ////

    @Override
    public BlockPos getDelegatePos(BlockState blockState, BlockPos blockPos) {
        return multiblockHandler.getCenterPos(blockPos, blockState, blockState.getValue(FACING));
    }

    //// СОСТОЯНИЯ БЛОКА ////

    /**
     * @deprecated Используйте {@link #createMultiblockStateDefinition(StateDefinition.Builder)}
     */
    @Override
    @Deprecated
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        // Примечание: этот метод вызывается из конструктора суперкласса до инициализации multiblockHandler
        super.createBlockStateDefinition(pBuilder.add(WATERLOGGED).add(FACING));
    }

    /**
     * Создает определение состояний для мультиблока
     * @param pBuilder построитель определений состояний
     */
    protected void createMultiblockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        this.multiblockHandler.createBlockStateDefinition(pBuilder.add(WATERLOGGED).add(FACING));
    }

    //// РАЗМЕЩЕНИЕ БЛОКА ////

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        /*
         * BlockPlaceContext корректируется в BlockItem для указания центральной позиции
         */
        final Direction direction = pContext.getHorizontalDirection().getOpposite();
        // Базовое состояние блока
        final boolean waterlogged = pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER;
        final BlockState blockState = this.defaultBlockState()
                .setValue(FACING, direction)
                .setValue(WATERLOGGED, waterlogged);
        // Делегирование обработчику мультиблока
        return multiblockHandler.getStateForPlacement(pContext, blockState, direction);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        // Обновление водозаполнения
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        // Проверка целостности мультиблока
        if(!multiblockHandler.canSurvive(pState, pLevel, pCurrentPos, pState.getValue(FACING))) {
            return getFluidState(pState).createLegacyBlock();
        }
        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        multiblockHandler.onBlockPlaced(pLevel, pPos, pState, pState.getValue(FACING));
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        // Если блок в позиции не этот, значит проверка предварительная
        if(!pLevel.getBlockState(pPos).is(this)) {
            return true;
        }
        // Проверка целостности мультиблока
        return multiblockHandler.canSurvive(pState, pLevel, pPos, pState.getValue(FACING));
    }

    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        if (!pLevel.isClientSide() && pPlayer.isCreative()) {
            multiblockHandler.preventCreativeDropFromCenterPart(pLevel, pPos, pState, pState.getValue(FACING), pPlayer);
        }
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState pState) {
        return PushReaction.BLOCK;
    }

    // TODO реализовать методы #rotation и #mirror

    /**
     * Удаляет все блоки мультиблока
     * @param level мир
     * @param centerPos позиция центра
     */
    public void removeAll(final Level level, final BlockPos centerPos) {
        final BlockPos.MutableBlockPos mutablePos = centerPos.mutable();
        getMultiblockHandler().iterateIndices(index -> {
            level.removeBlock(mutablePos.setWithOffset(centerPos, index), false);
        });
    }

    //// ВОДА ////

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    //// ФОРМЫ И КОЛЛИЗИИ ////

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.hasCollision ? getBlockShape(pState) : Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return getMultiblockShape(pState);
    }

    /**
     * Предварительно вычисляет формы для всех возможных состояний
     */
    protected void precalculateShapes() {
        blockShapes.clear();
        multiblockShapes.clear();
        // Вычисление центрированных визуальных форм
        final Map<Direction, VoxelShape> centeredVisualShapes = new EnumMap<>(Direction.class);
        centeredVisualShapes.putAll(ShapeUtils.rotateShapes(MultiblockHandler.ORIGIN_DIRECTION, createMultiblockShape()));
        // Итерация по всем возможным состояниям
        for(BlockState blockState : this.stateDefinition.getPossibleStates()) {
            // Кэширование индивидуальной формы
            blockShapes.put(blockState, this.shapeBuilder.apply(blockState));
            // Смещение центрированной формы для заданного поворота
            Direction direction = blockState.getValue(FACING);
            Vec3i index = multiblockHandler.getIndex(blockState);
            Vec3i offset = MultiblockHandler.indexToOffset(index, direction);
            VoxelShape shape = centeredVisualShapes.get(blockState.getValue(FACING))
                    .move(-offset.getX(), -offset.getY(),  -offset.getZ());
            // Кэширование смещенной визуальной формы
            multiblockShapes.put(blockState, shape);
        }
    }

    /**
     * Создает форму мультиблока, центрированную вокруг центрального блока
     * @return VoxelShape мультиблока
     */
    protected VoxelShape createMultiblockShape() {
        final BlockState blockState = multiblockHandler.getCenterState(defaultBlockState());
        final AtomicReference<VoxelShape> shape = new AtomicReference<>(Shapes.empty());
        multiblockHandler.iterateIndices(index -> {
            BlockState b = multiblockHandler.getIndexedState(blockState, index);
            shape.set(ShapeUtils.orUnoptimized(shape.get(), blockShapes.computeIfAbsent(b, this.shapeBuilder)
                    .move(-index.getX(), index.getY(), index.getZ())
            ));
        });
        return shape.get().optimize();
    }

    /**
     * @param blockState состояние блока
     * @return кэшированная форма для данного состояния
     */
    public VoxelShape getBlockShape(final BlockState blockState) {
        return blockShapes.get(blockState);
    }

    /**
     * @param blockState состояние блока
     * @return кэшированная форма мультиблока для данного состояния
     */
    public VoxelShape getMultiblockShape(final BlockState blockState) {
        return multiblockShapes.get(blockState);
    }

    //// ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ФОРМ ////

    /**
     * Создает построитель формы для мультиблока
     * @param handler обработчик мультиблока
     * @param template массив VoxelShape в порядке [высота][ширина][глубина]
     * @return построитель формы, учитывающий поворот
     */
    public static ShapeBuilder createMultiblockShapeBuilder(final MultiblockHandler handler, final VoxelShape[][][] template) {
        return blockState -> {
            final Vec3i index = handler.getIndex(blockState);
            final Vec3i dimensions = handler.getDimensions();
            final Direction facing =  blockState.getValue(FACING);
            int heightIndex = (index.getY() + dimensions.getY() / 2);
            int widthIndex = (index.getX() + dimensions.getX() / 2);
            int depthIndex = (index.getZ() + dimensions.getZ() / 2);
            final VoxelShape shape = template
                    [heightIndex]
                    [widthIndex]
                    [depthIndex];
            return ShapeUtils.rotateShape(MultiblockHandler.ORIGIN_DIRECTION, facing, shape);
        };
    }
}