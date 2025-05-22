package net.rezolv.obsidanum.block.block_entity_models;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;

import static net.rezolv.obsidanum.block.entity.renderer.HammerForgeRenderer.down_move;

public class HammerForgeModel<T extends Entity> extends HierarchicalModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("obsidanum", "hammer_forge"), "main");
	private final ModelPart main;
	private final ModelPart hammer;
	private final ModelPart bone;
	private final ModelPart inner;
	private final float originalGroupY;
	private final float originalInnerY;

	public HammerForgeModel(ModelPart root) {
		this.main = root.getChild("main");
		this.hammer = this.main.getChild("hammer");
		this.bone = this.main.getChild("bone");
		this.inner = this.main.getChild("inner");
		this.originalGroupY = hammer.y;
		this.originalInnerY = inner.y;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(6.0F, 31.0F, -6.0F));

		PartDefinition hammer = main.addOrReplaceChild("hammer", CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -6.0F, 6.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-10.0F, -6.0F, -10.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 32).addBox(-8.0F, -6.0F, -8.0F, 16.0F, 12.0F, 16.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(6.0F, -6.0F, 6.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(6.0F, -6.0F, -10.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -13.0F, 6.0F));

		PartDefinition bone = main.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(52, 12).addBox(-16.0F, -16.0F, 0.0F, 16.0F, 4.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -7.0F, -2.0F));

		PartDefinition inner = main.addOrReplaceChild("inner", CubeListBuilder.create().texOffs(4, 4).addBox(-6.0F, -12.0F, -6.0F, 12.0F, 16.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, -11.0F, 6.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
	}
	public static final AnimationDefinition move_down = AnimationDefinition.Builder.withLength(1.5F).looping()
			.addAnimation("hammer", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.0833F, KeyframeAnimations.posVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -15.8F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -13.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("hammer", new AnimationChannel(AnimationChannel.Targets.SCALE,
					new Keyframe(0.0833F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.125F, KeyframeAnimations.scaleVec(1.06F, 1.0F, 1.06F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.1667F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("inner", new AnimationChannel(AnimationChannel.Targets.ROTATION,
					new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.7083F, KeyframeAnimations.degreeVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.875F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.addAnimation("inner", new AnimationChannel(AnimationChannel.Targets.POSITION,
					new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.0833F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
					new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
			))
			.build();
	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
	public void resetPositions() {
		hammer.y = originalGroupY;
		inner.y = originalInnerY;
	}
	public void applyAnimation(float animationProgress) {
		this.resetPositions();
		float groupYOffset = getYOffsetForAnimation(move_down, animationProgress, "group");
		hammer.y = originalGroupY + groupYOffset;

		float innerYOffset = getYOffsetForAnimation(move_down, animationProgress, "inner");
		inner.y = originalInnerY + innerYOffset;
	}
	private float getYOffsetForAnimation(AnimationDefinition animation, float progress, String partName) {
		String animPartName = partName.equals("group") ? "hammer" : "inner";

		// Получаем все каналы анимации для нужной части
		List<AnimationChannel> channels = getAnimationChannels(animation, animPartName);

		if (channels.isEmpty()) {
			return 0F;
		}

		// Ищем канал с POSITION анимацией
		for (AnimationChannel channel : channels) {
			if (channel.target() == AnimationChannel.Targets.POSITION) {
				Keyframe[] keyframes = channel.keyframes();

				// Находим два ближайших ключевых кадра
				Keyframe prevFrame = null;
				Keyframe nextFrame = null;

				for (Keyframe frame : keyframes) {
					if (frame.timestamp() <= progress) {
						prevFrame = frame;
					} else {
						nextFrame = frame;
						break;
					}
				}

				// Если нет следующего кадра, используем последний
				if (nextFrame == null) {
					return prevFrame != null ? prevFrame.target().y() : 0F;
				}

				// Линейная интерполяция
				float t = (progress - prevFrame.timestamp()) / (nextFrame.timestamp() - prevFrame.timestamp());
				return prevFrame.target().y() + t * (nextFrame.target().y() - prevFrame.target().y());
			}
		}

		return 0F;
	}

	// Вспомогательный метод для получения каналов анимации по имени части
	private List<AnimationChannel> getAnimationChannels(AnimationDefinition animation, String partName) {
		// К сожалению, в Minecraft API нет прямого доступа к каналам анимации,
		// поэтому нам нужно создать свой способ их получения

		// В вашем случае мы можем использовать рефлексию или создать карту заранее
		// Вот простой вариант для вашей конкретной анимации:

		if (animation == move_down) {
			if (partName.equals("hammer")) {
				return List.of(
						new AnimationChannel(AnimationChannel.Targets.POSITION,
								new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.0833F, KeyframeAnimations.posVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -15.8F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -13.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
						),
						new AnimationChannel(AnimationChannel.Targets.SCALE,
								new Keyframe(0.0833F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.125F, KeyframeAnimations.scaleVec(1.06F, 1.0F, 1.06F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.1667F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)
						)
				);
			} else if (partName.equals("inner")) {
				return List.of(
						new AnimationChannel(AnimationChannel.Targets.ROTATION,
								new Keyframe(0.625F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.7083F, KeyframeAnimations.degreeVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.875F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
						),
						new AnimationChannel(AnimationChannel.Targets.POSITION,
								new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.0833F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
								new Keyframe(1.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
						)
				);
			}
		}

		return List.of();
	}
	@Override
	public ModelPart root() {
		return main;
	}
}