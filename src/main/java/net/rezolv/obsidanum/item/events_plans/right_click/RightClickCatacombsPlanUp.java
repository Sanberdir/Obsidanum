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
import net.rezolv.obsidanum.recipes.ForgeScrollCatacombsUpgradeRecipe;
import net.rezolv.obsidanum.sound.SoundsObs;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class RightClickCatacombsPlanUp {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();

        if (heldItem.getItem() != ItemsObs.UN_CATACOMBS_SCROLL_UP.get() || level.isClientSide()) {
            return;
        }

        // Проигрываем звук
        level.playSound(null,
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                SoundsObs.LEARN.get(),
                SoundSource.PLAYERS,
                1.0F,
                0.95F + RANDOM.nextFloat() * 0.1F);

        // Получаем рецепты
        List<ForgeScrollCatacombsUpgradeRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ForgeScrollCatacombsUpgradeRecipe.Type.FORGE_SCROLL_CATACOMBS_UPGRADE);

        if (recipes.isEmpty()) {
            Obsidanum.LOGGER.warn("No catacombs upgrade recipes found!");
            return;
        }

        ForgeScrollCatacombsUpgradeRecipe randomRecipe = recipes.get(RANDOM.nextInt(recipes.size()));
        ItemStack planItem = new ItemStack(ItemsObs.CATACOMBS_UPGRADE_PLAN.get());
        CompoundTag tag = new CompoundTag();

        // Сохраняем основную информацию
        tag.putString("RecipeID", randomRecipe.getId().toString());
        tag.putString("Upgrade", randomRecipe.upgrade);

        // Сохраняем ингредиенты как в первом классе
        ListTag ingredientsList = new ListTag();
        for (JsonObject ingredientJson : randomRecipe.getIngredientJsons()) {
            CompoundTag ingredientTag = new CompoundTag();

            // Добавляем количество, если указано
            if (ingredientJson.has("count")) {
                ingredientTag.putInt("Count", ingredientJson.get("count").getAsInt());
            } else {
                ingredientTag.putInt("Count", 1); // Значение по умолчанию
            }

            ingredientTag.putString("IngredientJson", ingredientJson.toString());
            ingredientsList.add(ingredientTag);
        }
        tag.put("Ingredients", ingredientsList);


        planItem.setTag(tag);

        // Обновляем инвентарь
        heldItem.shrink(1);
        if (heldItem.isEmpty()) {
            event.getEntity().setItemInHand(event.getHand(), planItem);
        } else {
            event.getEntity().getInventory().add(planItem);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}