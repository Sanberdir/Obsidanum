package net.rezolv.obsidanum.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ForgeCrucibleEntity extends BlockEntity implements WorldlyContainer {
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public List<ItemStack> depositedItems = new ArrayList<>(); // Новое поле для хранения истории

    // Метод для получения данных

    public void debugDepositedItems(String action) {
        Obsidanum.LOGGER.info("Deposited items after {}: {}", action, depositedItems);
    }

    // Метод для приема данных
    public void receiveScrollData(CompoundTag data) {
        this.receivedScrollData = data.copy();
        this.depositedItems.clear(); // Очищаем при новом рецепте
        setChanged();
        if(level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        debugDepositedItems("receiveScrollData");
    }
    public void clearCrucibleData() {
        if (level != null && !level.isClientSide()) {
            // Возвращаем все предметы, выбрасывая их на землю
            for (ItemStack stack : depositedItems) {
                if (!stack.isEmpty()) {
                    // Создаем предмет на земле
                    ItemEntity itemEntity = new ItemEntity(
                            level,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, // Позиция над тиглем
                            stack.copy() // Создаем копию стека, чтобы не изменять оригинал
                    );
                    itemEntity.setDefaultPickUpDelay(); // Устанавливаем задержку перед подбором
                    level.addFreshEntity(itemEntity); // Добавляем предмет в мир
                }
            }
            depositedItems.clear(); // Очищаем список
        }

        this.receivedScrollData = new CompoundTag();
        this.setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public boolean isCraftingReady = false;

    public void markReadyForCrafting() {
        this.isCraftingReady = true;
        setChanged();
    }
    // Сохраняем данные
    public CompoundTag receivedScrollData = new CompoundTag();
    public int lastUsedIndex = -1; // Сохраняем последний использованный индекс

    // Метод для получения данных
    public CompoundTag getReceivedData() {
        return this.receivedScrollData.copy();
    }

    public BlockPos getScrollPosition() {
        Direction facing = this.getBlockState().getValue(ForgeCrucible.FACING);
        return switch (facing) {
            case NORTH -> worldPosition.west();
            case SOUTH -> worldPosition.east();
            case EAST -> worldPosition.north();
            case WEST -> worldPosition.south();
            default -> null;
        };
    }

    // Обновлённый метод сохранения
    @Override
    public void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("CrucibleData", receivedScrollData);

        ListTag depositedList = new ListTag();
        for (ItemStack stack : depositedItems) {
            CompoundTag itemTag = new CompoundTag();
            stack.save(itemTag);
            depositedList.add(itemTag);
        }
        pTag.put("DepositedItems", depositedList);

    }

    // Обновлённый метод загрузки
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("CrucibleData")) {
            receivedScrollData = pTag.getCompound("CrucibleData");
        }

        depositedItems.clear();
        ListTag depositedList = pTag.getList("DepositedItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < depositedList.size(); i++) {
            depositedItems.add(ItemStack.of(depositedList.getCompound(i)));
        }
    }

    // Обновлённая синхронизация
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        // Сохраняем ВСЕ данные
        tag.put("CrucibleData", receivedScrollData);
        ListTag depositedList = new ListTag();
        for (ItemStack stack : depositedItems) {
            CompoundTag itemTag = new CompoundTag();
            stack.save(itemTag); // Сохраняем с прочностью
            depositedList.add(itemTag);
        }
        tag.put("DepositedItems", depositedList);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        // Загружаем ВСЕ данные
        load(tag); // Загружаем данные из тега
        depositedItems.clear();
        ListTag depositedList = tag.getList("DepositedItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < depositedList.size(); i++) {
            depositedItems.add(ItemStack.of(depositedList.getCompound(i)));
        }
    }

    // граница кода с NBT
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }
    public ForgeCrucibleEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.FORGE_CRUCIBLE.get(), pPos, pBlockState);
    }


    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox();
    }


    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        return true; // Позволяет размещать любые предметы через любую сторону
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true; // Позволяет забирать предметы через любую сторону
    }
    public int getAmountOfItem(ItemStack requiredStack) {
        int count = 0;
        for (ItemStack stack : depositedItems) {
            if (ItemStack.isSameItemSameTags(stack, requiredStack)) {
                count += stack.getCount();
            }
        }
        return count;
    }


    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
}