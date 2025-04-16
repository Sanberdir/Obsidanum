package net.rezolv.obsidanum.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RightForgeScrollEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public RightForgeScrollEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.RIGHT_FORGE_SCROLL.get(), pPos, pBlockState);
    }

    private CompoundTag scrollNBT = new CompoundTag();

    public void setScrollNBT(CompoundTag tag) {
        this.scrollNBT = tag.copy();
        this.setChanged();
    }

    // Изменяем метод saveAdditional
    @Override
    public void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("ScrollData", this.scrollNBT); // Исправляем ключ для хранения данных
    }

    // Изменяем метод load
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("ScrollData")) {
            this.scrollNBT = pTag.getCompound("ScrollData"); // Загружаем по новому ключу
        }
    }

    // Добавляем метод для безопасного получения NBT
    public CompoundTag getScrollNBT() {
        return this.scrollNBT.copy(); // Возвращаем копию, чтобы избежать модификаций
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
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return false;
    }
}