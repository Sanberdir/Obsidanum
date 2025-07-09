package net.rezolv.obsidanum.world.custom_placer_trees;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;

public class ObsidanumPlacers {

    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS =
            DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, Obsidanum.MOD_ID);

    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS =
            DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, Obsidanum.MOD_ID);

    public static final RegistryObject<TrunkPlacerType<ObsidanTrunkPlacer>> OBSIDAN_TRUNK_PLACER =
            TRUNK_PLACERS.register("obsidan_trunk_placer",
                    () -> new TrunkPlacerType<>(ObsidanTrunkPlacer.CODEC));

    public static final RegistryObject<FoliagePlacerType<ObsidanFoliagePlacer>> OBSIDAN_FOLIAGE_PLACER =
            FOLIAGE_PLACERS.register("obsidan_foliage_placer",
                    () -> new FoliagePlacerType<>(ObsidanFoliagePlacer.CODEC));
}