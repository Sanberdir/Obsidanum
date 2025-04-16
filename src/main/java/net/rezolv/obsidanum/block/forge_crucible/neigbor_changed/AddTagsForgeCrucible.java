package net.rezolv.obsidanum.block.forge_crucible.neigbor_changed;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.RightForgeScroll;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.entity.RightForgeScrollEntity;
import net.rezolv.obsidanum.block.enum_blocks.ScrollType;

public class AddTagsForgeCrucible {

    public static void handleNeighborUpdate(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        Direction facing = state.getValue(ForgeCrucible.FACING);
        BlockPos expectedRightPos = switch (facing) {
            case NORTH -> pos.west();
            case SOUTH -> pos.east();
            case EAST -> pos.north();
            case WEST -> pos.south();
            default -> null;
        };

        if (expectedRightPos != null && expectedRightPos.equals(fromPos)) {
            BlockState rightBlockState = level.getBlockState(expectedRightPos);
            BlockEntity crucibleEntity = level.getBlockEntity(pos);

            if (rightBlockState.getBlock() instanceof RightForgeScroll
                    && rightBlockState.hasProperty(RightForgeScroll.TYPE_SCROLL)) {

                ScrollType scrollType = rightBlockState.getValue(RightForgeScroll.TYPE_SCROLL);

                if (crucibleEntity instanceof ForgeCrucibleEntity crucible) {
                    if (scrollType == ScrollType.NONE) {
                        // Очищаем данные, если свиток деактивирован
                        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, null);
                        if (player != null) {
                            crucible.clearCrucibleData();
                        }
                    } else {
                        // Старая логика копирования данных
                        BlockEntity rightEntity = level.getBlockEntity(expectedRightPos);
                        if (rightEntity instanceof RightForgeScrollEntity scrollEntity) {
                            CompoundTag scrollNBT = scrollEntity.getScrollNBT();

                            CompoundTag dataToSend = new CompoundTag();
                            if (scrollNBT.contains("Ingredients", Tag.TAG_LIST)) {
                                dataToSend.put("Ingredients", scrollNBT.getList("Ingredients", Tag.TAG_COMPOUND));
                            }

                            if (scrollNBT.contains("RecipeResult", Tag.TAG_LIST)) {
                                dataToSend.put("RecipeResult", scrollNBT.getList("RecipeResult", Tag.TAG_COMPOUND));
                            }

                            if (scrollNBT.contains("RecipesPlans", Tag.TAG_STRING)) {
                                dataToSend.putString("RecipesPlans", scrollNBT.getString("RecipesPlans"));
                            }

                            crucible.receiveScrollData(dataToSend);
                            crucible.setChanged();
                            level.sendBlockUpdated(pos, state, state, 3);
                        }
                    }
                }
            }
        }
    }
}