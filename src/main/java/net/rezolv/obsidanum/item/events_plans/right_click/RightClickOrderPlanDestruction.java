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
import net.rezolv.obsidanum.recipes.ForgeScrollOrderDestructionRecipe;
import net.rezolv.obsidanum.sound.SoundsObs;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class RightClickOrderPlanDestruction {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();

        // Проверка: правильный предмет и серверная сторона
        if (heldItem.getItem() != ItemsObs.UN_ORDER_DESTRUCTION_SCROLL.get() || level.isClientSide()) {
            return;
        }

        // Получаем все рецепты разрушения
        List<ForgeScrollOrderDestructionRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ForgeScrollOrderDestructionRecipe.Type.FORGE_SCROLL_ORDER_DESTRUCTION);

        if (recipes.isEmpty()) {
            return;
        }

        // Выбираем случайный рецепт
        ForgeScrollOrderDestructionRecipe recipe = recipes.get(RANDOM.nextInt(recipes.size()));

        // Создаем новый предмет плана
        ItemStack plan = new ItemStack(ItemsObs.ORDER_DESTRUCTION_PLAN.get());
        CompoundTag tag = new CompoundTag();

        // Сохраняем ID рецепта
        tag.putString("RecipeID", recipe.getId().toString());

        // Сохраняем ингредиенты
        ListTag ingredientsList = new ListTag();
        CompoundTag ingredientTag = new CompoundTag();
        ingredientTag.putString("IngredientJson", recipe.getIngredientJson().toString());
        ingredientsList.add(ingredientTag);
        tag.put("Ingredients", ingredientsList);

        // Сохраняем все возможные выходы (мульти-вывод)
        ListTag multipleOutputsList = new ListTag();
        for (ItemStack output : recipe.getMultipleOutput()) {
            CompoundTag outputTag = new CompoundTag();
            output.save(outputTag);
            multipleOutputsList.add(outputTag);
        }
        tag.put("MultipleOutputs", multipleOutputsList);

        // Сохраняем JSON описания выходов для отображения
        ListTag outputsJsonList = new ListTag();
        for (JsonObject outJson : recipe.getMultipleOutputJsons()) {
            CompoundTag outTag = new CompoundTag();
            outTag.putString("OutputJson", outJson.toString());
            outputsJsonList.add(outTag);
        }
        tag.put("OutputJsons", outputsJsonList);

        // Применяем тег к предмету плана
        plan.setTag(tag);

        // Воспроизводим звук обучения
        level.playSound(null,
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                SoundsObs.LEARN.get(),
                SoundSource.PLAYERS,
                1.0F,
                0.8F + RANDOM.nextFloat() * 0.4F);

        // Уменьшаем исходный предмет
        heldItem.shrink(1);

        // Выдаем предмет плана игроку
        if (heldItem.isEmpty()) {
            event.getEntity().setItemInHand(event.getHand(), plan);
        } else {
            event.getEntity().getInventory().add(plan);
        }

        // Отмечаем событие как обработанное
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}