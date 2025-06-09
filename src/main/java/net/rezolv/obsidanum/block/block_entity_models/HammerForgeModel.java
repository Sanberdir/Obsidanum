package net.rezolv.obsidanum.block.block_entity_models;

import net.minecraft.resources.ResourceLocation;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;
import software.bernie.geckolib.model.GeoModel;

public class HammerForgeModel extends GeoModel<HammerForgeEntity> {
	@Override
	public ResourceLocation getModelResource(HammerForgeEntity animatable) {
		return new ResourceLocation(Obsidanum.MOD_ID, "geo/hammer_forge.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(HammerForgeEntity animatable) {
		return new ResourceLocation(Obsidanum.MOD_ID, "textures/block/hammer_forge.png");
	}

	@Override
	public ResourceLocation getAnimationResource(HammerForgeEntity animatable) {
		return new ResourceLocation(Obsidanum.MOD_ID, "animations/hammer_forge.animation.json");
	}
}