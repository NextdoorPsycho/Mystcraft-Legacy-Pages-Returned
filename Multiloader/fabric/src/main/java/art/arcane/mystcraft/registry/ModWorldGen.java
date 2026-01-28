package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.function.Supplier;

/**
 * Registers world generation components for Mystcraft Ages (Fabric).
 */
public final class ModWorldGen {

    public static final Supplier<Codec<? extends ChunkGenerator>> AGE_CHUNK_GENERATOR;
    public static final Supplier<Codec<? extends BiomeSource>> AGE_BIOME_SOURCE;

    static {
        Codec<? extends ChunkGenerator> chunkGenCodec = AgeChunkGenerator.CODEC;
        AGE_CHUNK_GENERATOR = () -> chunkGenCodec;

        Codec<? extends BiomeSource> biomeSourceCodec = AgeBiomeSource.CODEC;
        AGE_BIOME_SOURCE = () -> biomeSourceCodec;
    }

    private ModWorldGen() {
    }

    /**
     * Registers chunk generator and biome source codecs.
     */
    public static void register() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                new ResourceLocation(Mystcraft.MOD_ID, "age_chunk_generator"), AGE_CHUNK_GENERATOR.get());
        Registry.register(BuiltInRegistries.BIOME_SOURCE,
                new ResourceLocation(Mystcraft.MOD_ID, "age_biome_source"), AGE_BIOME_SOURCE.get());
    }
}
