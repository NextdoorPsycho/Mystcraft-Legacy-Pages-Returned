package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Personal pocket dimension: a hollow cube structure.
 * Inner space is 3x3 chunks (48 blocks) of void.
 * Surrounded by 3 blocks of wood (simplex pattern) then 5 blocks of bedrock.
 * Spawn point is at the center of the hollow interior.
 */
public final class PersonalPocketDimension {

  private static final int PERSONAL_UID_OFFSET = 1_000_000_000;
  private static final int PERSONAL_UID_RANGE = 1_000_000_000;

  /** Half the inner void space (3 chunks = 48 blocks, half = 24). */
  public static final int INNER_HALF_SIZE = 24;

  /** Wood shell thickness surrounding the inner void. */
  public static final int WOOD_THICKNESS = 3;

  /** Bedrock shell thickness surrounding the wood layer. */
  public static final int BEDROCK_THICKNESS = 5;

  /** Total half-size from center to outer bedrock edge. */
  public static final int TOTAL_HALF_SIZE = INNER_HALF_SIZE + WOOD_THICKNESS + BEDROCK_THICKNESS; // 32

  /** Inner void size (diameter) - used for world border. */
  public static final int INNER_SIZE = INNER_HALF_SIZE * 2; // 48

  /** Total pocket size (diameter including walls). */
  public static final int POCKET_SIZE = TOTAL_HALF_SIZE * 2; // 64

  /** Y coordinate of the center of the cube. */
  public static final int CENTER_Y = 64;

  /** Bottom of inner void. */
  public static final int INNER_MIN_Y = CENTER_Y - INNER_HALF_SIZE; // 40

  /** Top of inner void. */
  public static final int INNER_MAX_Y = CENTER_Y + INNER_HALF_SIZE - 1; // 87

  /** Spawn position at center of the cube. */
  private static final BlockPos POCKET_SPAWN = new BlockPos(0, CENTER_Y, 0);

  /** Minimum Y before boundary triggers (bottom of bedrock). */
  public static final int BOUNDARY_MIN_Y = CENTER_Y - TOTAL_HALF_SIZE; // 32

  /** Maximum Y before boundary triggers (top of bedrock). */
  public static final int BOUNDARY_MAX_Y = CENTER_Y + TOTAL_HALF_SIZE - 1; // 95

  private PersonalPocketDimension() {
  }

  public static int getPersonalAgeUid(UUID playerId) {
    int hash = playerId.hashCode();
    int uid = PERSONAL_UID_OFFSET + Math.floorMod(hash, PERSONAL_UID_RANGE);
    if (uid == 0 || uid == 1) {
      uid = PERSONAL_UID_OFFSET + 2;
    }
    return uid;
  }

  public static BlockPos getPocketSpawn() {
    return POCKET_SPAWN;
  }

  public static boolean isPersonalPocket(ServerLevel level) {
    AgeData ageData = AgeData.getIfPresent(level);
    return ageData != null && ageData.isPersonalPocket();
  }

  /**
   * Checks if a position is outside the inner void space (inside walls).
   * Used to teleport players back if they clip into the walls.
   */
  public static boolean isOutsideBoundary(double x, double z) {
    return x < -INNER_HALF_SIZE || x >= INNER_HALF_SIZE ||
           z < -INNER_HALF_SIZE || z >= INNER_HALF_SIZE;
  }

  /**
   * Checks if Y coordinate is outside the inner void space.
   */
  public static boolean isOutsideVerticalBoundary(double y) {
    return y < INNER_MIN_Y || y > INNER_MAX_Y;
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
    ageData.setSpawn(POCKET_SPAWN.getX(), POCKET_SPAWN.getY(), POCKET_SPAWN.getZ());

    configurePersonalRules(level);
    enforceBorder(level);

    return level;
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
    int borderSize = INNER_SIZE + 2; // +1 on each side
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
