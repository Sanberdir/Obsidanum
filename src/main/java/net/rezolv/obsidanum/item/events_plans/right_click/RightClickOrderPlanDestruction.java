package net.rezolv.obsidanum.item.events_plans.right_click;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.recipes.ForgeScrollOrderDestructionRecipe;
import net.rezolv.obsidanum.sound.SoundsObs;

import java.util.List;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class RightClickOrderPlanDestruction {
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();

        // Check: correct item and server side
        if (heldItem.getItem() != ItemsObs.UN_ORDER_DESTRUCTION_SCROLL.get() || level.isClientSide()) {
            return;
        }

        // Get all destruction recipes
        List<ForgeScrollOrderDestructionRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ForgeScrollOrderDestructionRecipe.Type.FORGE_SCROLL_ORDER_DESTRUCTION);

        if (recipes.isEmpty()) {
            return;
        }

        // Choose random recipe
        RandomSource random = level.getRandom();
        ForgeScrollOrderDestructionRecipe recipe = recipes.get(random.nextInt(recipes.size()));

        // Create new plan item
        ItemStack plan = new ItemStack(ItemsObs.ORDER_DESTRUCTION_PLAN.get());
        CompoundTag tag = new CompoundTag();

        // Save recipe ID
        tag.putString("RecipeID", recipe.getId().toString());

        // Save ingredients
        ListTag ingredientsList = new ListTag();
        CompoundTag ingredientTag = new CompoundTag();
        ingredientTag.putString("IngredientJson", recipe.getIngredientJson().toString());
        ingredientsList.add(ingredientTag);
        tag.put("Ingredients", ingredientsList);

        // Save all possible outputs (multiple outputs)
        ListTag multipleOutputsList = new ListTag();
        for (ItemStack output : recipe.getMultipleOutput()) {
            CompoundTag outputTag = new CompoundTag();
            output.save(outputTag);
            multipleOutputsList.add(outputTag);
        }
        tag.put("MultipleOutputs", multipleOutputsList);

        // Save output chances
        ListTag chancesList = new ListTag();
        for (float chance : recipe.getOutputChances()) {
            CompoundTag chanceTag = new CompoundTag();
            chanceTag.putFloat("Chance", chance);
            chancesList.add(chanceTag);
        }
        tag.put("OutputChances", chancesList);

        // Save JSON descriptions of outputs for display
        ListTag outputsJsonList = new ListTag();
        for (JsonObject outJson : recipe.getMultipleOutputJsons()) {
            CompoundTag outTag = new CompoundTag();
            outTag.putString("OutputJson", outJson.toString());
            outputsJsonList.add(outTag);
        }
        tag.put("OutputJsons", outputsJsonList);

        // Apply tag to plan item
        plan.setTag(tag);

        // Play learning sound
        level.playSound(null,
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                SoundsObs.LEARN.get(),
                SoundSource.PLAYERS,
                1.0F,
                0.8F + random.nextFloat() * 0.4F);

        // Decrease original item
        heldItem.shrink(1);

        // Give plan item to player
        if (heldItem.isEmpty()) {
            event.getEntity().setItemInHand(event.getHand(), plan);
        } else {
            event.getEntity().getInventory().add(plan);
        }

        // Mark event as handled
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}