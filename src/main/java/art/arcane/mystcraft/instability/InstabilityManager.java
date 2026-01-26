package art.arcane.mystcraft.instability;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.grammar.GrammarGenerator;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages instability effects in Mystcraft Ages.
 * Higher instability leads to more frequent and severe negative effects.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public final class InstabilityManager {

    // Effect thresholds (instability level required to trigger)
    private static final float THRESHOLD_DECAY = 20.0f;
    private static final float THRESHOLD_TRANSMUTE = 30.0f;
    private static final float THRESHOLD_LIGHTNING = 40.0f;
    private static final float THRESHOLD_METEOR = 60.0f;
    private static final float THRESHOLD_POISON = 80.0f;
    private static final float THRESHOLD_WITHER = 100.0f;

    // Base chances per tick (at maximum instability for each tier)
    private static final float CHANCE_DECAY = 0.001f;        // ~0.1% per tick
    private static final float CHANCE_TRANSMUTE = 0.002f;    // ~0.2% per tick
    private static final float CHANCE_LIGHTNING = 0.0005f;   // ~0.05% per tick
    private static final float CHANCE_METEOR = 0.0002f;      // ~0.02% per tick
    private static final float CHANCE_PLAYER_EFFECT = 0.0001f; // ~0.01% per tick

    private InstabilityManager() {
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        // Only process Mystcraft Ages
        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
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
     */
    private static void processInstabilityEffects(ServerLevel level, float instability) {
        RandomSource random = level.random;
        List<ServerPlayer> players = level.players();

        if (players.isEmpty()) {
            return; // No players, no effects
        }

        // Decay spreading
        if (instability >= THRESHOLD_DECAY && random.nextFloat() < calculateChance(instability, THRESHOLD_DECAY, CHANCE_DECAY)) {
            spawnDecay(level, players, random);
        }

        // Block transmutation
        if (instability >= THRESHOLD_TRANSMUTE && random.nextFloat() < calculateChance(instability, THRESHOLD_TRANSMUTE, CHANCE_TRANSMUTE)) {
            transmuteBlock(level, players, random);
        }

        // Lightning strikes
        if (instability >= THRESHOLD_LIGHTNING && random.nextFloat() < calculateChance(instability, THRESHOLD_LIGHTNING, CHANCE_LIGHTNING)) {
            spawnLightning(level, players, random);
        }

        // Meteor falls
        if (instability >= THRESHOLD_METEOR && random.nextFloat() < calculateChance(instability, THRESHOLD_METEOR, CHANCE_METEOR)) {
            spawnMeteor(level, players, random);
        }

        // Player effects
        if (instability >= THRESHOLD_POISON && random.nextFloat() < calculateChance(instability, THRESHOLD_POISON, CHANCE_PLAYER_EFFECT)) {
            applyPlayerEffects(level, players, instability, random);
        }
    }

    /**
     * Calculates the actual chance based on instability level above threshold.
     */
    private static float calculateChance(float instability, float threshold, float baseChance) {
        float excess = instability - threshold;
        float factor = Math.min(excess / 50.0f, 2.0f); // Cap at 2x base chance
        return baseChance * (1.0f + factor);
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
        // Wood -> Coal block, Air (burned), or Fire
        if (original.getBlock() instanceof net.minecraft.world.level.block.RotatedPillarBlock &&
            original.is(net.minecraft.tags.BlockTags.LOGS)) {
            return switch (random.nextInt(3)) {
                case 0 -> Blocks.COAL_BLOCK.defaultBlockState();
                case 1 -> Blocks.AIR.defaultBlockState();
                default -> Blocks.FIRE.defaultBlockState();
            };
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
            lightning.setVisualOnly(false);
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
     * Applies negative effects to players.
     */
    private static void applyPlayerEffects(ServerLevel level, List<ServerPlayer> players, float instability, RandomSource random) {
        ServerPlayer target = players.get(random.nextInt(players.size()));

        MobEffectInstance effect;
        if (instability >= THRESHOLD_WITHER && random.nextFloat() < 0.3f) {
            // Wither effect at very high instability
            effect = new MobEffectInstance(MobEffects.WITHER, 100, 0); // 5 seconds, level 1
        } else if (random.nextFloat() < 0.5f) {
            // Poison
            effect = new MobEffectInstance(MobEffects.POISON, 100, 0);
        } else {
            // Hunger
            effect = new MobEffectInstance(MobEffects.HUNGER, 200, 1);
        }

        target.addEffect(effect);
        Mystcraft.LOGGER.debug("Applied {} to player {}", effect.getEffect().getDescriptionId(), target.getName().getString());
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

        // Use grammar system to calculate instability
        long seed = System.currentTimeMillis();
        GrammarGenerator.GenerationResult result = GrammarGenerator.generateAge(symbols, seed);

        return result.getInstability();
    }

    /**
     * Calculates instability for a list of symbols directly.
     *
     * @param symbols List of IAgeSymbol objects
     * @param seed Random seed for generation
     * @return Total instability value
     */
    public static float calculateInstability(List<IAgeSymbol> symbols, long seed) {
        if (symbols == null || symbols.isEmpty()) {
            return 100.0f; // Blank age is very unstable
        }

        GrammarGenerator.GenerationResult result = GrammarGenerator.generateAge(symbols, seed);
        return result.getInstability();
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
}
