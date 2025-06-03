package net.rezolv.obsidanum.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.rezolv.obsidanum.block.entity.ForgeCrucibleEntity;

import java.util.HashMap;

public class HammerForgeGuiScreen extends AbstractContainerScreen<HammerForgeGuiMenu> {
	private final static HashMap<String, Object> guistate = HammerForgeGuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	private static final ResourceLocation TEXTURE = new ResourceLocation("obsidanum:textures/gui/hammer_forge_menu.png");

	public HammerForgeGuiScreen(HammerForgeGuiMenu container, Inventory inventory, Component text) {
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
		HammerForgeGuiRenderer.renderBackground(guiGraphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, TEXTURE);
		HammerForgeGuiRenderer.renderScrollItem(guiGraphics, this.font, world, x, y, z, leftPos, topPos);
		HammerForgeGuiRenderer.renderRecipeIngredients(guiGraphics, this.font, menu, world, new BlockPos(x, y, z), leftPos, topPos);
		HammerForgeGuiRenderer.renderRecipeResult(guiGraphics, this.font, menu.getBlockEntity(), leftPos, topPos);

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

	@Override
	public void init() {
		super.init();
	}
}