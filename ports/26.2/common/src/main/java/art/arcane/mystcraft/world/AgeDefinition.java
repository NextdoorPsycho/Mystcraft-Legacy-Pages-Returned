package art.arcane.mystcraft.world;

import art.arcane.mystcraft.util.NbtCompat;

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
import net.minecraft.resources.Identifier;
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
  private final List<Identifier> symbolIds;
  private final boolean personalPocket;
  private final boolean microDimensionsEnabled;
  private final int microDimensionRadiusChunks;
  private final int microDimensionExtraChunks;

  public AgeDefinition(
      long seed,
      @NotNull List<Identifier> symbolIds,
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
      @NotNull List<Identifier> symbolIds,
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
  public List<Identifier> getSymbolIds() {
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
    for (Identifier symbolId : symbolIds) {
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
    int schemaVersion = tag.getIntOr(TAG_SCHEMA_VERSION, 0);
    if (schemaVersion != CURRENT_SCHEMA_VERSION) {
      Mystcraft.LOGGER.error(
          "Cannot load Age definition schema {} (supported schema: {})",
          schemaVersion,
          CURRENT_SCHEMA_VERSION
      );
      return null;
    }

    if (!NbtCompat.contains(tag, TAG_SEED, NbtCompat.TAG_ANY_NUMERIC)) {
      Mystcraft.LOGGER.error("Cannot load Age definition without a generation seed");
      return null;
    }

    List<Identifier> symbolIds = new ArrayList<>();
    ListTag symbols = tag.getListOrEmpty(TAG_SYMBOLS);
    for (int i = 0; i < symbols.size(); i++) {
      String rawId = symbols.getStringOr(i, "");
      Identifier symbolId = Identifier.tryParse(rawId);
      if (symbolId == null) {
        Mystcraft.LOGGER.error("Cannot load Age definition with invalid symbol ID '{}'", rawId);
        return null;
      }
      symbolIds.add(symbolId);
    }

    return new AgeDefinition(
        tag.getLongOr(TAG_SEED, 0L),
        symbolIds,
        tag.getBooleanOr(TAG_PERSONAL_POCKET, false),
        tag.getBooleanOr(TAG_MICRO_DIMENSIONS_ENABLED, false),
        tag.getIntOr(TAG_MICRO_DIMENSION_RADIUS, 0),
        NbtCompat.contains(tag, TAG_MICRO_DIMENSION_EXTRA, NbtCompat.TAG_ANY_NUMERIC)
            ? tag.getIntOr(TAG_MICRO_DIMENSION_EXTRA, 0)
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
    for (Identifier symbolId : symbolIds) {
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
      @NotNull Identifier dimension
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
      CompoundTag root = NbtIo.readCompressed(dataFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
      CompoundTag data = NbtCompat.contains(root, LEGACY_DATA_WRAPPER, Tag.TAG_COMPOUND)
          ? root.getCompoundOrEmpty(LEGACY_DATA_WRAPPER)
          : root;

      int savedUID = data.getIntOr(LEGACY_TAG_AGE_UID, 0);
      if (savedUID > 0 && savedUID != ageUID) {
        Mystcraft.LOGGER.error(
            "Refusing to migrate Age {} from {} because the file belongs to Age {}",
            ageUID,
            dataFile,
            savedUID
        );
        return null;
      }

      List<Identifier> symbolIds = readLegacySymbolIds(data, ageUID, dataFile);
      if (symbolIds == null) {
        return null;
      }

      CompoundTag config = data.getCompoundOrEmpty(LEGACY_TAG_CONFIG);
      boolean personalPocket = config.getBooleanOr(TAG_PERSONAL_POCKET, false);
      boolean microDimensionsEnabled = config.getBooleanOr(TAG_MICRO_DIMENSIONS_ENABLED, false);
      int microDimensionRadius = config.getIntOr(TAG_MICRO_DIMENSION_RADIUS, 0);
      int microDimensionExtra = NbtCompat.contains(config, TAG_MICRO_DIMENSION_EXTRA, NbtCompat.TAG_ANY_NUMERIC)
          ? config.getIntOr(TAG_MICRO_DIMENSION_EXTRA, 0)
          : 1;

      boolean derivedSeed = false;
      long seed;
      if (NbtCompat.contains(data, LEGACY_TAG_GENERATION_SEED, NbtCompat.TAG_ANY_NUMERIC)) {
        seed = data.getLongOr(LEGACY_TAG_GENERATION_SEED, 0L);
      } else if (NbtCompat.contains(data, TAG_SEED, NbtCompat.TAG_ANY_NUMERIC)) {
        seed = data.getLongOr(TAG_SEED, 0L);
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
  private static List<Identifier> readLegacySymbolIds(
      @NotNull CompoundTag data,
      int ageUID,
      @NotNull Path dataFile
  ) {
    List<Identifier> symbolIds = new ArrayList<>();
    ListTag pages = data.getListOrEmpty(LEGACY_TAG_PAGES);
    for (int i = 0; i < pages.size(); i++) {
      try {
        ItemStack page = ItemStackNbt.load(pages.getCompoundOrEmpty(i));
        Identifier symbolId = Page.getSymbol(page);
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
    if (!NbtCompat.contains(data, LEGACY_TAG_AGE_UUID, Tag.TAG_STRING)) {
      return null;
    }

    try {
      return UUID.fromString(data.getStringOr(LEGACY_TAG_AGE_UUID, ""));
    } catch (IllegalArgumentException e) {
      Mystcraft.LOGGER.warn(
          "Legacy Age {} has invalid UUID '{}'",
          ageUID,
          data.getStringOr(LEGACY_TAG_AGE_UUID, "")
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
