package net.rezolv.obsidanum.gui.hammer_forge.recipes_render;

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
    public IItemHandler internal;
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

        BlockPos pos = extraData.readBlockPos();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.access = ContainerLevelAccess.create(world, pos);

        ForgeCrucibleEntity blockEntity = (ForgeCrucibleEntity) world.getBlockEntity(pos);
        if (blockEntity != null) {
            this.internal = blockEntity.itemHandler;
            this.bound = true;
        } else {
            this.internal = new ItemStackHandler(12); // Увеличиваем размер под все слоты (0-11)
        }

        // Слот результата (6) — нельзя помещать предметы вручную
        this.addSlot(new SlotItemHandler(internal, 6, 79, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Новые выходные слоты 7-11, все с запретом ручного помещения предметов:
        this.addSlot(new SlotItemHandler(internal, 7, 79, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(internal, 8, 59, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(internal, 9, 99, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(internal, 10, 39, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new SlotItemHandler(internal, 11, 119, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Слоты для ингредиентов (0-5)
        for (int i = 0; i < 6; i++) {
            int slotIndex = i;
            this.addSlot(new SlotItemHandler(internal, slotIndex, 35 + i * 18, 73) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (blockEntity == null) return false;
                    CompoundTag data = blockEntity.getReceivedData();
                    if (!data.contains("Ingredients")) return false;

                    ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
                    if (slotIndex >= ingredients.size()) return false;

                    try {
                        CompoundTag entry = ingredients.getCompound(slotIndex);
                        JsonObject json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
                        return matchesIngredient(stack, json);
                    } catch (Exception e) {
                        Obsidanum.LOGGER.error("Error checking ingredient match: {}", e.getMessage());
                        return false;
                    }
                }
            });
        }

        // Добавляем инвентарь игрока
        for (int si = 0; si < 3; ++si) {
            for (int sj = 0; sj < 9; ++sj) {
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 131 + si * 18));
            }
        }
        for (int si = 0; si < 9; ++si) {
            this.addSlot(new Slot(inv, si, 8 + si * 18, 189));
        }
    }
    private boolean matchesIngredient(ItemStack stack, JsonObject ingredientJson) {
        if (stack.isEmpty()) return false;

        if (ingredientJson.has("item")) {
            ResourceLocation itemId = new ResourceLocation(ingredientJson.get("item").getAsString());
            return ForgeRegistries.ITEMS.getValue(itemId) == stack.getItem();
        }

        if (ingredientJson.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(ingredientJson.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            return stack.is(tag);
        }

        return false;
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
        ItemStack copiedStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            copiedStack = originalStack.copy();

            // Если слот в инвентаре игрока (слоты 12-47)
            if (index >= 12) {
                // Перемещаем в слоты крафта/выхода (0-11)
                if (!this.moveItemStackTo(originalStack, 0, 12, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Если слот в слоте крафта или выхода (0-11)
            else {
                // Перемещаем в инвентарь игрока (12-47)
                if (!this.moveItemStackTo(originalStack, 12, 48, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == copiedStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return copiedStack;
    }


    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
        // Теперь предметы автоматически сохраняются в BlockEntity
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
