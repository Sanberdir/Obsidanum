package net.rezolv.obsidanum.block.entity.renderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.rezolv.obsidanum.block.block_entity_models.HammerForgeModel;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class HammerForgeRenderer extends GeoBlockRenderer<HammerForgeEntity> {
    public HammerForgeRenderer(BlockEntityRendererProvider.Context context) {
        super(new HammerForgeModel());

    }
}