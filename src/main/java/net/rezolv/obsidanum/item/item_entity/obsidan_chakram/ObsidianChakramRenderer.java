package net.rezolv.obsidanum.item.item_entity.obsidan_chakram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rezolv.obsidanum.item.ItemsObs;

public class ObsidianChakramRenderer extends EntityRenderer<ObsidianChakramEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("obsidanum", "textures/entity/projectiles/obsidian_chakram.png");
    private final ItemRenderer itemRenderer;

    public ObsidianChakramRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(ObsidianChakramEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS; // Используем текстуру атласа для рендеринга предмета
    }

    @Override
    public void render(ObsidianChakramEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        // Получаем углы ориентации
        float yaw = entity.isStopped() ? entity.getStoppedYaw() : entity.getYRot();
        float pitch = entity.isStopped() ? entity.getStoppedPitch() : entity.getXRot();

        // Базовый поворот для ориентации ребром
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));

        // Вращение в полёте
        if (!entity.isStopped()) {
            float spin = (entity.tickCount + partialTicks) * -45.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        }

        // Корректировка позиции и масштаба
        poseStack.translate(0, -0.15, 0);
        poseStack.scale(1.2F, 1.2F, 1.2F);

        // Рендер модели
        ItemStack stack = new ItemStack(ItemsObs.OBSIDIAN_CHAKRAM.get());
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, 0);
        itemRenderer.render(
                stack,
                ItemDisplayContext.GROUND,
                false,
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                model
        );

        poseStack.popPose();
    }
}