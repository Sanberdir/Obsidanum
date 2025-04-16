package net.rezolv.obsidanum.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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

public class ForgeScrollNetherRecipe implements Recipe<SimpleContainer> {
    private final NonNullList<JsonObject> ingredientJsons;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack output;
    private final ResourceLocation id;

    public ForgeScrollNetherRecipe(NonNullList<Ingredient> ingredients, ItemStack output, ResourceLocation id, NonNullList<JsonObject> ingredientJsons) {
        this.ingredients = ingredients;
        this.output = output != null ? output : ItemStack.EMPTY; // Защита от null
        this.id = id;
        this.ingredientJsons = ingredientJsons;
    }

    public NonNullList<JsonObject> getIngredientJsons() {
        return ingredientJsons;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        // Проверяем, что все ингредиенты совпадают
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient requiredIngredient = ingredients.get(i);
            ItemStack stackInSlot = container.getItem(i); // Слоты 0+ — ингредиенты

            if (!requiredIngredient.test(stackInSlot)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        ItemStack result = output.copy();
        CompoundTag tag = new CompoundTag();

        // Запись ингредиентов как JSON строк
        ListTag ingredientsTag = new ListTag();
        this.ingredientJsons.forEach(json -> ingredientsTag.add(StringTag.valueOf(json.toString())));
        tag.put("Ingredients", ingredientsTag);

        result.setTag(tag);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ForgeScrollNetherRecipe.Serializer.FORGE_SCROLL_NETHER;
    }

    @Override
    public RecipeType<?> getType() {
        return ForgeScrollNetherRecipe.Type.FORGE_SCROLL_NETHER;
    }

    public static class Type implements RecipeType<ForgeScrollNetherRecipe> {
        public static final ForgeScrollNetherRecipe.Type FORGE_SCROLL_NETHER = new ForgeScrollNetherRecipe.Type();
        public static final String ID = "forge_scroll_nether";
    }

    public static class Serializer implements RecipeSerializer<ForgeScrollNetherRecipe> {
        public static final ForgeScrollNetherRecipe.Serializer FORGE_SCROLL_NETHER = new ForgeScrollNetherRecipe.Serializer();
        public static final ResourceLocation ID = new ResourceLocation(Obsidanum.MOD_ID, "forge_scroll_nether");

        @Override
        public ForgeScrollNetherRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            // Чтение output
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(serializedRecipe, "output"));

            // Чтение ингредиентов
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(serializedRecipe, "ingredients");
            NonNullList<JsonObject> ingredientJsons = NonNullList.create();
            NonNullList<Ingredient> ingredients = NonNullList.create();

            for (JsonElement element : ingredientsJson) {
                JsonObject ingredientJson = element.getAsJsonObject();
                ingredientJsons.add(ingredientJson.deepCopy()); // Сохраняем копию JSON

                Ingredient ingredient;
                if (ingredientJson.has("tag")) {
                    ResourceLocation tagId = new ResourceLocation(GsonHelper.getAsString(ingredientJson, "tag"));
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                    ingredient = Ingredient.of(tag);
                } else {
                    Item item = GsonHelper.getAsItem(ingredientJson, "item");
                    ingredient = Ingredient.of(item);
                }
                ingredients.add(ingredient);
            }

            return new ForgeScrollNetherRecipe(ingredients, output, recipeId, ingredientJsons);
        }

        @Override
        public @Nullable ForgeScrollNetherRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            // Чтение ингредиентов
            int ingredientSize = buffer.readInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientSize, Ingredient.EMPTY);
            for (int i = 0; i < ingredientSize; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }

            // Чтение JSON ингредиентов
            NonNullList<JsonObject> ingredientJsons = NonNullList.create();
            int jsonCount = buffer.readVarInt();
            for (int i = 0; i < jsonCount; i++) {
                String jsonStr = buffer.readUtf();
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                ingredientJsons.add(json);
            }

            ItemStack output = buffer.readItem();

            return new ForgeScrollNetherRecipe(ingredients, output, recipeId, ingredientJsons);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ForgeScrollNetherRecipe recipe) {
            // Запись ингредиентов
            buffer.writeInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }

            // Запись JSON ингредиентов
            buffer.writeVarInt(recipe.ingredientJsons.size());
            for (JsonObject json : recipe.ingredientJsons) {
                buffer.writeUtf(json.toString());
            }

            buffer.writeItemStack(recipe.output, true);
        }
    }
}