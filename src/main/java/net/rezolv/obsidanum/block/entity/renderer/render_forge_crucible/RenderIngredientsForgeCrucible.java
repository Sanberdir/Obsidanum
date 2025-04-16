package net.rezolv.obsidanum.block.entity.renderer.render_forge_crucible;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.block.custom.ForgeCrucible;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

import java.util.ArrayList;
import java.util.List;

public class RenderIngredientsForgeCrucible {
    private static final int CYCLE_TICKS = 20;
    private static final float ITEM_SPACING = 0.3f;
    private static final float ROTATION_SPEED = 8f;

    public static void renderIngredients(ForgeCrucibleEntity blockEntity, float partialTick,
                                         PoseStack poseStack, MultiBufferSource buffer,
                                         int packedLight) {
        CompoundTag crucibleData = blockEntity.getReceivedData();
        if (!crucibleData.contains("Ingredients")) return;

        ListTag ingredients = crucibleData.getList("Ingredients", CompoundTag.TAG_COMPOUND);
        if (ingredients.isEmpty()) return;

        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(ForgeCrucible.FACING);

        List<RenderEntry> renderEntries = new ArrayList<>();

        for (int i = 0; i < ingredients.size(); i++) {
            CompoundTag entry = ingredients.getCompound(i);
            JsonObject json = JsonParser.parseString(entry.getString("IngredientJson")).getAsJsonObject();

            boolean isTag = json.has("tag");
            int required = json.get("count").getAsInt();

            // Считаем все подходящие предметы для тегов
            long current = blockEntity.depositedItems.stream()
                    .filter(stack -> {
                        Ingredient ing = Ingredient.fromJson(json);
                        return isTag ? ing.test(stack) : ItemStack.isSameItemSameTags(stack, ing.getItems()[0]);
                    })
                    .count();
            // Пропускаем выполненные ингредиенты
            if (current >= required) continue;

            // Генерация вариантов для рендера
            List<ItemStack> stacks = new ArrayList<>();
            if (json.has("item")) {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(json.get("item").getAsString()));
                if (item != null) stacks.add(new ItemStack(item));
            } else if (json.has("tag")) {
                TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(),
                        new ResourceLocation(json.get("tag").getAsString()));
                BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders ->
                        holders.forEach(holder -> stacks.add(new ItemStack(holder.value()))));
            }

            if (!stacks.isEmpty()) {
                renderEntries.add(new RenderEntry(stacks, json.has("tag")));
            }
        }

        if (renderEntries.isEmpty()) return;

        // Рендер ингредиентов
        float totalWidth = (renderEntries.size() - 1) * ITEM_SPACING;
        float startOffset = -totalWidth / 2;
        long gameTime = level.getGameTime();

        for (int i = 0; i < renderEntries.size(); i++) {
            RenderEntry entry = renderEntries.get(i);
            List<ItemStack> stacks = entry.stacks();

            int cycleIndex = (int) ((gameTime / CYCLE_TICKS) % stacks.size());
            ItemStack stack = stacks.get(cycleIndex);

            poseStack.pushPose();
            try {
                float xOffset = 0, zOffset = 0;
                switch (facing) {
                    case NORTH, SOUTH -> xOffset = startOffset + i * ITEM_SPACING;
                    case EAST, WEST -> zOffset = startOffset + i * ITEM_SPACING;
                }

                poseStack.translate(0.5 + xOffset, 2, 0.5 + zOffset);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                        (gameTime + partialTick) * ROTATION_SPEED));

                switch (facing) {
                    case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                    case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
                    case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270));
                }

                poseStack.scale(0.4f, 0.4f, 0.4f);

                itemRenderer.renderStatic(
                        stack,
                        ItemDisplayContext.GROUND,
                        getLightLevel(level, pos),
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffer,
                        level,
                        0
                );
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static int getLightLevel(Level level, BlockPos pos) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(blockLight, skyLight);
    }

    private record RenderEntry(List<ItemStack> stacks, boolean isTag) {
        boolean hasMultipleItems() {
            return stacks.size() > 1;
        }
    }
}