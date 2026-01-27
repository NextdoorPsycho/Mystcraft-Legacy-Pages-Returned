package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers Mystcraft villager professions and POI types.
 * The Archivist profession specializes in Mystcraft items and symbol trades.
 */
public final class ModVillagers {

    /**
     * POI type for the Archivist workstation (Lectern).
     * Villagers will claim this block as their workstation.
     */
    public static final RegistryObject<PoiType> ARCHIVIST_POI = MystcraftRegistries.POI_TYPES.register(
            "archivist",
            () -> new PoiType(
                    ImmutableSet.copyOf(ModBlocks.LECTERN.get().getStateDefinition().getPossibleStates()),
                    1,  // Max tickets (how many villagers can use this)
                    1   // Valid range
            )
    );

    /**
     * The Archivist villager profession.
     * Archivists trade Mystcraft items like pages, ink, and books.
     */
    public static final RegistryObject<VillagerProfession> ARCHIVIST = MystcraftRegistries.VILLAGER_PROFESSIONS.register(
            "archivist",
            () -> new VillagerProfession(
                    "archivist",
                    holder -> holder.value() == ARCHIVIST_POI.get(),
                    holder -> holder.value() == ARCHIVIST_POI.get(),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_LIBRARIAN
            )
    );

    /**
     * Forces static initialization of this class.
     * Call from main mod constructor.
     */
    public static void register() {
        Mystcraft.LOGGER.info("Registering Mystcraft villagers");
    }

    private ModVillagers() {
    }
}
