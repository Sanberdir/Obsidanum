package net.rezolv.obsidanum.item.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class UpgradeScrollsText extends Item {
    public UpgradeScrollsText(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // Получаем тег предмета
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // Добавляем заголовок
        tooltip.add(Component.translatable("tooltip.recipe_information").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // Отображаем тип улучшения
        if (tag.contains("Upgrade")) {
            String upgrade = tag.getString("Upgrade").toUpperCase();
            tooltip.add(Component.literal(upgrade).withStyle(ChatFormatting.GREEN));
        }

        // Отображаем tool_types с иконками
        if (tag.contains("ToolTypes")) {
            MutableComponent icons = Component.literal(" ");
            ListTag toolTypesTag = tag.getList("ToolTypes", Tag.TAG_STRING);

            for (int i = 0; i < toolTypesTag.size(); i++) {
                String toolType = toolTypesTag.getString(i);
                switch (toolType.toLowerCase()) {
                    case "pickaxe" -> icons.append(Component.literal("\uE000").withStyle(Style.EMPTY.withFont(new ResourceLocation("obsidanum", "tool_icons"))));
                    case "axe" -> icons.append(Component.literal("\uE001").withStyle(Style.EMPTY.withFont(new ResourceLocation("obsidanum", "tool_icons"))));
                    case "shovel" -> icons.append(Component.literal("\uE002").withStyle(Style.EMPTY.withFont(new ResourceLocation("obsidanum", "tool_icons"))));
                    case "sword" -> icons.append(Component.literal("\uE003").withStyle(Style.EMPTY.withFont(new ResourceLocation("obsidanum", "tool_icons"))));
                    case "hoe" -> icons.append(Component.literal("\uE004").withStyle(Style.EMPTY.withFont(new ResourceLocation("obsidanum", "tool_icons"))));
                }
            }

            tooltip.add(icons);
        }

        // Отображаем tool_kinds
        if (tag.contains("ToolKinds")) {
            tooltip.add(Component.translatable("tooltip.scrolls.tool_kinds").withStyle(ChatFormatting.GOLD));

            ListTag toolKindsTag = tag.getList("ToolKinds", Tag.TAG_STRING);
            for (int i = 0; i < toolKindsTag.size(); i++) {
                String toolKind = toolKindsTag.getString(i);
                tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(toolKind).withStyle(ChatFormatting.WHITE)));
            }
        }

        // Отображаем ингредиенты
        if (tag.contains("Ingredients")) {
            tooltip.add(Component.translatable("tooltip.scrolls.ingredients").withStyle(ChatFormatting.GOLD));

            ListTag ingredientList = tag.getList("Ingredients", Tag.TAG_COMPOUND);
            for (int i = 0; i < ingredientList.size(); i++) {
                CompoundTag ingredientTag = ingredientList.getCompound(i);

                // Проверяем, есть ли JSON ингредиента (для тегов)
                if (ingredientTag.contains("IngredientJson")) {
                    JsonObject ingredientJson = JsonParser.parseString(ingredientTag.getString("IngredientJson")).getAsJsonObject();
                    int count = ingredientJson.has("count") ? ingredientJson.get("count").getAsInt() : 1;

                    MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.GRAY);

                    if (ingredientJson.has("tag")) {
                        ResourceLocation tagId = new ResourceLocation(ingredientJson.get("tag").getAsString());
                        line.append(getTagComponent(tagId, count));
                    } else if (ingredientJson.has("item")) {
                        ResourceLocation itemId = new ResourceLocation(ingredientJson.get("item").getAsString());
                        line.append(getItemComponent(itemId, count));
                    }

                    tooltip.add(line);
                } else if (ingredientTag.contains("ItemStack")) {
                    // Если это конкретный предмет
                    ItemStack ingredient = ItemStack.of(ingredientTag.getCompound("ItemStack"));
                    int count = ingredient.getCount();

                    tooltip.add(
                            Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                                    .append(Component.literal(count + "x ").withStyle(ChatFormatting.YELLOW))
                                    .append(ingredient.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                    );
                }
            }
        }

        // Добавляем разделитель в конце
        tooltip.add(Component.translatable("tooltip.recipe_end").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }
    private Component getTagComponent(ResourceLocation tagId, int count) {
        MutableComponent component = Component.literal(count + "x #" + tagId).withStyle(ChatFormatting.BLUE);
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);

        // Получение предметов из тега для подсказки
        List<Item> items = ForgeRegistries.ITEMS.tags().getTag(tagKey).stream().toList();
        if (!items.isEmpty()) {
            Component hoverText = Component.literal("Предметы из тега:")
                    .append("\n" + items.stream()
                            .map(item -> new ItemStack(item).getHoverName().getString())
                            .collect(Collectors.joining("\n")));

            component.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
        }

        return component;
    }

    private Component getItemComponent(ResourceLocation itemId, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item != null) {
            return Component.literal(count + "x ").append(new ItemStack(item).getHoverName());
        } else {
            return Component.literal(count + "x " + itemId.toString());
        }
    }
}