package net.rezolv.obsidanum.item.events_plans.right_click;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.recipes.ForgeScrollNetherRecipe;
import net.rezolv.obsidanum.sound.SoundsObs;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class RightClickNetherPlan {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();

        // Проверяем, что это UN_NETHER_SCROLL и действие на сервере
        if (heldItem.getItem() != ItemsObs.UN_NETHER_SCROLL.get() || level.isClientSide()) {
            return;
        }

        // Получаем список всех рецептов катакомб
        List<ForgeScrollNetherRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ForgeScrollNetherRecipe.Type.FORGE_SCROLL_NETHER);

        if (recipes.isEmpty()) {
            return; // Нет доступных рецептов
        }
        // Проигрываем звук колокола
        level.playSound(null,
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                SoundsObs.LEARN.get(), // Звук колокола
                SoundSource.PLAYERS,    // Категория звука (для игроков)
                1.0F,                  // Громкость (1.0 = 100%)
                0.8F + RANDOM.nextFloat() * 0.4F); // Высота тона (случайное значение для естественности)

        // Возвращаем успешный результат взаимодействия
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        // Выбираем случайный рецепт
        ForgeScrollNetherRecipe randomRecipe = recipes.get(RANDOM.nextInt(recipes.size()));

        // Создаем новый предмет NETHER_PLAN
        ItemStack planItem = new ItemStack(ItemsObs.NETHER_PLAN.get());

        // Создаем NBT тег для предмета
        CompoundTag tag = new CompoundTag();

        // Записываем ID рецепта
        tag.putString("RecipeID", randomRecipe.getId().toString());

        // Записываем результат рецепта
        ListTag resultList = new ListTag();
        CompoundTag resultTag = new CompoundTag();
        ItemStack result = randomRecipe.getResultItem(level.registryAccess());
        result.save(resultTag);
        resultList.add(resultTag);
        tag.put("RecipeResult", resultList);

        // Записываем ингредиенты
        ListTag ingredientsList = new ListTag();
        for (JsonObject ingredientJson : randomRecipe.getIngredientJsons()) {
            CompoundTag ingredientTag = new CompoundTag();
            ingredientTag.putString("IngredientJson", ingredientJson.toString());
            ingredientsList.add(ingredientTag);
        }
        tag.put("Ingredients", ingredientsList);

        // Применяем тег к новому предмету
        planItem.setTag(tag);

        // Уменьшаем стак UN_NETHER_SCROLL на 1
        heldItem.shrink(1);

        // Даем игроку новый предмет
        if (heldItem.isEmpty()) {
            event.getEntity().setItemInHand(event.getHand(), planItem);
        } else {
            event.getEntity().getInventory().add(planItem);
        }

        // Возвращаем успешный результат взаимодействия
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}