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
            case EAST  -> pos.north();
            case WEST  -> pos.south();
            default    -> null;
        };

        if (expectedRightPos != null && expectedRightPos.equals(fromPos)) {
            var rightState = level.getBlockState(expectedRightPos);
            var be = level.getBlockEntity(pos);

            if (rightState.getBlock() instanceof RightForgeScroll
                    && rightState.hasProperty(RightForgeScroll.TYPE_SCROLL)
                    && be instanceof ForgeCrucibleEntity crucible) {

                ScrollType scrollType = rightState.getValue(RightForgeScroll.TYPE_SCROLL);

                // If scroll is deactivated - clear immediately
                if (scrollType == ScrollType.NONE) {
                    Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, null);
                    if (player != null) {
                        crucible.clearCrucibleData();
                    }
                    return;
                }

                // Otherwise copy data
                BlockEntity rightBe = level.getBlockEntity(expectedRightPos);
                if (rightBe instanceof RightForgeScrollEntity scrollEntity) {
                    CompoundTag scrollNBT = scrollEntity.getScrollNBT();
                    CompoundTag dataToSend = new CompoundTag();

                    // Add scroll type
                    dataToSend.putString("TypeScroll", scrollType.name());

                    // Copy basic data
                    if (scrollNBT.contains("Upgrade", Tag.TAG_STRING)) {
                        dataToSend.putString("Upgrade", scrollNBT.getString("Upgrade"));
                    }
                    if (scrollNBT.contains("Ingredients", Tag.TAG_LIST)) {
                        dataToSend.put("Ingredients", scrollNBT.getList("Ingredients", Tag.TAG_COMPOUND));
                    }
                    if (scrollNBT.contains("RecipeResult", Tag.TAG_LIST)) {
                        dataToSend.put("RecipeResult", scrollNBT.getList("RecipeResult", Tag.TAG_COMPOUND));
                    }
                    if (scrollNBT.contains("RecipesPlans", Tag.TAG_STRING)) {
                        dataToSend.putString("RecipesPlans", scrollNBT.getString("RecipesPlans"));
                    }

                    // Copy hammer strikes count (default to 1 if not present)
                    int hammerStrikes = scrollNBT.contains("HammerStrikes", Tag.TAG_INT)
                            ? scrollNBT.getInt("HammerStrikes")
                            : 1;
                    dataToSend.putInt("HammerStrikes", hammerStrikes);

                    // Handle Multiple Outputs with chances
                    if (scrollNBT.contains("MultipleOutputs", Tag.TAG_LIST)) {
                        ListTag multipleOutputs = scrollNBT.getList("MultipleOutputs", Tag.TAG_COMPOUND);
                        ListTag processedOutputs = new ListTag();

                        // Get chances if they exist in a separate list
                        ListTag chancesList = scrollNBT.contains("OutputChances", Tag.TAG_LIST)
                                ? scrollNBT.getList("OutputChances", Tag.TAG_COMPOUND)
                                : null;

                        for (int i = 0; i < multipleOutputs.size(); i++) {
                            Tag outputTag = multipleOutputs.get(i);
                            if (outputTag instanceof CompoundTag outputCompound) {
                                CompoundTag processed = new CompoundTag();

                                // Copy item data
                                if (outputCompound.contains("id", Tag.TAG_STRING)) {
                                    processed.putString("id", outputCompound.getString("id"));
                                }
                                if (outputCompound.contains("tag", Tag.TAG_COMPOUND)) {
                                    processed.put("tag", outputCompound.getCompound("tag"));
                                }

                                // Handle count
                                int count = outputCompound.contains("Count", Tag.TAG_INT)
                                        ? outputCompound.getInt("Count")
                                        : 1;
                                processed.putInt("Count", count);

                                // Handle chance - either from the output compound or from chances list
                                float chance = outputCompound.contains("Chance", Tag.TAG_FLOAT)
                                        ? Math.max(0.0f, Math.min(1.0f, outputCompound.getFloat("Chance"))) // Ограничение 0-1
                                        : 1.0f;
                                if (outputCompound.contains("Chance", Tag.TAG_FLOAT)) {
                                    chance = outputCompound.getFloat("Chance");
                                } else if (chancesList != null && i < chancesList.size()) {
                                    CompoundTag chanceTag = chancesList.getCompound(i);
                                    chance = chanceTag.getFloat("Chance");
                                }
                                processed.putFloat("Chance", chance);

                                processedOutputs.add(processed);
                            }
                        }

                        dataToSend.put("MultipleOutputs", multipleOutputs); // original data
                        dataToSend.put("ProcessedMultipleOutputs", processedOutputs); // processed data with chances
                    }

                    // Handle Bonus Outputs (similar to multiple outputs but with min/max)
                    if (scrollNBT.contains("BonusOutputs", Tag.TAG_LIST)) {
                        ListTag bonusOutputs = scrollNBT.getList("BonusOutputs", Tag.TAG_COMPOUND);
                        dataToSend.put("BonusOutputs", bonusOutputs);

                        ListTag processedBonuses = new ListTag();
                        for (Tag bonusTag : bonusOutputs) {
                            if (bonusTag instanceof CompoundTag bonusCompound) {
                                CompoundTag processed = new CompoundTag();

                                // Item data
                                if (bonusCompound.contains("id", Tag.TAG_STRING)) {
                                    processed.putString("id", bonusCompound.getString("id"));
                                }
                                if (bonusCompound.contains("tag", Tag.TAG_COMPOUND)) {
                                    processed.put("tag", bonusCompound.getCompound("tag"));
                                }

                                // Chance
                                float chance = bonusCompound.contains("Chance", Tag.TAG_FLOAT)
                                        ? bonusCompound.getFloat("Chance")
                                        : 1.0f;
                                processed.putFloat("Chance", chance);

                                // Quantity range
                                int min = bonusCompound.contains("Min", Tag.TAG_INT)
                                        ? bonusCompound.getInt("Min")
                                        : 1;
                                int max = bonusCompound.contains("Max", Tag.TAG_INT)
                                        ? bonusCompound.getInt("Max")
                                        : min;
                                if (max < min) max = min;
                                processed.putInt("Min", min);
                                processed.putInt("Max", max);

                                processedBonuses.add(processed);
                            }
                        }
                        dataToSend.put("ProcessedBonuses", processedBonuses);
                    }

                    // Send data to Crucible
                    crucible.receiveScrollData(dataToSend);
                    crucible.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }
    }
}