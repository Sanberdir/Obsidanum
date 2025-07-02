package net.rezolv.obsidanum.integration.recipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiScreen;
import net.rezolv.obsidanum.integration.recipe.OrderJeiRecipe;
import net.rezolv.obsidanum.recipes.ForgeScrollOrderRecipe;

import java.util.List;

@JeiPlugin
public class JEIRecipesOrder implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Obsidanum.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new OrderJeiRecipe(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<ForgeScrollOrderRecipe> recipes = recipeManager.getAllRecipesFor(ForgeScrollOrderRecipe.Type.FORGE_SCROLL_ORDER);
        registration.addRecipes(OrderJeiRecipe.ORDER_JEI_RECIPE_TYPE, recipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(ForgeCrucibleGuiScreen.class, 60, 30, 20, 30,
                OrderJeiRecipe.ORDER_JEI_RECIPE_TYPE);
    }
}