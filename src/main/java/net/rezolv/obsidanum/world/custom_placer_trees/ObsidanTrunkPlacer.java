package net.rezolv.obsidanum.world.custom_placer_trees;

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

import java.util.ArrayList;
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
            int freeTreeHeight,
            BlockPos pos,
            TreeConfiguration config
    ) {
        setDirtAt(level, blockSetter, random, pos.below(), config);

        List<FoliagePlacer.FoliageAttachment> foliageAttachments = new ArrayList<>();
        int height = freeTreeHeight + 2 + random.nextInt(5); // base +2–6 extra

        // Main trunk
        for (int i = 0; i < height; i++) {
            placeLog(level, blockSetter, random, pos.above(i), config);
        }

        // Top foliage
        BlockPos top = pos.above(height);
        foliageAttachments.add(new FoliagePlacer.FoliageAttachment(top, 0, false));

        // Four branch levels, branches start directly from trunk
        int branchLevels = 4;
        for (int lvl = 1; lvl <= branchLevels; lvl++) {
            int y = height - lvl * (height / (branchLevels + 1)) - random.nextInt(2);
            BlockPos trunkPoint = pos.above(y);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                generateBranch(level, blockSetter, random, trunkPoint, dir, foliageAttachments, config);
            }
        }

        return foliageAttachments;
    }

    private void generateBranch(
            LevelSimulatedReader level,
            BiConsumer<BlockPos, BlockState> blockSetter,
            RandomSource random,
            BlockPos start,
            Direction dir,
            List<FoliagePlacer.FoliageAttachment> foliageAttachments,
            TreeConfiguration config
    ) {
        BlockPos current = start;
        int length = 2 + random.nextInt(4); // 2–5 blocks long

        // Grow branch with natural kinks
        for (int i = 0; i < length; i++) {
            current = current.relative(dir);
            if (random.nextFloat() < 0.3f) {
                current = current.above();
            } else if (random.nextFloat() < 0.2f) {
                current = current.below();
            }
            placeLog(level, blockSetter, random, current, config);
        }

        // Attach foliage at end
        BlockPos leafPos = current.above();
        placeLog(level, blockSetter, random, leafPos, config);
        foliageAttachments.add(new FoliagePlacer.FoliageAttachment(leafPos, 0, false));

        // Small side twig
        if (random.nextBoolean()) {
            Direction twigDir = Direction.Plane.HORIZONTAL.stream()
                    .filter(d -> d != dir && d != dir.getOpposite())
                    .skip(random.nextInt(2)).findFirst().orElse(dir);
            BlockPos twigBase = start.relative(dir, (length / 2));
            BlockPos twigCurrent = twigBase;
            int twigLen = 1 + random.nextInt(2);
            for (int i = 0; i < twigLen; i++) {
                twigCurrent = twigCurrent.relative(twigDir);
                placeLog(level, blockSetter, random, twigCurrent, config);
            }
            BlockPos twigLeaf = twigCurrent.above();
            placeLog(level, blockSetter, random, twigLeaf, config);
            foliageAttachments.add(new FoliagePlacer.FoliageAttachment(twigLeaf, 0, false));
        }
    }
}