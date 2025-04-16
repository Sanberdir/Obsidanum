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
import net.rezolv.obsidanum.item.upgrade.ObsidanumToolUpgrades;

import javax.annotation.Nullable;

public class ForgeScrollUpgradeRecipe implements Recipe<SimpleContainer> {
    private final NonNullList<JsonObject> ingredientJsons;
    private final NonNullList<Ingredient> ingredients;
    private final Ingredient tool; // Изменено с ItemStack на Ingredient для поддержки тегов
    private final NonNullList<String> toolTypes;
    private final NonNullList<String> toolKinds;
    private ItemStack output;
    private final ResourceLocation id;
    private final String upgrade;

    public NonNullList<JsonObject> getIngredientJsons() {
        return ingredientJsons;
    }

    public ForgeScrollUpgradeRecipe(NonNullList<Ingredient> ingredients, Ingredient tool,
                                    NonNullList<String> toolTypes, NonNullList<String> toolKinds,
                                    ItemStack output, ResourceLocation id, String upgrade,
                                    NonNullList<JsonObject> ingredientJsons) {
        this.ingredients = ingredients;
        this.ingredientJsons = ingredientJsons;
        this.tool = tool;
        this.toolTypes = toolTypes;
        this.toolKinds = toolKinds;
        this.output = output != null ? output : ItemStack.EMPTY; // Защита от null
        this.id = id;
        this.upgrade = upgrade;
    }

    public String getUpgrade() {
        return this.upgrade;
    }
    public void setOutput(ItemStack output) {
        this.output = output;
    }
    public NonNullList<String> getToolTypes() {
        return toolTypes;
    }

    public NonNullList<String> getToolKinds() {
        return toolKinds;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        // Проверяем, что инструмент совпадает (теперь через Ingredient)
        if (!tool.test(container.getItem(0))) {
            return false;
        }

        // Проверяем, что все ингредиенты совпадают
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient requiredIngredient = ingredients.get(i);
            ItemStack stackInSlot = container.getItem(i + 1); // Слот 0 — инструмент, слоты 1+ — ингредиенты

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

    public Ingredient getTool() { // Изменено с ItemStack на Ingredient
        return tool;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        ItemStack result = output.copy();
        CompoundTag tag = new CompoundTag();

        // Запись существующих данных
        tag.putString("Upgrade", this.upgrade);

        ListTag toolTypesTag = new ListTag();
        this.toolTypes.forEach(t -> toolTypesTag.add(StringTag.valueOf(t)));
        tag.put("ToolTypes", toolTypesTag);

        ListTag toolKindsTag = new ListTag();
        this.toolKinds.forEach(k -> toolKindsTag.add(StringTag.valueOf(k)));
        tag.put("ToolKinds", toolKindsTag);

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
        return Serializer.FORGE_SCROLL_UPGRADE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.FORGE_SCROLL_UPGRADE;
    }

    public static class Type implements RecipeType<ForgeScrollUpgradeRecipe> {
        public static final Type FORGE_SCROLL_UPGRADE = new Type();
        public static final String ID = "forge_scroll_upgrade";
    }

    public static class Serializer implements RecipeSerializer<ForgeScrollUpgradeRecipe> {
        public static final Serializer FORGE_SCROLL_UPGRADE = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation(Obsidanum.MOD_ID, "forge_scroll_upgrade");

        @Override
        public ForgeScrollUpgradeRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            // Чтение tool (теперь как Ingredient)
            Ingredient tool;
            if (serializedRecipe.has("tool")) {
                JsonObject toolJson = GsonHelper.getAsJsonObject(serializedRecipe, "tool");
                if (toolJson.has("tag")) {
                    ResourceLocation tagId = new ResourceLocation(GsonHelper.getAsString(toolJson, "tag"));
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                    tool = Ingredient.of(tag);
                } else {
                    Item item = GsonHelper.getAsItem(toolJson, "item");
                    tool = Ingredient.of(item);
                }
            } else {
                tool = Ingredient.EMPTY;
            }

            // Убираем чтение output из JSON
            ItemStack output = ItemStack.EMPTY; // или null, если вы хотите, чтобы output был задан позже

            String upgrade = GsonHelper.getAsString(serializedRecipe, "upgrade");

            // Чтение tool_types
            NonNullList<String> toolTypes = NonNullList.create();
            if (serializedRecipe.has("tool_types")) {
                JsonArray toolTypesJson = GsonHelper.getAsJsonArray(serializedRecipe, "tool_types");
                for (JsonElement element : toolTypesJson) {
                    toolTypes.add(element.getAsString());
                }
            }

            // Чтение tool_kinds
            NonNullList<String> toolKinds = NonNullList.create();
            if (serializedRecipe.has("tool_kinds")) {
                JsonArray toolKindsJson = GsonHelper.getAsJsonArray(serializedRecipe, "tool_kinds");
                for (JsonElement element : toolKindsJson) {
                    toolKinds.add(element.getAsString());
                }
            }

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

            return new ForgeScrollUpgradeRecipe(ingredients, tool, toolTypes, toolKinds, output, recipeId, upgrade, ingredientJsons);
        }

        @Override
        public @Nullable ForgeScrollUpgradeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            // Чтение tool (теперь как Ingredient)
            Ingredient tool = Ingredient.fromNetwork(buffer);

            // Чтение tool_types
            int toolTypesSize = buffer.readVarInt();
            NonNullList<String> toolTypes = NonNullList.withSize(toolTypesSize, "");
            for (int i = 0; i < toolTypesSize; i++) {
                toolTypes.set(i, buffer.readUtf());
            }

            // Чтение tool_kinds
            int toolKindsSize = buffer.readVarInt();
            NonNullList<String> toolKinds = NonNullList.withSize(toolKindsSize, "");
            for (int i = 0; i < toolKindsSize; i++) {
                toolKinds.set(i, buffer.readUtf());
            }

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
            String upgrade = buffer.readUtf();

            return new ForgeScrollUpgradeRecipe(ingredients, tool, toolTypes, toolKinds, output, recipeId, upgrade, ingredientJsons);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ForgeScrollUpgradeRecipe recipe) {
            // Запись tool (теперь как Ingredient)
            recipe.tool.toNetwork(buffer);

            // Запись tool_types
            buffer.writeVarInt(recipe.toolTypes.size());
            for (String type : recipe.toolTypes) {
                buffer.writeUtf(type);
            }

            // Запись tool_kinds
            buffer.writeVarInt(recipe.toolKinds.size());
            for (String kind : recipe.toolKinds) {
                buffer.writeUtf(kind);
            }

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
            buffer.writeUtf(recipe.upgrade);
        }
    }
}