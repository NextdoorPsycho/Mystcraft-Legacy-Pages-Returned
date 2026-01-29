package art.arcane.mystcraft.world.structure;

import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.function.Supplier;

/**
 * Common accessor for registered structure types.
 * Platform modules populate these suppliers during initialization.
 */
public final class ModStructures {

  public static Supplier<StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY;
  public static Supplier<StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE;
  public static Supplier<StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY;

  private ModStructures() {
  }
}
