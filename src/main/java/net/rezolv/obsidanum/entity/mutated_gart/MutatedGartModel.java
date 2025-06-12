package net.rezolv.obsidanum.entity.mutated_gart;// Made with Blockbench 4.10.4

import net.minecraft.resources.ResourceLocation;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.entity.HammerForgeEntity;
import software.bernie.geckolib.model.GeoModel;

public class MutatedGartModel extends GeoModel<MutatedGart> {
	@Override
	public ResourceLocation getModelResource(MutatedGart animatable) {
		return new ResourceLocation(Obsidanum.MOD_ID, "geo/mutated_gart.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MutatedGart animatable) {
		return new ResourceLocation(Obsidanum.MOD_ID, "textures/entity/mutated_gart/mutated_gart.png");
	}

	@Override
	public ResourceLocation getAnimationResource(MutatedGart animatable) {
		return new ResourceLocation(Obsidanum.MOD_ID, "animations/mutated_gart.animation.json");
	}
}