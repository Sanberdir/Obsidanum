package net.rezolv.obsidanum.block.block_entity_models;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class HammerForgeModel<T extends Entity> extends HierarchicalModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("obsidanum", "hammer_forge"), "main");
	private final ModelPart main;
	private final ModelPart group;
	private final ModelPart bone;

	public HammerForgeModel(ModelPart root) {
		this.main = root.getChild("main");
		this.group = this.main.getChild("group");
		this.bone = this.main.getChild("bone");
		this.originalGroupY = group.y; // Сохраняем исходное положение Y

	}
	private final float originalGroupY;
	public void setGroupYOffset(float yOffset) {
		// Инвертируем смещение из-за поворота модели
		group.y = originalGroupY - yOffset; // Минус вместо плюса
	}

	public void resetGroupY() {
		group.y = originalGroupY;
	}
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(4, 4).addBox(-12.0F, -16.0F, 0.0F, 12.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, 31.0F, -6.0F));

		PartDefinition group = main.addOrReplaceChild("group", CubeListBuilder.create().texOffs(0, 0).addBox(-32.0F, -12.0F, 14.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-32.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 32).addBox(-30.0F, -12.0F, 0.0F, 16.0F, 12.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-16.0F, -12.0F, 14.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-16.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(16.0F, -7.0F, -2.0F));

		PartDefinition bone = main.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(53, 12).addBox(-16.0F, -16.0F, 0.0F, 16.0F, 4.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -7.0F, -2.0F));

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

	@Override
	public ModelPart root() {
		return main;
	}
}