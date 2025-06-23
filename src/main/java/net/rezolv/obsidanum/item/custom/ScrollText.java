package net.rezolv.obsidanum.item.custom;

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

public class ScrollText extends Item {
    public ScrollText(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // Main header
        tooltip.add(Component.translatable("tooltip.recipe_information").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // Icon indicating scroll type
        MutableComponent typeScrollIcon = Component.literal("\uE005")
                .withStyle(style -> style.withFont(new ResourceLocation("obsidanum", "tool_icons")));

        // Result icon depending on quantity
        MutableComponent resultIcon = Component.literal("\uE007") // Default: single result
                .withStyle(style -> style.withFont(new ResourceLocation("obsidanum", "tool_icons")));

        if (tag.contains("RecipeResult")) {
            ListTag resultList = tag.getList("RecipeResult", Tag.TAG_COMPOUND);
            int totalCount = 0;
            for (int i = 0; i < resultList.size(); i++) {
                CompoundTag resultTag = resultList.getCompound(i);
                ItemStack result = ItemStack.of(resultTag);
                totalCount += result.getCount();
            }
            if (totalCount > 1) {
                resultIcon = Component.literal("\uE006")
                        .withStyle(style -> style.withFont(new ResourceLocation("obsidanum", "tool_icons")));
            }
        }

        // Combine icons inline
// Display results (RecipeResult)
        if (tag.contains("RecipeResult")) {
            ListTag resultList = tag.getList("RecipeResult", Tag.TAG_COMPOUND);
            for (int i = 0; i < resultList.size(); i++) {
                CompoundTag resultTag = resultList.getCompound(i);
                ItemStack result = ItemStack.of(resultTag);
                int count = result.getCount();

                tooltip.add(result.getHoverName().copy().withStyle(ChatFormatting.WHITE));
            }
        }

// Combine icons inline (after results)
        MutableComponent iconsLine = Component.literal("")
                .append(typeScrollIcon)
                .append(" ")
                .append(resultIcon);
        tooltip.add(iconsLine);

        // Display ingredients (RecipeIngredients)
        if (tag.contains("Ingredients")) {
            tooltip.add(Component.translatable("tooltip.scrolls.ingredients").withStyle(ChatFormatting.GOLD));
            ListTag ingredientList = tag.getList("Ingredients", Tag.TAG_COMPOUND);
            for (int i = 0; i < ingredientList.size(); i++) {
                CompoundTag ingredientTag = ingredientList.getCompound(i);
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

        // Final separator
        tooltip.add(Component.translatable("tooltip.recipe_end").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }


    private Component getTagComponent(ResourceLocation tagId, int count) {
        MutableComponent component = Component.literal(count + "x ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("tag." + tagId.getNamespace() + ":" + tagId.getPath()).withStyle(ChatFormatting.BLUE));
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
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
            return Component.literal(count + "x ").withStyle(ChatFormatting.YELLOW)
                    .append(new ItemStack(item).getHoverName().copy().withStyle(ChatFormatting.WHITE));
        } else {
            return Component.literal(count + "x " + itemId.toString()).withStyle(ChatFormatting.RED);
        }
    }
}
