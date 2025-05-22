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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.rezolv.obsidanum.effect.EffectsObs;
import net.rezolv.obsidanum.item.item_entity.pot_grenade.fog.PotGrenadeFog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PreConfusionOverlay {
    private static final ResourceLocation[] OVERLAY_TEXTURES = {
            new ResourceLocation("obsidanum", "textures/overlay/morok_stage_1.png"),
            new ResourceLocation("obsidanum", "textures/overlay/morok_stage_2.png")
    };

    // Временные константы (в тиках)
    private static final float MAX_EXPOSURE_TIME_LVL1 = 5 * 20; // 5 секунд
    private static final float MAX_EXPOSURE_TIME_LVL2 = 10 * 20; // 10 секунд

    // Скорости изменения (можно настроить)
    private static final float FADE_IN_SPEED = 0.4f;  // Медленное появление
    private static final float FADE_OUT_SPEED = 2.5f; // Быстрое исчезновение

    private static final Map<UUID, PlayerFogState> playerStates = new HashMap<>();

    public static final IGuiOverlay PRE_CONFUSION_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        UUID playerId = player.getUUID();
        PlayerFogState state = playerStates.computeIfAbsent(playerId, k -> new PlayerFogState());

        boolean currentlyInFog = isPlayerInFog(player);
        if (currentlyInFog) {
            state.fogExposureTime = Mth.clamp(state.fogExposureTime + FADE_IN_SPEED, 0, MAX_EXPOSURE_TIME_LVL2);
        } else {
            state.fogExposureTime = Mth.clamp(state.fogExposureTime - FADE_OUT_SPEED, 0, MAX_EXPOSURE_TIME_LVL2);
        }

        if (state.fogExposureTime > 0) {
            // Первый слой с плавным появлением
            float stage1Progress = Mth.clamp(state.fogExposureTime / MAX_EXPOSURE_TIME_LVL1, 0, 1);
            float stage1Alpha = smoothStep(0.1f, 0.7f, stage1Progress);
            drawOverlay(guiGraphics, OVERLAY_TEXTURES[0], stage1Alpha, screenWidth, screenHeight);

            // Второй слой с еще более плавным появлением
            if (state.fogExposureTime > MAX_EXPOSURE_TIME_LVL1) {
                float stage2Progress = Mth.clamp(
                        (state.fogExposureTime - MAX_EXPOSURE_TIME_LVL1) /
                                (MAX_EXPOSURE_TIME_LVL2 - MAX_EXPOSURE_TIME_LVL1),
                        0, 1
                );
                float stage2Alpha = smoothStep(0.2f, 0.8f, stage2Progress);
                drawOverlay(guiGraphics, OVERLAY_TEXTURES[1], stage2Alpha, screenWidth, screenHeight);
            }
        }

        // Очистка неактивных состояний
        playerStates.keySet().removeIf(uuid -> {
            if (minecraft.level == null) return true;
            Player p = minecraft.level.getPlayerByUUID(uuid);
            return p == null || !p.isAlive();
        });
    };

    // Реализация smoothStep с использованием Mth
    private static float smoothStep(float edge0, float edge1, float x) {
        x = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return x * x * (3 - 2 * x);
    }

    private static boolean isPlayerInFog(Player player) {
        return !player.level().getEntitiesOfClass(PotGrenadeFog.class, player.getBoundingBox()).isEmpty();
    }

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

    private static class PlayerFogState {
        float fogExposureTime = 0;
    }
}