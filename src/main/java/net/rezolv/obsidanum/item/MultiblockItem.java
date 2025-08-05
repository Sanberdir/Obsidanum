/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package net.rezolv.obsidanum.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.rezolv.obsidanum.block.custom.multiblock_provider.MultiblockHandler;
import net.rezolv.obsidanum.block.custom.multiblock_provider.RotatingMultiblock;

import javax.annotation.Nullable;

public class MultiblockItem extends BlockItem {

    public MultiblockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
        if(!(pBlock instanceof RotatingMultiblock)) {
            throw new IllegalArgumentException("MultiblockItem is only valid for HorizontalMultiblock, not " + pBlock.getClass().getName());
        }
    }

    @Override
    @Nullable
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext pContext) {
        final Direction direction = pContext.getHorizontalDirection();
        final MultiblockHandler multiblockHandler = ((RotatingMultiblock) this.getBlock()).getMultiblockHandler();
        // determine the index of the clicked position and the desired center position
        final Vec3i minIndex = multiblockHandler.getMinIndex();
        final Vec3i clickedIndex = new Vec3i(0, minIndex.getY(), minIndex.getZ());
        final BlockPos center = MultiblockHandler.getCenterPos(pContext.getClickedPos(), clickedIndex, direction.getOpposite());
        // create a block place context at the center position
        return BlockPlaceContext.at(pContext, center, direction);
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }
}
