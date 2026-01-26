package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

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

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) return;

        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData == null) return;

        // Apply weather control
        handleWeather(level, ageData);

        // Apply environmental effects
        handleEnvironmentEffects(level, ageData);

        // Handle accelerated time
        handleAcceleratedTime(level, ageData);
    }

    /**
     * Controls weather based on the Age's weather type setting.
     */
    private static void handleWeather(ServerLevel level, AgeData ageData) {
        String weatherType = ageData.getWeatherType();

        switch (weatherType) {
            case "off" -> {
                // Clear weather always
                if (level.isRaining() || level.isThundering()) {
                    level.setWeatherParameters(6000, 0, false, false);
                }
            }
            case "always", "rain" -> {
                // Always raining (not thunder)
                if (!level.isRaining() || level.isThundering()) {
                    level.setWeatherParameters(0, 6000, true, false);
                }
            }
            case "snow" -> {
                // Always raining (snow handled by biome temperature)
                if (!level.isRaining()) {
                    level.setWeatherParameters(0, 6000, true, false);
                }
            }
            case "storm" -> {
                // Always thunderstorm
                if (!level.isThundering()) {
                    level.setWeatherParameters(0, 6000, true, true);
                }
            }
            case "cloudy" -> {
                // Raining but with reduced rain level effect (visual only)
                // Forge doesn't easily support this, so we'll just keep it not raining
                // but cloudy effect would need client-side rendering
            }
            case "fast" -> {
                // Weather changes faster - reduce duration
                // This is handled by vanilla, we just let it cycle naturally
                // but can force shorter weather periods
            }
            case "slow" -> {
                // Weather changes slower - extend duration
                // This is harder to implement without reflection
            }
            // "normal" - let vanilla handle it
        }
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

        // Random lightning
        if (ageData.isLightningEnabled()) {
            // 2% chance per second per player
            for (ServerPlayer player : level.players()) {
                if (random.nextFloat() < 0.02f) {
                    spawnLightningNearPlayer(level, player);
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
     * Handles accelerated time (faster day/night cycle).
     */
    private static void handleAcceleratedTime(ServerLevel level, AgeData ageData) {
        if (!ageData.isAcceleratedEnabled()) return;

        // Advance time faster - add extra time each tick
        // Normal: 24000 ticks = 20 minutes
        // Accelerated: double speed = 10 minutes
        long dayTime = level.getDayTime();
        level.setDayTime(dayTime + 1); // Adds 1 extra tick, making time 2x faster
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
