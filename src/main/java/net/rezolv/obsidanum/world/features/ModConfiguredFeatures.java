package net.rezolv.obsidanum.world.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.FancyTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.block.BlocksObs;
import net.rezolv.obsidanum.world.custom_placer_trees.ObsidanFoliagePlacer;
import net.rezolv.obsidanum.world.custom_placer_trees.ObsidanTrunkPlacer;

import java.util.List;
import java.util.OptionalInt;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> OBSIDAN_TREE = registerKey("obsidan_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OVERWORLD_ONYX_KEY = registerKey("onyx");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        List<OreConfiguration.TargetBlockState> overworldOnyxOres = List.of(
                OreConfiguration.target(deepslateReplaceables, BlocksObs.ONYX.get().defaultBlockState()));

        register(context, OVERWORLD_ONYX_KEY, Feature.ORE, new OreConfiguration(overworldOnyxOres, 60));

        // 🔧 ВАЖНО: теперь дерево тоже регистрируется
        register(context, OBSIDAN_TREE, Feature.TREE, createObsidanTree().build());
    }
    private static TreeConfiguration.TreeConfigurationBuilder createObsidanTree() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(BlocksObs.OBSIDAN_WOOD_LOG.get()),
                new ObsidanTrunkPlacer(7, 5, 0), // Базовая высота 7, +0-5 случайных
                BlockStateProvider.simple(BlocksObs.OBSIDAN_WOOD_LEAVES.get()),
                new ObsidanFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 3),
                new TwoLayersFeatureSize(1, 0, 2)
        ).ignoreVines();
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(Obsidanum.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}