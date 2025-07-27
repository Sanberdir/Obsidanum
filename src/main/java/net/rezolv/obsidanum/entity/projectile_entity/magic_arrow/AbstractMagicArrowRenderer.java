package net.rezolv.obsidanum.entity.projectile_entity.magic_arrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMagicArrowRenderer <T extends AbstractMagicArrow> extends EntityRenderer<T> {
    public AbstractMagicArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        float textureU1 = 0.0F;
        float textureV1 = 0.0F;
        float textureU2 = 0.5F;
        float textureV2 = 0.15625F;
        float textureU3 = 0.0F;
        float textureV3 = 0.15625F;
        float textureU4 = 0.15625F;
        float textureV4 = 0.3125F;
        float scale = 0.05625F;

        float shakeAmount = (float)entity.shakeTime - partialTicks;
        if (shakeAmount > 0.0F) {
            float shakeRotation = -Mth.sin(shakeAmount * 3.0F) * shakeAmount;
            poseStack.mulPose(Axis.ZP.rotationDegrees(shakeRotation));
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        VertexConsumer vertexBuilder = buffer.getBuffer(RenderType.entityCutout(this.getTextureLocation(entity)));
        PoseStack.Pose poseStackEntry = poseStack.last();
        Matrix4f poseMatrix = poseStackEntry.pose();
        Matrix3f normalMatrix = poseStackEntry.normal();

        // Draw arrow tip
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, -2, -2, textureU1, textureV2, -1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, -2, 2, textureU4, textureV2, -1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, 2, 2, textureU4, textureV4, -1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, 2, -2, textureU1, textureV4, -1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, 2, -2, textureU1, textureV2, 1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, 2, 2, textureU4, textureV2, 1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, -2, 2, textureU4, textureV4, 1, 0, 0, packedLight);
        this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -7, -2, -2, textureU1, textureV4, 1, 0, 0, packedLight);

        // Draw arrow shaft (4 sides)
        for(int i = 0; i < 4; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -8, -2, 0, textureU1, textureV1, 0, 1, 0, packedLight);
            this.addVertex(poseMatrix, normalMatrix, vertexBuilder, 8, -2, 0, textureU2, textureV1, 0, 1, 0, packedLight);
            this.addVertex(poseMatrix, normalMatrix, vertexBuilder, 8, 2, 0, textureU2, textureV2, 0, 1, 0, packedLight);
            this.addVertex(poseMatrix, normalMatrix, vertexBuilder, -8, 2, 0, textureU1, textureV2, 0, 1, 0, packedLight);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    public void addVertex(Matrix4f poseMatrix, Matrix3f normalMatrix, VertexConsumer vertexBuilder,
                          int x, int y, int z, float u, float v,
                          int normalX, int normalY, int normalZ, int packedLight) {
        vertexBuilder.vertex(poseMatrix, (float)x, (float)y, (float)z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, (float)normalX, (float)normalY, (float)normalZ)
                .endVertex();
    }
}