package net.rezolv.obsidanum.item.upgrade;

public final class UpgradeLibrary {

    private UpgradeLibrary() {}

    // Получаем макс уровеньь для
    public static int getMaxLevel(ObsidanumToolUpgrades upgrade) {
        if (upgrade == ObsidanumToolUpgrades.RICH_HARVEST) {
            return 3;
        }
        return 1;
    }

    // Богатый урожай уровни
    public static int getRichHarvestMultiplier(int level) {
        switch (level) {
            case 1: return 2;
            case 2: return 3;
            case 3: return 4;
            default:
                throw new IllegalArgumentException("Invalid Rich Harvest level: " + level);
        }
    }

}