package net.rezolv.obsidanum.structures.processors;


import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class RSProcessors {
    // Создаём DeferredRegister для StructureProcessorType
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSOR = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, "obsidanum");

    // Регистрируем WATERLOGGING_FIX_PROCESSOR
    public static final RegistryObject<StructureProcessorType<WaterloggingFixProcessor>> WATERLOGGING_FIX_PROCESSOR = STRUCTURE_PROCESSOR.register(
            "waterlogging_fix_processor",
            () -> () -> WaterloggingFixProcessor.CODEC
    );

    public static final RegistryObject<StructureProcessorType<ScrollShelfProcessor>> SCROLL_SHELF_PROCESSOR = STRUCTURE_PROCESSOR.register(
            "scroll_shelf_processor",
            () -> () -> ScrollShelfProcessor.CODEC
    );
}