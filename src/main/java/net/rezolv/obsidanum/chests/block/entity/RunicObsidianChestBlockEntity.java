package net.rezolv.obsidanum.chests.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.chests.block.ObsidanumChestsTypes;
import net.rezolv.obsidanum.chests.inventory.ObsidanumChestMenu;

public class RunicObsidianChestBlockEntity extends AbstractObsidanumChestBlockEntity {

  public RunicObsidianChestBlockEntity(BlockPos blockPos, BlockState blockState) {
    super(ObsidanumChestsBlockEntityTypes.RUNIC_OBSIDIAN_CHEST.get(), blockPos, blockState, ObsidanumChestsTypes.RUNIC_OBSIDIAN, BlocksObs.RUNIC_OBSIDIAN_CHEST::get);
  }

  @Override
  protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
    return ObsidanumChestMenu.createRunicObsidianContainer(containerId, playerInventory, this);
  }
}