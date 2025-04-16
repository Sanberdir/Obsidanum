package net.rezolv.obsidanum.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.block_entity_models.HammerForgeEntityModel;
import net.rezolv.obsidanum.block.custom.HammerForge;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;

public class HammerForgeRenderer <T extends HammerForgeEntity> implements BlockEntityRenderer<T> {
    private static final HammerForgeEntityModel MODEL = new HammerForgeEntityModel();
    private static final ResourceLocation TEXTURE = new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/hammer_forge_entity/hammer_forge_entity.png");

    protected final RandomSource random = RandomSource.create();

    public HammerForgeRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }

    @Override
    public void render(T valve, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        poseStack.pushPose();
        BlockState state = valve.getBlockState();
        Direction dir = state.getValue(HammerForge.FACING);
        if (dir == Direction.UP) {
            poseStack.translate(0.5F, 1.5F, 0.5F);
        } else if (dir == Direction.DOWN) {
            poseStack.translate(0.5F, -0.5F, 0.5F);
        } else if (dir == Direction.NORTH) {
            poseStack.translate(0.5, 0.5F, -0.5F);
        } else if (dir == Direction.EAST) {
            poseStack.translate(1.5F, 0.5F, 0.5F);
        } else if (dir == Direction.SOUTH) {
            poseStack.translate(0.5, 0.5F, 1.5F);
        } else if (dir == Direction.WEST) {
            poseStack.translate(-0.5F, 0.5F, 0.5F);
        }
        poseStack.mulPose(dir.getOpposite().getRotation());
        MODEL.renderToBuffer(poseStack, bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1);
        poseStack.popPose();
    }

    public int getViewDistance() {
        return 256;
    }
}
