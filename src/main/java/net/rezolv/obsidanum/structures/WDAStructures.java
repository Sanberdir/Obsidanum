package net.rezolv.obsidanum.structures;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;

public class WDAStructures {

    public static final DeferredRegister<StructureType<?>> DEFERRED_REGISTRY_STRUCTURE = DeferredRegister.create(Registries.STRUCTURE_TYPE, Obsidanum.MOD_ID);

    public static final RegistryObject<StructureType<WDAGenericStructures>> GENERIC_STRUCTURES = DEFERRED_REGISTRY_STRUCTURE.register("generic_structures", () -> () -> WDAGenericStructures.CODEC);
}