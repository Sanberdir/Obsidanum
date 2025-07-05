package net.rezolv.obsidanum.gui.forge_crucible.destruction_render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.render_types.ScrollItemRenderer;
import java.util.HashMap;

public class ForgeCrucibleDestructionScreen extends AbstractContainerScreen<ForgeCrucibleDestructionMenu> {
    private final static HashMap<String, Object> guistate = ForgeCrucibleDestructionMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;

    private static final ResourceLocation TEXTURE = new ResourceLocation("obsidanum:textures/gui/forge_crucible_menu_destruction.png");

    public ForgeCrucibleDestructionScreen(ForgeCrucibleDestructionMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        ScrollItemRenderer.render(guiGraphics, this.font, world, x, y, z, leftPos, topPos);
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Можно добавить текст здесь если нужно
    }

}