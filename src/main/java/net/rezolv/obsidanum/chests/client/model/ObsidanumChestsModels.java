package net.rezolv.obsidanum.chests.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.chests.block.ObsidanumChestsTypes;
import org.jetbrains.annotations.NotNull;

public class ObsidanumChestsModels {

  public static final ResourceLocation AZURE_OBSIDIAN_CHEST_LOCATION = new ResourceLocation(Obsidanum.MOD_ID, "model/azure/azure_obsidian_chest");
  public static final ResourceLocation OBSIDIAN_CHEST_LOCATION = new ResourceLocation(Obsidanum.MOD_ID, "model/obsidian/obsidian_chest");
  public static final ResourceLocation RUNIC_OBSIDIAN_CHEST_LOCATION = new ResourceLocation(Obsidanum.MOD_ID, "model/runic_obsidian_chest");
  public static final ResourceLocation VANILLA_CHEST_LOCATION = new ResourceLocation("entity/chest/normal");

  public static final ResourceLocation LOOTR_OBSIDIAN_CHEST =
          new ResourceLocation("obsidanum", "model/obsidian/lootr_normal");
  public static final ResourceLocation LOOTR_AZURE_CHEST =
          new ResourceLocation("obsidanum", "model/azure/lootr_normal");

  public static final ResourceLocation LOOTR_OPEN_OBSIDIAN_CHEST =
          new ResourceLocation("obsidanum", "model/obsidian/lootr_opened");
  public static final ResourceLocation LOOTR_OPEN_AZURE_CHEST =
          new ResourceLocation("obsidanum", "model/azure/lootr_opened");
  private static boolean isLootrLoaded() {
    return ModList.get().isLoaded("lootr"); // Только Forge
  }

  public static ResourceLocation chooseChestTexture(ObsidanumChestsTypes type) {
    return getResourceLocation(
            type,
            isLootrLoaded() ? LOOTR_AZURE_CHEST : AZURE_OBSIDIAN_CHEST_LOCATION,
            isLootrLoaded() ? LOOTR_OBSIDIAN_CHEST : OBSIDIAN_CHEST_LOCATION,
            RUNIC_OBSIDIAN_CHEST_LOCATION,
            VANILLA_CHEST_LOCATION // Динамическая замена
    );
  }

  @NotNull
  private static ResourceLocation getResourceLocation(ObsidanumChestsTypes type, ResourceLocation azureObsidianChestLocation, ResourceLocation obsidianChestLocation, ResourceLocation runicObsidianChestLocation, ResourceLocation vanillaChestLocation) {
    return switch (type) {
      case AZURE_OBSIDIAN -> azureObsidianChestLocation;
      case OBSIDIAN -> obsidianChestLocation;
      case RUNIC_OBSIDIAN -> runicObsidianChestLocation;
      default -> vanillaChestLocation;
    };
  }
}
