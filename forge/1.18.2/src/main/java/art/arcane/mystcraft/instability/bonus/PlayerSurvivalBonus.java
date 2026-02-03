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
 * Instability bonus that rewards players for surviving in an Age.
 * The longer a player survives in the Age, the more stability bonus they provide.
 * Death resets the bonus to zero.
 * <p>
 * This is the inverse of PlayerKilledBonus - it rewards survival with stability.
 * <p>
 * Event registration is handled by the platform module. The common logic methods
 * (onPlayerDeath, onPlayerLogin, onPlayerLogout, onPlayerChangedDimension)
 * are called from platform-specific event handlers.
 * <p>
 * 1.18.2 version - uses TranslatableComponent instead of Component.translatable().
 */
public class PlayerSurvivalBonus implements IInstabilityBonus {

  private final String playerName;
  private final UUID playerId;
  private final int dimensionId;

  private final int maxBonus;
  private final float growthRate;
  private float currentBonus;
  private boolean playerInWorld;

  /**
   * Creates a player survival bonus tracker.
   *
   * @param playerId    The player's UUID
   * @param playerName  The player's name
   * @param dimensionId The dimension ID of the Age
   * @param maxBonus    Maximum stability bonus when player survives long enough
   * @param growthRate  How fast the bonus grows per tick while player is in world
   */
  public PlayerSurvivalBonus(UUID playerId, String playerName, int dimensionId, int maxBonus, float growthRate) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.dimensionId = dimensionId;
    this.maxBonus = maxBonus;
    this.growthRate = growthRate;
    this.currentBonus = 0;
    this.playerInWorld = false;
  }

  @Override
  public String getName() {
    return "Player Survival: " + playerName;
  }

  @Override
  public int getValue() {
    // Return positive value (bonus = reduces instability)
    return (int) currentBonus;
  }

  @Override
  public void tick(ServerLevel level) {
    if (playerInWorld) {
      // Increase bonus while player is in the world
      currentBonus = Math.min(maxBonus, currentBonus + growthRate);
    } else {
      // Slowly decay bonus when player is not present (but slower than growth)
      currentBonus = Math.max(0, currentBonus - (growthRate * 0.1f));
    }
  }

  /**
   * Called when a player dies. Should be invoked from platform event handlers.
   */
  public void onPlayerDeath(Player player) {
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

    // Reset bonus on death
    currentBonus = 0;
    announceToAge(player.getLevel(), "instability.bonus.survival.death", playerName);
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
    if (ageId == dimensionId) {
      playerInWorld = true;
      announceToAge(player.getLevel(), "instability.bonus.survival.enter", playerName);
    }
  }

  /**
   * Called when a player logs out. Should be invoked from platform event handlers.
   */
  public void onPlayerLogout(ServerPlayer player) {
    if (!player.getUUID().equals(playerId)) {
      return;
    }

    if (AgeDimensionFactory.isMystcraftAge(player.getLevel().dimension())) {
      int ageId = getAgeUID(player.getLevel());
      if (ageId == dimensionId) {
        playerInWorld = false;
        announceToAge(player.getLevel(), "instability.bonus.survival.leave", playerName);
      }
    }
  }

  /**
   * Called when a player changes dimension. Should be invoked from platform event handlers.
   */
  public void onPlayerChangedDimension(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
    if (!player.getUUID().equals(playerId)) {
      return;
    }

    // Check if leaving our dimension
    if (AgeDimensionFactory.isMystcraftAge(from)) {
      int fromAgeId = getAgeUIDFromKey(from);
      if (fromAgeId == dimensionId) {
        playerInWorld = false;
        announceToAge(player.getLevel(), "instability.bonus.survival.leave", playerName);
      }
    }

    // Check if entering our dimension
    if (AgeDimensionFactory.isMystcraftAge(to)) {
      int toAgeId = getAgeUIDFromKey(to);
      if (toAgeId == dimensionId) {
        playerInWorld = true;
        announceToAge(player.getLevel(), "instability.bonus.survival.enter", playerName);
      }
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
   * Checks if the tracked player is currently in the world.
   */
  public boolean isPlayerInWorld() {
    return playerInWorld;
  }

  /**
   * Cleans up this bonus tracker.
   */
  public void cleanup() {
    // No event bus to unregister from in common - platform handles cleanup
  }
}
