package net.rezolv.obsidanum.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;
import net.rezolv.obsidanum.block.entity.renderer.render_forge_crucible.RenderResultForgeCrucible;

public class ForgeCrucibleEntityRenderer implements BlockEntityRenderer<ForgeCrucibleEntity> {
    public ForgeCrucibleEntityRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public void render(ForgeCrucibleEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack,
                       MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        RenderResultForgeCrucible.renderResult(pBlockEntity, pPartialTick, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
    }

}