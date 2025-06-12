package net.rezolv.obsidanum.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.block.custom.HammerForge;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

public class HammerForgeEntity extends BaseContainerBlockEntity implements WorldlyContainer, GeoAnimatable {
    public HammerForgeEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.HAMMER_FORGE.get(), pPos, pBlockState);

    }
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    public boolean isPowered() {
        if (this.level == null) return false;
        return this.getBlockState().getValue(HammerForge.POWERED);
    }
    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof HammerForge forge) {
                boolean shouldBePowered = forge.checkForPressedCorners(level, worldPosition);
                if (shouldBePowered != state.getValue(HammerForge.POWERED)) {
                    level.setBlock(worldPosition, state.setValue(HammerForge.POWERED, shouldBePowered), Block.UPDATE_ALL);
                    level.scheduleTick(worldPosition, state.getBlock(), 1);
                }
            }
        }
    }
    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return null;
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return null;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {

    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    private boolean wasPowered = false; // Добавляем флаг для отслеживания состояния

    private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> state) {
        boolean isPowered = isPowered();

        if (isPowered && !wasPowered) {
            // Только при переходе из выключенного в включенное состояние
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.down_move", Animation.LoopType.PLAY_ONCE));
            wasPowered = true;
        } else if (!isPowered) {
            wasPowered = false;
            state.getController().setAnimation(RawAnimation.begin()
                    .then("animation.idle", Animation.LoopType.LOOP));
        }

        return PlayState.CONTINUE;
    }
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return RenderUtils.getCurrentTick();
    }
}
