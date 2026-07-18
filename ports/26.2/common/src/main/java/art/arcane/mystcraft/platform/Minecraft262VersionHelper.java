package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.platform.services.IVersionHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared implementation of the Minecraft APIs that changed for the 26.2 port.
 */
public final class Minecraft262VersionHelper implements IVersionHelper {

  private static final Map<String, String> LEGACY_SAVED_DATA_NAMES = Map.of(
      "age_data", "mystcraft_age_data",
      "ages", "mystcraft_ages",
      "personal_pocket", "mystcraft_personal_pocket",
      "age_returns", "mystcraft_age_returns",
      "guidebook", "mystcraft_guidebook",
      "link_permissions", "mystcraft_link_permissions"
  );

  private final Set<Path> migrationChecks = ConcurrentHashMap.newKeySet();

  @Override
  public String getMinecraftVersion() {
    return "26.2";
  }

  @Override
  public <T extends SavedData> T computeSavedData(ServerLevel level, SavedDataType<T> type) {
    migrateLegacySavedData(level, type);
    return level.getDataStorage().computeIfAbsent(type);
  }

  private void migrateLegacySavedData(ServerLevel level, SavedDataType<?> type) {
    Identifier id = type.id();
    if (!Mystcraft.MOD_ID.equals(id.getNamespace())) {
      return;
    }

    String legacyName = LEGACY_SAVED_DATA_NAMES.get(id.getPath());
    if (legacyName == null) {
      return;
    }

    Path worldRoot = level.getServer().getWorldPath(LevelResource.ROOT);
    Path dimensionRoot = DimensionType.getStorageFolder(level.dimension(), worldRoot);
    Path dataDirectory = dimensionRoot.resolve("data");
    Path target = id.withSuffix(".dat").resolveAgainst(dataDirectory);
    Path source = dataDirectory.resolve(legacyName + ".dat");
    migrateLegacyFile(source, target, level.dimension().identifier().toString());
  }

  MigrationResult migrateLegacyFile(Path source, Path target, String dimensionId) {
    Path migrationKey = target.toAbsolutePath().normalize();
    if (!migrationChecks.add(migrationKey)) {
      return MigrationResult.ALREADY_CHECKED;
    }

    if (Files.exists(target)) {
      return MigrationResult.TARGET_EXISTS;
    }

    if (!Files.isRegularFile(source)) {
      return MigrationResult.SOURCE_MISSING;
    }

    try {
      Files.createDirectories(target.getParent());
    } catch (IOException exception) {
      return migrationFailed(source, target, dimensionId, migrationKey, exception);
    }

    try {
      Files.copy(source, target);
    } catch (FileAlreadyExistsException ignored) {
      // Another caller completed the same copy after the existence check.
      return MigrationResult.TARGET_EXISTS;
    } catch (IOException exception) {
      return migrationFailed(source, target, dimensionId, migrationKey, exception);
    }

    Mystcraft.LOGGER.info("Copied legacy saved data {} to {} for dimension {}",
        source, target, dimensionId);
    return MigrationResult.COPIED;
  }

  private MigrationResult migrationFailed(Path source, Path target, String dimensionId,
                                           Path migrationKey, IOException exception) {
    migrationChecks.remove(migrationKey);
    Mystcraft.LOGGER.error("Failed to copy legacy saved data {} to {} for dimension {}",
        source, target, dimensionId, exception);
    return MigrationResult.FAILED;
  }

  enum MigrationResult {
    COPIED,
    TARGET_EXISTS,
    SOURCE_MISSING,
    ALREADY_CHECKED,
    FAILED
  }
}
