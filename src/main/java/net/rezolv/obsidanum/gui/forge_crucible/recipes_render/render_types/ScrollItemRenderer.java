package net.rezolv.obsidanum.gui.forge_crucible.recipes_render.render_types;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.RightForgeScroll;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.entity.RightForgeScrollEntity;
import net.rezolv.obsidanum.block.enum_blocks.ScrollType;
import net.rezolv.obsidanum.item.ItemsObs;

public class ScrollItemRenderer {
    public static void render(GuiGraphics guiGraphics, Font font, Level world,
                              int x, int y, int z, int leftPos, int topPos) {
        ForgeCrucibleEntity crucible = getCrucible(world, x, y, z);
        if (crucible == null) return;

        BlockPos scrollPos = findScrollPosition(crucible);
        if (scrollPos == null) return;

        RightForgeScrollEntity scroll = getScrollEntity(world, scrollPos);
        if (scroll == null) return;

        ItemStack scrollStack = prepareScrollStack(world, scrollPos, scroll);
        if (scrollStack.isEmpty()) return;

        renderScroll(guiGraphics, font, scrollStack, leftPos, topPos);
    }

    private static ForgeCrucibleEntity getCrucible(Level world, int x, int y, int z) {
        BlockEntity blockEntity = world.getBlockEntity(new BlockPos(x, y, z));
        return blockEntity instanceof ForgeCrucibleEntity crucible ? crucible : null;
    }

    private static BlockPos findScrollPosition(ForgeCrucibleEntity crucible) {
        Direction facing = crucible.getBlockState().getValue(ForgeCrucible.FACING);
        return switch (facing) {
            case NORTH -> crucible.getBlockPos().west();
            case SOUTH -> crucible.getBlockPos().east();
            case EAST -> crucible.getBlockPos().north();
            case WEST -> crucible.getBlockPos().south();
            default -> null;
        };
    }

    private static RightForgeScrollEntity getScrollEntity(Level world, BlockPos scrollPos) {
        BlockEntity scrollEntity = world.getBlockEntity(scrollPos);
        return scrollEntity instanceof RightForgeScrollEntity scroll ? scroll : null;
    }

    private static ItemStack prepareScrollStack(Level world, BlockPos scrollPos, RightForgeScrollEntity scroll) {
        ScrollType scrollType = world.getBlockState(scrollPos).getValue(RightForgeScroll.TYPE_SCROLL);
        if (scrollType == ScrollType.NONE) {
            return ItemStack.EMPTY;
        }

        ItemStack scrollStack = getScrollItemStack(scrollType);
        CompoundTag nbt = scroll.getScrollNBT();
        if (!nbt.isEmpty()) {
            scrollStack.setTag(nbt.copy());
        }
        return scrollStack;
    }

    private static void renderScroll(GuiGraphics guiGraphics, Font font, ItemStack scrollStack, int leftPos, int topPos) {
        int scrollX = leftPos + 119;
        int scrollY = topPos + 105;

        guiGraphics.renderItem(scrollStack, scrollX, scrollY);
        guiGraphics.renderItemDecorations(font, scrollStack, scrollX, scrollY);
    }

    private static ItemStack getScrollItemStack(ScrollType type) {
        return switch (type) {
            case NETHER -> ItemsObs.NETHER_PLAN.get().getDefaultInstance();
            case ORDER -> ItemsObs.ORDER_PLAN.get().getDefaultInstance();
            case CATACOMBS -> ItemsObs.CATACOMBS_PLAN.get().getDefaultInstance();
            case UPDATE -> ItemsObs.UPGRADE_PLAN.get().getDefaultInstance();
            default -> ItemStack.EMPTY;
        };
    }
}
