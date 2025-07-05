package net.rezolv.obsidanum.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.rezolv.obsidanum.Obsidanum;

import javax.annotation.Nullable;

public class ForgeScrollOrderDestructionRecipe implements Recipe<SimpleContainer> {
    private final Ingredient ingredient;
    private final JsonObject ingredientJson;
    private final NonNullList<ItemStack> multipleOutput;
    private final NonNullList<JsonObject> multipleOutputJsons;
    private final ResourceLocation id;

    public ForgeScrollOrderDestructionRecipe(Ingredient ingredient, JsonObject ingredientJson,
                                             NonNullList<ItemStack> multipleOutput, NonNullList<JsonObject> multipleOutputJsons,
                                             ResourceLocation id) {
        this.ingredient = ingredient;
        this.ingredientJson = ingredientJson;
        this.multipleOutput = multipleOutput;
        this.multipleOutputJsons = multipleOutputJsons;
        this.id = id;
    }

    public JsonObject getIngredientJson() {
        return ingredientJson;
    }

    public NonNullList<JsonObject> getMultipleOutputJsons() {
        return multipleOutputJsons;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return ingredient.test(container.getItem(0));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        // Default to first output
        return multipleOutput.get(0).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return multipleOutput.get(0).copy();
    }

    public NonNullList<ItemStack> getMultipleOutput() {
        return multipleOutput;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.FORGE_SCROLL_ORDER_DESTRUCTION;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.FORGE_SCROLL_ORDER_DESTRUCTION;
    }

    public static class Type implements RecipeType<ForgeScrollOrderDestructionRecipe> {
        public static final Type FORGE_SCROLL_ORDER_DESTRUCTION = new Type();
        public static final String ID = "scrolls/forge_scroll_order_destruction";
    }

    public static class Serializer implements RecipeSerializer<ForgeScrollOrderDestructionRecipe> {
        public static final Serializer FORGE_SCROLL_ORDER_DESTRUCTION = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation(Obsidanum.MOD_ID, "scrolls/forge_scroll_order_destruction");

        @Override
        public ForgeScrollOrderDestructionRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            // Read single ingredient
            JsonObject ingJson = GsonHelper.getAsJsonObject(json, "ingredients");
            Ingredient ingredient;
            if (ingJson.has("tag")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(GsonHelper.getAsString(ingJson, "tag")));
                ingredient = Ingredient.of(tag);
            } else {
                Item item = GsonHelper.getAsItem(ingJson, "item");
                ingredient = Ingredient.of(item);
            }

            // Read multiple output
            JsonArray multipleOutputJson = GsonHelper.getAsJsonArray(json, "multiple_output");
            NonNullList<ItemStack> multipleOutput = NonNullList.create();
            NonNullList<JsonObject> multipleOutputJsons = NonNullList.create();
            for (JsonElement elem : multipleOutputJson) {
                JsonObject outJson = elem.getAsJsonObject();
                multipleOutputJsons.add(outJson.deepCopy());
                multipleOutput.add(ShapedRecipe.itemStackFromJson(outJson));
            }

            return new ForgeScrollOrderDestructionRecipe(ingredient, ingJson.deepCopy(), multipleOutput, multipleOutputJsons, recipeId);
        }

        @Nullable
        @Override
        public ForgeScrollOrderDestructionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            // Read ingredient
            JsonObject ingJson = JsonParser.parseString(buffer.readUtf()).getAsJsonObject();
            Ingredient ingredient = Ingredient.fromNetwork(buffer);

            // Read output
            int count = buffer.readVarInt();
            NonNullList<ItemStack> multipleOutput = NonNullList.withSize(count, ItemStack.EMPTY);
            NonNullList<JsonObject> multipleOutputJsons = NonNullList.create();
            for (int i = 0; i < count; i++) {
                JsonObject outJson = JsonParser.parseString(buffer.readUtf()).getAsJsonObject();
                multipleOutputJsons.add(outJson);
                multipleOutput.set(i, buffer.readItem());
            }

            return new ForgeScrollOrderDestructionRecipe(ingredient, ingJson, multipleOutput, multipleOutputJsons, recipeId);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ForgeScrollOrderDestructionRecipe recipe) {
            // Write ingredient
            buffer.writeUtf(recipe.ingredientJson.toString());
            recipe.ingredient.toNetwork(buffer);

            // Write output
            int size = recipe.multipleOutput.size();
            buffer.writeVarInt(size);
            for (int i = 0; i < size; i++) {
                JsonObject outJson = recipe.multipleOutputJsons.get(i);
                buffer.writeUtf(outJson.toString());
                buffer.writeItemStack(recipe.multipleOutput.get(i), true);
            }
        }
    }
}