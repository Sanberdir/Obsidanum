package net.rezolv.obsidanum.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.gui.forge_crucible.destruction_render.ForgeCrucibleDestructionMenu;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiMenu;
import net.rezolv.obsidanum.gui.forge_crucible.repair_render.ForgeCrucibleRepairMenu;
import net.rezolv.obsidanum.gui.forge_crucible.upgrade_render.ForgeCrucibleUpgradeMenu;

public class ObsidanumMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Obsidanum.MOD_ID);

    public static final RegistryObject<MenuType<ForgeCrucibleGuiMenu>> FORGE_CRUCIBLE_GUI = REGISTRY.register("forge_crucible_gui",
            () -> IForgeMenuType.create(ForgeCrucibleGuiMenu::new));

    public static final RegistryObject<MenuType<ForgeCrucibleUpgradeMenu>> FORGE_CRUCIBLE_GUI_UPGRADE = REGISTRY.register("forge_crucible_gui_upgrade",
            () -> IForgeMenuType.create(ForgeCrucibleUpgradeMenu::new));

    public static final RegistryObject<MenuType<ForgeCrucibleRepairMenu>> FORGE_CRUCIBLE_GUI_REPAIR = REGISTRY.register("forge_crucible_gui_repair",
            () -> IForgeMenuType.create(ForgeCrucibleRepairMenu::new));

    public static final RegistryObject<MenuType<ForgeCrucibleDestructionMenu>> FORGE_CRUCIBLE_GUI_DESTRUCTION = REGISTRY.register("forge_crucible_gui_destruction",
            () -> IForgeMenuType.create(ForgeCrucibleDestructionMenu::new));
}
