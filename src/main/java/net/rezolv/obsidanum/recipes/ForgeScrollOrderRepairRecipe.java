package net.rezolv.obsidanum.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.rezolv.obsidanum.Obsidanum;

import javax.annotation.Nullable;
import java.util.Objects;

public class ForgeScrollOrderRepairRecipe implements Recipe<SimpleContainer> {
    public final NonNullList<Ingredient> ingredients;
    private final NonNullList<JsonObject> ingredientJsons;
    private final ResourceLocation id;
    private final int requiredScrolls;

    public ForgeScrollOrderRepairRecipe(NonNullList<Ingredient> ingredients,
                                        NonNullList<JsonObject> ingredientJsons,
                                        ResourceLocation id,
                                        int requiredScrolls) {
        this.ingredients = Objects.requireNonNull(ingredients);
        this.ingredientJsons = Objects.requireNonNull(ingredientJsons);
        this.id = Objects.requireNonNull(id);
        this.requiredScrolls = Math.max(1, requiredScrolls);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        // Проверяем основные ингредиенты
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack stackInSlot = container.getItem(i);
            if (!ingredients.get(i).test(stackInSlot)) {
                return false;
            }
        }

        // Проверяем количество свитков
        ItemStack scrollStack = container.getItem(ingredients.size());
        return !scrollStack.isEmpty() && scrollStack.getCount() >= requiredScrolls;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        ItemStack scroll = container.getItem(ingredients.size()).copy();
        scroll.setCount(1); // Возвращаем только один улучшенный свиток

        CompoundTag tag = scroll.getOrCreateTag();

        // Сохраняем информацию о рецепте в NBT
        CompoundTag recipeInfo = new CompoundTag();
        ListTag ingredientsTag = new ListTag();
        ingredientJsons.forEach(json -> ingredientsTag.add(StringTag.valueOf(json.toString())));
        recipeInfo.put("Ingredients", ingredientsTag);
        recipeInfo.putInt("RequiredScrolls", requiredScrolls);

        tag.put("RecipeInfo", recipeInfo);
        return scroll;
    }

    public NonNullList<JsonObject> getIngredientJsons() {
        return ingredientJsons;
    }


    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY; // Нет статического результата
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.FORGE_SCROLL_ORDER_REPAIR;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.FORGE_SCROLL_ORDER_REPAIR;
    }

    public static class Type implements RecipeType<ForgeScrollOrderRepairRecipe> {
        public static final Type FORGE_SCROLL_ORDER_REPAIR = new Type();
        public static final String ID = "scrolls/forge_scroll_order_repair";
    }

    public static class Serializer implements RecipeSerializer<ForgeScrollOrderRepairRecipe> {
        public static final Serializer FORGE_SCROLL_ORDER_REPAIR = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation(Obsidanum.MOD_ID, "scrolls/forge_scroll_order_repair");

        @Override
        public ForgeScrollOrderRepairRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ingredients = NonNullList.create();
            NonNullList<JsonObject> ingredientJsons = NonNullList.create();

            // Сохраняем и оригинальные JSON и создаем Ingredient
            for (JsonElement element : ingredientsJson) {
                JsonObject ingredientJson = element.getAsJsonObject().deepCopy();
                ingredientJsons.add(ingredientJson);
                ingredients.add(Ingredient.fromJson(ingredientJson));
            }

            int requiredScrolls = GsonHelper.getAsInt(json, "required_scrolls", 1);

            return new ForgeScrollOrderRepairRecipe(ingredients, ingredientJsons, recipeId, requiredScrolls);
        }

        @Nullable
        @Override
        public ForgeScrollOrderRepairRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int ingredientCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
            NonNullList<JsonObject> ingredientJsons = NonNullList.create();

            for (int i = 0; i < ingredientCount; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));

                // Читаем JSON ингредиента
                String jsonStr = buffer.readUtf();
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                ingredientJsons.add(json);
            }

            int requiredScrolls = buffer.readVarInt();

            return new ForgeScrollOrderRepairRecipe(ingredients, ingredientJsons, recipeId, requiredScrolls);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ForgeScrollOrderRepairRecipe recipe) {
            buffer.writeVarInt(recipe.ingredients.size());

            // Записываем и ингредиенты и их JSON-представление
            for (int i = 0; i < recipe.ingredients.size(); i++) {
                recipe.ingredients.get(i).toNetwork(buffer);
                buffer.writeUtf(recipe.ingredientJsons.get(i).toString());
            }

            buffer.writeVarInt(recipe.requiredScrolls);
        }
    }
}