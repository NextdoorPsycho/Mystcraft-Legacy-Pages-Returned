package art.arcane.mystcraft.world.structure;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers Mystcraft structure types.
 */
public class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Mystcraft.MOD_ID);

    /**
     * Abandoned Library - surface structure that spawns in plains/forest biomes.
     * Contains symbol pages, descriptive books, and Mystcraft workstations.
     */
    public static final RegistryObject<StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY =
            STRUCTURE_TYPES.register("abandoned_library",
                    () -> () -> AbandonedLibraryStructure.CODEC);

    /**
     * Underground Archive - underground structure with rare symbols.
     */
    public static final RegistryObject<StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE =
            STRUCTURE_TYPES.register("underground_archive",
                    () -> () -> UndergroundArchiveStructure.CODEC);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}
