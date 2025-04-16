package net.rezolv.obsidanum.chests.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.chests.block.entity.ObsidanumChestsBlockEntityTypes;
import net.rezolv.obsidanum.chests.block.entity.ObsidianChestBlockEntity;

import javax.annotation.Nullable;

public class ObsidianChestBlock extends AbstractObsidanumChestBlock {

  public ObsidianChestBlock(Properties properties) {
    super(properties, ObsidanumChestsBlockEntityTypes.OBSIDIAN_CHEST::get, ObsidanumChestsTypes.OBSIDIAN);
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
    return new ObsidianChestBlockEntity(blockPos, blockState);
  }
}
