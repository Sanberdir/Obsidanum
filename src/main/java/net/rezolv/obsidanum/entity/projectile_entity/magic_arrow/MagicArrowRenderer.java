package net.rezolv.obsidanum.entity.projectile_entity.magic_arrow;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rezolv.obsidanum.Obsidanum;

public class MagicArrowRenderer extends AbstractMagicArrowRenderer<MagicArrow> {
    public static final ResourceLocation MAGIC_ARROW = new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/projectiles/magic_arrow.png");

    public MagicArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public ResourceLocation getTextureLocation(MagicArrow magicArrow) {
        return MAGIC_ARROW;
    }
}