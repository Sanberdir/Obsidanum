package net.rezolv.obsidanum.effect.effects.effect_overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rezolv.obsidanum.effect.EffectsObs;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CustomHeartsRenderer {
    private static final ResourceLocation CUSTOM_HEARTS =
            new ResourceLocation("obsidanum", "textures/gui/custom_hearts.png");

    // Размеры сердца на текстуре
    private static final int HEART_WIDTH = 9;
    private static final int HEART_HEIGHT = 9;

    // UV-координаты (текстура: пустое | полное | половинчатое)
    private static final int U_EMPTY = 0;    // Пустое (0, 0)
    private static final int U_FULL = 9;     // Полное (9, 0)
    private static final int U_HALF = 18;    // Половинчатое (18, 0)
    private static final int V_ALL = 0;

    @SubscribeEvent
    public static void onRenderHealthBar(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            // Проверяем, что игрок не в креативном режиме
            if (player.isCreative() || player.isSpectator()) {
                return; // Не отображаем сердца в креативе или режиме наблюдателя
            }

            MobEffectInstance flashEffect = player.getEffect(EffectsObs.MOROK.get());
            if (flashEffect != null) {
                event.setCanceled(true);
                drawCustomHearts(
                        event.getGuiGraphics(),
                        player,
                        event.getWindow().getGuiScaledWidth(),
                        event.getWindow().getGuiScaledHeight()
                );
            }
        }
    }

    private static void drawCustomHearts(GuiGraphics guiGraphics, LocalPlayer player,
                                         int screenWidth, int screenHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float health = player.getHealth();
        int maxHealth = (int) player.getMaxHealth();
        int heartsCount = (int) Math.ceil(maxHealth / 2.0);

        int xBase = screenWidth / 2 - 91;
        int yBase = screenHeight - 39;

        // Уменьшаем расстояние на 2 пикселя (9 - 2 = 7 пикселей между сердцами)
        for (int i = 0; i < heartsCount; i++) {
            int x = xBase + i * (HEART_WIDTH - 1); // Было: i * HEART_WIDTH
            float remainingHealth = health - (i * 2);

            if (remainingHealth >= 2.0f) {
                blitHeart(guiGraphics, x, yBase, U_FULL, V_ALL);
            } else if (remainingHealth >= 1.0f) {
                blitHeart(guiGraphics, x, yBase, U_HALF, V_ALL);
            } else {
                blitHeart(guiGraphics, x, yBase, U_EMPTY, V_ALL);
            }
        }

        RenderSystem.disableBlend();
    }

    private static void blitHeart(GuiGraphics guiGraphics, int x, int y, int u, int v) {
        guiGraphics.blit(
                CUSTOM_HEARTS,
                x, y,
                u, v,
                HEART_WIDTH, HEART_HEIGHT,
                27, 9 // Размер текстуры 27x9
        );
    }
}