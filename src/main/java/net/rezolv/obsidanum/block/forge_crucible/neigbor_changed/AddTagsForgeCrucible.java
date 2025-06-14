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

                // Если свиток деактивирован — сразу чистим
                if (scrollType == ScrollType.NONE) {
                    Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, null);
                    if (player != null) {
                        crucible.clearCrucibleData();
                    }
                    return;
                }

                // Иначе копируем данные
                BlockEntity rightBe = level.getBlockEntity(expectedRightPos);
                if (rightBe instanceof RightForgeScrollEntity scrollEntity) {
                    CompoundTag scrollNBT = scrollEntity.getScrollNBT();
                    CompoundTag dataToSend = new CompoundTag();

                    // <-- Вставляем сразу наш новый тег TypeScroll
                    dataToSend.putString("TypeScroll", scrollType.name());

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

                        ListTag processedBonuses = new ListTag();
                        for (Tag bonusTag : bonusOutputs) {
                            if (bonusTag instanceof CompoundTag bonusCompound) {
                                CompoundTag processed = new CompoundTag();
                                // Item
                                if (bonusCompound.contains("Item", Tag.TAG_COMPOUND)) {
                                    processed.put("Item", bonusCompound.getCompound("Item"));
                                }
                                // Chance
                                float chance = bonusCompound.contains("Chance", Tag.TAG_FLOAT)
                                        ? bonusCompound.getFloat("Chance")
                                        : 1.0f;
                                processed.putFloat("Chance", chance);
                                // Min/Max
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

                    // Шлём в сущность crucible
                    crucible.receiveScrollData(dataToSend);
                    crucible.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }
    }
}
