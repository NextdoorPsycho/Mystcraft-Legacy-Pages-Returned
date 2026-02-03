package art.arcane.mystcraft.event;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;
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
 * <p>
 * 1.18.2 version - uses TranslatableComponent instead of Component.translatable().
 */
public class AgeDeathHandler {

  private static final Random RANDOM = new Random();

  // Tracks death counts per player per Age dimension (resets on server restart)
  private static final Map<UUID, Map<ResourceKey<Level>, Integer>> DEATH_COUNTS = new ConcurrentHashMap<>();

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
   * Sends narrative death messages.
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
    // 1.18.2: Use TranslatableComponent instead of Component.translatable()
    Component message = new TranslatableComponent(messageKey, playerName);

    // Send to dying player and all players in the same Age
    for (ServerPlayer p : level.players()) {
      p.sendMessage(message, net.minecraft.Util.NIL_UUID);
    }
    // Also send to the dying player if they're not in the player list yet (edge case)
    if (!level.players().contains(player)) {
      player.sendMessage(message, net.minecraft.Util.NIL_UUID);
    }

    // Deaths no longer add instability or apply respawn debuffs.
  }

  /**
   * Handles player respawn after dying in a Mystcraft Age.
   */
  public static void onPlayerRespawn(ServerPlayer player, ServerLevel level) {
    if (!MystcraftConfig.deathEffectsEnabled.get()) {
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
   * 1.18.2: Use DamageSource methods.
   */
  private static String getCauseSpecificMessage(DamageSource source) {
    String msgId = source.getMsgId();

    // Void/fall damage
    if (msgId.equals("outOfWorld") || msgId.equals("fall")) {
      return MSG_VOID_FALL;
    }
    // Fire damage
    if (msgId.equals("inFire") || msgId.equals("onFire") || msgId.equals("lava")) {
      return MSG_FIRE;
    }
    // Magic/wither damage
    if (msgId.equals("wither") || msgId.equals("magic") || msgId.equals("indirectMagic")) {
      // Check if this is decay block damage (magic type from DecayBlock)
      if (msgId.contains("decay")) {
        return MSG_DECAY;
      }
      return MSG_MAGIC;
    }
    // Explosion damage
    if (msgId.equals("explosion") || msgId.equals("explosion.player")) {
      return MSG_EXPLOSION;
    }
    // Mob damage
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

  /**
   * Disables vanilla death messages for Mystcraft Ages so custom messages can replace them.
   */
  public static void configureAgeGameRules(ServerLevel level) {
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return;
    }
    level.getGameRules().getRule(GameRules.RULE_SHOWDEATHMESSAGES)
        .set(false, level.getServer());
  }
}
