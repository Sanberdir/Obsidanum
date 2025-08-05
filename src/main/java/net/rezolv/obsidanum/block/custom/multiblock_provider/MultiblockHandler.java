/*
 * Copyright (c) 2023 Skyler James
 * Разрешение на использование, модификацию и распространение этого ПО, частично или полностью,
 * предоставляется под лицензией GNU LGPLv3 (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package net.rezolv.obsidanum.block.custom.multiblock_provider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Класс для управления мультиблоками (составными блоками из нескольких частей).
 * Обеспечивает создание, размещение и проверку многоблочных структур.
 */
@Immutable
public class MultiblockHandler {

    // Константы направления и центра
    public static final Direction ORIGIN_DIRECTION = Direction.NORTH; // Направление по умолчанию для ориентации
    public static final Vec3i CENTER_INDEX = Vec3i.ZERO; // Индекс центрального блока (0,0,0)

    // Названия свойств для размеров
    protected static final String WIDTH = "width";  // Ширина (X-ось)
    protected static final String HEIGHT = "height"; // Высота (Y-ось)
    protected static final String DEPTH = "depth";   // Глубина (Z-ось)

    /* Свойства для индексов по осям */
    // Свойства для ширины (X-ось)
    protected static final IntegerProperty WIDTH_1_2 = IntegerProperty.create(WIDTH, 1, 2);
    protected static final IntegerProperty WIDTH_0_2 = IntegerProperty.create(WIDTH, 0, 2);

    // Свойства для высоты (Y-ось)
    protected static final IntegerProperty HEIGHT_1_2 = IntegerProperty.create(HEIGHT, 1, 2);
    protected static final IntegerProperty HEIGHT_0_2 = IntegerProperty.create(HEIGHT, 0, 2);

    // Свойства для глубины (Z-ось)
    protected static final IntegerProperty DEPTH_1_2 = IntegerProperty.create(DEPTH, 1, 2);
    protected static final IntegerProperty DEPTH_0_2 = IntegerProperty.create(DEPTH, 0, 2);

    // Массивы свойств для быстрого доступа по максимальному значению
    protected static final IntegerProperty[] WIDTH_BY_MAX_VALUE = new IntegerProperty[] { null, WIDTH_1_2, WIDTH_0_2};
    protected static final IntegerProperty[] HEIGHT_BY_MAX_VALUE = new IntegerProperty[] { null, HEIGHT_1_2, HEIGHT_0_2};
    protected static final IntegerProperty[] DEPTH_BY_MAX_VALUE = new IntegerProperty[] { null, DEPTH_1_2, DEPTH_0_2};

    // Предопределенные конфигурации мультиблоков (различные размеры)
    public static final MultiblockHandler MULTIBLOCK_3X3X3 = new MultiblockHandler(3, 3, 3);
    public static final MultiblockHandler MULTIBLOCK_3X3X1 = new MultiblockHandler(3, 3, 1);
    public static final MultiblockHandler MULTIBLOCK_3X2X1 = new MultiblockHandler(3, 2, 1);
    public static final MultiblockHandler MULTIBLOCK_3X1X1 = new MultiblockHandler(3, 1, 1);
    public static final MultiblockHandler MULTIBLOCK_3X1X2 = new MultiblockHandler(3, 1, 2);
    public static final MultiblockHandler MULTIBLOCK_3X1X3 = new MultiblockHandler(3, 1, 3);
    public static final MultiblockHandler MULTIBLOCK_2X3X1 = new MultiblockHandler(2, 3, 1);
    public static final MultiblockHandler MULTIBLOCK_2X2X2 = new MultiblockHandler(2, 2, 2);
    public static final MultiblockHandler MULTIBLOCK_2X2X1 = new MultiblockHandler(2, 2, 1);
    public static final MultiblockHandler MULTIBLOCK_2X1X2 = new MultiblockHandler(2, 1, 2);
    public static final MultiblockHandler MULTIBLOCK_2X1X1 = new MultiblockHandler(2, 1, 1);
    public static final MultiblockHandler MULTIBLOCK_1X3X1 = new MultiblockHandler(1, 3, 1);
    public static final MultiblockHandler MULTIBLOCK_1X2X1 = new MultiblockHandler(1, 2, 1);
    public static final MultiblockHandler MULTIBLOCK_1X1X2 = new MultiblockHandler(1, 1, 2);

    // Поля класса
    protected final @Nullable IntegerProperty widthProperty;  // Свойство ширины (может быть null)
    protected final @Nullable IntegerProperty heightProperty; // Свойство высоты (может быть null)
    protected final @Nullable IntegerProperty depthProperty;  // Свойство глубины (может быть null)

    protected final Vec3i dimensions;  // Размеры мультиблока (ширина, высота, глубина)
    protected final Vec3i minIndex;    // Минимальные индексы блоков относительно центра
    protected final Vec3i maxIndex;    // Максимальные индексы блоков относительно центра
    protected final Map<Direction, BoundingBox> bounds; // Границы мультиблока для каждого направления

    /**
     * Конструктор мультиблока
     * @param width ширина (X-ось)
     * @param height высота (Y-ось)
     * @param depth глубина (Z-ось)
     * @throws IllegalArgumentException если размеры меньше 1
     */
    public MultiblockHandler(final int width, final int height, final int depth) {
        // Проверка валидности размеров
        if(width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException(String.format("[MultiblockHandler] width, height, and depth must be greater than zero! Provided [{}, {}, {}]", width, height, depth));
        }
        this.dimensions = new Vec3i(width, height, depth);
        // Расчет минимальных и максимальных индексов относительно центра
        this.minIndex = new Vec3i(-(width - 1) / 2, -(height - 1) / 2, -(depth - 1) / 2);
        this.maxIndex = new Vec3i(width / 2, height / 2, depth / 2);
        // Получение свойств для каждого измерения
        this.widthProperty = getWidthProperty(width);
        this.heightProperty = getHeightProperty(height);
        this.depthProperty = getDepthProperty(depth);
        // Создание ограничивающей рамки и ее повернутых вариантов
        final BoundingBox boundingBox = BoundingBox.fromCorners(minIndex, maxIndex);
        this.bounds = createRotatedBoundingBoxMap(boundingBox, ORIGIN_DIRECTION);
    }

    //// GETTERS ////

    /**
     * @return свойство ширины или null, если не используется
     */
    public @Nullable IntegerProperty getWidthProperty() {
        return widthProperty;
    }

    /**
     * @return свойство высоты или null, если не используется
     */
    public @Nullable IntegerProperty getHeightProperty() {
        return heightProperty;
    }

    /**
     * @return свойство глубины или null, если не используется
     */
    public @Nullable IntegerProperty getDepthProperty() {
        return depthProperty;
    }

    /**
     * @return копию размеров мультиблока (ширина, высота, глубина)
     */
    public Vec3i getDimensions() {
        return new Vec3i(this.dimensions.getX(), this.dimensions.getY(), this.dimensions.getZ());
    }

    /**
     * @return копию минимальных индексов относительно центра
     */
    public Vec3i getMinIndex() {
        return new Vec3i(this.minIndex.getX(), this.minIndex.getY(), this.minIndex.getZ());
    }

    /**
     * @return копию максимальных индексов относительно центра
     */
    public Vec3i getMaxIndex() {
        return new Vec3i(this.maxIndex.getX(), this.maxIndex.getY(), this.maxIndex.getZ());
    }

    /**
     * @param direction направление
     * @return ограничивающую рамку для заданного направления
     */
    public BoundingBox getBounds(final Direction direction) {
        return bounds.get(direction);
    }

    //// ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ////

    /**
     * Находит центральную позицию мультиблока по позиции части и ее состоянию
     * @param pos позиция блока
     * @param blockState состояние блока (части мультиблока)
     * @param direction направление
     * @return позицию центра мультиблока
     */
    public BlockPos getCenterPos(final BlockPos pos, final BlockState blockState, final Direction direction) {
        return getCenterPos(pos, getIndex(blockState), direction);
    }

    /**
     * Находит минимальную позицию в мультиблоке относительно центра
     * @param center центральная позиция
     * @param direction направление
     * @return минимальную позицию
     */
    public BlockPos getMin(final BlockPos center, final Direction direction) {
        return center.offset(MultiblockHandler.indexToOffset(minIndex, direction));
    }

    /**
     * Находит максимальную позицию в мультиблоке относительно центра
     * @param center центральная позиция
     * @param direction направление
     * @return максимальную позицию
     */
    public BlockPos getMax(final BlockPos center, final Direction direction) {
        return center.offset(MultiblockHandler.indexToOffset(maxIndex, direction));
    }

    /**
     * Возвращает все позиции блоков в мультиблоке
     * @param center центральная позиция
     * @param facing направление
     * @return итерируемый объект с позициями всех блоков
     */
    public Iterable<BlockPos> getPositions(final BlockPos center, final Direction facing) {
        final BlockPos min = getMin(center, facing);
        final BlockPos max = getMax(center, facing);
        return BlockPos.betweenClosed(min, max);
    }

    //// МЕТОДЫ ДЛЯ РАБОТЫ СО СВОЙСТВАМИ ////

    /**
     * Возвращает свойство ширины по максимальному значению
     * @param maxWidth максимальная ширина
     * @return свойство или null
     */
    public static @Nullable IntegerProperty getWidthProperty(final int maxWidth) {
        final int index = Mth.clamp(maxWidth - 1, 0, WIDTH_BY_MAX_VALUE.length - 1);
        return WIDTH_BY_MAX_VALUE[index];
    }

    /**
     * Возвращает свойство высоты по максимальному значению
     * @param maxHeight максимальная высота
     * @return свойство или null
     */
    public static @Nullable IntegerProperty getHeightProperty(final int maxHeight) {
        final int index = Mth.clamp(maxHeight - 1, 0, HEIGHT_BY_MAX_VALUE.length - 1);
        return HEIGHT_BY_MAX_VALUE[index];
    }

    /**
     * Возвращает свойство глубины по максимальному значению
     * @param maxDepth максимальная глубина
     * @return свойство или null
     */
    public static @Nullable IntegerProperty getDepthProperty(final int maxDepth) {
        final int index = Mth.clamp(maxDepth - 1, 0, DEPTH_BY_MAX_VALUE.length - 1);
        return DEPTH_BY_MAX_VALUE[index];
    }

    /**
     * Проверяет, является ли блок центральным по его состоянию
     * @param blockState состояние блока
     * @return true если блок центральный
     */
    public boolean isCenterState(final BlockState blockState) {
        return getIndex(blockState).equals(CENTER_INDEX);
    }

    /**
     * Добавляет свойства в определение состояния блока
     * @param builder строитель определений состояний
     * @return строитель с добавленными свойствами
     */
    public StateDefinition.Builder<Block, BlockState> createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        if(widthProperty != null) {
            builder.add(widthProperty);
        }
        if(heightProperty != null) {
            builder.add(heightProperty);
        }
        if(depthProperty != null) {
            builder.add(depthProperty);
        }
        return builder;
    }

    /**
     * Возвращает состояние блока для центра мультиблока
     * @param blockState базовое состояние
     * @return состояние для центра
     */
    public BlockState getCenterState(BlockState blockState) {
        return getIndexedState(blockState, CENTER_INDEX);
    }

    /**
     * Возвращает индексы блока относительно центра
     * @param blockState состояние блока
     * @return вектор индексов (width, height, depth)
     */
    public Vec3i getIndex(final BlockState blockState) {
        final int width = widthProperty != null ? (blockState.getValue(widthProperty) - dimensions.getX() / 2) : 0;
        final int height = heightProperty != null ? (blockState.getValue(heightProperty) - dimensions.getY() / 2) : 0;
        final int depth = depthProperty != null ? (blockState.getValue(depthProperty) - dimensions.getZ() / 2) : 0;
        return new Vec3i(width, height, depth);
    }

    /**
     * Возвращает состояние блока для заданного индекса
     * @param blockState базовое состояние
     * @param index требуемый индекс
     * @return состояние блока для заданного индекса
     */
    public BlockState getIndexedState(final BlockState blockState, final Vec3i index) {
        BlockState mutableBlockState = blockState;
        if(widthProperty != null) {
            mutableBlockState = mutableBlockState.setValue(widthProperty, index.getX() + dimensions.getX() / 2);
        }
        if(heightProperty != null) {
            mutableBlockState = mutableBlockState.setValue(heightProperty, index.getY() + dimensions.getY() / 2);
        }
        if(depthProperty != null) {
            mutableBlockState = mutableBlockState.setValue(depthProperty, index.getZ() + dimensions.getZ() / 2);
        }
        return mutableBlockState;
    }

    //// МЕТОДЫ ДЛЯ РАЗМЕЩЕНИЯ БЛОКОВ ////

    /**
     * Размещает все блоки мультиблока в мире
     * @param level мир
     * @param pos позиция размещения
     * @param blockState состояние базового блока
     * @param direction направление
     */
    public void onBlockPlaced(Level level, BlockPos pos, BlockState blockState, Direction direction) {
        // Определяем центр
        final BlockPos center = getCenterPos(pos, blockState, direction);
        // Размещаем все блоки мультиблока
        iterateIndices(index -> {
            // Пропускаем центральный блок
            if(index.equals(CENTER_INDEX)) return;
            // Вычисляем позицию блока
            BlockPos p = center.offset(indexToOffset(index, direction));
            // Проверяем, нужно ли делать блок водонепроницаемым
            boolean waterlogged = level.getFluidState(p).getType() == Fluids.WATER;
            // Получаем состояние блока для текущего индекса
            final BlockState state = getIndexedState(blockState.setValue(BlockStateProperties.WATERLOGGED, waterlogged), index);
            // Размещаем блок в мире
            level.setBlock(p, state, Block.UPDATE_ALL);
        });
    }

    /**
     * Проверяет возможность размещения мультиблока
     * @param context контекст размещения
     * @param blockState состояние базового блока
     * @param facing направление
     * @return состояние для размещения или null, если нельзя разместить
     */
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context, BlockState blockState, Direction facing) {
        final Level level = context.getLevel();
        // Центральная позиция
        final BlockPos center = context.getClickedPos();
        // Проверяем, можно ли разместить все блоки
        if(!allPositions(center, facing, p -> level.isInWorldBounds(p) && level.getBlockState(p).canBeReplaced(context))) {
            return null;
        }
        // Возвращаем состояние для центрального блока
        return getCenterState(blockState);
    }

    /**
     * Проверяет, может ли мультиблок существовать в текущем положении
     * @param blockState состояние блока
     * @param level мир
     * @param pos позиция
     * @param facing направление
     * @return true если все блоки на месте
     */
    public boolean canSurvive(final BlockState blockState, final LevelReader level, final BlockPos pos, final Direction facing) {
        return allPositions(getCenterPos(pos, blockState, facing), facing, p -> level.getBlockState(p).is(blockState.getBlock()));
    }

    /**
     * Удаляет центральный блок без выпадения предмета (для креатива)
     * @param level мир
     * @param pos позиция
     * @param blockState состояние блока
     * @param facing направление
     * @param player игрок
     */
    public void preventCreativeDropFromCenterPart(Level level, BlockPos pos, BlockState blockState, Direction facing, Player player) {
        final BlockPos origin = getCenterPos(pos, blockState, facing);
        final BlockState originState = level.getBlockState(origin);
        if(originState.is(blockState.getBlock()) && getIndex(originState).equals(CENTER_INDEX)) {
            level.setBlock(origin, originState.getFluidState().createLegacyBlock(), Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_ALL);
        }
    }

    //// МЕТОДЫ ДЛЯ РАБОТЫ С ПОЗИЦИЯМИ ////

    /**
     * Проверяет все позиции в мультиблоке на соответствие условию
     * @param center центр
     * @param facing направление
     * @param predicate условие
     * @return true если все позиции соответствуют
     */
    public boolean allPositions(final BlockPos center, final Direction facing, final Predicate<BlockPos> predicate) {
        for(BlockPos p : getPositions(center, facing)) {
            if(!predicate.test(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверяет любую позицию в мультиблоке на соответствие условию
     * @param center центр
     * @param facing направление
     * @param predicate условие
     * @return true если хотя бы одна позиция соответствует
     */
    public boolean anyPositions(final BlockPos center, final Direction facing, final Predicate<BlockPos> predicate) {
        for(BlockPos p : getPositions(center, facing)) {
            if(predicate.test(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Перебирает все индексы в мультиблоке
     * @param consumer обработчик для каждого индекса
     */
    public void iterateIndices(final Consumer<Vec3i> consumer) {
        // Перебираем все индексы по осям
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for(int x = minIndex.getX(); x <= maxIndex.getX(); x++) {
            for(int y = minIndex.getY(); y <= maxIndex.getY(); y++) {
                for(int z = minIndex.getZ(); z <= maxIndex.getZ(); z++) {
                    consumer.accept(mutable.set(x, y, z));
                }
            }
        }
    }

    /**
     * Создает карту ограничивающих рамок для каждого направления
     * @param boundingBox исходная рамка
     * @param from начальное направление
     * @return карта направление → рамка
     */
    protected static Map<Direction, BoundingBox> createRotatedBoundingBoxMap(final BoundingBox boundingBox, final Direction from) {
        final Map<Direction, BoundingBox> map = new EnumMap<>(Direction.class);
        map.put(from, boundingBox);
        BoundingBox box = boundingBox;
        // Поворачиваем рамку для каждого направления
        for(int i = 0; i < 3; i++) {
            Direction direction = Direction.from2DDataValue(from.get2DDataValue() + i + 1);
            box = new BoundingBox(1 - box.maxZ(), box.minY(), box.minX(), 1 - box.minZ(), box.maxY(), box.maxX());
            map.put(direction, box);
        }
        return map;
    }

    /**
     * Находит центральную позицию по позиции блока и его индексу
     * @param pos позиция блока
     * @param index индекс блока
     * @param direction направление
     * @return позицию центра
     */
    public static BlockPos getCenterPos(final BlockPos pos, final Vec3i index, final Direction direction) {
        // Вычисляем смещение по индексу и направлению
        final Vec3i offset = indexToOffset(index, direction);
        // Вычисляем центральную позицию
        return pos.subtract(offset);
    }

    /**
     * Преобразует индекс в смещение с учетом направления
     * @param index индекс
     * @param direction направление
     * @return вектор смещения
     */
    public static Vec3i indexToOffset(final Vec3i index, final Direction direction) {
        switch (direction) {
            default:
            case NORTH: return new Vec3i(-index.getX(), index.getY(), index.getZ());
            case EAST: return new Vec3i(-index.getZ(), index.getY(), -index.getX());
            case SOUTH: return new Vec3i(index.getX(), index.getY(), -index.getZ());
            case WEST: return new Vec3i(index.getZ(), index.getY(), index.getX());
        }
    }
}