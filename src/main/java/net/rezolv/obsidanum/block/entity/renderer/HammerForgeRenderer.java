package net.rezolv.obsidanum.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.block_entity_models.HammerForgeModel;
import net.rezolv.obsidanum.block.custom.HammerForge;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;

public class HammerForgeRenderer implements BlockEntityRenderer<HammerForgeEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/hammer_forge_entity/hammer_forge.png");
    private final HammerForgeModel<?> model;
    private float animationProgress = 0;

    public HammerForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new HammerForgeModel<>(context.bakeLayer(HammerForgeModel.LAYER_LOCATION));
    }

    @Override
    public void render(HammerForgeEntity blockEntity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Обновляем прогресс анимации
        animationProgress += 0.01f;
        if (animationProgress > 2*Math.PI) {
            animationProgress -= 2*Math.PI;
        }

        // Вычисляем смещение
        float verticalOffset = (float)Math.sin(animationProgress) * 0.7f;

        poseStack.pushPose();
        // Центрируем и поворачиваем модель
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.mulPose(Axis.YP.rotationDegrees(getRotation(blockEntity.getBlockState())));

        // Сбрасываем позу модели перед анимацией
        model.resetPose();

        // Применяем анимацию только к group части
        model.group.y += verticalOffset;

        // Рендерим всю модель
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));
        model.renderToBuffer(poseStack, vc, packedLight, packedOverlay, 1f, 1f, 1f, 1f);

        poseStack.popPose();
    }

    private float getRotation(BlockState state) {
        return switch (state.getValue(HammerForge.FACING)) {
            case NORTH ->   0;
            case SOUTH -> 180;
            case WEST  -> 270;
            case EAST  ->  90;
            case UP    ->  90;
            case DOWN  -> -90;
        };
    }
}