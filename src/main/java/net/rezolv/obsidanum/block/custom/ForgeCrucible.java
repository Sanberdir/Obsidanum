package net.rezolv.obsidanum.block.custom;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.network.NetworkHooks;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.enum_blocks.ScrollType;
import net.rezolv.obsidanum.block.forge_crucible.neigbor_changed.*;
import net.rezolv.obsidanum.gui.forge_crucible.destruction_render.ForgeCrucibleDestructionMenu;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiMenu;
import net.rezolv.obsidanum.gui.forge_crucible.repair_render.ForgeCrucibleRepairMenu;
import net.rezolv.obsidanum.gui.forge_crucible.upgrade_render.ForgeCrucibleUpgradeMenu;
import org.jetbrains.annotations.Nullable;


public class ForgeCrucible extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public ForgeCrucible(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ForgeCrucibleEntity) {
            ((ForgeCrucibleEntity) be).tick();
        }
        level.scheduleTick(pos, this, 1);
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ForgeCrucibleEntity crucible)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer server) {
            // узнаём, какой scrollType пришёл из соседнего блока
            String typeName = crucible.getReceivedData().getString("TypeScroll");
            ScrollType type = ScrollType.valueOf(typeName.isEmpty() ? "NONE" : typeName);

            MenuProvider provider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable(
                            type == ScrollType.NONE
                                    ? "container.obsidanum.forge_crucible_repair"
                                    : "container.obsidanum.forge_crucible"
                    );
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player ply) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos);

                    if ((type == ScrollType.NETHER)||(type == ScrollType.CATACOMBS)||(type == ScrollType.ORDER)) {
                        return new ForgeCrucibleGuiMenu(id, inv, buf);
                    }
                    if ((type == ScrollType.UPDATE_CATACOMBS)||(type == ScrollType.UPDATE_NETHER||(type == ScrollType.UPDATE_ORDER))) {
                        return new ForgeCrucibleUpgradeMenu(id, inv, buf);
                    }
                    if ((type == ScrollType.REPAIR)) {
                        return new ForgeCrucibleRepairMenu(id, inv, buf);
                    }
                    if ((type == ScrollType.DESTRUCTION)) {
                        return new ForgeCrucibleDestructionMenu(id, inv, buf);
                    }
                    else return null;
                }
            };

            NetworkHooks.openScreen(server, provider, pos);
        }

        return InteractionResult.SUCCESS;
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ForgeCrucibleEntity crucible) {
            // Get the scroll type
            String typeName = crucible.getReceivedData().getString("TypeScroll");
            ScrollType type = ScrollType.valueOf(typeName.isEmpty() ? "NONE" : typeName);
            AddTagsForgeCrucible.handleNeighborUpdate(state, level, pos, fromPos);

            // Only run these handlers for specific scroll types
            if (type == ScrollType.NETHER || type == ScrollType.CATACOMBS || type == ScrollType.ORDER) {
                LeftCornerCompleteRecipe.handleNeighborUpdate(state, level, pos, fromPos);
            }
            if (type == ScrollType.UPDATE_NETHER || type == ScrollType.UPDATE_CATACOMBS|| type == ScrollType.UPDATE_ORDER) {
                LeftCornerCompleteUp.handleNeighborUpdate(state, level, pos, fromPos);
            }
            if (type == ScrollType.REPAIR ) {
                LeftCornerCompleteRepair.handleNeighborUpdate(state, level, pos, fromPos);
            }
            if (type == ScrollType.DESTRUCTION ) {
                LeftCornerCompleteDestruction.handleNeighborUpdate(state, level, pos, fromPos);
            }
        }
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ForgeCrucibleEntity(pPos, pState);
    }
}