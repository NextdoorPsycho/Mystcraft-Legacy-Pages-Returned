package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Factory for creating and managing Age dimensions.
 * <p>
 * 1.18.2 stub version - provides basic helper methods without full dimension creation.
 * Full dimension creation will be added when more world gen classes are ported.
 */
public class AgeDimensionFactory {

  public static final String DIMENSION_PREFIX = "mystcraft_age_";
  public static final String POCKET_PREFIX = "mystcraft_pocket_";

  private AgeDimensionFactory() {
  }

  /**
   * Checks if a dimension key is a Mystcraft Age dimension.
   */
  public static boolean isMystcraftAge(ResourceKey<Level> dimension) {
    if (dimension == null) return false;
    ResourceLocation location = dimension.location();
    return location.getNamespace().equals(Mystcraft.MOD_ID) &&
        (location.getPath().startsWith(DIMENSION_PREFIX) ||
            location.getPath().startsWith(POCKET_PREFIX));
  }

  /**
   * Gets the Age UID from a dimension key, or null if not an Age.
   */
  public static Integer getAgeUID(ResourceKey<Level> dimension) {
    if (!isMystcraftAge(dimension)) return null;

    ResourceLocation location = dimension.location();
    String path = location.getPath();

    if (path.startsWith(DIMENSION_PREFIX)) {
      try {
        return Integer.parseInt(path.substring(DIMENSION_PREFIX.length()));
      } catch (NumberFormatException e) {
        return null;
      }
    } else if (path.startsWith(POCKET_PREFIX)) {
      // Pocket dimensions use UUIDs, not UIDs
      return null;
    }

    return null;
  }

  /**
   * Gets or creates an Age dimension.
   * <p>
   * Note: Full dimension creation is not yet implemented for 1.18.2.
   * This stub version only returns existing dimensions.
   */
  public static ServerLevel getOrCreateAgeDimension(MinecraftServer server, int ageUID) {
    ResourceLocation dimensionId = new ResourceLocation(Mystcraft.MOD_ID, DIMENSION_PREFIX + ageUID);
    ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY, dimensionId);

    return server.getLevel(dimensionKey);
  }

  /**
   * Creates a new Age dimension.
   * <p>
   * Not yet implemented for 1.18.2.
   */
  public static ServerLevel createAgeDimension(MinecraftServer server, int ageUID,
                                               java.util.UUID ageUUID, AgeDirectorImpl director) {
    Mystcraft.LOGGER.warn("Age dimension creation not yet implemented for 1.18.2");
    return null;
  }

  /**
   * Applies micro dimension world border if enabled.
   */
  public static void applyMicroDimensionBorder(ServerLevel level, AgeData ageData) {
    // Not yet implemented for 1.18.2
  }
}
