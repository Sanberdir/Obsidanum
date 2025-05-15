package net.rezolv.obsidanum.block.block_entity_models;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import static net.rezolv.obsidanum.block.entity.renderer.HammerForgeRenderer.down_move;

public class HammerForgeModel<T extends Entity> extends HierarchicalModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("obsidanum", "hammer_forge"), "main");
	private final ModelPart main;
	private final ModelPart group;
	private final ModelPart bone;
	private final ModelPart inner;
	private final float originalGroupY;
	private final float originalInnerY;

	public HammerForgeModel(ModelPart root) {
		this.main = root.getChild("main");
		this.group = this.main.getChild("group");
		this.bone = this.main.getChild("bone");
		this.inner = this.main.getChild("inner");
		this.originalGroupY = group.y;
		this.originalInnerY = inner.y;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(6.0F, 31.0F, -6.0F));

		PartDefinition group = main.addOrReplaceChild("group", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-32.0F, -12.0F, 14.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
						.texOffs(0, 0).addBox(-32.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
						.texOffs(0, 32).addBox(-30.0F, -12.0F, 0.0F, 16.0F, 12.0F, 16.0F, new CubeDeformation(0.0F))
						.texOffs(0, 0).addBox(-16.0F, -12.0F, 14.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
						.texOffs(0, 0).addBox(-16.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(16.0F, -7.0F, -2.0F));

		PartDefinition bone = main.addOrReplaceChild("bone", CubeListBuilder.create()
						.texOffs(53, 12).addBox(-16.0F, -16.0F, 0.0F, 16.0F, 4.0F, 16.0F, new CubeDeformation(0.0F)),
				PartPose.offset(2.0F, -7.0F, -2.0F));

		PartDefinition inner = main.addOrReplaceChild("inner", CubeListBuilder.create()
						.texOffs(4, 4).addBox(-12.0F, -23.0F, 0.0F, 12.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
	public void resetPositions() {
		group.y = originalGroupY;
		inner.y = originalInnerY;
	}
	public void applyAnimation(float animationProgress) {
		// Сбрасываем позиции перед применением анимации
		this.resetPositions();

		// Теперь НЕ инвертируем Y, так как хотим сохранить оригинальное направление анимации
		float groupYOffset = getYOffsetForAnimation(down_move, animationProgress, "group");
		group.y = originalGroupY + groupYOffset; // Убрали минус

		float innerYOffset = getYOffsetForAnimation(down_move, animationProgress, "inner");
		inner.y = originalInnerY + innerYOffset; // Убрали минус
	}
	private float getYOffsetForAnimation(AnimationDefinition animation, float progress, String partName) {
		if (partName.equals("group")) {
			if (progress < 0.0417F) return 0F;
			if (progress < 0.0833F) return 16F;
			if (progress < 0.125F) return 15.8F;
			if (progress < 0.1667F) return 16F;
			if (progress < 0.5F) return 16F;
			if (progress < 0.75F) return 13F;
			if (progress < 1.0F) return 0F;
			if (progress < 1.5F) return 0F;
			return 0F;
		} else if (partName.equals("inner")) {
			if (progress < 0) return 0F;
			if (progress < 0.0833F) return 4F;
			if (progress < 0.5F) return 4F;
			if (progress < 0.75F) return 3F;
			if (progress < 1.0F) return 0F;
			if (progress < 1.5F) return 0F;
			return 0F;
		}
		return 0F;
	}
	@Override
	public ModelPart root() {
		return main;
	}
}