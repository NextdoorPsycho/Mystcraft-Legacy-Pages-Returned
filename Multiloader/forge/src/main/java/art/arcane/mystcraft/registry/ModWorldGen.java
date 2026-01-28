package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers world generation components for Mystcraft Ages.
 */
public class ModWorldGen {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Mystcraft.MOD_ID);

    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Mystcraft.MOD_ID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> AGE_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("age_chunk_generator", () -> AgeChunkGenerator.CODEC);

    public static final RegistryObject<Codec<? extends BiomeSource>> AGE_BIOME_SOURCE =
            BIOME_SOURCES.register("age_biome_source", () -> AgeBiomeSource.CODEC);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
