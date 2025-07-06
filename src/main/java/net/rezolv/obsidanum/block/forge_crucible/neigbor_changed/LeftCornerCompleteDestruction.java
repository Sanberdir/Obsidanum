package net.rezolv.obsidanum.block.forge_crucible.neigbor_changed;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.LeftCornerLevel;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.Obsidanum;

public class LeftCornerCompleteDestruction {

    public static void handleNeighborUpdate(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        Direction facing = state.getValue(ForgeCrucible.FACING);
        BlockPos expectedLeftPos = getLeftPos(pos, facing);

        if (expectedLeftPos != null && expectedLeftPos.equals(fromPos)) {
            BlockState leftBlockState = level.getBlockState(expectedLeftPos);
            BlockEntity crucibleEntity = level.getBlockEntity(pos);

            if (leftBlockState.getBlock() instanceof LeftCornerLevel &&
                    leftBlockState.getValue(LeftCornerLevel.IS_PRESSED)) {

                if (crucibleEntity instanceof ForgeCrucibleEntity crucible) {
                    if (validateInputItem(crucible)) {
                        processDestructionRecipe(crucible, level.getRandom());
                    }
                }
            }
        }
    }

    private static BlockPos getLeftPos(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> pos.east();
            case SOUTH -> pos.west();
            case EAST -> pos.south();
            case WEST -> pos.north();
            default -> null;
        };
    }

    private static boolean validateInputItem(ForgeCrucibleEntity crucible) {
        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients", Tag.TAG_LIST)) return false;

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        if (ingredients.isEmpty()) return false;

        // Check slot 5 (input slot)
        ItemStack inputStack = crucible.itemHandler.getStackInSlot(5);
        if (inputStack.isEmpty()) return false;

        try {
            CompoundTag ingredient = ingredients.getCompound(0);
            JsonObject json = JsonParser.parseString(ingredient.getString("IngredientJson")).getAsJsonObject();

            // Check required count if specified
            int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;
            if (inputStack.getCount() < requiredCount) {
                return false;
            }

            return matchesRecipeIngredient(inputStack, json);
        } catch (Exception e) {
            Obsidanum.LOGGER.error("Error validating input item: {}", e.getMessage());
            return false;
        }
    }

    private static boolean matchesRecipeIngredient(ItemStack stack, JsonObject json) {
        if (stack.isEmpty()) return false;

        if (json.has("item")) {
            ResourceLocation itemId = new ResourceLocation(json.get("item").getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return item != null && stack.is(item);
        }

        if (json.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(json.get("tag").getAsString());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            return stack.is(tag);
        }

        return false;
    }

    private static void processDestructionRecipe(ForgeCrucibleEntity crucible, RandomSource random) {
        if (!validateInputItem(crucible)) return;

        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("MultipleOutputs", Tag.TAG_LIST)) return;

        ListTag multipleOutputs = data.getList("MultipleOutputs", Tag.TAG_COMPOUND);
        if (multipleOutputs.isEmpty()) return;

        // Debug output
        System.out.println("[DEBUG] Starting recipe processing");
        System.out.println("[DEBUG] Total outputs: " + multipleOutputs.size());

        // Get ingredient info
        CompoundTag ingredient = data.getList("Ingredients", Tag.TAG_COMPOUND).getCompound(0);
        JsonObject json = JsonParser.parseString(ingredient.getString("IngredientJson")).getAsJsonObject();
        int requiredCount = json.has("count") ? json.get("count").getAsInt() : 1;

        // Consume input item
        crucible.itemHandler.extractItem(5, requiredCount, false);

        // Process outputs with chances
        ListTag processedOutputs = new ListTag();
        ListTag outputChances = data.contains("OutputChances", Tag.TAG_LIST)
                ? data.getList("OutputChances", Tag.TAG_COMPOUND)
                : new ListTag();

        // Fallback: Try to get chances from ProcessedMultipleOutputs if OutputChances is empty
        if (outputChances.isEmpty() && data.contains("ProcessedMultipleOutputs", Tag.TAG_LIST)) {
            ListTag processed = data.getList("ProcessedMultipleOutputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < processed.size(); i++) {
                CompoundTag tag = processed.getCompound(i);
                if (tag.contains("Chance", Tag.TAG_FLOAT)) {
                    CompoundTag chanceTag = new CompoundTag();
                    chanceTag.putFloat("Chance", tag.getFloat("Chance"));
                    outputChances.add(chanceTag);
                }
            }
        }

        for (int i = 0; i < multipleOutputs.size(); i++) {
            CompoundTag outputTag = multipleOutputs.getCompound(i);
            float chance = 1.0f;

            // Priority 1: Chance from output tag itself
            if (outputTag.contains("Chance", Tag.TAG_FLOAT)) {
                chance = Math.max(0, Math.min(1, outputTag.getFloat("Chance")));
                System.out.println("[DEBUG] Output " + i + " using direct chance: " + chance);
            }
            // Priority 2: Chance from OutputChances list
            else if (i < outputChances.size()) {
                CompoundTag chanceTag = outputChances.getCompound(i);
                chance = Math.max(0, Math.min(1, chanceTag.getFloat("Chance")));
                System.out.println("[DEBUG] Output " + i + " using list chance: " + chance);
            }

            float roll = random.nextFloat();
            System.out.println("[DEBUG] Roll for output " + i + ": " + roll + " vs chance " + chance);

            if (roll <= chance) {
                // Remove chance to prevent stacking issues
                CompoundTag resultTag = outputTag.copy();
                resultTag.remove("Chance");
                processedOutputs.add(resultTag);
                System.out.println("[DEBUG] Output " + i + " SUCCESS");
            } else {
                System.out.println("[DEBUG] Output " + i + " FAILED");
            }
        }

        // Distribute outputs
        distributeMultipleOutputs(crucible, processedOutputs);

        crucible.setChanged();
    }

    private static void distributeMultipleOutputs(ForgeCrucibleEntity crucible, ListTag multipleOutputs) {
        // Don't clear slots, but add to existing items

        // Distribute outputs to slots (0-4)
        for (int i = 0; i < Math.min(multipleOutputs.size(), 5); i++) {
            CompoundTag outputTag = multipleOutputs.getCompound(i);
            ItemStack resultStack = ItemStack.of(outputTag);
            ItemStack currentStack = crucible.itemHandler.getStackInSlot(i);

            if (currentStack.isEmpty()) {
                // If slot is empty - just place the result
                crucible.itemHandler.setStackInSlot(i, resultStack);
            } else if (ItemStack.isSameItemSameTags(currentStack, resultStack)) {
                // If same item is already in slot - increase count
                int newCount = currentStack.getCount() + resultStack.getCount();
                int maxStackSize = currentStack.getMaxStackSize();

                if (newCount <= maxStackSize) {
                    // If fits in one stack - just increase
                    currentStack.setCount(newCount);
                } else {
                    // If doesn't fit - keep max, lose the rest
                    currentStack.setCount(maxStackSize);
                    Obsidanum.LOGGER.warn("Output slot {} overflow for item {}", i, currentStack.getItem());
                }
            } else {
                // If slot has different item - leave as is (don't replace)
                Obsidanum.LOGGER.warn("Output slot {} already contains different item", i);
            }
        }
    }
}