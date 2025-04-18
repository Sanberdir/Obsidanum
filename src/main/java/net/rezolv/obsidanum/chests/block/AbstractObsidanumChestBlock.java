package net.rezolv.obsidanum.chests.block;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rezolv.obsidanum.chests.block.entity.AbstractObsidanumChestBlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class AbstractObsidanumChestBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

  public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

  public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

  protected static final VoxelShape AABB = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

  private static final DoubleBlockCombiner.Combiner<AbstractObsidanumChestBlockEntity, Optional<Container>> CHEST_COMBINER = new DoubleBlockCombiner.Combiner<>() {
    @Override
    public Optional<Container> acceptDouble(AbstractObsidanumChestBlockEntity blockEntityOne, AbstractObsidanumChestBlockEntity blockEntityTwo) {
      return Optional.of(new CompoundContainer(blockEntityOne, blockEntityTwo));
    }

    @Override
    public Optional<Container> acceptSingle(AbstractObsidanumChestBlockEntity blockEntity) {
      return Optional.of(blockEntity);
    }

    @Override
    public Optional<Container> acceptNone() {
      return Optional.empty();
    }
  };

  private static final DoubleBlockCombiner.Combiner<AbstractObsidanumChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER = new DoubleBlockCombiner.Combiner<>() {
    @Override
    public Optional<MenuProvider> acceptDouble(AbstractObsidanumChestBlockEntity blockEntityOne, AbstractObsidanumChestBlockEntity blockEntityTwo) {
      return Optional.empty();
    }

    @Override
    public Optional<MenuProvider> acceptSingle(AbstractObsidanumChestBlockEntity blockEntity) {
      return Optional.of(blockEntity);
    }

    @Override
    public Optional<MenuProvider> acceptNone() {
      return Optional.empty();
    }
  };

  private final ObsidanumChestsTypes type;

  protected final Supplier<BlockEntityType<? extends AbstractObsidanumChestBlockEntity>> blockEntityType;

  public AbstractObsidanumChestBlock(Properties properties, Supplier<BlockEntityType<? extends AbstractObsidanumChestBlockEntity>> blockEntityType, ObsidanumChestsTypes type) {
    super(properties);

    this.type = type;
    this.blockEntityType = blockEntityType;

    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, Boolean.FALSE));
  }

  public static DoubleBlockCombiner.BlockType getBlockType(BlockState blockState) {
    return DoubleBlockCombiner.BlockType.SINGLE;
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  @Deprecated
  public BlockState updateShape(BlockState blockState, Direction direction, BlockState facingState, LevelAccessor levelAccessor, BlockPos currentPos, BlockPos facingPos) {
    if (blockState.getValue(WATERLOGGED)) {
      levelAccessor.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
    }

    return super.updateShape(blockState, direction, facingState, levelAccessor, currentPos, facingPos);
  }

  @Override
  @Deprecated
  public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext context) {
    return AABB;
  }

  public static Direction getConnectedDirection(BlockState blockState) {
    return blockState.getValue(FACING).getCounterClockWise();
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction direction = context.getHorizontalDirection().getOpposite();
    FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());

    return this.defaultBlockState().setValue(FACING, direction).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
  }

  @Override
  @Deprecated
  public FluidState getFluidState(BlockState blockState) {
    return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
  }

  @Override
  public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
    BlockEntity blockEntity = level.getBlockEntity(blockPos);

    if (blockEntity instanceof AbstractObsidanumChestBlockEntity) {
      ((AbstractObsidanumChestBlockEntity) blockEntity).wasPlaced(livingEntity, itemStack);

      if (itemStack.hasCustomHoverName()) {
        ((AbstractObsidanumChestBlockEntity) blockEntity).setCustomName(itemStack.getHoverName());
      }
    }
  }

  @Override
  @Deprecated
  public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newState, boolean isMoving) {
    if (!blockState.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(blockPos);
      if (blockEntity instanceof AbstractObsidanumChestBlockEntity) {
        ((AbstractObsidanumChestBlockEntity) blockEntity).removeAdornments();

        Containers.dropContents(level, blockPos, (AbstractObsidanumChestBlockEntity) blockEntity);
        level.updateNeighbourForOutputSignal(blockPos, this);
      }

      super.onRemove(blockState, level, blockPos, newState, isMoving);
    }
  }

  @Override
  @Deprecated
  public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
    if (level.isClientSide) {
      return InteractionResult.SUCCESS;
    } else {
      MenuProvider menuProvider = this.getMenuProvider(blockState, level, blockPos);

      if (menuProvider != null) {
        player.openMenu(menuProvider);
        player.awardStat(this.getOpenChestStat());
      }

      return InteractionResult.CONSUME;
    }
  }

  protected Stat<ResourceLocation> getOpenChestStat() {
    return Stats.CUSTOM.get(Stats.OPEN_CHEST);
  }

  public BlockEntityType<? extends AbstractObsidanumChestBlockEntity> blockEntityType() {
    return this.blockEntityType.get();
  }

  @Nullable
  public static Container getContainer(AbstractObsidanumChestBlock chestBlock, BlockState blockState, Level level, BlockPos blockPos, boolean ignoreBlockedChest) {
    return chestBlock.combine(blockState, level, blockPos, ignoreBlockedChest).<Optional<Container>>apply(CHEST_COMBINER).orElse((Container) null);
  }

  public DoubleBlockCombiner.NeighborCombineResult<? extends AbstractObsidanumChestBlockEntity> combine(BlockState blockState, Level level, BlockPos blockPos, boolean ignoreBlockedChest) {
    BiPredicate<LevelAccessor, BlockPos> biPredicate;

    if (ignoreBlockedChest) {
      biPredicate = (levelAccessor, blockPos1) -> {
        return false;
      };
    } else {
      biPredicate = AbstractObsidanumChestBlock::isChestBlockedAt;
    }

    return DoubleBlockCombiner.combineWithNeigbour(this.blockEntityType.get(), AbstractObsidanumChestBlock::getBlockType, AbstractObsidanumChestBlock::getConnectedDirection, FACING, blockState, level, blockPos, biPredicate);
  }

  @Nullable
  public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
    return this.combine(blockState, level, blockPos, false).<Optional<MenuProvider>>apply(MENU_PROVIDER_COMBINER).orElse((MenuProvider) null);
  }

  public static DoubleBlockCombiner.Combiner<AbstractObsidanumChestBlockEntity, Float2FloatFunction> opennessCombiner(final LidBlockEntity lidBlockEntity) {
    return new DoubleBlockCombiner.Combiner<>() {
      public Float2FloatFunction acceptDouble(AbstractObsidanumChestBlockEntity blockEntityOne, AbstractObsidanumChestBlockEntity blockEntityTwo) {
        return (lidBlockEntity) -> Math.max(blockEntityOne.getOpenNess(lidBlockEntity), blockEntityTwo.getOpenNess(lidBlockEntity));
      }

      public Float2FloatFunction acceptSingle(AbstractObsidanumChestBlockEntity blockEntity) {
        return blockEntity::getOpenNess;
      }

      public Float2FloatFunction acceptNone() {
        return lidBlockEntity::getOpenNess;
      }
    };
  }

  @Override
  @Nullable
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
    return level.isClientSide ? createTickerHelper(blockEntityType, this.blockEntityType(), AbstractObsidanumChestBlockEntity::lidAnimateTick) : null;
  }

  public static boolean isChestBlockedAt(LevelAccessor levelAccessor, BlockPos blockPos) {
    return isBlockedChestByBlock(levelAccessor, blockPos) || isCatSittingOnChest(levelAccessor, blockPos);
  }

  private static boolean isBlockedChestByBlock(BlockGetter blockGetter, BlockPos blockPos) {
    BlockPos above = blockPos.above();

    return blockGetter.getBlockState(above).isRedstoneConductor(blockGetter, above);
  }

  private static boolean isCatSittingOnChest(LevelAccessor levelAccessor, BlockPos blockPos) {
    List<Cat> list = levelAccessor.getEntitiesOfClass(Cat.class, new AABB((double) blockPos.getX(), (double) (blockPos.getY() + 1), (double) blockPos.getZ(), (double) (blockPos.getX() + 1), (double) (blockPos.getY() + 2), (double) (blockPos.getZ() + 1)));

    if (!list.isEmpty()) {
      for (Cat cat : list) {
        if (cat.isInSittingPose()) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState blockState) {
    return true;
  }

  @Override
  public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
    return AbstractContainerMenu.getRedstoneSignalFromContainer(getContainer(this, blockState, level, blockPos, false));
  }

  @Override
  public BlockState rotate(BlockState blockState, Rotation rotation) {
    return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
  }

  @Override
  public BlockState mirror(BlockState blockState, Mirror mirror) {
    return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> blockBlockStateBuilder) {
    blockBlockStateBuilder.add(FACING, WATERLOGGED);
  }

  @Override
  public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
    return false;
  }

  @Override
  public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource random) {
    BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);

    if (blockEntity instanceof AbstractObsidanumChestBlockEntity) {
      ((AbstractObsidanumChestBlockEntity) blockEntity).recheckOpen();
    }
  }

  @Nullable
  public static ObsidanumChestsTypes getTypeFromItem(Item itemIn) {
    return getTypeFromBlock(Block.byItem(itemIn));
  }

  @Nullable
  public static ObsidanumChestsTypes getTypeFromBlock(Block block) {
    return block instanceof AbstractObsidanumChestBlock ? ((AbstractObsidanumChestBlock) block).getType() : null;
  }

  public ObsidanumChestsTypes getType() {
    return this.type;
  }
}
