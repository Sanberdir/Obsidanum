package net.rezolv.obsidanum.block.forge_crucible.neigbor_changed;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
                        // Clear data if scroll is deactivated
                        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, null);
                        if (player != null) {
                            crucible.clearCrucibleData();
                        }
                    } else {
                        // Copy data from scroll to crucible
                        BlockEntity rightEntity = level.getBlockEntity(expectedRightPos);
                        if (rightEntity instanceof RightForgeScrollEntity scrollEntity) {
                            CompoundTag scrollNBT = scrollEntity.getScrollNBT();

                            CompoundTag dataToSend = new CompoundTag();

                            // Copy ingredients
                            if (scrollNBT.contains("Ingredients", Tag.TAG_LIST)) {
                                dataToSend.put("Ingredients", scrollNBT.getList("Ingredients", Tag.TAG_COMPOUND));
                            }

                            // Copy main result
                            if (scrollNBT.contains("RecipeResult", Tag.TAG_LIST)) {
                                dataToSend.put("RecipeResult", scrollNBT.getList("RecipeResult", Tag.TAG_COMPOUND));
                            }

                            // Copy recipe plans
                            if (scrollNBT.contains("RecipesPlans", Tag.TAG_STRING)) {
                                dataToSend.putString("RecipesPlans", scrollNBT.getString("RecipesPlans"));
                            }

                            // Copy and process bonus outputs
                            if (scrollNBT.contains("BonusOutputs", Tag.TAG_LIST)) {
                                ListTag bonusOutputs = scrollNBT.getList("BonusOutputs", Tag.TAG_COMPOUND);
                                dataToSend.put("BonusOutputs", bonusOutputs);

                                // Create processed version with min/max support
                                ListTag processedBonuses = new ListTag();
                                for (Tag bonusTag : bonusOutputs) {
                                    if (bonusTag instanceof CompoundTag bonusCompound) {
                                        CompoundTag processed = new CompoundTag();

                                        // Copy item data
                                        if (bonusCompound.contains("Item", Tag.TAG_COMPOUND)) {
                                            processed.put("Item", bonusCompound.getCompound("Item"));
                                        }

                                        // Copy chance (default to 1.0 if not specified)
                                        float chance = bonusCompound.contains("Chance", Tag.TAG_FLOAT)
                                                ? bonusCompound.getFloat("Chance")
                                                : 1.0f;
                                        processed.putFloat("Chance", chance);

                                        // Handle min/max quantities
                                        int min = bonusCompound.contains("Min", Tag.TAG_INT)
                                                ? bonusCompound.getInt("Min")
                                                : 1; // Default to 1 if not specified

                                        int max = bonusCompound.contains("Max", Tag.TAG_INT)
                                                ? bonusCompound.getInt("Max")
                                                : min; // Default to min if not specified

                                        // Ensure max is not less than min
                                        if (max < min) {
                                            max = min;
                                        }

                                        processed.putInt("Min", min);
                                        processed.putInt("Max", max);

                                        processedBonuses.add(processed);
                                    }
                                }
                                dataToSend.put("ProcessedBonuses", processedBonuses);
                            }

                            crucible.receiveScrollData(dataToSend);
                            crucible.setChanged();
                            level.sendBlockUpdated(pos, state, state, 3);
                        }

                        if (scrollType == ScrollType.NONE) {
                            Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, null);
                            if (player != null) {
                                crucible.clearCrucibleData(); // Clear data
                            }
                        }
                    }
                }
            }
        }
    }
}