package net.rezolv.obsidanum.gui.forge_crucible.destruction_render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.gui.ObsidanumMenus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ForgeCrucibleDestructionMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    protected ForgeCrucibleDestructionMenu(@Nullable MenuType<?> pMenuType, int pContainerId, Level world, Player entity) {
        super(pMenuType, pContainerId);
        this.world = world;
        this.entity = entity;
    }

    public final static HashMap<String, Object> guistate = new HashMap<>();
    public final Level world;
    public final Player entity;
    public int x, y, z;
    private ContainerLevelAccess access = ContainerLevelAccess.NULL;
    public IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private boolean bound = false;
    private Supplier<Boolean> boundItemMatcher = null;
    private Entity boundEntity = null;
    private BlockEntity boundBlockEntity = null;

    @Nullable
    public ForgeCrucibleEntity getBlockEntity() {
        if (this.world == null) return null;
        return this.world.getBlockEntity(new BlockPos(x, y, z)) instanceof ForgeCrucibleEntity be ? be : null;
    }

    public ForgeCrucibleDestructionMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ObsidanumMenus.FORGE_CRUCIBLE_GUI_DESTRUCTION.get(), id);
        this.entity = inv.player;
        this.world = inv.player.level();

        BlockPos pos = extraData.readBlockPos();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.access = ContainerLevelAccess.create(world, pos);

        ForgeCrucibleEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            this.internal = blockEntity.itemHandler;
            this.bound = true;
        } else {
            this.internal = new ItemStackHandler(6);
        }

        // Output slots (0-4)
        for (int i = 0; i < 5; i++) {
            this.addSlot(new SlotItemHandler(internal, i, getSlotX(i), getSlotY(i)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // Output slots are not for placing items
                }
            });
        }

        // Input slot (5)
        this.addSlot(new SlotItemHandler(internal, 5, 119, 58));

        // Player inventory (6-32)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + (row + 1) * 9, 47 + col * 18, 131 + row * 18));
            }
        }

        // Hotbar (33-41)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 47 + col * 18, 189));
        }
    }

    private int getSlotX(int index) {
        return switch (index) {
            case 0 -> 119;
            case 1 -> 87;
            case 2 -> 151;
            case 3 -> 72;
            case 4 -> 166;
            case 5 -> 119;
            default -> 0;
        };
    }

    private int getSlotY(int index) {
        return switch (index) {
            case 0 -> 11;
            case 1, 2 -> 26;
            case 3, 4, 5 -> 58;
            default -> 0;
        };
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
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // Player inventory or hotbar -> container
            if (index >= 6) {
                // Try to move to input slot (5)
                if (this.slots.get(5).mayPlace(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 5, 6, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (index < 33) {
                        if (!this.moveItemStackTo(itemstack1, 33, 42, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 6, 33, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            // Container -> player inventory
            else if (!this.moveItemStackTo(itemstack1, 6, 42, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}