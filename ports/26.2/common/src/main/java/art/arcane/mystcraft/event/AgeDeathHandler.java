package art.arcane.mystcraft.event;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles thematic death effects when players die in Mystcraft Ages. Sends
 * narrative death messages, applies instability surges, and inflicts respawn
 * debuffs based on the Age's instability level.
 */
public class AgeDeathHandler {

  private static final Random RANDOM = new Random();

  private static final Map<UUID, Map<ResourceKey<Level>, Integer>> DEATH_COUNTS = new ConcurrentHashMap<>();

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

  private static final String MSG_VOID_FALL = "mystcraft.death.cause.void_fall";
  private static final String MSG_FIRE = "mystcraft.death.cause.fire";
  private static final String MSG_MAGIC = "mystcraft.death.cause.magic";
  private static final String MSG_EXPLOSION = "mystcraft.death.cause.explosion";
  private static final String MSG_MOB = "mystcraft.death.cause.mob";
  private static final String MSG_DECAY = "mystcraft.death.cause.decay";

  private static final String[] MESSAGES_REPEAT = {
      "mystcraft.death.repeat.remembers",
      "mystcraft.death.repeat.ink_knows",
      "mystcraft.death.repeat.how_many"
  };

  /**
   * Handles player death in a Mystcraft Age. Sends narrative death messages.
   */
  public static void onPlayerDeath(ServerPlayer player, DamageSource source, ServerLevel level) {
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) return;

    if (!MystcraftConfig.deathEffectsEnabled.get()) return;

    AgeData ageData = AgeData.getIfPresent(level);
    if (ageData == null) return;

    float instability = ageData.getInstability();
    String playerName = player.getName().getString();
    ResourceKey<Level> dimensionKey = level.dimension();

    int deathCount = incrementDeathCount(player.getUUID(), dimensionKey);

    String messageKey = selectDeathMessage(instability, deathCount, source);
    Component message = Component.translatable(messageKey, playerName);

    for (ServerPlayer p : level.players()) {
      p.sendSystemMessage(message);
    }

    if (!level.players().contains(player)) {
      player.sendSystemMessage(message);
    }

  }

  /**
   * Handles player respawn after dying in a Mystcraft Age.
   */
  public static void onPlayerRespawn(ServerPlayer player, ServerLevel level) {
    if (!MystcraftConfig.deathEffectsEnabled.get()) {
    }
  }

  private static String selectDeathMessage(float instability, int deathCount, DamageSource source) {

    if (deathCount >= 3 && RANDOM.nextFloat() < 0.6f) {
      return MESSAGES_REPEAT[RANDOM.nextInt(MESSAGES_REPEAT.length)];
    }

    String causeMessage = getCauseSpecificMessage(source);
    if (causeMessage != null && RANDOM.nextFloat() < 0.5f) {
      return causeMessage;
    }

    if (instability > 80.0f) {
      return MESSAGES_HIGH[RANDOM.nextInt(MESSAGES_HIGH.length)];
    } else if (instability >= 50.0f) {
      return MESSAGES_MEDIUM[RANDOM.nextInt(MESSAGES_MEDIUM.length)];
    } else {
      return MESSAGES_LOW[RANDOM.nextInt(MESSAGES_LOW.length)];
    }
  }

  private static String getCauseSpecificMessage(DamageSource source) {
    if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.FALL)) {
      return MSG_VOID_FALL;
    }
    if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA)) {
      return MSG_FIRE;
    }
    if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {

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

  private static int incrementDeathCount(UUID playerId, ResourceKey<Level> dimension) {
    Map<ResourceKey<Level>, Integer> playerDeaths = DEATH_COUNTS.computeIfAbsent(
        playerId, k -> new HashMap<>());
    int count = playerDeaths.getOrDefault(dimension, 0) + 1;
    playerDeaths.put(dimension, count);
    return count;
  }

  /**
   * Retained as a level-load hook for loader parity. In 26.2 gamerules are
   * server-global; {@code ServerPlayerDeathMessageMixin} scopes suppression to
   * the player currently dying in an Age instead of mutating the server rule.
   */
  public static void configureAgeGameRules(ServerLevel level) {
    // Intentionally no-op on 26.2.
  }
}
