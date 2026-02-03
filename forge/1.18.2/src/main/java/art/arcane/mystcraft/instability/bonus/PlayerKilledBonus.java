package art.arcane.mystcraft.instability.bonus;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.instability.InstabilityBonusManager.IInstabilityBonus;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Instability bonus that increases instability when a player dies in an Age.
 * Death caused by another player gives full penalty, other deaths give half.
 * The penalty decays over time.
 * <p>
 * Event registration is handled by the platform module. The common logic methods
 * (onPlayerDeath, onPlayerLogin, onPlayerChangedDimension) are called from
 * platform-specific event handlers.
 * <p>
 * 1.18.2 version - uses TranslatableComponent instead of Component.translatable().
 */
public class PlayerKilledBonus implements IInstabilityBonus {

  private final String playerName;
  private final UUID playerId;
  private final int dimensionId;

  private final int maxPenalty;
  private final float decayRate;
  private float currentPenalty;

  /**
   * Creates a player killed bonus tracker.
   *
   * @param playerId    The player's UUID
   * @param playerName  The player's name
   * @param dimensionId The dimension ID of the Age
   * @param maxPenalty  Maximum penalty when player is killed
   * @param decayRate   How fast the penalty decays per tick
   */
  public PlayerKilledBonus(UUID playerId, String playerName, int dimensionId, int maxPenalty, float decayRate) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.dimensionId = dimensionId;
    this.maxPenalty = maxPenalty;
    this.decayRate = decayRate;
    this.currentPenalty = 0;
  }

  @Override
  public String getName() {
    return "Player Killed: " + playerName;
  }

  @Override
  public int getValue() {
    // Return negative value (penalty = increases instability)
    return -(int) currentPenalty;
  }

  @Override
  public void tick(ServerLevel level) {
    // Decay the penalty over time
    if (currentPenalty > 0) {
      currentPenalty = Math.max(0, currentPenalty - decayRate);
    }
  }

  /**
   * Called when a player dies. Should be invoked from platform event handlers.
   *
   * @param player       The player who died
   * @param killerPlayer The player who killed them, or null if not a PvP death
   */
  public void onPlayerDeath(Player player, Player killerPlayer) {
    // Check if this is our tracked player
    if (!player.getUUID().equals(playerId)) {
      return;
    }

    // Check if death occurred in our dimension
    if (!AgeDimensionFactory.isMystcraftAge(player.getLevel().dimension())) {
      return;
    }

    int ageId = getAgeUID(player.getLevel());
    if (ageId != dimensionId) {
      return;
    }

    // Check if killed by another player (PvP death)
    if (killerPlayer != null && !killerPlayer.getUUID().equals(playerId)) {
      // Full penalty for PvP death
      currentPenalty = maxPenalty;
      announceToAge(player.getLevel(), "instability.bonus.death",
          playerName, killerPlayer.getName().getString());
    } else {
      // Half penalty for other deaths
      currentPenalty = Math.max(currentPenalty, maxPenalty / 2.0f);
      announceToAge(player.getLevel(), "instability.bonus.death.partial", playerName);
    }
  }

  /**
   * Called when a player logs in. Should be invoked from platform event handlers.
   */
  public void onPlayerLogin(ServerPlayer player) {
    if (!player.getUUID().equals(playerId)) {
      return;
    }

    if (!AgeDimensionFactory.isMystcraftAge(player.getLevel().dimension())) {
      return;
    }

    int ageId = getAgeUID(player.getLevel());
    if (ageId == dimensionId && currentPenalty > 0) {
      // 1.18.2: Use TranslatableComponent instead of Component.translatable()
      player.sendMessage(new TranslatableComponent("instability.bonus.death.alert", playerName), net.minecraft.Util.NIL_UUID);
    }
  }

  /**
   * Called when a player changes dimension. Should be invoked from platform event handlers.
   */
  public void onPlayerChangedDimension(ServerPlayer player, ResourceKey<Level> to) {
    if (!player.getUUID().equals(playerId)) {
      return;
    }

    if (!AgeDimensionFactory.isMystcraftAge(to)) {
      return;
    }

    int ageId = getAgeUIDFromKey(to);
    if (ageId == dimensionId && currentPenalty > 0) {
      // 1.18.2: Use TranslatableComponent instead of Component.translatable()
      player.sendMessage(new TranslatableComponent("instability.bonus.death.alert", playerName), net.minecraft.Util.NIL_UUID);
    }
  }

  /**
   * Gets the tracked player's UUID.
   */
  public UUID getPlayerId() {
    return playerId;
  }

  /**
   * Gets the tracked dimension ID.
   */
  public int getDimensionId() {
    return dimensionId;
  }

  /**
   * Gets the Age UID from a level.
   */
  private int getAgeUID(Level level) {
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server == null) return -1;
    return AgeManager.get(server).getAgeUID(level.dimension());
  }

  /**
   * Gets the Age UID from a dimension key.
   */
  private int getAgeUIDFromKey(ResourceKey<Level> key) {
    MinecraftServer server = Mystcraft.getCurrentServer();
    if (server == null) return -1;
    return AgeManager.get(server).getAgeUID(key);
  }

  private void announceToAge(Level level, String key, Object... args) {
    if (level instanceof ServerLevel serverLevel) {
      // 1.18.2: Use TranslatableComponent instead of Component.translatable()
      Component message = new TranslatableComponent(key, args);
      for (ServerPlayer player : serverLevel.players()) {
        player.sendMessage(message, net.minecraft.Util.NIL_UUID);
      }
    }
  }

  /**
   * Cleans up this bonus tracker.
   */
  public void cleanup() {
    // No event bus to unregister from in common - platform handles cleanup
  }
}
