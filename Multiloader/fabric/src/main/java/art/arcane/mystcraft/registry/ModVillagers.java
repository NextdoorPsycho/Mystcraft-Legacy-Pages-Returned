package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.function.Supplier;

/**
 * Registers Mystcraft villager professions and POI types (Fabric).
 * The Archivist profession specializes in Mystcraft items and symbol trades.
 */
public final class ModVillagers {

    /**
     * POI type for the Archivist workstation (Lectern).
     * Registered via Fabric's PointOfInterestHelper which handles the registry and tag setup.
     */
    public static final Supplier<PoiType> ARCHIVIST_POI;

    /**
     * The Archivist villager profession.
     * Archivists trade Mystcraft items like pages, ink, and books.
     */
    public static final Supplier<VillagerProfession> ARCHIVIST;

    static {
        PoiType poiType = PointOfInterestHelper.register(
                new ResourceLocation(Mystcraft.MOD_ID, "archivist"),
                1, // Max tickets
                1, // Valid range
                FabricModBlocks.LECTERN.get());
        ARCHIVIST_POI = () -> poiType;

        VillagerProfession profession = new VillagerProfession(
                "archivist",
                holder -> holder.value() == poiType,
                holder -> holder.value() == poiType,
                ImmutableSet.of(),
                ImmutableSet.of(),
                SoundEvents.VILLAGER_WORK_LIBRARIAN
        );
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION,
                new ResourceLocation(Mystcraft.MOD_ID, "archivist"), profession);
        ARCHIVIST = () -> profession;
    }

    private ModVillagers() {
    }

    /**
     * Forces static initialization of this class.
     * Call from main mod initializer.
     */
    public static void register() {
        Mystcraft.LOGGER.info("Registering Mystcraft villagers");
    }
}
