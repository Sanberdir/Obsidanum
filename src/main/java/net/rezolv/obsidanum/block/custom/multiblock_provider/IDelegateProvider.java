package net.rezolv.obsidanum.block.custom.multiblock_provider;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IDelegateProvider {

    /**
     * @param blockState the block state
     * @param blockPos the block position
     * @return the block position to delegate to
     */
    BlockPos getDelegatePos(final BlockState blockState, final BlockPos blockPos);
}
