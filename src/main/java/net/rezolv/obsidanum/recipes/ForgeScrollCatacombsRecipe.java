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
import java.util.ArrayList;
import java.util.List;

public class ForgeScrollCatacombsRecipe implements Recipe<SimpleContainer> {
    private final NonNullList<JsonObject> ingredientJsons;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack output;
    private final ResourceLocation id;
    private final List<BonusOutput> bonusOutputs;

    public ForgeScrollCatacombsRecipe(NonNullList<Ingredient> ingredients, ItemStack output, ResourceLocation id,
                                      NonNullList<JsonObject> ingredientJsons, List<BonusOutput> bonusOutputs) {
        this.ingredients = ingredients;
        this.output = output != null ? output : ItemStack.EMPTY;
        this.id = id;
        this.ingredientJsons = ingredientJsons;
        this.bonusOutputs = bonusOutputs != null ? bonusOutputs : new ArrayList<>();
    }

    public NonNullList<JsonObject> getIngredientJsons() {
        return ingredientJsons;
    }

    public List<BonusOutput> getBonusOutputs() {
        return bonusOutputs;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient requiredIngredient = ingredients.get(i);
            ItemStack stackInSlot = container.getItem(i);

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

        // Store ingredients as JSON strings
        ListTag ingredientsTag = new ListTag();
        this.ingredientJsons.forEach(json -> ingredientsTag.add(StringTag.valueOf(json.toString())));
        tag.put("Ingredients", ingredientsTag);

        // Store bonus outputs if any
        if (!bonusOutputs.isEmpty()) {
            ListTag bonusesTag = new ListTag();
            for (BonusOutput bonus : bonusOutputs) {
                CompoundTag bonusTag = new CompoundTag();
                bonusTag.put("Item", bonus.itemStack().save(new CompoundTag()));
                bonusTag.putFloat("Chance", bonus.chance());
                bonusesTag.add(bonusTag);
            }
            tag.put("BonusOutputs", bonusesTag);
        }

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
        return Serializer.FORGE_SCROLL_CATACOMBS;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.FORGE_SCROLL_CATACOMBS;
    }

    public record BonusOutput(ItemStack itemStack, float chance) {
        public BonusOutput {
            if (itemStack == null) itemStack = ItemStack.EMPTY;
            if (chance < 0) chance = 0;
            if (chance > 1) chance = 1;
        }
    }

    public static class Type implements RecipeType<ForgeScrollCatacombsRecipe> {
        public static final Type FORGE_SCROLL_CATACOMBS = new Type();
        public static final String ID = "forge_scroll_catacombs";
    }

    public static class Serializer implements RecipeSerializer<ForgeScrollCatacombsRecipe> {
        public static final Serializer FORGE_SCROLL_CATACOMBS = new Serializer();
        public static final ResourceLocation ID = new ResourceLocation(Obsidanum.MOD_ID, "forge_scroll_catacombs");

        @Override
        public ForgeScrollCatacombsRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            // Read main output
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(serializedRecipe, "output"));

            // Read ingredients
            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(serializedRecipe, "ingredients");
            NonNullList<JsonObject> ingredientJsons = NonNullList.create();
            NonNullList<Ingredient> ingredients = NonNullList.create();

            for (JsonElement element : ingredientsJson) {
                JsonObject ingredientJson = element.getAsJsonObject();
                ingredientJsons.add(ingredientJson.deepCopy());

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

            // Read bonus outputs
            List<BonusOutput> bonusOutputs = new ArrayList<>();
            if (serializedRecipe.has("bonus_outputs")) {
                JsonArray bonusesJson = GsonHelper.getAsJsonArray(serializedRecipe, "bonus_outputs");
                for (JsonElement bonusElement : bonusesJson) {
                    JsonObject bonusObj = bonusElement.getAsJsonObject();
                    ItemStack bonusStack = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(bonusObj, "item"));
                    float chance = GsonHelper.getAsFloat(bonusObj, "chance", 0.5f);
                    bonusOutputs.add(new BonusOutput(bonusStack, chance));
                }
            }

            return new ForgeScrollCatacombsRecipe(ingredients, output, recipeId, ingredientJsons, bonusOutputs);
        }

        @Override
        public @Nullable ForgeScrollCatacombsRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            // Read ingredients
            int ingredientSize = buffer.readInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientSize, Ingredient.EMPTY);
            for (int i = 0; i < ingredientSize; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }

            // Read ingredient JSONs
            NonNullList<JsonObject> ingredientJsons = NonNullList.create();
            int jsonCount = buffer.readVarInt();
            for (int i = 0; i < jsonCount; i++) {
                String jsonStr = buffer.readUtf();
                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                ingredientJsons.add(json);
            }

            // Read main output
            ItemStack output = buffer.readItem();

            // Read bonus outputs
            List<BonusOutput> bonusOutputs = new ArrayList<>();
            int bonusCount = buffer.readVarInt();
            for (int i = 0; i < bonusCount; i++) {
                ItemStack bonusStack = buffer.readItem();
                float chance = buffer.readFloat();
                bonusOutputs.add(new BonusOutput(bonusStack, chance));
            }

            return new ForgeScrollCatacombsRecipe(ingredients, output, recipeId, ingredientJsons, bonusOutputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ForgeScrollCatacombsRecipe recipe) {
            // Write ingredients
            buffer.writeInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }

            // Write ingredient JSONs
            buffer.writeVarInt(recipe.ingredientJsons.size());
            for (JsonObject json : recipe.ingredientJsons) {
                buffer.writeUtf(json.toString());
            }

            // Write main output
            buffer.writeItemStack(recipe.output, true);

            // Write bonus outputs
            buffer.writeVarInt(recipe.bonusOutputs.size());
            for (BonusOutput bonus : recipe.bonusOutputs) {
                buffer.writeItem(bonus.itemStack());
                buffer.writeFloat(bonus.chance());
            }
        }
    }
}