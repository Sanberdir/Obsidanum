package net.rezolv.obsidanum.world.custom_placer_trees;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.List;
import java.util.function.BiConsumer;

public class ObsidanTrunkPlacer extends TrunkPlacer {
    public static final Codec<ObsidanTrunkPlacer> CODEC = RecordCodecBuilder.create(instance ->
            trunkPlacerParts(instance).apply(instance, ObsidanTrunkPlacer::new));

    public ObsidanTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ObsidanumPlacers.OBSIDAN_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(
            LevelSimulatedReader level,
            BiConsumer<BlockPos, BlockState> blockSetter,
            RandomSource random,
            int treeHeight,
            BlockPos startPos,
            TreeConfiguration config
    ) {
        List<FoliagePlacer.FoliageAttachment> foliageAttachments = Lists.newArrayList();

        // Подготовка земли под деревом (4 блока)
        BlockPos dirtPos = startPos.below();
        setDirtAt(level, blockSetter, random, dirtPos, config);
        setDirtAt(level, blockSetter, random, dirtPos.east(), config);
        setDirtAt(level, blockSetter, random, dirtPos.south(), config);
        setDirtAt(level, blockSetter, random, dirtPos.south().east(), config);

        // Устанавливаем высоту дерева 4-5 блоков
        int trunkHeight = 4 + random.nextInt(2);
        int startX = startPos.getX();
        int startY = startPos.getY();
        int startZ = startPos.getZ();

        // Генерация основного ствола (2x2)
        for (int height = 0; height < trunkHeight; height++) {
            BlockPos logPos = new BlockPos(startX, startY + height, startZ);
            placeLog(level, blockSetter, random, logPos, config);
            placeLog(level, blockSetter, random, logPos.east(), config);
            placeLog(level, blockSetter, random, logPos.south(), config);
            placeLog(level, blockSetter, random, logPos.east().south(), config);
        }

        BlockPos topCenter = startPos.above(trunkHeight - 1).offset(1, 0, 1);
        foliageAttachments.add(new FoliagePlacer.FoliageAttachment(topCenter, 3, true));

        // Генерация 4-6 основных ветвей
        int numBranches = 4 + random.nextInt(3);
        for (int i = 0; i < numBranches; i++) {
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int branchLength = 2 + random.nextInt(2); // Длина 2-3 блока

            BlockPos branchStart = topCenter.below(random.nextInt(2));
            BlockPos currentPos = branchStart;

            // Строим изогнутую ветвь
            for (int j = 0; j < branchLength; j++) {
                // Плавный изгиб
                if (j > 0 && random.nextFloat() < 0.3f) {
                    direction = direction.getClockWise();
                }

                // Плавный подъем
                int yOffset = (j == branchLength - 1) ? 0 : random.nextInt(2);
                currentPos = currentPos.offset(
                        direction.getStepX(),
                        yOffset,
                        direction.getStepZ()
                );

                placeLog(level, blockSetter, random, currentPos, config);

                // Добавляем мини-кроны на концы ветвей
                if (j == branchLength - 1) {
                    foliageAttachments.add(new FoliagePlacer.FoliageAttachment(
                            currentPos.above(),
                            1 + random.nextInt(2),
                            false
                    ));
                }
            }
        }

        return foliageAttachments;
    }
}