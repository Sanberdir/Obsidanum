package net.rezolv.obsidanum.structures.processors;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.rezolv.obsidanum.block.custom.Scroolshelf;

public class ScrollShelfProcessor extends StructureProcessor {
    private static final float SCROLL_CHANCE = 0.15f;

    public static final Codec<ScrollShelfProcessor> CODEC = Codec.unit(ScrollShelfProcessor::new);

    private ScrollShelfProcessor() {}

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos pos, BlockPos pos2,
                                                             StructureTemplate.StructureBlockInfo infoIn1, StructureTemplate.StructureBlockInfo infoIn2,
                                                             StructurePlaceSettings settings) {

        BlockState state = infoIn2.state();

        // Проверяем, что это блок Scroolshelf и в нем нет свитка
        if (state.getBlock() instanceof Scroolshelf && !state.getValue(Scroolshelf.SCROLL_PRESENT)) {
            // Используем рандом из настроек размещения
            if (settings.getRandom(infoIn2.pos()).nextFloat() < SCROLL_CHANCE) {
                // Создаем новое состояние со свитком
                BlockState newState = state
                        .setValue(Scroolshelf.HAS_SCROLL, true)
                        .setValue(Scroolshelf.SCROLL_PRESENT, true);

                return new StructureTemplate.StructureBlockInfo(
                        infoIn2.pos(),
                        newState,
                        infoIn2.nbt() // Сохраняем оригинальные NBT-данные
                );
            }
        }

        return infoIn2;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return RSProcessors.SCROLL_SHELF_PROCESSOR.get(); // Замените на ваш RegistryObject
    }
}