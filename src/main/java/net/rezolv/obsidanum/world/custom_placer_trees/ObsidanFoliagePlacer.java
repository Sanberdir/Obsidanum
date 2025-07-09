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
                    .and(Codec.intRange(0, 16).fieldOf("height").forGetter(fp -> fp.height))
                    .apply(instance, ObsidanFoliagePlacer::new));

    private final int height;

    public ObsidanFoliagePlacer(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset);
        this.height = height;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ObsidanumPlacers.OBSIDAN_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader levelSimulatedReader, FoliageSetter foliageSetter, RandomSource randomSource, TreeConfiguration treeConfiguration, int i, FoliageAttachment foliageAttachment, int i1, int i2, int i3) {
        {
            BlockPos center = foliageAttachment.pos();

            // Генерация пышной шарообразной листвы
            for (int y = -1; y <= 1; y++) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        int distance = Math.abs(x) + Math.abs(z);
                        if ((distance <= 2 && y == 0) || (distance <= 1 && y != 0)) {
                            placeLeavesRow(levelSimulatedReader, foliageSetter, randomSource, treeConfiguration,
                                    center.offset(x, y, z), 0, 0, foliageAttachment.doubleTrunk());
                        }
                    }
                }
            }
        }
    }



    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return this.height;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return false;
    }
}