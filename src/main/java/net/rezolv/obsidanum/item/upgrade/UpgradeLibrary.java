package net.rezolv.obsidanum.item.upgrade;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

public final class UpgradeLibrary {

    private UpgradeLibrary() {}


    // Шансы для археолога
    // Шансы для археолога
    private static final float ARCHAEOLOGIST_LEVEL_1_CHANCE = 0.10f; // 10%
    private static final float ARCHAEOLOGIST_LEVEL_2_CHANCE = 0.25f; // 25%

    // Лут для разных типов блоков
    private static final Map<Block, Item[]> ARCHAEOLOGIST_LOOT_TABLE = Map.of(
            Blocks.DIRT, new Item[] {
                    Items.BONE,
                    Items.COAL,
                    Items.ARROW,
                    Items.FLINT
            },
            Blocks.SAND, new Item[] {
                    Items.GOLD_NUGGET,
                    Items.IRON_NUGGET,
                    Items.NAUTILUS_SHELL,
                    Items.CANDLE
            },
            Blocks.GRAVEL, new Item[] {
                    Items.FLINT,
                    Items.IRON_NUGGET,
                    Items.GOLD_NUGGET,
                    Items.ARROW
            },
            Blocks.SOUL_SAND, new Item[] {
                    Items.BONE,
                    Items.ARROW,
                    Items.GUNPOWDER,
                    Items.BLAZE_POWDER
            },
            Blocks.SOUL_SOIL, new Item[] {
                    Items.BONE,
                    Items.BLAZE_POWDER,
                    Items.ARROW,
                    Items.QUARTZ
            }
    );

    public static boolean tryFindArchaeologistLoot(int archaeologistLevel, RandomSource random) {
        if (archaeologistLevel == 1) {
            return random.nextFloat() < ARCHAEOLOGIST_LEVEL_1_CHANCE;
        } else if (archaeologistLevel >= 2) {
            return random.nextFloat() < ARCHAEOLOGIST_LEVEL_2_CHANCE;
        }
        return false;
    }

    public static Item getRandomArchaeologistLoot(Block block, RandomSource random) {
        Item[] possibleLoot = ARCHAEOLOGIST_LOOT_TABLE.get(block);
        if (possibleLoot == null || possibleLoot.length == 0) {
            return Items.AIR;
        }
        return possibleLoot[random.nextInt(possibleLoot.length)];
    }

    // Получаем макс уровеньь для
    public static int getMaxLevel(ObsidanumToolUpgrades upgrade) {
        switch (upgrade) {
            case LONG_HANDLE:
            case ARCHAEOLOGIST:
            case HARVESTER:
                return 2;
            case RICH_HARVEST:
                return 3;
            case STRENGTH:
            case BALANCING:
            case SHARPENING:
                return 10;
            default:
                return 1;
        }
    }

    private static final float HARVESTER_LEVEL_1 = 0.2f; // 20%
    private static final float HARVESTER_LEVEL_2 = 0.4f; // 40%
    public static boolean shouldDoubleDrops(int harvesterLevel, RandomSource random) {
        if (harvesterLevel == 1) {
            return random.nextFloat() < HARVESTER_LEVEL_1;
        } else if (harvesterLevel >= 2) {
            return random.nextFloat() < HARVESTER_LEVEL_2;
        }
        return false;
    }

    public static int getLongHandleBonus(int level) {
        switch (level) {
            case 1: return 2;   // +1 блок
            case 2: return 4;   // +2 блока
            default: return 0;
        }
    }
}