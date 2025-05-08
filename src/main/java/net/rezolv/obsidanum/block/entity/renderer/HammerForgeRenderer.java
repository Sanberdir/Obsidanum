package net.rezolv.obsidanum.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.block_entity_animations.HammerForgeAnimation;
import net.rezolv.obsidanum.block.block_entity_models.HammerForgeModel;
import net.rezolv.obsidanum.block.custom.HammerForge;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;

import java.util.WeakHashMap;

public class HammerForgeRenderer implements BlockEntityRenderer<HammerForgeEntity> {
    private final HammerForgeModel<?> model;
    private static final ResourceLocation TEXTURE = new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/hammer_forge_entity/hammer_forge.png");

    private final WeakHashMap<HammerForgeEntity, AnimationState> animationStates = new WeakHashMap<>();

    private static class AnimationState {
        long startTime;
        boolean wasPowered;
    }

    public HammerForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new HammerForgeModel<>(context.bakeLayer(HammerForgeModel.LAYER_LOCATION));
    }

    @Override
    public void render(HammerForgeEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.mulPose(Axis.YP.rotationDegrees(getRotation(blockEntity.getBlockState())));

        AnimationState state = animationStates.computeIfAbsent(blockEntity, k -> new AnimationState());
        boolean isPowered = blockEntity.isPowered();
        float yOffset = 0.0F;

        if (isPowered && !state.wasPowered) {
            state.startTime = Minecraft.getInstance().level.getGameTime();
        }

        if (isPowered) {
            long elapsedTicks = Minecraft.getInstance().level.getGameTime() - state.startTime;
            float elapsedSeconds = (elapsedTicks + partialTick) / 20.0F;

            if (elapsedSeconds < HammerForgeAnimation.down_move.lengthInSeconds()) {
                yOffset = calculateYOffset(elapsedSeconds);
            }
        }

        // Обновляем состояние питания для следующего кадра
        state.wasPowered = isPowered;

        // Применяем смещение с инверсией из-за поворота модели
        model.setGroupYOffset(-yOffset);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private float calculateYOffset(float elapsedSeconds) {
        if (elapsedSeconds <= 0.0833F) {
            // Быстрое движение вверх (8.0F за 0.0833 секунды)
            return 8.0F * (elapsedSeconds / 0.0833F);
        } else if (elapsedSeconds <= 1.0F) {
            // Медленный возврат в исходное положение
            float progress = (elapsedSeconds - 0.0833F) / 0.9167F;
            return 8.0F - (8.0F * progress);
        }
        return 0.0F;
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