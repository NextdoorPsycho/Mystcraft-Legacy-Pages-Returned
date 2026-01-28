package art.arcane.mystcraft.world.structure;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;

/** Registers Mystcraft structure types. */
public class NeoForgeModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Mystcraft.MOD_ID);

    /** Abandoned Library - surface structure that spawns in plains/forest biomes. */
    public static final DeferredHolder<StructureType<?>, StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY =
            STRUCTURE_TYPES.register("abandoned_library",
                    () -> () -> AbandonedLibraryStructure.CODEC);

    /** Underground Archive - underground structure with rare symbols. */
    public static final DeferredHolder<StructureType<?>, StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE =
            STRUCTURE_TYPES.register("underground_archive",
                    () -> () -> UndergroundArchiveStructure.CODEC);

    /** Scattered Library - small standalone library buildings scattered across the overworld. */
    public static final DeferredHolder<StructureType<?>, StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY =
            STRUCTURE_TYPES.register("scattered_library",
                    () -> () -> ScatteredLibraryStructure.CODEC);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
    }
}
