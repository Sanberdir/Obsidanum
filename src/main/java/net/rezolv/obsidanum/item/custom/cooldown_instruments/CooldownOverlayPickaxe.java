package net.rezolv.obsidanum.item.custom.cooldown_instruments;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.rezolv.obsidanum.item.custom.ObsidanPickaxe;
import net.rezolv.obsidanum.item.custom.ObsidanPickaxe;

public class CooldownOverlayPickaxe {
    private static final Minecraft minecraft = Minecraft.getInstance();

    public static final IGuiOverlay COOLDOWN_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        if (minecraft.player == null || minecraft.level == null) return;

        ItemStack mainHandItem = minecraft.player.getMainHandItem();
        ItemStack offHandItem = minecraft.player.getOffhandItem();
        ItemStack pickaxeStack = null;
        boolean inMain = false;

        if (mainHandItem.getItem() instanceof ObsidanPickaxe) {
            pickaxeStack = mainHandItem;
            inMain = true;
        } else if (offHandItem.getItem() instanceof ObsidanPickaxe) {
            pickaxeStack = offHandItem;
            inMain = false;
        }

        if (pickaxeStack != null) {
            long currentTime = minecraft.level.getGameTime();
            long cooldownEnd = pickaxeStack.getOrCreateTag().getLong("CooldownEndTime");

            if (currentTime < cooldownEnd) {
                float progress = 1.0f - (float)(cooldownEnd - currentTime) / ObsidanPickaxe.COOLDOWN_DURATION;
                renderCooldown(guiGraphics, screenWidth, screenHeight, inMain, progress);
            } else {
                renderReady(guiGraphics, screenWidth, screenHeight, inMain);
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

        int backgroundColor = 0x00000000; // 25% dark
        int fillColor = 0xA88A2BE2;      // 50% violet

        // фон
        guiGraphics.fill(x, y, x + slotSize, y + slotSize, backgroundColor);
        // вертикальное заполнение сверху вниз
        if (progress > 0f) {
            int filled = (int)(slotSize * progress);
            guiGraphics.fill(x, y, x + slotSize, y + filled, fillColor);
        }
    }

    private static void renderReady(GuiGraphics guiGraphics, int width, int height, boolean inMainHand) {
        int slotSize = 16;
        int offset = 3;
        int x, y = height - 22 + offset;
        if (inMainHand) {
            int selected = minecraft.player.getInventory().selected;
            x = (width / 2) - 91 + selected * 20 + offset;
        } else {
            x = (width / 2) + 91 + 2 + offset;
        }

        int readyColor = 0x4008e8de; // 50% green
        guiGraphics.fill(x, y, x + slotSize, y + slotSize, readyColor);
    }
}