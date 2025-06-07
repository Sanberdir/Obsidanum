package net.rezolv.obsidanum.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.gui.hammer_forge.recipes_render.HammerForgeGuiMenu;

public class ObsidanumMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Obsidanum.MOD_ID);
    public static final RegistryObject<MenuType<HammerForgeGuiMenu>> HAMMER_FORGE_GUI = REGISTRY.register("hammer_forge_gui", () -> IForgeMenuType.create(HammerForgeGuiMenu::new));
}
