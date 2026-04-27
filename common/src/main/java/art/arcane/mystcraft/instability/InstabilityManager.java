package art.arcane.mystcraft.instability;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import art.arcane.mystcraft.util.MobEffectCompat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages instability effects in Mystcraft Ages.
 * Higher instability leads to more frequent and severe negative effects.
 * <p>
 * This system mirrors the original Mystcraft approach:
 * - Instability accumulates from missing/conflicting symbols
 * - Effects only trigger once instability exceeds configurable thresholds
 * - Effect frequency scales with how far instability exceeds the threshold
 * - A global multiplier allows server admins to tune difficulty
 */
public final class InstabilityManager {

  // Positive effects pool: effect, duration ticks, amplifier
  private static final Object[][] POSITIVE_EFFECTS = {
      {MobEffects.MOVEMENT_SPEED, 600, 0},        // Speed 30s
      {MobEffects.DIG_SPEED, 600, 0},              // Haste 30s
      {MobEffects.DAMAGE_BOOST, 400, 0},            // Strength 20s
      {MobEffects.JUMP, 600, 0},                    // Jump Boost 30s
      {MobEffects.REGENERATION, 200, 0},             // Regeneration 10s
      {MobEffects.DAMAGE_RESISTANCE, 300, 0},        // Resistance 15s
      {MobEffects.NIGHT_VISION, 1200, 0},            // Night Vision 60s
  };

  // --- Config Accessors ---
  // Negative effects pool: effect, duration ticks, amplifier
  private static final Object[][] NEGATIVE_EFFECTS = {
      {MobEffects.WITHER, 100, 0},                  // Wither 5s
      {MobEffects.POISON, 100, 0},                   // Poison 5s
      {MobEffects.HUNGER, 200, 1},                   // Hunger 10s amp 2
      {MobEffects.MOVEMENT_SLOWDOWN, 160, 0},        // Slowness 8s
      {MobEffects.WEAKNESS, 160, 0},                 // Weakness 8s
      {MobEffects.BLINDNESS, 100, 0},                // Blindness 5s
      {MobEffects.CONFUSION, 120, 0},                // Nausea 6s
      {MobEffects.DIG_SLOWDOWN, 160, 0},             // Mining Fatigue 8s
  };

  private InstabilityManager() {
  }

  private static float getThresholdDecay() {
    return MystcraftConfig.thresholdDecay.get().floatValue();
  }

  private static float getThresholdTransmute() {
    return MystcraftConfig.thresholdTransmute.get().floatValue();
  }

  private static float getThresholdLightning() {
    return MystcraftConfig.thresholdLightning.get().floatValue();
  }

  private static float getThresholdMeteor() {
    return MystcraftConfig.thresholdMeteor.get().floatValue();
  }

  private static float getThresholdPoison() {
    return MystcraftConfig.thresholdPoison.get().floatValue();
  }

  private static float getThresholdWither() {
    return MystcraftConfig.thresholdWither.get().floatValue();
  }

  private static float getChanceDecay() {
    return MystcraftConfig.chanceDecay.get().floatValue();
  }

  private static float getChanceTransmute() {
    return MystcraftConfig.chanceTransmute.get().floatValue();
  }

  private static float getChanceLightning() {
    return MystcraftConfig.chanceLightning.get().floatValue();
  }

  private static float getChanceMeteor() {
    return MystcraftConfig.chanceMeteor.get().floatValue();
  }

  private static float getChancePlayerEffect() {
    return MystcraftConfig.chancePlayerEffect.get().floatValue();
  }

  private static float getEffectMultiplier() {
    return MystcraftConfig.instabilityMultiplier.get().floatValue();
  }

  private static boolean isInstabilityEnabled() {
    return MystcraftConfig.instabilityEnabled.get();
  }

  /**
   * Handles a level tick for instability effects.
   * Called from platform-specific event handlers.
   */
  public static void onLevelTick(ServerLevel level) {
    // Check if instability system is enabled
    if (!isInstabilityEnabled()) {
      return;
    }

    // Only process Mystcraft Ages
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return;
    }

    if (level.players().isEmpty()) {
      return;
    }

    // Get age instability
    AgeData ageData = AgeData.getIfPresent(level);
    if (ageData == null) {
      return;
    }

    float instability = ageData.getInstability();
    if (instability <= 0) {
      return;
    }

    // Process instability effects
    processInstabilityEffects(level, instability);
  }

  /**
   * Processes all instability effects for a level.
   * Uses configurable thresholds, chances, and global multiplier.
   */
  private static void processInstabilityEffects(ServerLevel level, float instability) {
    RandomSource random = level.random;
    List<ServerPlayer> players = level.players();

    if (players.isEmpty()) {
      return; // No players, no effects
    }

    // Get the global effect multiplier from config
    float multiplier = getEffectMultiplier();
    if (multiplier <= 0) {
      return; // Effects disabled via multiplier
    }

    // Decay spreading
    float thresholdDecay = getThresholdDecay();
    if (instability >= thresholdDecay && random.nextFloat() < calculateChance(instability, thresholdDecay, getChanceDecay(), multiplier)) {
      spawnDecay(level, players, random);
    }

    // Block transmutation
    float thresholdTransmute = getThresholdTransmute();
    if (instability >= thresholdTransmute && random.nextFloat() < calculateChance(instability, thresholdTransmute, getChanceTransmute(), multiplier)) {
      transmuteBlock(level, players, random);
    }

    // Lightning strikes
    float thresholdLightning = getThresholdLightning();
    if (instability >= thresholdLightning && random.nextFloat() < calculateChance(instability, thresholdLightning, getChanceLightning(), multiplier)) {
      spawnLightning(level, players, random);
    }

    // Meteor falls
    float thresholdMeteor = getThresholdMeteor();
    if (instability >= thresholdMeteor && random.nextFloat() < calculateChance(instability, thresholdMeteor, getChanceMeteor(), multiplier)) {
      spawnMeteor(level, players, random);
    }

    // Player effects (poison, hunger, wither)
    float thresholdPoison = getThresholdPoison();
    if (instability >= thresholdPoison && random.nextFloat() < calculateChance(instability, thresholdPoison, getChancePlayerEffect(), multiplier)) {
      applyPlayerEffects(level, players, instability, random);
    }
  }

  /**
   * Calculates the actual chance based on instability level above threshold.
   *
   * @param instability Current instability level
   * @param threshold   Threshold for this effect type
   * @param baseChance  Base probability per tick
   * @param multiplier  Global effect multiplier from config
   * @return Final chance to roll against
   */
  private static float calculateChance(float instability, float threshold, float baseChance, float multiplier) {
    float excess = instability - threshold;
    float factor = Math.min(excess / 30.0f, 5.0f); // Scale up to 6x base (1 + 5)
    return baseChance * (1.0f + factor) * multiplier;
  }

  /**
   * Spawns decay blocks near players.
   */
  private static void spawnDecay(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
    ServerPlayer target = players.get(random.nextInt(players.size()));
    int range = 32;

    int x = target.getBlockX() + random.nextIntBetweenInclusive(-range, range);
    int z = target.getBlockZ() + random.nextIntBetweenInclusive(-range, range);
    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

    BlockPos pos = new BlockPos(x, y, z);
    BlockState targetState = level.getBlockState(pos);

    // Only replace solid blocks (not air, water, bedrock)
    if (targetState.isAir() || targetState.is(Blocks.BEDROCK) || targetState.is(Blocks.WATER)) {
      return;
    }

    // Spawn a random decay type
    BlockState decayState = ModBlocks.DECAY.get().defaultBlockState();
    level.setBlock(pos, decayState, 3);

    Mystcraft.LOGGER.debug("Spawned decay at {}", pos);
  }

  /**
   * Transmutes a random block near a player into another block type.
   * Creates unexpected terrain changes in unstable ages.
   */
  private static void transmuteBlock(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
    ServerPlayer target = players.get(random.nextInt(players.size()));
    int range = 24;

    int x = target.getBlockX() + random.nextIntBetweenInclusive(-range, range);
    int z = target.getBlockZ() + random.nextIntBetweenInclusive(-range, range);
    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) - 1;

    // Try a few times to find a valid block
    for (int attempt = 0; attempt < 5; attempt++) {
      BlockPos pos = new BlockPos(x, y - attempt, z);
      BlockState currentState = level.getBlockState(pos);

      // Skip air, bedrock, and other special blocks
      if (currentState.isAir() || currentState.is(Blocks.BEDROCK) ||
          currentState.is(Blocks.WATER) || currentState.is(Blocks.LAVA) ||
          currentState.getBlock() instanceof net.minecraft.world.level.block.BaseEntityBlock) {
        continue;
      }

      // Choose a transmutation result
      BlockState newState = chooseTransmutation(currentState, random);
      if (newState != null && !newState.equals(currentState)) {
        level.setBlock(pos, newState, 3);
        Mystcraft.LOGGER.debug("Transmuted {} to {} at {}", currentState.getBlock(), newState.getBlock(), pos);
        return;
      }
    }
  }

  /**
   * Chooses what to transmute a block into.
   */
  private static BlockState chooseTransmutation(BlockState original, RandomSource random) {
    // Stone -> Cobblestone, Gravel, Sand, or Obsidian
    if (original.is(Blocks.STONE)) {
      return switch (random.nextInt(4)) {
        case 0 -> Blocks.COBBLESTONE.defaultBlockState();
        case 1 -> Blocks.GRAVEL.defaultBlockState();
        case 2 -> Blocks.SAND.defaultBlockState();
        default -> Blocks.OBSIDIAN.defaultBlockState();
      };
    }
    // Dirt -> Sand, Clay, Soul Sand, or Gravel
    if (original.is(Blocks.DIRT) || original.is(Blocks.GRASS_BLOCK)) {
      return switch (random.nextInt(4)) {
        case 0 -> Blocks.SAND.defaultBlockState();
        case 1 -> Blocks.CLAY.defaultBlockState();
        case 2 -> Blocks.SOUL_SAND.defaultBlockState();
        default -> Blocks.GRAVEL.defaultBlockState();
      };
    }
    // Sand -> Glass, Sandstone, or Soul Sand
    if (original.is(Blocks.SAND)) {
      return switch (random.nextInt(3)) {
        case 0 -> Blocks.GLASS.defaultBlockState();
        case 1 -> Blocks.SANDSTONE.defaultBlockState();
        default -> Blocks.SOUL_SAND.defaultBlockState();
      };
    }
    // Wood -> Coal block or Air (burned)
    if (original.getBlock() instanceof net.minecraft.world.level.block.RotatedPillarBlock &&
        original.is(net.minecraft.tags.BlockTags.LOGS)) {
      return random.nextBoolean()
          ? Blocks.COAL_BLOCK.defaultBlockState()
          : Blocks.AIR.defaultBlockState();
    }
    // Leaves -> Air (decay)
    if (original.is(net.minecraft.tags.BlockTags.LEAVES)) {
      return Blocks.AIR.defaultBlockState();
    }
    // Ores -> Stone (loss of ore) or different ore
    if (original.is(Blocks.IRON_ORE) || original.is(Blocks.COPPER_ORE) ||
        original.is(Blocks.COAL_ORE) || original.is(Blocks.GOLD_ORE)) {
      return random.nextBoolean() ? Blocks.STONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
    }
    // Cobblestone -> Mossy Cobblestone or Stone
    if (original.is(Blocks.COBBLESTONE)) {
      return random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
    // Water -> Ice or nothing
    if (original.is(Blocks.WATER)) {
      return Blocks.ICE.defaultBlockState();
    }
    // Default: no transmutation
    return null;
  }

  /**
   * Spawns lightning near players.
   */
  private static void spawnLightning(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
    ServerPlayer target = players.get(random.nextInt(players.size()));
    int range = 64;

    double x = target.getX() + random.nextIntBetweenInclusive(-range, range);
    double z = target.getZ() + random.nextIntBetweenInclusive(-range, range);
    double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int) x, (int) z);

    net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
    if (lightning != null) {
      lightning.moveTo(x, y, z);
      lightning.setVisualOnly(true);
      level.addFreshEntity(lightning);

      Mystcraft.LOGGER.debug("Spawned instability lightning at {}, {}, {}", x, y, z);
    }
  }

  /**
   * Spawns a meteor above players.
   */
  private static void spawnMeteor(ServerLevel level, List<ServerPlayer> players, RandomSource random) {
    ServerPlayer target = players.get(random.nextInt(players.size()));
    int range = 48;

    double x = target.getX() + random.nextIntBetweenInclusive(-range, range);
    double z = target.getZ() + random.nextIntBetweenInclusive(-range, range);
    double y = Math.min(target.getY() + 100 + random.nextInt(50), level.getMaxBuildHeight() - 1);

    int size = 1 + random.nextInt(3); // Size 1-3

    MeteorEntity meteor = new MeteorEntity(level, x, y, z, size);
    level.addFreshEntity(meteor);

    Mystcraft.LOGGER.debug("Spawned meteor at {}, {}, {} with size {}", x, y, z, size);
  }

  /**
   * Applies a random potion effect to a player.
   * Unstable ages are chaotic: ~30% chance positive, ~70% chance negative.
   */
  private static void applyPlayerEffects(ServerLevel level, List<ServerPlayer> players, float instability, RandomSource random) {
    ServerPlayer target = players.get(random.nextInt(players.size()));

    Object[][] pool;
    if (random.nextFloat() < 0.3f) {
      pool = POSITIVE_EFFECTS;
    } else {
      pool = NEGATIVE_EFFECTS;
    }

    Object[] chosen = pool[random.nextInt(pool.length)];
    net.minecraft.world.effect.MobEffect effect = (net.minecraft.world.effect.MobEffect) chosen[0];
    int duration = (int) chosen[1];
    int amplifier = (int) chosen[2];

    MobEffectInstance instance = MobEffectCompat.createInstance(effect, duration, amplifier);
    if (instance != null) {
      target.addEffect(instance);
    }
    Mystcraft.LOGGER.debug("Applied {} to player {}", effect.getDescriptionId(), target.getName().getString());
  }

  /**
   * Calculates instability for a set of symbol identifiers.
   * Uses the grammar system for proper symbol cost calculation.
   *
   * @param symbolIds List of symbol identifiers (ResourceLocation strings)
   * @return Total instability value
   */
  public static float calculateInstability(List<String> symbolIds) {
    if (symbolIds == null || symbolIds.isEmpty()) {
      return 100.0f; // Blank age is very unstable
    }

    // Convert symbol IDs to actual symbols
    List<IAgeSymbol> symbols = new ArrayList<>();
    for (String symbolId : symbolIds) {
      IAgeSymbol symbol = SymbolRegistry.get(new ResourceLocation(symbolId));
      if (symbol != null) {
        symbols.add(symbol);
      }
    }

    // Use CFG-based AgeBuilder for instability calculation
    long seed = System.currentTimeMillis();
    AgeBuilder builder = new AgeBuilder(symbols, seed);
    return builder.getInstability();
  }

  /**
   * Calculates instability for a list of symbols directly.
   *
   * @param symbols List of IAgeSymbol objects
   * @param seed    Random seed for generation
   * @return Total instability value
   */
  public static float calculateInstability(List<IAgeSymbol> symbols, long seed) {
    if (symbols == null || symbols.isEmpty()) {
      return 100.0f; // Blank age is very unstable
    }

    AgeBuilder builder = new AgeBuilder(symbols, seed);
    return builder.getInstability();
  }

  /**
   * Adds instability to an Age.
   */
  public static void addInstability(ServerLevel level, float amount) {
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return;
    }

    AgeData ageData = AgeData.get(level);
    ageData.addInstability(amount);

    Mystcraft.LOGGER.debug("Added {} instability to age, total: {}", amount, ageData.getInstability());
  }

  /**
   * Checks if an Age with the given instability level is allowed to be created/linked.
   *
   * @param instability The instability level of the Age
   * @return true if the Age is allowed, false if it should be blocked
   */
  public static boolean isAgeAllowed(float instability) {
    if (MystcraftConfig.allowUnstableAges.get()) {
      return true;
    }

    float maxAllowed = MystcraftConfig.maxAllowedInstability.get().floatValue();
    return instability <= maxAllowed;
  }

  /**
   * Gets the maximum allowed instability from config.
   *
   * @return Maximum allowed instability value
   */
  public static float getMaxAllowedInstability() {
    return MystcraftConfig.maxAllowedInstability.get().floatValue();
  }

  /**
   * Checks if the instability system is enabled.
   *
   * @return true if instability effects are active
   */
  public static boolean isEnabled() {
    return isInstabilityEnabled();
  }

  /**
   * Gets the current effect multiplier from config.
   *
   * @return The multiplier applied to all effect chances
   */
  public static float getMultiplier() {
    return getEffectMultiplier();
  }

  /**
   * Gets a human-readable instability rating for display purposes.
   *
   * @param instability The instability value
   * @return A string rating (Stable, Unstable, Dangerous, etc.)
   */
  public static String getInstabilityRating(float instability) {
    if (instability <= 0) {
      return "Perfectly Stable";
    } else if (instability <= 10) {
      return "Stable";
    } else if (instability <= 30) {
      return "Slightly Unstable";
    } else if (instability <= 50) {
      return "Unstable";
    } else if (instability <= 80) {
      return "Very Unstable";
    } else if (instability <= 100) {
      return "Dangerous";
    } else if (instability <= 150) {
      return "Extremely Dangerous";
    } else {
      return "Catastrophic";
    }
  }
}
