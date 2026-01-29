package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.structure.AbandonedLibraryStructure;
import art.arcane.mystcraft.world.structure.ScatteredLibraryStructure;
import art.arcane.mystcraft.world.structure.UndergroundArchiveStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.function.Supplier;

/**
 * Registers Mystcraft structure types for Fabric 1.20.1.
 */
public final class FabricModStructures {

    public static Supplier<StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY;
    public static Supplier<StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE;
    public static Supplier<StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY;

    private FabricModStructures() {
    }

    public static void register() {
        StructureType<AbandonedLibraryStructure> abandonedLibrary = () -> AbandonedLibraryStructure.CODEC;
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
                new ResourceLocation(Mystcraft.MOD_ID, "abandoned_library"), abandonedLibrary);
        ABANDONED_LIBRARY = () -> abandonedLibrary;

        StructureType<UndergroundArchiveStructure> undergroundArchive = () -> UndergroundArchiveStructure.CODEC;
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
                new ResourceLocation(Mystcraft.MOD_ID, "underground_archive"), undergroundArchive);
        UNDERGROUND_ARCHIVE = () -> undergroundArchive;

        StructureType<ScatteredLibraryStructure> scatteredLibrary = () -> ScatteredLibraryStructure.CODEC;
        Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
                new ResourceLocation(Mystcraft.MOD_ID, "scattered_library"), scatteredLibrary);
        SCATTERED_LIBRARY = () -> scatteredLibrary;

        Mystcraft.LOGGER.info("[Mystcraft] Registered 3 structure types");
    }
}
