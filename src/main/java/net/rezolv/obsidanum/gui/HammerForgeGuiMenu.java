package net.rezolv.obsidanum.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.entity.RightForgeScrollEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class HammerForgeGuiMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    protected HammerForgeGuiMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Level world, Player entity) {
        super(pMenuType, pContainerId);
        this.world = world;
        this.entity = entity;
    }


    public final static HashMap<String, Object> guistate = new HashMap<>();
    public final Level world;
    public final Player entity;
    public int x, y, z;
    private ContainerLevelAccess access = ContainerLevelAccess.NULL;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private boolean bound = false;
    private Supplier<Boolean> boundItemMatcher = null;
    private Entity boundEntity = null;
    private BlockEntity boundBlockEntity = null;
    @Nullable
    public ForgeCrucibleEntity getBlockEntity() {
        if (this.world == null) return null;
        BlockEntity be = this.world.getBlockEntity(new BlockPos(x, y, z));
        return be instanceof ForgeCrucibleEntity ? (ForgeCrucibleEntity) be : null;
    }
    public HammerForgeGuiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ObsidanumMenus.HAMMER_FORGE_GUI.get(), id);
        this.entity = inv.player;
        this.world = inv.player.level();
        this.internal = new ItemStackHandler(0); // Изменено на 0 слотов

        BlockPos pos = null;
        if (extraData != null) {
            pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            access = ContainerLevelAccess.create(world, pos);
        }

        if (pos != null) {
            if (extraData.readableBytes() == 1) { // bound to item
                byte hand = extraData.readByte();
                ItemStack itemstack = hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem();
                this.boundItemMatcher = () -> itemstack == (hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem());
                itemstack.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                    this.internal = capability;
                    this.bound = true;
                });
            } else if (extraData.readableBytes() > 1) { // bound to entity
                extraData.readByte(); // drop padding
                boundEntity = world.getEntity(extraData.readVarInt());
                if (boundEntity != null)
                    boundEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                        this.internal = capability;
                        this.bound = true;
                    });
            } else { // might be bound to block
                boundBlockEntity = this.world.getBlockEntity(pos);
                if (boundBlockEntity != null)
                    boundBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
                        this.internal = capability;
                        this.bound = true;
                    });
            }
        }

        this.internal = new ItemStackHandler(7); // Changed to 7 slots (6 input + 1 output)
        this.addSlot(new SlotItemHandler(internal, 6, 79, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output slot can't be manually filled
            }
        });

        for (int i = 0; i < 6; i++) {

            int finalI = i;
            this.addSlot(new SlotItemHandler(internal, finalI, 35 + i * 18, 73) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    CompoundTag data = getBlockEntity().getReceivedData();
                    if (!data.contains("Ingredients")) return false;

                    ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
                    if (finalI >= ingredients.size()) return false;

                    try {
                        CompoundTag entry = ingredients.getCompound(finalI);
                        JsonObject json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
                        ItemStack requiredStack = getStackForIngredient(json);
                        return ItemStack.isSameItemSameTags(stack, requiredStack);
                    } catch (Exception e) {
                        return false;
                    }
                }
            });
        }

// Добавляем 3 ряда основного инвентаря (27 слотов)
        for (int si = 0; si < 3; ++si) {
            for (int sj = 0; sj < 9; ++sj) {

                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 131 + si * 18));
            }
        }

// Добавляем слоты горячей панели (9 слотов)
        for (int si = 0; si < 9; ++si) {

            this.addSlot(new Slot(inv, si, 8 + si * 18, 189));
        }
    }
    private ItemStack getStackForIngredient(JsonObject json) {
        if (json.has("item")) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(json.get("item").getAsString()));
            return new ItemStack(item);
        } else if (json.has("tag")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    new ResourceLocation(json.get("tag").getAsString()));
            return ForgeRegistries.ITEMS.tags()
                    .getTag(tag)
                    .stream()
                    .findFirst()
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }
    @Override
    public boolean stillValid(Player player) {
        if (this.bound) {
            if (this.boundItemMatcher != null)
                return this.boundItemMatcher.get();
            else if (this.boundBlockEntity != null)
                return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
            else if (this.boundEntity != null)
                return this.boundEntity.isAlive();
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Упрощена логика быстрого перемещения, так как нет специальных слотов
            if (!this.moveItemStackTo(itemstack1, 0, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
                for (int j = 0; j < internal.getSlots(); ++j) {
                    playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
                }
            } else {
                for (int i = 0; i < internal.getSlots(); ++i) {
                    playerIn.getInventory().placeItemBackInInventory(internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
                }
            }
        }
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
