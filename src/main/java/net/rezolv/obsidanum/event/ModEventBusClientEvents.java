package net.rezolv.obsidanum.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.block_entity_models.HammerForgeModel;
import net.rezolv.obsidanum.block.entity.ModBlockEntities;
import net.rezolv.obsidanum.block.entity.renderer.ForgeCrucibleEntityRenderer;
import net.rezolv.obsidanum.block.entity.renderer.HammerForgeRenderer;
import net.rezolv.obsidanum.block.entity.renderer.PranaCrystallRenderer;
import net.rezolv.obsidanum.effect.effects.effect_overlay.ConfusionOverlay;
import net.rezolv.obsidanum.effect.effects.effect_overlay.PreConfusionOverlay;
import net.rezolv.obsidanum.entity.ModEntities;
import net.rezolv.obsidanum.entity.ModModelLayers;
import net.rezolv.obsidanum.entity.gart.GartModel;
import net.rezolv.obsidanum.entity.meat_beetle.MeetBeetleModel;
import net.rezolv.obsidanum.entity.mutated_gart.MutatedGart;
import net.rezolv.obsidanum.entity.mutated_gart.MutatedGartModel;
import net.rezolv.obsidanum.entity.obsidian_elemental.ObsidianElementalModel;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.item.custom.*;
import net.rezolv.obsidanum.item.custom.cooldown_instruments.*;
import net.rezolv.obsidanum.item.entity.client.ModModelLayersItem;
import net.rezolv.obsidanum.particle.*;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("axe_cooldown", CooldownOverlayAxe.COOLDOWN_OVERLAY);
        event.registerAboveAll("hoe_cooldown", CooldownOverlayHoe.COOLDOWN_OVERLAY);
        event.registerAboveAll("shovel_cooldown", CooldownOverlayShovel.COOLDOWN_OVERLAY);
        event.registerAboveAll("sword_cooldown", CooldownOverlaySword.COOLDOWN_OVERLAY);
        event.registerAboveAll("pickaxe_cooldown", CooldownOverlayPickaxe.COOLDOWN_OVERLAY);
    }
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayersItem.OBSIDAN_BOAT_LAYER, BoatModel::createBodyModel);
        event.registerLayerDefinition(ModModelLayersItem.OBSIDAN_CHEST_BOAT_LAYER, ChestBoatModel::createBodyModel);
        event.registerLayerDefinition(ModModelLayers.OBSIDIAN_ELEMENTAL, ObsidianElementalModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.MEET_BEETLE, MeetBeetleModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.GART, GartModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticlesObs.NETHER_FLAME_PARTICLES.get(), NetherFlameProjectileParticle.Provider::new);
        event.registerSpriteSet(ParticlesObs.GLINT_PURPLE_PARTICLES.get(), GlintPurple.Provider::new);
        event.registerSpriteSet(ParticlesObs.GLINT_BLUE_PARTICLES.get(), GlintBlue.Provider::new);
        event.registerSpriteSet(ParticlesObs.GLINT_CRIMSON_PARTICLES.get(), GlintBlue.Provider::new);
        event.registerSpriteSet(ParticlesObs.NETHER_FLAME_PROJECTILE_PARTICLES.get(), NetherFlameProjectileParticle.Provider::new);
        event.registerSpriteSet(ParticlesObs.NETHER_FLAME2_PARTICLES.get(), Nether2FlameParticle.Provider::new);
        event.registerSpriteSet(ParticlesObs.BAGELL_FLAME_PARTICLES.get(), BagellFlameParticle.Provider::new);
        event.registerSpriteSet(ParticlesObs.BAGELL_TABLE_PARTICLES.get(), BagelEnchantParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        new PreConfusionOverlay();
        new ConfusionOverlay();

        ItemProperties.register(ItemsObs.OBSIDAN_SWORD.get(), new ResourceLocation("activated"),
                (stack, world, entity, seed) -> stack.getItem() instanceof ObsidanSword && ((ObsidanSword) stack.getItem()).isActivated(new ItemStack(ItemsObs.OBSIDAN_SWORD.get())) ? 1.0F : 0.0F);
        ItemProperties.register(ItemsObs.OBSIDAN_SHOVEL.get(), new ResourceLocation("activated"),
                (stack, world, entity, seed) -> stack.getItem() instanceof ObsidanShovel && ((ObsidanShovel) stack.getItem()).isActivated(new ItemStack(ItemsObs.OBSIDAN_SHOVEL.get())) ? 1.0F : 0.0F);
        ItemProperties.register(ItemsObs.OBSIDAN_HOE.get(), new ResourceLocation("activated"),
                (stack, world, entity, seed) -> stack.getItem() instanceof ObsidanHoe && ((ObsidanHoe) stack.getItem()).isActivated(new ItemStack(ItemsObs.OBSIDAN_HOE.get())) ? 1.0F : 0.0F);
        ItemProperties.register(ItemsObs.OBSIDAN_AXE.get(), new ResourceLocation("activated"),
                (stack, world, entity, seed) -> stack.getItem() instanceof ObsidanAxe && ((ObsidanAxe) stack.getItem()).isActivated(new ItemStack(ItemsObs.OBSIDAN_AXE.get())) ? 1.0F : 0.0F);
        ItemProperties.register(ItemsObs.OBSIDAN_PICKAXE.get(), new ResourceLocation("activated"),
                (stack, world, entity, seed) -> stack.getItem() instanceof ObsidanPickaxe && ((ObsidanPickaxe) stack.getItem()).isActivated(new ItemStack(ItemsObs.OBSIDAN_PICKAXE.get())) ? 1.0F : 0.0F);
    }
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("pre_confusion_overlay", PreConfusionOverlay.PRE_CONFUSION_OVERLAY);
        event.registerAboveAll("confusion_overlay", ConfusionOverlay.CONFUSION_OVERLAY);
    }

    @SubscribeEvent
    public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FORGE_CRUCIBLE.get(), ForgeCrucibleEntityRenderer::new);

        event.registerBlockEntityRenderer(ModBlockEntities.HAMMER_FORGE.get(), HammerForgeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PRANA_CRYSTALL.get(), PranaCrystallRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.OBSIDAN_SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.OBSIDAN_HANGING_SIGN.get(), HangingSignRenderer::new);
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.HAMMER_FORGE.get(), HammerForgeRenderer::new);
    }


}