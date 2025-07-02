package net.rezolv.obsidanum.integration.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.recipes.ForgeScrollNetherRecipe;

import java.util.List;

public class NetherJeiRecipe implements IRecipeCategory<ForgeScrollNetherRecipe> {
    public static final ResourceLocation UID = new ResourceLocation(Obsidanum.MOD_ID, "scrolls/forge_scroll_nether");
    public static final ResourceLocation TEXTURE = new ResourceLocation(Obsidanum.MOD_ID,
            "textures/gui/forge_crucible_menu.png");

    public static final RecipeType<ForgeScrollNetherRecipe> NETHER_JEI_RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(Obsidanum.MOD_ID, "forge_scroll_nether"), ForgeScrollNetherRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public NetherJeiRecipe(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 39, 0, 176, 130);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ItemsObs.NETHER_PLAN.get()));
    }

    @Override
    public RecipeType<ForgeScrollNetherRecipe> getRecipeType() {
        return NETHER_JEI_RECIPE_TYPE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          ForgeScrollNetherRecipe recipe,
                          IFocusGroup focuses) {
        // Основной результат
        builder.addSlot(RecipeIngredientRole.OUTPUT, 79, 26)
                .addItemStack(recipe.getResultItem(null).copy()) // count должен уже быть в ItemStack
                .setSlotName("main_output");

        // Ингредиенты
        int[] inputX = {35, 53, 71, 89, 107, 125};
        int inputY = 73;
        List<Ingredient> ingredients = recipe.getIngredients();
        List<JsonObject> ingredientJsons = recipe.getIngredientJsons();

        for (int i = 0; i < Math.min(inputX.length, ingredients.size()); i++) {
            Ingredient ingredient = ingredients.get(i);
            JsonObject json = i < ingredientJsons.size() ? ingredientJsons.get(i) : new JsonObject();
            int count = 1;

            if (json.has("count")) {
                JsonElement countEl = json.get("count");
                if (countEl.isJsonPrimitive() && countEl.getAsJsonPrimitive().isNumber()) {
                    count = countEl.getAsInt();
                }
            }

            // Ingredient может содержать несколько ItemStack’ов (альтернатива). Мы клонируем и задаём count.
            builder.addSlot(RecipeIngredientRole.INPUT, inputX[i], inputY)
                    .addItemStacks(getItemsWithCount(ingredient, count));
        }
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.obsidanum.nether_jei_recipe");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    // Расширение для удобства — добавить count ко всем ItemStack из Ingredient
    private static List<ItemStack> getItemsWithCount(Ingredient ingredient, int count) {
        ItemStack[] matchingStacks = ingredient.getItems();
        return java.util.Arrays.stream(matchingStacks)
                .map(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(count);
                    return copy;
                })
                .toList();
    }
    @Override
    public void draw(ForgeScrollNetherRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        ItemStack diamondStack = new ItemStack(ItemsObs.NETHER_PLAN.get());
        graphics.renderItem(diamondStack, 80, 105);
    }
}
