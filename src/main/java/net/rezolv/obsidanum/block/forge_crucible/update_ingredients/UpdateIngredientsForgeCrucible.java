package net.rezolv.obsidanum.block.forge_crucible.update_ingredients;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

import java.util.List;
import java.util.ListIterator;

public class UpdateIngredientsForgeCrucible {
    public static InteractionResult handleInteraction(Level level, Player player, ItemStack heldStack,
                                                      ForgeCrucibleEntity crucible, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!heldStack.isEmpty()) {
            return handleItemDeposit(level, player, heldStack, crucible, pos, state);
        } else if (player.isShiftKeyDown()) {
            return handleItemWithdrawal(level, player, crucible, pos, state);
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleItemDeposit(Level level, Player player, ItemStack stack,
                                                       ForgeCrucibleEntity crucible, BlockPos pos, BlockState state) {
        CompoundTag data = crucible.getReceivedData();
        if (!data.contains("Ingredients", Tag.TAG_LIST)) {
            return InteractionResult.PASS;
        }

        ListTag ingredients = data.getList("Ingredients", Tag.TAG_COMPOUND);
        boolean allRequirementsMet = true;
        boolean itemUsed = false;

        // Проверяем все ингредиенты
        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag ingTag = ingredients.getCompound(i);
            try {
                JsonObject json = JsonParser.parseString(ingTag.getString("IngredientJson")).getAsJsonObject();
                if (processIngredient(crucible, stack, json)) {
                    stack.shrink(1);
                    itemUsed = true;
                    break;
                }

                // Проверяем выполнение требований для каждого ингредиента
                if (!checkIngredientRequirement(crucible, json)) {
                    allRequirementsMet = false;
                }

            } catch (Exception e) {
                return InteractionResult.FAIL;
            }
        }

        if (itemUsed) {
            crucible.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);

            // Проверяем выполнение всех требований после добавления
            if (allRequirementsMet) {
                crucible.markReadyForCrafting();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    private static boolean checkIngredientRequirement(ForgeCrucibleEntity crucible, JsonObject json) {
        int required = json.get("count").getAsInt();
        boolean isTag = json.has("tag");
        Ingredient ingredient = Ingredient.fromJson(json);

        long currentCount = crucible.depositedItems.stream()
                .filter(stack -> {
                    ItemStack copy = stack.copy();
                    if (copy.isDamaged()) copy.setDamageValue(0);
                    return isTag ?
                            ingredient.test(copy) :
                            ItemStack.isSameItemSameTags(copy, copy);
                })
                .count();

        return currentCount >= required;
    }

    // В классе ForgeCrucibleEntity


    private static boolean processIngredient(ForgeCrucibleEntity crucible, ItemStack stack, JsonObject json) {
        if (!json.has("count")) {
            return false;
        }
        int required = json.get("count").getAsInt();

        // Определяем тип ингредиента (тег или предмет)
        boolean isTag = json.has("tag");
        Ingredient ingredient = Ingredient.fromJson(json);

        // Игнорируем прочность при проверке
        ItemStack stackToCheck = stack.copy();
        if (stackToCheck.isDamaged()) {
            stackToCheck.setDamageValue(0);
        }

        // Проверяем соответствие ингредиенту
        if (!ingredient.test(stackToCheck)) return false;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());

        // Считаем текущее количество (игнорируя прочность)
        long current = crucible.depositedItems.stream()
                .filter(deposited -> {
                    ItemStack depositedCopy = deposited.copy();
                    if (depositedCopy.isDamaged()) {
                        depositedCopy.setDamageValue(0);
                    }
                    // Для тегов: проверяем принадлежность к тегу
                    if (isTag) {
                        return ingredient.test(depositedCopy);
                    }
                    // Для предметов: точное совпадение
                    else {
                        return ItemStack.isSameItemSameTags(depositedCopy, stackToCheck);
                    }
                })
                .count();

        // Если достигнут лимит, не добавляем
        if (current >= required) {
            return false;
        }

        // Добавляем предмет с обнуленной прочностью
        ItemStack stackToAdd = stack.copy();
        if (stackToAdd.isDamaged()) {
            stackToAdd.setDamageValue(0); // Игнорируем прочность при добавлении
        }
        stackToAdd.setCount(1); // Убедимся, что добавляется только 1 предмет
        crucible.depositedItems.add(stackToAdd);

        // Обновляем данные и синхронизируем с клиентом
        crucible.setChanged();
        if (crucible.getLevel() != null) {
            crucible.getLevel().sendBlockUpdated(crucible.getBlockPos(), crucible.getBlockState(), crucible.getBlockState(), 3);
        }
        return true;
    }

    private static InteractionResult handleItemWithdrawal(Level level, Player player,
                                                          ForgeCrucibleEntity crucible, BlockPos pos, BlockState state) {
        if (crucible.depositedItems.isEmpty()) return InteractionResult.PASS;

        // Извлекаем в обратном порядке (последние добавленные первыми)
        ListIterator<ItemStack> iterator = crucible.depositedItems.listIterator(crucible.depositedItems.size());

        while (iterator.hasPrevious()) {
            ItemStack stack = iterator.previous();

            if (!stack.isEmpty()) {
                // Возвращаем ровно 1 предмет
                ItemStack returnStack = stack.copy();
                returnStack.setCount(1); // Убедимся, что возвращается только 1 предмет
                giveOrDropItem(player, returnStack);

                // Удаляем только 1 предмет из списка
                iterator.remove();
                crucible.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static void giveOrDropItem(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            // Если инвентарь полон, выбрасываем ровно 1 предмет
            ItemEntity itemEntity = new ItemEntity(
                    player.level(),
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    stack.copy()
            );
            itemEntity.setDefaultPickUpDelay();
            player.level().addFreshEntity(itemEntity);
        }
    }
}
