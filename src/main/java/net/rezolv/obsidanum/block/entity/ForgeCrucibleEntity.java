package net.rezolv.obsidanum.block.entity;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ForgeCrucibleEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public final ItemStackHandler itemHandler = new ItemStackHandler(12) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final LazyOptional<? extends IItemHandler>[] handlers = new LazyOptional[Direction.values().length];

    public List<ItemStack> depositedItems = new ArrayList<>(); // Новое поле для хранения истории

    // Метод для получения данных
    public boolean isIngredientSatisfied(int index) {
        if (!receivedScrollData.contains("Ingredients", Tag.TAG_LIST)) return false;
        ListTag ingredientsTag = receivedScrollData.getList("Ingredients", Tag.TAG_COMPOUND);
        if (index < 0 || index >= ingredientsTag.size()) return false;

        CompoundTag ingredientTag = ingredientsTag.getCompound(index);
        int requiredCount = ingredientTag.getInt("count");

        if (ingredientTag.contains("item")) {
            ItemStack required = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ingredientTag.getString("item"))));
            int actualCount = getAmountOfItem(required);
            return actualCount >= requiredCount;
        }

        if (ingredientTag.contains("tag")) {
            String tagId = ingredientTag.getString("tag");
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
            int totalCount = 0;
            for (ItemStack stack : depositedItems) {
                if (stack.is(tagKey)) {
                    totalCount += stack.getCount();
                }
            }
            return totalCount >= requiredCount;
        }

        return false;
    }
    // Метод для приема данных
    public void receiveScrollData(CompoundTag data) {
        this.receivedScrollData = data.copy();

        // Очищаем слоты ингредиентов при получении пустого рецепта
        if (!data.contains("Ingredients") || data.getList("Ingredients", Tag.TAG_COMPOUND).isEmpty()) {
            for (int i = 0; i < 6; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    // Выбрасываем предмет
                    ItemEntity itemEntity = new ItemEntity(
                            level,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1.0,
                            worldPosition.getZ() + 0.5,
                            stack.copy()
                    );
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);

                    // Очищаем слот
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }

        this.setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public void clearCrucibleData() {
        if (level != null && !level.isClientSide()) {
            // Собираем предметы только из слотов ингредиентов (0-5)
            List<ItemStack> itemsToDrop = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    itemsToDrop.add(stack.copy());
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY); // Очищаем слот
                }
            }

            // Выбрасываем предметы в мир
            for (ItemStack stack : itemsToDrop) {
                ItemEntity itemEntity = new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5,
                        stack
                );
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }

        this.receivedScrollData = new CompoundTag();
        this.depositedItems.clear(); // Очищаем историю
        this.setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 256;
    }
    // Сохраняем данные
    public CompoundTag receivedScrollData = new CompoundTag();

    // Метод для получения данных
    public CompoundTag getReceivedData() {
        return this.receivedScrollData.copy();
    }

    // Обновлённый метод сохранения
    @Override
    public void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("CrucibleData", receivedScrollData);
        pTag.put("Inventory", itemHandler.serializeNBT());

        ListTag depositedList = new ListTag();
        for (ItemStack stack : depositedItems) {
            depositedList.add(stack.save(new CompoundTag()));
        }
        pTag.put("DepositedItems", depositedList);
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ForgeCrucibleGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }


    // Обновлённый метод загрузки
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("CrucibleData")) {
            receivedScrollData = pTag.getCompound("CrucibleData");
        }
        if (pTag.contains("Inventory")) {
            itemHandler.deserializeNBT(pTag.getCompound("Inventory"));
        }

        depositedItems.clear();
        if (pTag.contains("DepositedItems")) {
            ListTag depositedList = pTag.getList("DepositedItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < depositedList.size(); i++) {
                depositedItems.add(ItemStack.of(depositedList.getCompound(i)));
            }
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
        // Добавьте инициализацию:
        Arrays.fill(handlers, LazyOptional.empty());
        for (Direction dir : Direction.values()) {
            handlers[dir.ordinal()] = LazyOptional.of(() -> new SidedInvWrapper(this, dir));
        }
    }


    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox();
    }


    @Override
    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, this.getContainerSize()).toArray();
    }


    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(index, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Можно добавить ограничения, например:
        // return !stack.is(Items.DIRT); // Запретить помещать землю
        return true; // Пока разрешаем любые предметы
    }
    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true; // Позволяет забирать предметы через любую сторону
    }
    public int getAmountOfItem(ItemStack requiredStack) {
        int count = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
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

    public int getContainerSize() {
        return itemHandler.getSlots(); // Вернет количество слотов (7 в вашем случае)
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(itemHandler.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            items.set(i, itemHandler.getStackInSlot(i));
        }
        return items;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
        setChanged();
    }


    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                (double)this.worldPosition.getX() + 0.5D,
                (double)this.worldPosition.getY() + 0.5D,
                (double)this.worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }
    @Override
    public void setRemoved() {
        super.setRemoved();
        for (LazyOptional<? extends IItemHandler> handler : handlers)
            handler.invalidate();
    }
    protected void setItems(NonNullList<ItemStack> stacks) {
        // Убедитесь, что количество слотов совпадает
        int slotsToCopy = Math.min(stacks.size(), itemHandler.getSlots());
        for (int i = 0; i < slotsToCopy; i++) {
            itemHandler.setStackInSlot(i, stacks.get(i));
        }
    }
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
            return handlers[facing.ordinal()].cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public void clearContent() {
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}