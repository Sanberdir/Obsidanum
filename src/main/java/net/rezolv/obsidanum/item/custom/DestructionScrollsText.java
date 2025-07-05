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

public class DestructionScrollsText extends Item {
    public DestructionScrollsText(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // Main header
        tooltip.add(Component.translatable("tooltip.recipe_information").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // Ингредиенты
        if (tag.contains("Ingredients")) {
            tooltip.add(Component.translatable("tooltip.scrolls.ingredients").withStyle(ChatFormatting.GOLD));
            try {
                ListTag ingredients = tag.getList("Ingredients", Tag.TAG_COMPOUND);
                for (Tag element : ingredients) {
                    CompoundTag entry = (CompoundTag) element;
                    JsonObject ingredientJson = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();
                    addIngredientLine(tooltip, ingredientJson);
                }
            } catch (Exception e) {
                tooltip.add(Component.literal(" §7• §cОшибка: " + e.getMessage()));
            }
        }

        tooltip.add(Component.translatable("tooltip.recipe_end").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    // Новый метод для отображения ItemStack
    private void addOutputStackLine(List<Component> tooltip, ItemStack stack, boolean isBonus) {
        MutableComponent line = Component.literal(" §7• ")
                .append(Component.literal(stack.getCount() + "x ").withStyle(isBonus ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.YELLOW))
                .append(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));

        tooltip.add(line);
    }

    private void addIngredientLine(List<Component> tooltip, JsonObject ingredientJson) {
        int count = ingredientJson.has("count") ? ingredientJson.get("count").getAsInt() : 1;
        MutableComponent line = Component.literal(" §7• ")
                .append(Component.literal(count + "x ").withStyle(ChatFormatting.YELLOW));

        if (ingredientJson.has("tag")) {
            ResourceLocation tagId = new ResourceLocation(ingredientJson.get("tag").getAsString());
            line.append(getTagComponent(tagId));
        } else if (ingredientJson.has("item")) {
            ResourceLocation itemId = new ResourceLocation(ingredientJson.get("item").getAsString());
            line.append(getItemComponent(itemId));
        }

        tooltip.add(line);
    }

    private Component getItemComponent(ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return Component.literal(itemId.toString())
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
        }
        return new ItemStack(item).getHoverName().copy()
                .withStyle(ChatFormatting.WHITE);
    }

    private Component getTagComponent(ResourceLocation tagId) {
        MutableComponent component = Component.translatable("tag." + tagId.getNamespace() + "." + tagId.getPath())
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);

        try {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            List<Item> items = ForgeRegistries.ITEMS.tags().getTag(tagKey).stream().toList();

            if (!items.isEmpty()) {
                Component hoverText = Component.literal("Содержимое тега:\n")
                        .withStyle(ChatFormatting.GRAY)
                        .append(items.stream()
                                .limit(10)
                                .map(item -> "§7• §f" + new ItemStack(item).getHoverName().getString())
                                .collect(Collectors.joining("\n")));

                component.withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                        .withUnderlined(true));
            }
        } catch (Exception e) {
            return Component.literal(tagId.toString())
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
        }

        return component;
    }
}