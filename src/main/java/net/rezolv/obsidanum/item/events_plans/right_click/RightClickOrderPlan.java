package net.rezolv.obsidanum.item.events_plans.right_click;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.recipes.ForgeScrollOrderRecipe;
import net.rezolv.obsidanum.sound.SoundsObs;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class RightClickOrderPlan {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();

        // Check if this is UN_ORDER_SCROLL and we're on server
        if (heldItem.getItem() != ItemsObs.UN_ORDER_SCROLL.get() || level.isClientSide()) {
            return;
        }

        // Get all order recipes
        List<ForgeScrollOrderRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ForgeScrollOrderRecipe.Type.FORGE_SCROLL_ORDER);

        if (recipes.isEmpty()) {
            return; // No available recipes
        }

        // Choose random recipe
        ForgeScrollOrderRecipe randomRecipe = recipes.get(RANDOM.nextInt(recipes.size()));

        // Create new ORDER_PLAN item
        ItemStack planItem = new ItemStack(ItemsObs.ORDER_PLAN.get());

        // Create NBT tag for the item
        CompoundTag tag = new CompoundTag();

        // Write recipe ID
        tag.putString("RecipeID", randomRecipe.getId().toString());

        // Write recipe result
        ListTag resultList = new ListTag();
        CompoundTag resultTag = new CompoundTag();
        ItemStack result = randomRecipe.getResultItem(level.registryAccess());
        result.save(resultTag);
        resultList.add(resultTag);
        tag.put("RecipeResult", resultList);

        // Write ingredients
        ListTag ingredientsList = new ListTag();
        for (JsonObject ingredientJson : randomRecipe.getIngredientJsons()) {
            CompoundTag ingredientTag = new CompoundTag();
            ingredientTag.putString("IngredientJson", ingredientJson.toString());
            ingredientsList.add(ingredientTag);
        }
        tag.put("Ingredients", ingredientsList);

        // Write bonus outputs if present
        if (!randomRecipe.getBonusOutputs().isEmpty()) {
            ListTag bonusOutputsList = new ListTag();
            for (ForgeScrollOrderRecipe.BonusOutput bonus : randomRecipe.getBonusOutputs()) {
                CompoundTag bonusTag = new CompoundTag();
                CompoundTag itemTag = new CompoundTag();
                bonus.itemStack().save(itemTag);
                bonusTag.put("Item", itemTag);
                bonusTag.putFloat("Chance", bonus.chance());
                bonusOutputsList.add(bonusTag);
            }
            tag.put("BonusOutputs", bonusOutputsList);
        }

        // Apply tag to the new item
        planItem.setTag(tag);

        // Play bell sound
        level.playSound(null,
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                SoundsObs.LEARN.get(),
                SoundSource.PLAYERS,
                1.0F,
                0.8F + RANDOM.nextFloat() * 0.4F);

        // Decrease UN_ORDER_SCROLL stack by 1
        heldItem.shrink(1);

        // Give player the new item
        if (heldItem.isEmpty()) {
            event.getEntity().setItemInHand(event.getHand(), planItem);
        } else {
            event.getEntity().getInventory().add(planItem);
        }

        // Return success result
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}