package net.rezolv.obsidanum.block.forge_crucible.neigbor_changed;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.LeftCornerLevel;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class LeftCornerCompleteDestruction {
    private static final Map<String, List<RecipeOutput>> RECIPES = new HashMap<>();
    private static boolean recipesLoaded = false;

    public static class RecipeOutput {
        public String item;
        public float chance;
        public int min_count;
        public int max_count;
    }

    public static class Recipe {
        public String input_tag;
        public List<RecipeOutput> outputs;
    }

    public static void loadRecipes(ResourceManager resourceManager) {
        if (recipesLoaded) return;

        try {
            InputStream is = resourceManager.getResource(new ResourceLocation("obsidanum", "recipes/scrolls/destruction_recipes.json")).get().open();
            InputStreamReader reader = new InputStreamReader(is);

            List<Recipe> recipeList = new Gson().fromJson(reader, new TypeToken<List<Recipe>>(){}.getType());

            for (Recipe recipe : recipeList) {
                RECIPES.put(recipe.input_tag, recipe.outputs);
            }

            recipesLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleNeighborUpdate(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        if (!recipesLoaded) {
            loadRecipes(level.getServer().getResourceManager());
        }

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

        ItemStack inputStack = crucible.itemHandler.getStackInSlot(5);
        return !inputStack.isEmpty();
    }

    private static void processDestructionRecipe(ForgeCrucibleEntity crucible, RandomSource random) {
        ItemStack inputStack = crucible.itemHandler.getStackInSlot(5);
        if (inputStack.isEmpty()) return;

        crucible.itemHandler.extractItem(5, 1, false);

        ListTag processedOutputs = new ListTag();
        boolean hasMatchedRecipe = false;

        // Проверяем все теги предмета (кроме "default")
        for (Map.Entry<String, List<RecipeOutput>> entry : RECIPES.entrySet()) {
            String tagName = entry.getKey();
            if (!tagName.equals("default") && inputStack.is(TagKey.create(Registries.ITEM, new ResourceLocation(tagName)))) {
                hasMatchedRecipe = true;
                for (RecipeOutput output : entry.getValue()) {
                    if (random.nextFloat() <= output.chance) {
                        CompoundTag outputTag = new CompoundTag();
                        outputTag.putString("id", output.item);
                        outputTag.putInt("Count", random.nextInt(output.max_count - output.min_count + 1) + output.min_count);
                        processedOutputs.add(outputTag);
                    }
                }
            }
        }

        // Если не нашли подходящего рецепта, применяем "default"
        if (!hasMatchedRecipe && RECIPES.containsKey("default")) {
            for (RecipeOutput output : RECIPES.get("default")) {
                if (random.nextFloat() <= output.chance) {
                    CompoundTag outputTag = new CompoundTag();
                    outputTag.putString("id", output.item);
                    outputTag.putInt("Count", random.nextInt(output.max_count - output.min_count + 1) + output.min_count);
                    processedOutputs.add(outputTag);
                }
            }
        }

        distributeMultipleOutputs(crucible, processedOutputs);
        crucible.setChanged();
    }

    private static void distributeMultipleOutputs(ForgeCrucibleEntity crucible, ListTag multipleOutputs) {
        for (int i = 0; i < Math.min(multipleOutputs.size(), 5); i++) {
            CompoundTag outputTag = multipleOutputs.getCompound(i);
            ItemStack resultStack = ItemStack.of(outputTag);
            ItemStack currentStack = crucible.itemHandler.getStackInSlot(i);

            if (currentStack.isEmpty()) {
                crucible.itemHandler.setStackInSlot(i, resultStack);
            } else if (ItemStack.isSameItemSameTags(currentStack, resultStack)) {
                int newCount = currentStack.getCount() + resultStack.getCount();
                int maxStackSize = currentStack.getMaxStackSize();

                if (newCount <= maxStackSize) {
                    currentStack.setCount(newCount);
                } else {
                    currentStack.setCount(maxStackSize);
                }
            }
        }
    }
}