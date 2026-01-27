package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IWeatherController;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.instability.InstabilityController;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.weather.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles server-side Age effects based on symbol configuration.
 * - Weather control
 * - Environment effects (meteors, lightning, explosions)
 * - PvP prevention
 * - Accelerated time
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class AgeEffectsHandler {

    private static final Random random = new Random();

    // Per-dimension weather controller instances, lazily created from the weather type string.
    // These maintain internal state (timers, rain levels) across ticks.
    private static final Map<ResourceKey<Level>, IWeatherController> weatherControllers = new ConcurrentHashMap<>();

    // Per-dimension instability controllers that manage deck-based effect activation and ticking.
    private static final Map<ResourceKey<Level>, InstabilityController> instabilityControllers = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) return;

        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData == null) return;

        // Apply weather control
        handleWeather(level, ageData);

        // Apply environmental effects (flag-based: meteors, lightning, explosions, scorched)
        handleEnvironmentEffects(level, ageData);

        // Tick instability controller (deck-based: decay, crumble, erosion, potion effects, etc.)
        tickInstabilityController(level, ageData);

        // Handle time scaling (accelerated, slow, static, etc.)
        handleTimescale(level, ageData);
    }

    /**
     * Controls weather using the actual IWeatherController implementation.
     * Controllers are cached per-dimension and maintain their own internal state
     * (timers, rain levels, transitions).
     * All weather types including "normal" are handled by Mystcraft controllers
     * because vanilla weather cycling is unreliable in custom dimensions.
     */
    private static void handleWeather(ServerLevel level, AgeData ageData) {
        String weatherType = ageData.getWeatherType();

        // Get or create the weather controller for this dimension
        IWeatherController controller = weatherControllers.computeIfAbsent(
                level.dimension(), key -> createWeatherController(weatherType));

        // Let the controller manage weather state
        controller.updateWeather(level);
    }

    /**
     * Creates an IWeatherController instance from the weather type string.
     */
    private static IWeatherController createWeatherController(String type) {
        return switch (type) {
            case "off" -> new WeatherControllerNever();
            case "always", "rain" -> new WeatherControllerAlwaysRain();
            case "snow" -> new WeatherControllerSnow();
            case "storm", "thunder" -> new WeatherControllerAlwaysThunder();
            case "cloudy" -> new WeatherControllerCloudy();
            case "fast" -> new WeatherControllerFast();
            case "slow" -> new WeatherControllerSlow();
            case "blizzard" -> new WeatherControllerBlizzard();
            default -> new WeatherControllerNormal();
        };
    }

    /**
     * Handles environmental effects like meteors, lightning, and explosions.
     */
    private static void handleEnvironmentEffects(ServerLevel level, AgeData ageData) {
        // Only process occasionally to reduce performance impact
        if (level.getGameTime() % 20 != 0) return;

        // Get players in this dimension
        if (level.players().isEmpty()) return;

        // Meteors
        if (ageData.areMeteorsEnabled()) {
            // 1% chance per second per player
            for (ServerPlayer player : level.players()) {
                if (random.nextFloat() < 0.01f) {
                    spawnMeteorNearPlayer(level, player);
                }
            }
        }

        // Random lightning - frequent strikes across the landscape
        if (ageData.isLightningEnabled()) {
            for (ServerPlayer player : level.players()) {
                // ~15% chance per second per player, plus 1-3 extra bolts each time
                if (random.nextFloat() < 0.15f) {
                    int bolts = 1 + random.nextInt(3);
                    for (int i = 0; i < bolts; i++) {
                        spawnLightningNearPlayer(level, player);
                    }
                }
            }
        }

        // Random explosions
        if (ageData.areExplosionsEnabled()) {
            // 0.5% chance per second per player
            for (ServerPlayer player : level.players()) {
                if (random.nextFloat() < 0.005f) {
                    spawnExplosionNearPlayer(level, player);
                }
            }
        }

        // Scorched earth (fire spread, block damage)
        if (ageData.isScorchedEnabled()) {
            // Handled by making blocks more flammable - would need more complex implementation
            // For now, just spawn occasional fire
            if (random.nextFloat() < 0.01f) {
                for (ServerPlayer player : level.players()) {
                    if (random.nextFloat() < 0.1f) {
                        BlockPos pos = player.blockPosition().offset(
                                random.nextInt(32) - 16,
                                random.nextInt(16) - 8,
                                random.nextInt(32) - 16
                        );
                        if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).isSolid()) {
                            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    /**
     * Ticks the instability controller for this level.
     * The controller manages deck-based effect activation (decay, crumble, erosion, potions, etc.)
     * and ticks all active IEnvironmentalEffect instances on loaded chunks near players.
     */
    private static void tickInstabilityController(ServerLevel level, AgeData ageData) {
        if (!MystcraftConfig.instabilityEnabled.get()) return;
        if (ageData.getInstability() <= 0) return;
        if (level.players().isEmpty()) return;

        InstabilityController controller = instabilityControllers.computeIfAbsent(
                level.dimension(),
                key -> new InstabilityController(level, ageData, level.getSeed()));

        if (!controller.isEnabled()) return;

        // Tick effects on chunks near each player
        for (ServerPlayer player : level.players()) {
            int chunkX = player.getBlockX() >> 4;
            int chunkZ = player.getBlockZ() >> 4;

            // Process a radius of chunks around each player
            int radius = 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                    if (chunk != null) {
                        controller.tick(chunk);
                    }
                }
            }
        }
    }

    /**
     * Handles day/night cycle speed based on the timescale value.
     * Timescale 1.0 = normal (no-op), 2.0 = double speed, 0.5 = half speed, 0.0 = frozen.
     * The accelerated flag is treated as timescale 2.0 for backwards compatibility.
     */
    private static void handleTimescale(ServerLevel level, AgeData ageData) {
        float timescale = ageData.getTimescale();

        // Accelerated flag acts as timescale 2.0 if no explicit timescale was set
        if (ageData.isAcceleratedEnabled() && timescale == 1.0f) {
            timescale = 2.0f;
        }

        if (timescale == 1.0f) return;

        long dayTime = level.getDayTime();

        if (timescale == 0.0f) {
            // Static time: rewind the tick that just happened
            level.setDayTime(dayTime - 1);
        } else if (timescale > 1.0f) {
            // Faster: add extra ticks (e.g. timescale 2.0 adds 1 extra tick per game tick)
            int extraTicks = Math.round(timescale - 1.0f);
            level.setDayTime(dayTime + extraTicks);
        } else {
            // Slower: periodically rewind ticks to reduce effective speed.
            // For timescale 0.5, we need to cancel every other tick's time advancement.
            // Interval = 1 / (1 - timescale). E.g. 0.5 -> every 2 ticks, 0.25 -> every 1.33 ticks.
            // Use a fractional accumulator approach via game time modulus.
            float skipRate = 1.0f - timescale;
            long gameTime = level.getGameTime();
            // Determine how many ticks to rewind this tick using a consistent pattern
            if ((gameTime % Math.max(1, Math.round(1.0f / skipRate))) != 0) {
                level.setDayTime(dayTime - 1);
            }
        }
    }

    /**
     * Spawns a meteor near a player.
     */
    private static void spawnMeteorNearPlayer(ServerLevel level, ServerPlayer player) {
        double x = player.getX() + (random.nextDouble() - 0.5) * 64;
        double z = player.getZ() + (random.nextDouble() - 0.5) * 64;
        double y = player.getY() + 50 + random.nextDouble() * 30;

        try {
            MeteorEntity meteor = new MeteorEntity(ModEntities.METEOR.get(), level);
            meteor.setPos(x, y, z);
            level.addFreshEntity(meteor);
        } catch (Exception e) {
            // Meteor entity may not be fully implemented yet
            Mystcraft.LOGGER.debug("Could not spawn meteor: {}", e.getMessage());
        }
    }

    /**
     * Spawns lightning near a player.
     */
    private static void spawnLightningNearPlayer(ServerLevel level, ServerPlayer player) {
        double x = player.getX() + (random.nextDouble() - 0.5) * 48;
        double z = player.getZ() + (random.nextDouble() - 0.5) * 48;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) x, (int) z);

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(Vec3.atBottomCenterOf(new BlockPos((int) x, y, (int) z)));
            lightning.setVisualOnly(false);
            level.addFreshEntity(lightning);
        }
    }

    /**
     * Spawns an explosion near a player.
     */
    private static void spawnExplosionNearPlayer(ServerLevel level, ServerPlayer player) {
        double x = player.getX() + (random.nextDouble() - 0.5) * 32;
        double z = player.getZ() + (random.nextDouble() - 0.5) * 32;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) x, (int) z);

        // Create explosion - size 2 is relatively small
        level.explode(null, x, y, z, 2.0f, Level.ExplosionInteraction.BLOCK);
    }

    /**
     * Prevents PvP damage in Ages with PvP disabled.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;

        Level level = victim.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!AgeDimensionFactory.isMystcraftAge(serverLevel.dimension())) return;

        AgeData ageData = AgeData.getIfPresent(serverLevel);
        if (ageData == null) return;

        if (!ageData.isPvPEnabled()) {
            event.setCanceled(true);
        }
    }
}
