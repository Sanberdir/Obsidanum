package net.rezolv.obsidanum.world.custom_placer_trees;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public class ObsidanFoliagePlacer extends FoliagePlacer {
    public static final Codec<ObsidanFoliagePlacer> CODEC = RecordCodecBuilder.create(instance ->
            foliagePlacerParts(instance)
                    .apply(instance, ObsidanFoliagePlacer::new));

    public ObsidanFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ObsidanumPlacers.OBSIDAN_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(
            LevelSimulatedReader level,
            FoliageSetter foliageSetter,
            RandomSource random,
            TreeConfiguration config,
            int maxTreeHeight,
            FoliageAttachment attachment,
            int foliageRadius,
            int foliageOffset,
            int foliageHeight
    ) {
        BlockPos centerPos = attachment.pos();
        int radius = attachment.radiusOffset() + random.nextInt(2);

        // Плотная шарообразная крона как у дуба
        for (int y = -1; y <= 2; y++) {
            int layerRadius = radius;

            if (y < 0) {
                layerRadius -= 1; // Нижний слой меньше
            } else if (y > 0) {
                layerRadius = Math.max(1, radius - y); // Сужаем кверху
            }

            this.placeLeavesRow(level, foliageSetter, random, config, centerPos.above(y), layerRadius, 0, false);
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int treeHeight, TreeConfiguration config) {
        return 3; // Фиксированная высота кроны
    }

    @Override
    protected boolean shouldSkipLocationSigned(
            RandomSource random,
            int relativeX,
            int relativeY,
            int relativeZ,
            int radius,
            boolean isDoubleTrunk
    ) {
        // Меньше пропусков для плотной кроны (только 10% случайных пропусков)
        return random.nextFloat() < 0.1f;
    }

    @Override
    protected boolean shouldSkipLocation(
            RandomSource random,
            int relativeX,
            int relativeY,
            int relativeZ,
            int radius,
            boolean isDoubleTrunk
    ) {
        // Сохраняем естественные пропуски по краям
        if (relativeY == -1) {
            return relativeX == radius && relativeZ == radius;
        }
        return relativeX + relativeZ > radius * 2 - 1;
    }
}