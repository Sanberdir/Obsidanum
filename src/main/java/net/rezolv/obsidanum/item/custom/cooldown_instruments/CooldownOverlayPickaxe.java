package net.rezolv.obsidanum.item.custom.cooldown_instruments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.rezolv.obsidanum.item.custom.ObsidanHoe;
import net.rezolv.obsidanum.item.custom.ObsidanPickaxe;
import net.rezolv.obsidanum.item.custom.ObsidanPickaxe;

public class CooldownOverlayPickaxe {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static final IGuiOverlay COOLDOWN_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        if (minecraft.player == null || minecraft.level == null) return;

        ItemStack mainHandItem = minecraft.player.getMainHandItem();
        ItemStack offHandItem = minecraft.player.getOffhandItem();
        ItemStack hoeStack = null;
        boolean inMain = false;

        if (mainHandItem.getItem() instanceof ObsidanPickaxe) {
            hoeStack = mainHandItem;
            inMain = true;
        } else if (offHandItem.getItem() instanceof ObsidanPickaxe) {
            hoeStack = offHandItem;
            inMain = false;
        }

        if (hoeStack != null) {
            long currentTime = minecraft.level.getGameTime();
            long cooldownEnd = hoeStack.getOrCreateTag().getLong("CooldownEndTime");

            if (currentTime < cooldownEnd) {
                float progress = 1.0f - (float)(cooldownEnd - currentTime) / ObsidanPickaxe.COOLDOWN_DURATION;
                renderCooldown(guiGraphics, screenWidth, screenHeight, inMain, progress);
            }
        }
    };


    private static void renderCooldown(GuiGraphics guiGraphics, int width, int height, boolean inMainHand, float progress) {
        int slotSize = 16;
        int offset = 3;
        int x, y = height - 22 + offset;
        if (inMainHand) {
            int selected = minecraft.player.getInventory().selected;
            x = (width / 2) - 91 + selected * 20 + offset;
        } else {
            x = (width / 2) + 91 + 2 + offset;
        }

        int backgroundColor = 0x00000000;
        int fillColor = 0x99cccccc;

        // Фон (прозрачный)
        guiGraphics.fill(x, y, x + slotSize, y + slotSize, backgroundColor);

        // Высота заполнения, оставляем 2 пикселя для полоски прочности
        int maxFillHeight = slotSize - 2;
        int filledHeight = (int) (maxFillHeight * progress);
        int startY = y + (maxFillHeight - filledHeight);

        // Рисуем заполнение КД, не затрагивая нижние 2 пикселя
        guiGraphics.fill(x, startY, x + slotSize, startY + filledHeight, fillColor);
    }
}