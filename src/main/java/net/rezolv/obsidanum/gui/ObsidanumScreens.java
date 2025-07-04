package net.rezolv.obsidanum.gui;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.rezolv.obsidanum.gui.forge_crucible.recipes_render.ForgeCrucibleGuiScreen;
import net.rezolv.obsidanum.gui.forge_crucible.repair_render.ForgeCrucibleRepairScreen;
import net.rezolv.obsidanum.gui.forge_crucible.upgrade_render.ForgeCrucibleUpgradeScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ObsidanumScreens {
    @SubscribeEvent
    public static void clientLoad(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ObsidanumMenus.FORGE_CRUCIBLE_GUI.get(), ForgeCrucibleGuiScreen::new);
            MenuScreens.register(ObsidanumMenus.FORGE_CRUCIBLE_GUI_UPGRADE.get(), ForgeCrucibleUpgradeScreen::new);
            MenuScreens.register(ObsidanumMenus.FORGE_CRUCIBLE_GUI_REPAIR.get(), ForgeCrucibleRepairScreen::new);
        });
    }
}
