package net.rezolv.obsidanum.item.upgrade;

public final class UpgradeLibrary {

    private UpgradeLibrary() {}

    // Получаем макс уровеньь для
    public static int getMaxLevel(ObsidanumToolUpgrades upgrade) {
        switch (upgrade) {
            case RICH_HARVEST:
            case STRENGTH:
                return 3;
            default:
                return 1;
        }
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
    public static float getStrengthChance(int level) {
        switch (level) {
            case 1: return 0.3f; // 30%
            case 2: return 0.5f; // 50%
            case 3: return 0.7f; // 70%
            default:
                throw new IllegalArgumentException("Invalid Strength level: " + level);
        }
    }
}