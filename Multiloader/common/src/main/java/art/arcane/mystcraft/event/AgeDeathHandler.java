package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles thematic death effects when players die in Mystcraft Ages.
 * Sends narrative death messages, applies instability surges, and inflicts respawn debuffs
 * based on the Age's instability level.
 */
public class AgeDeathHandler {

    private static final Random RANDOM = new Random();

    // Tracks death counts per player per Age dimension (resets on server restart)
    private static final Map<UUID, Map<ResourceKey<Level>, Integer>> DEATH_COUNTS = new ConcurrentHashMap<>();

    // Tracks the instability level at death for players who died in an unstable Age.
    // Used to scale respawn debuffs proportionally. Removed on respawn.
    private static final Map<UUID, Float> DEATH_INSTABILITY = new ConcurrentHashMap<>();

    // --- Death Messages by Instability Tier ---

    private static final String[] MESSAGES_LOW = {
            "mystcraft.death.low.release",
            "mystcraft.death.low.ink_dries",
            "mystcraft.death.low.thread_unravels"
    };

    private static final String[] MESSAGES_MEDIUM = {
            "mystcraft.death.medium.shudders",
            "mystcraft.death.medium.stricken",
            "mystcraft.death.medium.no_mourn"
    };

    private static final String[] MESSAGES_HIGH = {
            "mystcraft.death.high.devours",
            "mystcraft.death.high.ink_runs_black",
            "mystcraft.death.high.reality_tears",
            "mystcraft.death.high.already_dying"
    };

    // --- Cause-specific Messages ---

    private static final String MSG_VOID_FALL = "mystcraft.death.cause.void_fall";
    private static final String MSG_FIRE = "mystcraft.death.cause.fire";
    private static final String MSG_MAGIC = "mystcraft.death.cause.magic";
    private static final String MSG_EXPLOSION = "mystcraft.death.cause.explosion";
    private static final String MSG_MOB = "mystcraft.death.cause.mob";
    private static final String MSG_DECAY = "mystcraft.death.cause.decay";

    // --- Repeat Death Messages ---

    private static final String[] MESSAGES_REPEAT = {
            "mystcraft.death.repeat.remembers",
            "mystcraft.death.repeat.ink_knows",
            "mystcraft.death.repeat.how_many"
    };

    /**
     * Handles player death in a Mystcraft Age.
     * Sends narrative death messages, applies instability surges, and stores data for respawn debuffs.
     */
    public static void onPlayerDeath(ServerPlayer player, DamageSource source, ServerLevel level) {
        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) return;

        if (!MystcraftConfig.deathEffectsEnabled.get()) return;

        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData == null) return;

        float instability = ageData.getInstability();
        String playerName = player.getName().getString();
        ResourceKey<Level> dimensionKey = level.dimension();

        // Track death count
        int deathCount = incrementDeathCount(player.getUUID(), dimensionKey);

        // Select and send death message
        String messageKey = selectDeathMessage(instability, deathCount, source);
        Component message = Component.translatable(messageKey, playerName);

        // Send to dying player and all players in the same Age
        for (ServerPlayer p : level.players()) {
            p.sendSystemMessage(message);
        }
        // Also send to the dying player if they're not in the player list yet (edge case)
        if (!level.players().contains(player)) {
            player.sendSystemMessage(message);
        }

        // Instability surge and respawn debuffs scale with severity above 50.
        // severity is 0.0 at instability 50, 1.0 at instability 100+
        if (instability >= 50.0f) {
            float severity = Math.min((instability - 50.0f) / 50.0f, 1.0f);

            // Surge scales: 0.5 at 50 instability up to 8.0 at 100+
            float surge = 0.5f + (severity * 7.5f);
            ageData.addInstability(surge);
            Mystcraft.LOGGER.debug("[AgeDeathHandler] Death instability surge: +{} (severity={}, now {})",
                    surge, severity, ageData.getInstability());

            // Store instability for scaled respawn debuffs
            DEATH_INSTABILITY.put(player.getUUID(), instability);
        }
    }

    /**
     * Handles player respawn after dying in a Mystcraft Age.
     * Applies debuffs scaled to the instability level at the time of death.
     */
    public static void onPlayerRespawn(ServerPlayer player, ServerLevel level) {
        if (!MystcraftConfig.deathEffectsEnabled.get()) return;

        Float instability = DEATH_INSTABILITY.remove(player.getUUID());
        if (instability == null) return;

        // severity: 0.0 at instability 50, 1.0 at instability 100+
        float severity = Math.min((instability - 50.0f) / 50.0f, 1.0f);

        // Slowness: always present at 50+, duration scales 4s to 20s
        int slowDuration = (int) (80 + severity * 320); // 4s (80 ticks) to 20s (400 ticks)
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, 0));

        // Mining Fatigue: kicks in at severity 0.3 (instability ~65), duration scales 5s to 30s
        if (severity >= 0.3f) {
            int fatigueDuration = (int) (100 + ((severity - 0.3f) / 0.7f) * 500); // 5s to 30s
            int fatigueAmp = severity >= 0.7f ? 1 : 0; // Amplifier II only at instability ~85+
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, fatigueDuration, fatigueAmp));
        }

        // Darkness: kicks in at severity 0.6 (instability ~80), duration scales 3s to 10s
        if (severity >= 0.6f) {
            int darknessDuration = (int) (60 + ((severity - 0.6f) / 0.4f) * 140); // 3s to 10s
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, darknessDuration, 0));
        }
    }

    /**
     * Selects the appropriate death message based on instability, death count, and damage source.
     */
    private static String selectDeathMessage(float instability, int deathCount, DamageSource source) {
        // Repeat death messages override after 3+ deaths in same Age
        if (deathCount >= 3 && RANDOM.nextFloat() < 0.6f) {
            return MESSAGES_REPEAT[RANDOM.nextInt(MESSAGES_REPEAT.length)];
        }

        // Cause-specific messages override tier-based (any instability)
        String causeMessage = getCauseSpecificMessage(source);
        if (causeMessage != null && RANDOM.nextFloat() < 0.5f) {
            return causeMessage;
        }

        // Tier-based messages
        if (instability > 80.0f) {
            return MESSAGES_HIGH[RANDOM.nextInt(MESSAGES_HIGH.length)];
        } else if (instability >= 50.0f) {
            return MESSAGES_MEDIUM[RANDOM.nextInt(MESSAGES_MEDIUM.length)];
        } else {
            return MESSAGES_LOW[RANDOM.nextInt(MESSAGES_LOW.length)];
        }
    }

    /**
     * Returns a cause-specific death message key, or null if no special message applies.
     */
    private static String getCauseSpecificMessage(DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.FALL)) {
            return MSG_VOID_FALL;
        }
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)) {
            return MSG_FIRE;
        }
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            // Check if this is decay block damage (magic type from DecayBlock)
            if (source.getMsgId().contains("decay")) {
                return MSG_DECAY;
            }
            return MSG_MAGIC;
        }
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            return MSG_EXPLOSION;
        }
        if (source.getEntity() != null && !(source.getEntity() instanceof ServerPlayer)) {
            return MSG_MOB;
        }
        return null;
    }

    /**
     * Increments and returns the death count for a player in a specific Age.
     */
    private static int incrementDeathCount(UUID playerId, ResourceKey<Level> dimension) {
        Map<ResourceKey<Level>, Integer> playerDeaths = DEATH_COUNTS.computeIfAbsent(
                playerId, k -> new HashMap<>());
        int count = playerDeaths.getOrDefault(dimension, 0) + 1;
        playerDeaths.put(dimension, count);
        return count;
    }
}
