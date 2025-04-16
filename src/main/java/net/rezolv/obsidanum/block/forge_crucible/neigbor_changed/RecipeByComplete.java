package net.rezolv.obsidanum.block.forge_crucible.neigbor_changed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.custom.LeftCornerLevel;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

public class RecipeByComplete {
    public static void recipeByComplete(BlockState state, Level level, BlockPos pos, BlockPos fromPos) {
        Direction facing = state.getValue(ForgeCrucible.FACING);
        BlockPos expectedRightPos = switch (facing) {
            case NORTH -> pos.east();
            case SOUTH -> pos.west();
            case EAST -> pos.south();
            case WEST -> pos.north();
            default -> null;
        };

        if (expectedRightPos != null && expectedRightPos.equals(fromPos)) {
            BlockState leftBlockState = level.getBlockState(expectedRightPos);
            BlockEntity crucibleEntity = level.getBlockEntity(pos);

            if (leftBlockState.getBlock() instanceof LeftCornerLevel
                    && leftBlockState.hasProperty(LeftCornerLevel.IS_PRESSED)) {

                boolean isPressed = leftBlockState.getValue(LeftCornerLevel.IS_PRESSED);
                if (isPressed && crucibleEntity instanceof ForgeCrucibleEntity crucible) {
                    handleResult(level, pos, crucible);
                }
            }
        }
    }

    private static void handleResult(Level level, BlockPos pos, ForgeCrucibleEntity crucible) {
        CompoundTag data = crucible.getReceivedData();

        if (!data.contains("RecipeResult", Tag.TAG_LIST) || !data.contains("Ingredients", Tag.TAG_LIST)) {
            return;
        }

        if (!validateIngredients(crucible, data)) {
            return;
        }

        ListTag results = data.getList("RecipeResult", Tag.TAG_COMPOUND);
        for (int i = 0; i < results.size(); i++) {
            CompoundTag resultTag = results.getCompound(i);
            ItemStack resultStack = ItemStack.of(resultTag);

            ItemEntity itemEntity = new ItemEntity(
                    level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    resultStack.copy()
            );
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

            if (resultStack.getCount() > 1 && level.getRandom().nextFloat() < 0.3f) {
                int extraCount = 1 + level.getRandom().nextInt(3); // от 1 до 3
                ItemStack extraStack = resultStack.copy();
                extraStack.setCount(extraCount);
                ItemEntity extraEntity = new ItemEntity(
                        level,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        extraStack
                );
                extraEntity.setDefaultPickUpDelay();
                level.addFreshEntity(extraEntity);
            }
        }

        resetIngredients(crucible);
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }

    private static boolean validateIngredients(ForgeCrucibleEntity crucible, CompoundTag data) {
        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);

        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag ingTag = ingredients.getCompound(i);
            try {
                JsonObject json = JsonParser.parseString(ingTag.getString("IngredientJson")).getAsJsonObject();
                int required = json.get("count").getAsInt();
                Ingredient ingredient = Ingredient.fromJson(json);

                long matches = crucible.depositedItems.stream()
                        .filter(stack -> ingredient.test(stack))
                        .count();

                if (matches < required) {
                    return false;
                }

            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public static void resetIngredients(ForgeCrucibleEntity crucible) {
        crucible.depositedItems.clear();
        crucible.setChanged();
    }
}
