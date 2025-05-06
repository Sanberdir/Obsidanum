package net.rezolv.obsidanum.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.block_entity_models.HammerForgeModel;
import net.rezolv.obsidanum.block.custom.HammerForge;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;

public class HammerForgeRenderer implements BlockEntityRenderer<HammerForgeEntity> {
    private final HammerForgeModel<?> model;
    private static final ResourceLocation TEXTURE = new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/hammer_forge_entity/hammer_forge.png");

    public HammerForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new HammerForgeModel<>(context.bakeLayer(HammerForgeModel.LAYER_LOCATION));
    }

    @Override
    public void render(HammerForgeEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // Центрирование модели
        poseStack.translate(0.5, 1.5, 0.5);

        // Корректировка ориентации
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                getRotation(blockEntity.getBlockState())
        ));

        // Получение буфера для рендеринга
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));

        // Рендер модели
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private float getRotation(BlockState state) {
        return switch (state.getValue(HammerForge.FACING)) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            case UP -> 90;
            case DOWN -> -90;
        };
    }
}