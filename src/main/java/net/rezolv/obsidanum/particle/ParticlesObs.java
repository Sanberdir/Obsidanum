package net.rezolv.obsidanum.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;

public class ParticlesObs {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Obsidanum.MOD_ID);

    public static final RegistryObject<SimpleParticleType> NETHER_FLAME_PARTICLES =
            PARTICLE_TYPES.register("nether_flame_particles", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> GLINT_PURPLE_PARTICLES =
            PARTICLE_TYPES.register("glint_purple_particles", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> GLINT_BLUE_PARTICLES =
            PARTICLE_TYPES.register("glint_blue_particles", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> GLINT_CRIMSON_PARTICLES =
            PARTICLE_TYPES.register("glint_crimson_particles", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> NETHER_FLAME_PROJECTILE_PARTICLES =
            PARTICLE_TYPES.register("nether_flame_projectile_particles", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> BAGELL_FLAME_PARTICLES =
            PARTICLE_TYPES.register("bagell_flame_particles", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> NETHER_FLAME2_PARTICLES =
            PARTICLE_TYPES.register("nether_flame2_particles", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BAGELL_TABLE_PARTICLES =
            PARTICLE_TYPES.register("bagell_table_particles", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }

}
