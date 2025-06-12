package net.rezolv.obsidanum.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiMenu;
import net.rezolv.obsidanum.gui.forge_crucible.repair_render.ForgeCrucibleRepairMenu;

public class ObsidanumMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Obsidanum.MOD_ID);

    public static final RegistryObject<MenuType<ForgeCrucibleGuiMenu>> FORGE_CRUCIBLE_GUI = REGISTRY.register("forge_crucible_gui",
            () -> IForgeMenuType.create(ForgeCrucibleGuiMenu::new));

    public static final RegistryObject<MenuType<ForgeCrucibleRepairMenu>> FORGE_CRUCIBLE_GUI_REPAIR = REGISTRY.register("forge_crucible_gui_repair",
            () -> IForgeMenuType.create(ForgeCrucibleRepairMenu::new));
}
