package net.rezolv.obsidanum.entity.mutated_gart;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rezolv.obsidanum.Obsidanum;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MutatedGartRenderer extends GeoEntityRenderer<MutatedGart> {
    public MutatedGartRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MutatedGartModel());
    }

    @Override
    public ResourceLocation getTextureLocation(MutatedGart animatable) {
        return new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/mutated_gart/mutated_gart.png");
    }

    @Override
    public void render(MutatedGart entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if(entity.isBaby()) {
            poseStack.scale(0.4f, 0.4f, 0.4f);
        }
        poseStack.scale(1.5f, 1.5f, 1.5f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}