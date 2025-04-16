package net.rezolv.obsidanum.block.block_entity_models;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;

public class HammerForgeEntityModel extends AdvancedEntityModel<Entity> {
	public AdvancedModelBox root;
	public AdvancedModelBox piston;
	public AdvancedModelBox top;
	public AdvancedModelBox hammer;

	public HammerForgeEntityModel() {
		texWidth = 128;
		texHeight = 128;

		root = new AdvancedModelBox(this);
		root.setRotationPoint(0.0F, 24.0F, 0.0F);

		piston = new AdvancedModelBox(this);
		piston.setRotationPoint(0.0F, -6.5F, 0.0F);
		piston.setTextureOffset(4, 4).addBox(-6.0F, -10.5F, -6.0F, 12.0F, 16.0F, 12.0F, 0.0F, false);
		root.addChild(piston);

		top = new AdvancedModelBox(this);
		top.setRotationPoint(0.0F, -14.0F, 0.0F);
		top.setTextureOffset(52, 12).addBox(-8.0F, -2.0F, -8.0F, 16.0F, 4.0F, 16.0F, 0.0F, false);
		root.addChild(top);

		hammer = new AdvancedModelBox(this);
		hammer.setRotationPoint(0.0F, -6.0F, 0.0F);
		hammer.setTextureOffset(0, 0).addBox(-10.0F, -6.0F, 6.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
		hammer.setTextureOffset(0, 0).addBox(-10.0F, -6.0F, -10.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
		hammer.setTextureOffset(0, 32).addBox(-8.0F, -6.0F, -8.0F, 16.0F, 12.0F, 16.0F, 0.0F, false);
		hammer.setTextureOffset(0, 0).addBox(6.0F, -6.0F, 6.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
		hammer.setTextureOffset(0, 0).addBox(6.0F, -6.0F, -10.0F, 4.0F, 12.0F, 4.0F, 0.0F, false);
		root.addChild(hammer);

		this.updateDefaultPose();
	}
	@Override
	public Iterable<BasicModelPart> parts() {
		return ImmutableList.of(root);
	}
	@Override
	public void setupAnim(Entity entity, float limbSwing, float lifetime, float down, float netHeadYaw, float headPitch) {
		this.resetToDefaultPose();

	}
	@Override
	public Iterable<AdvancedModelBox> getAllParts() {
		return ImmutableList.of(root, piston, hammer,top);

	}
}