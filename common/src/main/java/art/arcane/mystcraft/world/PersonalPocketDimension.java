package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Personal pocket dimension: a hollow rectangular structure.
 * Inner space is configurable separately for XZ (horizontal) and Y (vertical).
 * XZ can be up to 8192 blocks (half-size 4096).
 * Y is limited by Minecraft's dimension height limits.
 * Surrounded by configurable inner shell then outer shell.
 * Spawn point is at the bottom of the hollow interior.
 *
 * Size validation automatically caps to Minecraft version limits:
 * - 1.20.2+: max dimension height 4064 (min_y -2032 to max_y 2032)
 * - Earlier versions may have smaller limits; values will be capped accordingly.
 */
public final class PersonalPocketDimension {

  private static final int PERSONAL_UID_OFFSET = 1_000_000_000;
  private static final int PERSONAL_UID_RANGE = 1_000_000_000;

  /**
   * Minecraft dimension limits for personal pockets.
   * These represent the maximum possible range in 1.20.2+ with custom dimension types.
   * For personal pockets, we use a dimension type configured to these limits.
   */
  public static final int MIN_BUILD_Y = -2032;
  public static final int MAX_BUILD_Y = 2032;
  public static final int DIMENSION_HEIGHT = MAX_BUILD_Y - MIN_BUILD_Y; // 4064

  /** Absolute minimum inner half-size (4 block diameter minimum). */
  public static final int MIN_HALF_SIZE = 2;

  /** Maximum half-size for config (8192 block diameter). */
  public static final int MAX_HALF_SIZE = 4096;

  /** Track if we've logged version-based capping warnings. */
  private static boolean loggedYCap = false;
  private static boolean loggedXZCap = false;

  private PersonalPocketDimension() {
  }

  // --- Dimension limit detection ---

  /**
   * Returns the maximum supported Y half-size based on Minecraft's dimension limits.
   * Accounts for shell thickness to ensure the complete pocket fits within limits.
   * This automatically adapts to the version's capabilities.
   */
  public static int getMaxSupportedHalfSizeY() {
    int shellThickness = getInnerThickness() + getOuterThickness();
    int availableHeight = DIMENSION_HEIGHT - (shellThickness * 2);
    return Math.max(MIN_HALF_SIZE, availableHeight / 2);
  }

  // --- Configurable dimensions with validation and fallback ---

  /** Half the inner void space horizontally (X/Z axes). Range: 2-4096 (4 to 8192 blocks). */
  public static int getInnerHalfSizeXZ() {
    int requested = MystcraftConfig.pocketInnerHalfSizeXZ.get();
    int clamped = Math.max(MIN_HALF_SIZE, Math.min(MAX_HALF_SIZE, requested));

    if (clamped != requested && !loggedXZCap) {
      Mystcraft.LOGGER.warn("[PersonalPocket] XZ half-size {} exceeds limits, capped to {} (max diameter: {} blocks)",
          requested, clamped, clamped * 2);
      loggedXZCap = true;
    }

    return clamped;
  }

  /**
   * Half the inner void space vertically (Y axis).
   * Capped to config limit (4096) and then further to Minecraft's dimension height if needed.
   */
  public static int getInnerHalfSizeY() {
    int requested = MystcraftConfig.pocketInnerHalfSizeY.get();
    int maxSupported = Math.min(MAX_HALF_SIZE, getMaxSupportedHalfSizeY());
    int clamped = Math.max(MIN_HALF_SIZE, Math.min(maxSupported, requested));

    if (clamped != requested && !loggedYCap) {
      if (requested > MAX_HALF_SIZE) {
        Mystcraft.LOGGER.warn("[PersonalPocket] Y half-size {} exceeds config limit, capped to {} (max diameter: {} blocks)",
            requested, clamped, clamped * 2);
      } else {
        Mystcraft.LOGGER.warn("[PersonalPocket] Y half-size {} exceeds Minecraft dimension limits, capped to {} (max height: {} blocks)",
            requested, clamped, clamped * 2);
        Mystcraft.LOGGER.info("[PersonalPocket] Minecraft 1.20.2 max dimension height: {} blocks. " +
            "With shell thickness {}, max inner Y half-size is {}.",
            DIMENSION_HEIGHT, getInnerThickness() + getOuterThickness(), getMaxSupportedHalfSizeY());
      }
      loggedYCap = true;
    }

    return clamped;
  }

  /** Inner shell thickness (from config, clamped to valid range). */
  public static int getInnerThickness() {
    return Math.max(1, Math.min(32, MystcraftConfig.pocketInnerThickness.get()));
  }

  /** Outer shell thickness (from config, clamped to valid range). */
  public static int getOuterThickness() {
    return Math.max(1, Math.min(32, MystcraftConfig.pocketOuterThickness.get()));
  }

  /** Center Y coordinate (from config, adjusted if needed to fit within build limits). */
  public static int getCenterY() {
    int requestedCenterY = MystcraftConfig.pocketCenterY.get();
    int totalHalfSizeY = getTotalHalfSizeY();

    // Ensure pocket fits within dimension build limits
    int minCenterY = MIN_BUILD_Y + totalHalfSizeY;
    int maxCenterY = MAX_BUILD_Y - totalHalfSizeY;

    if (maxCenterY < minCenterY) {
      // Pocket too large to fit - use midpoint and log warning
      Mystcraft.LOGGER.warn("[PersonalPocket] Pocket Y size ({} blocks) too large for dimension, centering at Y=0", totalHalfSizeY * 2);
      return 0;
    }

    return Math.max(minCenterY, Math.min(maxCenterY, requestedCenterY));
  }

  /** Total half-size from center to outer edge (horizontal). */
  public static int getTotalHalfSizeXZ() {
    return getInnerHalfSizeXZ() + getInnerThickness() + getOuterThickness();
  }

  /** Total half-size from center to outer edge (vertical). */
  public static int getTotalHalfSizeY() {
    return getInnerHalfSizeY() + getInnerThickness() + getOuterThickness();
  }

  /** Inner void size horizontally (diameter) - used for world border. */
  public static int getInnerSizeXZ() {
    return getInnerHalfSizeXZ() * 2;
  }

  /** Inner void size vertically (diameter). */
  public static int getInnerSizeY() {
    return getInnerHalfSizeY() * 2;
  }

  /** Total pocket size horizontally (diameter including walls). */
  public static int getPocketSizeXZ() {
    return getTotalHalfSizeXZ() * 2;
  }

  /** Total pocket size vertically (diameter including walls). */
  public static int getPocketSizeY() {
    return getTotalHalfSizeY() * 2;
  }

  /** Bottom of inner void (spawn floor). */
  public static int getInnerMinY() {
    return getCenterY() - getInnerHalfSizeY();
  }

  /** Top of inner void. */
  public static int getInnerMaxY() {
    return getCenterY() + getInnerHalfSizeY() - 1;
  }

  /** Minimum Y before boundary triggers (bottom of outer shell). */
  public static int getBoundaryMinY() {
    return getCenterY() - getTotalHalfSizeY();
  }

  /** Maximum Y before boundary triggers (top of outer shell). */
  public static int getBoundaryMaxY() {
    return getCenterY() + getTotalHalfSizeY() - 1;
  }

  /** Gets the configured inner block palette, falling back to oak_planks if all invalid. */
  public static java.util.List<BlockState> getInnerBlockPalette() {
    java.util.List<String> blockIds = MystcraftConfig.pocketInnerBlockPalette.get();
    java.util.List<BlockState> palette = new java.util.ArrayList<>();

    for (String blockId : blockIds) {
      ResourceLocation loc = ResourceLocation.tryParse(blockId);
      if (loc != null) {
        Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(loc);
        if (block != Blocks.AIR) {
          palette.add(block.defaultBlockState());
        } else {
          Mystcraft.LOGGER.warn("[PersonalPocket] Invalid inner block '{}', skipping", blockId);
        }
      } else {
        Mystcraft.LOGGER.warn("[PersonalPocket] Invalid inner block ID '{}', skipping", blockId);
      }
    }

    if (palette.isEmpty()) {
      Mystcraft.LOGGER.warn("[PersonalPocket] No valid inner blocks configured, using oak_planks");
      palette.add(Blocks.OAK_PLANKS.defaultBlockState());
    }

    return palette;
  }

  /** Gets the configured outer block, falling back to bedrock if invalid. */
  public static BlockState getOuterBlock() {
    String blockId = MystcraftConfig.pocketOuterBlock.get();
    ResourceLocation loc = ResourceLocation.tryParse(blockId);
    if (loc != null) {
      Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(loc);
      if (block != Blocks.AIR) {
        return block.defaultBlockState();
      }
    }
    Mystcraft.LOGGER.warn("[PersonalPocket] Invalid outer block '{}', using bedrock", blockId);
    return Blocks.BEDROCK.defaultBlockState();
  }

  public static int getPersonalAgeUid(UUID playerId) {
    int hash = playerId.hashCode();
    int uid = PERSONAL_UID_OFFSET + Math.floorMod(hash, PERSONAL_UID_RANGE);
    if (uid == 0 || uid == 1) {
      uid = PERSONAL_UID_OFFSET + 2;
    }
    return uid;
  }

  /** Returns spawn position beside the bookstand (offset by 1 block on X axis). */
  public static BlockPos getPocketSpawn() {
    return new BlockPos(1, getInnerMinY() + 1, 0);
  }

  /** Returns the position for the bookstand (center of the pocket floor). */
  public static BlockPos getBookstandPos() {
    return new BlockPos(0, getInnerMinY() + 1, 0);
  }

  /** Returns the position for the floor block under the bookstand. */
  public static BlockPos getFloorBlockPos() {
    return new BlockPos(0, getInnerMinY(), 0);
  }

  public static boolean isPersonalPocket(ServerLevel level) {
    AgeData ageData = AgeData.getIfPresent(level);
    return ageData != null && ageData.isPersonalPocket();
  }

  /**
   * Checks if a position is outside the entire pocket structure (beyond outer shell).
   * This is a failsafe - players should never reach here normally.
   * Teleports them back if they somehow clip through the outer walls.
   */
  public static boolean isOutsideBoundary(double x, double z) {
    int totalHalfSizeXZ = getTotalHalfSizeXZ();
    return x < -totalHalfSizeXZ || x >= totalHalfSizeXZ ||
           z < -totalHalfSizeXZ || z >= totalHalfSizeXZ;
  }

  /**
   * Checks if Y coordinate is outside the entire pocket structure (beyond bedrock).
   * This is a failsafe - players should never reach here normally.
   */
  public static boolean isOutsideVerticalBoundary(double y) {
    return y < getBoundaryMinY() || y > getBoundaryMaxY();
  }

  @Nullable
  public static ServerLevel getOrCreate(MinecraftServer server, UUID owner) {
    if (!MystcraftConfig.enablePersonalLinkBooks.get()) {
      Mystcraft.LOGGER.debug("[PersonalPocket] Personal link books disabled, not creating pocket for {}", owner);
      return null;
    }
    int ageUID = getPersonalAgeUid(owner);
    AgeManager ageManager = AgeManager.get(server);
    ResourceLocation dimLoc = ageManager.getDimension(ageUID);
    if (dimLoc != null) {
      ServerLevel existing = server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, dimLoc));
      if (existing != null) {
        Mystcraft.LOGGER.debug("[PersonalPocket] Using loaded pocket {} for {}", dimLoc, owner);
        return existing;
      }
      ServerLevel loaded = AgeDimensionFactory.getOrCreateAgeDimension(server, ageUID);
      if (loaded != null) {
        Mystcraft.LOGGER.info("[PersonalPocket] Loaded pocket {} for {}", dimLoc, owner);
        return loaded;
      }
      Mystcraft.LOGGER.warn("[PersonalPocket] Failed to load existing pocket {} for {}, recreating", dimLoc, owner);
    }

    AgeDirectorImpl director = buildPersonalDirector(server);
    UUID ageUUID = owner;

    Mystcraft.LOGGER.info("[PersonalPocket] Creating new pocket dimension for {}", owner);
    ServerLevel level = AgeDimensionFactory.createAgeDimension(server, ageUID, ageUUID, director);
    if (level == null) {
      return null;
    }

    ResourceLocation newDimLoc = new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_age_" + ageUID);
    ageManager.registerAge(ageUID, newDimLoc, ageUUID);

    AgeData ageData = AgeData.get(level);
    ageData.setAgeUID(ageUID);
    ageData.setAgeUUID(ageUUID);
    ageData.setAgeName("Personal Pocket");
    ageData.copyFromDirector(director);
    BlockPos spawn = getPocketSpawn();
    ageData.setSpawn(spawn.getX(), spawn.getY(), spawn.getZ());

    configurePersonalRules(level);
    enforceBorder(level);

    // Place the return bookstand with linkbook pointing back to where the player came from
    placeReturnBookstand(level, owner);

    return level;
  }

  /**
   * Places a bookstand in the center of the pocket floor with a linkbook
   * that links back to where the player entered from.
   */
  private static void placeReturnBookstand(ServerLevel level, UUID owner) {
    // Get the return link data (stored before pocket creation)
    CompoundTag returnLink = PersonalPocketData.get(level.getServer()).getReturnLink(owner);
    if (returnLink == null) {
      // Fallback to overworld spawn if no return link exists
      ServerLevel overworld = level.getServer().overworld();
      returnLink = new CompoundTag();
      LinkOptions.setDimensionUID(returnLink, LinkingManager.getDimensionUID(overworld));
      LinkOptions.setSpawn(returnLink, overworld.getSharedSpawnPos());
      LinkOptions.setSpawnYaw(returnLink, 0.0f);
      Mystcraft.LOGGER.warn("[PersonalPocket] No return link found for {}, using overworld spawn", owner);
    }

    // Place floor block at center
    BlockPos floorPos = getFloorBlockPos();
    level.setBlock(floorPos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);

    // Place bookstand on top of floor
    BlockPos bookstandPos = getBookstandPos();
    BlockState bookstandState = ModBlocks.BOOKSTAND.get().defaultBlockState();
    level.setBlock(bookstandPos, bookstandState, 2);

    // Get the bookstand block entity and put a linkbook on it
    BlockEntity blockEntity = level.getBlockEntity(bookstandPos);
    if (blockEntity instanceof BookstandBlockEntity bookstand) {
      // Create the return linkbook
      ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
      CompoundTag tag = linkbook.getOrCreateTag();

      // Copy return link data to the linkbook
      Integer dimUID = LinkOptions.getDimensionUID(returnLink);
      if (dimUID != null) {
        LinkOptions.setDimensionUID(tag, dimUID);
      }
      BlockPos spawnPos = LinkOptions.getSpawn(returnLink);
      if (spawnPos != null) {
        LinkOptions.setSpawn(tag, spawnPos);
      }
      Float yaw = LinkOptions.getSpawnYaw(returnLink);
      if (yaw != null) {
        LinkOptions.setSpawnYaw(tag, yaw);
      }
      LinkOptions.setDisplayName(tag, "Return Home");
      tag.putBoolean("NoDecay", true);

      // Place the linkbook on the bookstand
      bookstand.setBook(linkbook);

      Mystcraft.LOGGER.info("[PersonalPocket] Placed return bookstand at {} with linkbook pointing to UID {}",
          bookstandPos, dimUID);
    } else {
      Mystcraft.LOGGER.warn("[PersonalPocket] Failed to get bookstand block entity at {}", bookstandPos);
    }
  }

  private static AgeDirectorImpl buildPersonalDirector(MinecraftServer server) {
    AgeDirectorImpl director = new AgeDirectorImpl(0L);
    director.setPersonalPocket(true);
    director.setTerrainType("personal");
    director.setTerrainBlock(Blocks.SMOOTH_STONE.defaultBlockState());
    director.setWeatherType("off");
    director.setLightingType("bright");
    director.setSunVisible(true);
    director.setMoonVisible(false);
    director.setStarsVisible(false);
    director.setTimescale(0.0f);

    // Disable all world generation features
    director.setCavesEnabled(false);
    director.setRavinesEnabled(false);
    director.setFloatingIslandsEnabled(false);
    director.setSkylandsEnabled(false);
    director.setVillagesEnabled(false);
    director.setDungeonsEnabled(false);
    director.setMineshaftsEnabled(false);
    director.setStrongholdsEnabled(false);
    director.setPillagerOutpostsEnabled(false);
    director.setRuinedPortalsEnabled(false);
    director.setAncientCitiesEnabled(false);
    director.setTrailRuinsEnabled(false);
    director.setOceanMonumentsEnabled(false);
    director.setWitchHutsEnabled(false);
    director.setDesertTemplesEnabled(false);
    director.setJungleTemplesEnabled(false);
    director.setWoodlandMansionsEnabled(false);
    director.setEndCitiesEnabled(false);
    director.setBastionRemnantsEnabled(false);
    director.setIgloosEnabled(false);
    director.setShipwrecksEnabled(false);
    director.setOceanRuinsEnabled(false);
    director.setBuriedTreasureEnabled(false);
    director.setNetherFossilsEnabled(false);
    director.setDripstoneCavesEnabled(false);
    director.setLushCavesEnabled(false);
    director.setDeepDarkEnabled(false);
    director.setSurfaceLakesEnabled(false);
    director.setDeepLakesEnabled(false);
    director.setCrystalsEnabled(false);
    director.setDenseOresEnabled(false);
    director.setExplosionsEnabled(false);
    director.setMeteorsEnabled(false);
    director.setLightningEnabled(false);
    director.setScorchedEnabled(false);
    director.setObelisksEnabled(false);
    director.setRainbowEnabled(false);
    director.setSpheresEnabled(false);
    director.setSpikesEnabled(false);
    director.setStarFissureEnabled(false);
    director.setStarFissureExplicit(true);
    director.setStarFissureParams(new JsonObject());
    director.setTendrilsEnabled(false);
    director.setVerticalTendrilsEnabled(false);
    director.setPerlinWormsEnabled(false);
    director.setHugeTreesEnabled(false);
    director.setNetherFortEnabled(false);
    director.setPvPEnabled(true);
    director.setOresDisabled(true);
    director.setHasSea(false);

    // Use The Void biome for empty sky appearance
    Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
    Holder<Biome> voidBiome = biomeRegistry.getHolderOrThrow(Biomes.THE_VOID);
    director.setBiomeController("single");
    director.addBiome(voidBiome);

    return director;
  }

  public static void configurePersonalRules(ServerLevel level) {
    level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, level.getServer());
    level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
    level.setDayTime(18000L);
  }

  /**
   * Enforces the world border for the personal pocket.
   * The border matches the inner void space (48x48) centered at origin.
   * Acts as both visual indicator and physical barrier.
   */
  public static void enforceBorder(ServerLevel level) {
    WorldBorder border = level.getWorldBorder();

    boolean changed = false;
    if (border.getCenterX() != 0.0 || border.getCenterZ() != 0.0) {
      border.setCenter(0.0, 0.0);
      changed = true;
    }
    int borderSize = getInnerSizeXZ() + 2; // +1 on each side
    if (border.getSize() != borderSize) {
      Mystcraft.LOGGER.info("[PersonalPocket] Setting world border size from {} to {}", border.getSize(), borderSize);
      border.setSize(borderSize);
      changed = true;
    }
    // Warning distance: visual indicator when approaching edge
    if (border.getWarningBlocks() != 20) {
      border.setWarningBlocks(20);
      changed = true;
    }
    // Enable damage as additional enforcement
    if (border.getDamagePerBlock() != 0.5) {
      border.setDamagePerBlock(0.5);
      changed = true;
    }
    if (border.getDamageSafeZone() != 5.0) {
      border.setDamageSafeZone(5.0);
      changed = true;
    }

    if (changed) {
      Mystcraft.LOGGER.info("[PersonalPocket] Border enforced: center=({},{}), size={}, warning={}, damage={}/block",
          border.getCenterX(), border.getCenterZ(), border.getSize(), border.getWarningBlocks(), border.getDamagePerBlock());

      // Explicitly sync border to all players in this dimension
      // Forge custom dimensions may not auto-sync world border changes
      syncBorderToPlayers(level, border);
    }
  }

  /**
   * Sends the world border state to all players in the dimension.
   * Required for Forge custom dimensions where border changes may not auto-sync.
   */
  public static void syncBorderToPlayers(ServerLevel level, WorldBorder border) {
    ClientboundInitializeBorderPacket packet = new ClientboundInitializeBorderPacket(border);
    for (ServerPlayer player : level.players()) {
      player.connection.send(packet);
      Mystcraft.LOGGER.debug("[PersonalPocket] Synced border to player {}", player.getName().getString());
    }
  }
}
