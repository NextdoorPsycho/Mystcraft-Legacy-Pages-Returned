package art.arcane.mystcraft.event;

import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.world.PersonalPocketData;
import art.arcane.mystcraft.world.PersonalPocketDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/**
 * Handles player escape from personal pocket dimensions.
 * <p>
 * The personal pocket is a 256x256 void area. When players cross the dimensional
 * boundaries (walls, floor, or ceiling), they are teleported back to the spawn platform.
 * Death in the pocket also triggers teleportation back to the entry point.
 */
public final class PersonalPocketEscapeHandler {

  private PersonalPocketEscapeHandler() {
  }

  /**
   * Handles death in personal pocket. Prevents actual death and teleports player back.
   *
   * @return true if death was prevented, false if not in personal pocket
   */
  public static boolean handleDeath(ServerPlayer player, DamageSource source) {
    if (!(player.level() instanceof ServerLevel level)) {
      return false;
    }
    if (!PersonalPocketDimension.isPersonalPocket(level)) {
      return false;
    }
    teleportToEntryPoint(player, level);
    return true;
  }

  /**
   * Checks if player has crossed any boundary of the personal pocket.
   * Called every tick for players in personal pockets.
   * <p>
   * Boundaries:
   * - X/Z: ±128 blocks from center (256x256 area)
   * - Y floor: -64 (void)
   * - Y ceiling: 320
   */
  public static void checkBoundaries(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level)) {
      return;
    }
    if (!PersonalPocketDimension.isPersonalPocket(level)) {
      return;
    }

    double x = player.getX();
    double y = player.getY();
    double z = player.getZ();

    // Check horizontal boundaries (dimensional walls)
    if (PersonalPocketDimension.isOutsideBoundary(x, z)) {
      teleportToSpawn(player, level);
      return;
    }

    // Check vertical boundaries (floor/ceiling)
    if (PersonalPocketDimension.isOutsideVerticalBoundary(y)) {
      teleportToSpawn(player, level);
    }
  }

  /**
   * Teleports player back to the pocket spawn platform.
   * Used when crossing dimensional boundaries within the pocket.
   */
  private static void teleportToSpawn(ServerPlayer player, ServerLevel pocketLevel) {
    BlockPos spawn = PersonalPocketDimension.getPocketSpawn();

    // Reset player state
    player.setDeltaMovement(0.0, 0.0, 0.0);
    player.fallDistance = 0.0f;

    // Teleport to spawn platform center
    player.teleportTo(
        pocketLevel,
        spawn.getX() + 0.5,
        spawn.getY(),
        spawn.getZ() + 0.5,
        player.getYRot(),
        player.getXRot()
    );

    // Play linking sound to indicate boundary crossing
    pocketLevel.playSound(null, spawn, ModSounds.LINKING_LINK.get(), SoundSource.PLAYERS, 0.5f, 1.2f);
  }

  /**
   * Teleports player back to their entry point (where they linked from).
   * Used when dying or explicitly leaving the pocket.
   */
  private static void teleportToEntryPoint(ServerPlayer player, ServerLevel pocketLevel) {
    MinecraftServer server = pocketLevel.getServer();
    CompoundTag link = PersonalPocketData.get(server).getReturnLink(player.getUUID());

    ServerLevel targetLevel = server.overworld();
    BlockPos targetPos = targetLevel.getSharedSpawnPos();
    float yaw = player.getYRot();

    if (link != null) {
      Integer uid = LinkOptions.getDimensionUID(link);
      BlockPos spawn = LinkOptions.getSpawn(link);
      float linkYaw = LinkOptions.getSpawnYaw(link);

      // Only use return link if it points to a different dimension (not back to this pocket)
      if (uid != null && spawn != null && uid != PersonalPocketDimension.getPersonalAgeUid(player.getUUID())) {
        ServerLevel found = LinkingManager.findDimensionByUID(server, uid);
        if (found != null) {
          targetLevel = found;
          targetPos = spawn;
          yaw = linkYaw;
        }
      }
    }

    // Restore player to full health and clear negative effects
    player.setHealth(player.getMaxHealth());
    player.setRemainingFireTicks(0);
    player.setDeltaMovement(0.0, 0.0, 0.0);
    player.fallDistance = 0.0f;

    // Teleport to entry point
    player.teleportTo(
        targetLevel,
        targetPos.getX() + 0.5,
        targetPos.getY(),
        targetPos.getZ() + 0.5,
        yaw,
        player.getXRot()
    );

    // Play linking sound
    targetLevel.playSound(null, targetPos, ModSounds.LINKING_LINK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

    // Sync abilities if changing dimensions
    if (targetLevel.dimension() != Level.OVERWORLD) {
      player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }
  }
}
