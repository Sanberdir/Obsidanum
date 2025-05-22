package net.rezolv.obsidanum.effect.effects.effect_overlay;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.rezolv.obsidanum.effect.EffectsObs;

public class ConfusionOverlay {
    private static final ResourceLocation[] OVERLAY_TEXTURES = {
            new ResourceLocation("obsidanum", "textures/overlay/morok_stage_1.png"),
            new ResourceLocation("obsidanum", "textures/overlay/morok_stage_2.png")
    };

    public static final IGuiOverlay CONFUSION_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        MobEffectInstance effect = minecraft.player.getEffect(EffectsObs.FLASH.get());
        if (effect == null) return;

        int amplifier = effect.getAmplifier(); // 0 для уровня I, 1 для уровня II

        // Для первого уровня - только первый слой с прозрачностью 0.7
        if (amplifier == 0) {
            drawOverlay(guiGraphics, OVERLAY_TEXTURES[0], 0.7f, screenWidth, screenHeight);
        }
        // Для второго уровня - оба слоя с полной прозрачностью
        else if (amplifier == 1) {
            drawOverlay(guiGraphics, OVERLAY_TEXTURES[0], 1.0f, screenWidth, screenHeight);
            drawOverlay(guiGraphics, OVERLAY_TEXTURES[1], 1.0f, screenWidth, screenHeight);
        }
    };

    private static void drawOverlay(GuiGraphics guiGraphics, ResourceLocation texture, float alpha, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(0, height, 0).uv(0, 1).endVertex();
        buffer.vertex(width, height, 0).uv(1, 1).endVertex();
        buffer.vertex(width, 0, 0).uv(1, 0).endVertex();
        buffer.vertex(0, 0, 0).uv(0, 0).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}