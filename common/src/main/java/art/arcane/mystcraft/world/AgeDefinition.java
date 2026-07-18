package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ItemStackNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The durable input needed to reconstruct an Age's generation director after a
 * server restart. This deliberately stores the ordered description rather than
 * a transient {@link AgeDirectorImpl} instance.
 */
public final class AgeDefinition {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  private static final String TAG_SCHEMA_VERSION = "SchemaVersion";
  private static final String TAG_SEED = "Seed";
  private static final String TAG_SYMBOLS = "Symbols";
  private static final String TAG_PERSONAL_POCKET = "PersonalPocket";
  private static final String TAG_MICRO_DIMENSIONS_ENABLED = "MicroDimensionsEnabled";
  private static final String TAG_MICRO_DIMENSION_RADIUS = "MicroDimensionRadiusChunks";
  private static final String TAG_MICRO_DIMENSION_EXTRA = "MicroDimensionExtraChunks";

  private static final String LEGACY_DATA_FILE = Mystcraft.MOD_ID + "_age_data.dat";
  private static final String LEGACY_DATA_FILE_OLD = LEGACY_DATA_FILE + "_old";
  private static final String LEGACY_DATA_WRAPPER = "data";
  private static final String LEGACY_TAG_AGE_UID = "AgeUID";
  private static final String LEGACY_TAG_AGE_UUID = "AgeUUID";
  private static final String LEGACY_TAG_PAGES = "Pages";
  private static final String LEGACY_TAG_GENERATION_SEED = "GenerationSeed";
  private static final String LEGACY_TAG_CONFIG = "AgeConfig";

  private final long seed;
  private final List<ResourceLocation> symbolIds;
  private final boolean personalPocket;
  private final boolean microDimensionsEnabled;
  private final int microDimensionRadiusChunks;
  private final int microDimensionExtraChunks;

  public AgeDefinition(
      long seed,
      @NotNull List<ResourceLocation> symbolIds,
      boolean personalPocket,
      boolean microDimensionsEnabled,
      int microDimensionRadiusChunks,
      int microDimensionExtraChunks
  ) {
    this.seed = seed;
    this.symbolIds = List.copyOf(Objects.requireNonNull(symbolIds, "symbolIds"));
    this.personalPocket = personalPocket;
    this.microDimensionsEnabled = microDimensionsEnabled && !personalPocket;
    this.microDimensionRadiusChunks = Math.max(0, microDimensionRadiusChunks);
    this.microDimensionExtraChunks = Math.max(0, microDimensionExtraChunks);
  }

  /**
   * Captures the generation-relevant settings from a freshly built director.
   */
  @NotNull
  public static AgeDefinition fromDirector(
      @NotNull List<ResourceLocation> symbolIds,
      @NotNull AgeDirectorImpl director
  ) {
    return new AgeDefinition(
        director.getSeed(),
        symbolIds,
        director.isPersonalPocket(),
        director.isMicroDimensionsEnabled(),
        director.getMicroDimensionRadiusChunks(),
        director.getMicroDimensionExtraChunks()
    );
  }

  /**
   * Creates the fixed definition used by personal pocket Ages.
   */
  @NotNull
  public static AgeDefinition personal(long seed) {
    return new AgeDefinition(seed, List.of(), true, false, 0, 0);
  }

  public long getSeed() {
    return seed;
  }

  @NotNull
  public List<ResourceLocation> getSymbolIds() {
    return symbolIds;
  }

  public boolean isPersonalPocket() {
    return personalPocket;
  }

  public boolean isMicroDimensionsEnabled() {
    return microDimensionsEnabled;
  }

  public int getMicroDimensionRadiusChunks() {
    return microDimensionRadiusChunks;
  }

  public int getMicroDimensionExtraChunks() {
    return microDimensionExtraChunks;
  }

  /**
   * Serializes this definition using the current schema.
   */
  @NotNull
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putInt(TAG_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
    tag.putLong(TAG_SEED, seed);

    ListTag symbols = new ListTag();
    for (ResourceLocation symbolId : symbolIds) {
      symbols.add(StringTag.valueOf(symbolId.toString()));
    }
    tag.put(TAG_SYMBOLS, symbols);

    tag.putBoolean(TAG_PERSONAL_POCKET, personalPocket);
    tag.putBoolean(TAG_MICRO_DIMENSIONS_ENABLED, microDimensionsEnabled);
    tag.putInt(TAG_MICRO_DIMENSION_RADIUS, microDimensionRadiusChunks);
    tag.putInt(TAG_MICRO_DIMENSION_EXTRA, microDimensionExtraChunks);
    return tag;
  }

  /**
   * Loads a definition. Unsupported or malformed schemas are rejected so an
   * Age cannot be recreated with guessed generation settings.
   */
  @Nullable
  public static AgeDefinition load(@NotNull CompoundTag tag) {
    int schemaVersion = tag.getInt(TAG_SCHEMA_VERSION);
    if (schemaVersion != CURRENT_SCHEMA_VERSION) {
      Mystcraft.LOGGER.error(
          "Cannot load Age definition schema {} (supported schema: {})",
          schemaVersion,
          CURRENT_SCHEMA_VERSION
      );
      return null;
    }

    if (!tag.contains(TAG_SEED, Tag.TAG_ANY_NUMERIC)) {
      Mystcraft.LOGGER.error("Cannot load Age definition without a generation seed");
      return null;
    }

    List<ResourceLocation> symbolIds = new ArrayList<>();
    ListTag symbols = tag.getList(TAG_SYMBOLS, Tag.TAG_STRING);
    for (int i = 0; i < symbols.size(); i++) {
      String rawId = symbols.getString(i);
      ResourceLocation symbolId = ResourceLocation.tryParse(rawId);
      if (symbolId == null) {
        Mystcraft.LOGGER.error("Cannot load Age definition with invalid symbol ID '{}'", rawId);
        return null;
      }
      symbolIds.add(symbolId);
    }

    return new AgeDefinition(
        tag.getLong(TAG_SEED),
        symbolIds,
        tag.getBoolean(TAG_PERSONAL_POCKET),
        tag.getBoolean(TAG_MICRO_DIMENSIONS_ENABLED),
        tag.getInt(TAG_MICRO_DIMENSION_RADIUS),
        tag.contains(TAG_MICRO_DIMENSION_EXTRA, Tag.TAG_ANY_NUMERIC)
            ? tag.getInt(TAG_MICRO_DIMENSION_EXTRA)
            : 1
    );
  }

  /**
   * Rebuilds the transient director used by dynamic dimension creation.
   * Missing registered symbols reject reconstruction rather than silently
   * producing different terrain at an existing chunk boundary.
   */
  @Nullable
  public AgeDirectorImpl buildDirector(@NotNull MinecraftServer server) {
    if (personalPocket) {
      return PersonalPocketDimension.buildPersonalDirector(server, seed);
    }

    List<IAgeSymbol> symbols = new ArrayList<>(symbolIds.size());
    for (ResourceLocation symbolId : symbolIds) {
      IAgeSymbol symbol = SymbolRegistry.get(symbolId);
      if (symbol == null) {
        Mystcraft.LOGGER.error(
            "Cannot reconstruct Age: required symbol {} is not registered",
            symbolId
        );
        return null;
      }
      symbols.add(symbol);
    }

    AgeDirectorImpl director = new AgeBuilder(symbols, seed).build();
    director.setMicroDimensions(
        microDimensionsEnabled,
        microDimensionRadiusChunks,
        microDimensionExtraChunks
    );
    return director;
  }

  /**
   * Recovers a definition from the pre-definition per-dimension SavedData.
   */
  @Nullable
  public static LegacyMigration loadLegacy(
      @NotNull MinecraftServer server,
      int ageUID,
      @NotNull ResourceLocation dimension
  ) {
    Path dataDirectory = server.getWorldPath(LevelResource.ROOT)
        .resolve("dimensions")
        .resolve(dimension.getNamespace())
        .resolve(dimension.getPath())
        .resolve("data");
    Path dataFile = dataDirectory.resolve(LEGACY_DATA_FILE);
    if (!Files.isRegularFile(dataFile)) {
      Path oldDataFile = dataDirectory.resolve(LEGACY_DATA_FILE_OLD);
      if (!Files.isRegularFile(oldDataFile)) {
        return null;
      }
      dataFile = oldDataFile;
    }

    try {
      CompoundTag root = NbtIo.readCompressed(dataFile.toFile());
      CompoundTag data = root.contains(LEGACY_DATA_WRAPPER, Tag.TAG_COMPOUND)
          ? root.getCompound(LEGACY_DATA_WRAPPER)
          : root;

      int savedUID = data.getInt(LEGACY_TAG_AGE_UID);
      if (savedUID > 0 && savedUID != ageUID) {
        Mystcraft.LOGGER.error(
            "Refusing to migrate Age {} from {} because the file belongs to Age {}",
            ageUID,
            dataFile,
            savedUID
        );
        return null;
      }

      List<ResourceLocation> symbolIds = readLegacySymbolIds(data, ageUID, dataFile);
      if (symbolIds == null) {
        return null;
      }

      CompoundTag config = data.getCompound(LEGACY_TAG_CONFIG);
      boolean personalPocket = config.getBoolean(TAG_PERSONAL_POCKET);
      boolean microDimensionsEnabled = config.getBoolean(TAG_MICRO_DIMENSIONS_ENABLED);
      int microDimensionRadius = config.getInt(TAG_MICRO_DIMENSION_RADIUS);
      int microDimensionExtra = config.contains(TAG_MICRO_DIMENSION_EXTRA, Tag.TAG_ANY_NUMERIC)
          ? config.getInt(TAG_MICRO_DIMENSION_EXTRA)
          : 1;

      boolean derivedSeed = false;
      long seed;
      if (data.contains(LEGACY_TAG_GENERATION_SEED, Tag.TAG_ANY_NUMERIC)) {
        seed = data.getLong(LEGACY_TAG_GENERATION_SEED);
      } else if (data.contains(TAG_SEED, Tag.TAG_ANY_NUMERIC)) {
        seed = data.getLong(TAG_SEED);
      } else if (personalPocket) {
        seed = 0L;
      } else {
        seed = AgeSeed.deriveFromSymbolIds(symbolIds);
        derivedSeed = true;
        Mystcraft.LOGGER.warn(
            "Legacy Age {} has no persisted generation seed. Derived seed {} from {} ordered symbols; existing chunk boundaries may have seams because the original seed cannot be recovered",
            ageUID,
            seed,
            symbolIds.size()
        );
      }

      UUID ageUUID = readLegacyUUID(data, ageUID);
      AgeDefinition definition = new AgeDefinition(
          seed,
          symbolIds,
          personalPocket,
          microDimensionsEnabled,
          microDimensionRadius,
          microDimensionExtra
      );

      Mystcraft.LOGGER.info("Recovered Age {} definition from {}", ageUID, dataFile);
      return new LegacyMigration(definition, ageUUID, dataFile, derivedSeed);
    } catch (IOException | RuntimeException e) {
      Mystcraft.LOGGER.error("Failed to recover legacy Age {} definition from {}", ageUID, dataFile, e);
      return null;
    }
  }

  @Nullable
  private static List<ResourceLocation> readLegacySymbolIds(
      @NotNull CompoundTag data,
      int ageUID,
      @NotNull Path dataFile
  ) {
    List<ResourceLocation> symbolIds = new ArrayList<>();
    ListTag pages = data.getList(LEGACY_TAG_PAGES, Tag.TAG_COMPOUND);
    for (int i = 0; i < pages.size(); i++) {
      try {
        ItemStack page = ItemStackNbt.load(pages.getCompound(i));
        ResourceLocation symbolId = Page.getSymbol(page);
        if (symbolId != null) {
          symbolIds.add(symbolId);
        }
      } catch (RuntimeException e) {
        Mystcraft.LOGGER.error(
            "Cannot recover legacy Age {}: page {} in {} is malformed",
            ageUID,
            i,
            dataFile,
            e
        );
        return null;
      }
    }
    return symbolIds;
  }

  @Nullable
  private static UUID readLegacyUUID(@NotNull CompoundTag data, int ageUID) {
    if (!data.contains(LEGACY_TAG_AGE_UUID, Tag.TAG_STRING)) {
      return null;
    }

    try {
      return UUID.fromString(data.getString(LEGACY_TAG_AGE_UUID));
    } catch (IllegalArgumentException e) {
      Mystcraft.LOGGER.warn(
          "Legacy Age {} has invalid UUID '{}'",
          ageUID,
          data.getString(LEGACY_TAG_AGE_UUID)
      );
      return null;
    }
  }

  /**
   * Result of importing the old per-dimension data format.
   */
  public record LegacyMigration(
      @NotNull AgeDefinition definition,
      @Nullable UUID ageUUID,
      @NotNull Path sourceFile,
      boolean seedDerivedFromPages
  ) {
  }
}
