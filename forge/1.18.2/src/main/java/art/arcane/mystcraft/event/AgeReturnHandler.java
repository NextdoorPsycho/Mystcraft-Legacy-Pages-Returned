package art.arcane.mystcraft.event;

import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeReturnData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/**
 * Returns players to their entry point on death in Mystcraft Ages.
 * 1.18.2 version - uses entity.getLevel() (same as 1.19.2).
 */
public final class AgeReturnHandler {

  private AgeReturnHandler() {
  }

  /**
   * Handles death in a Mystcraft Age. Prevents death and returns player to entry point.
   *
   * @return true if death was prevented, false otherwise
   */
  public static boolean handleDeath(ServerPlayer player, DamageSource source) {
    if (!MystcraftConfig.safeStories.get()) {
      return false;
    }
    Level levelRaw = player.getLevel();
    if (!(levelRaw instanceof ServerLevel level)) {
      return false;
    }
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return false;
    }

    int ageUID = AgeDimensionFactory.getAgeUID(level.dimension());
    if (ageUID <= 0) {
      return false;
    }

    CompoundTag link = AgeReturnData.get(level.getServer()).getReturnLink(player.getUUID(), ageUID);
    if (link == null) {
      return false;
    }

    teleportToEntryPoint(player, level, link);
    return true;
  }

  private static void teleportToEntryPoint(ServerPlayer player, ServerLevel currentLevel, CompoundTag link) {
    MinecraftServer server = currentLevel.getServer();
    ServerLevel targetLevel = server.overworld();
    BlockPos targetPos = targetLevel.getSharedSpawnPos();
    float yaw = player.getYRot();

    Integer uid = LinkOptions.getDimensionUID(link);
    BlockPos spawn = LinkOptions.getSpawn(link);
    float linkYaw = LinkOptions.getSpawnYaw(link);

    if (uid != null && spawn != null) {
      ServerLevel found = LinkingManager.findDimensionByUID(server, uid);
      if (found != null) {
        targetLevel = found;
        targetPos = spawn;
        yaw = linkYaw;
      }
    }

    player.setHealth(player.getMaxHealth());
    player.setRemainingFireTicks(0);
    player.setDeltaMovement(0.0, 0.0, 0.0);
    player.fallDistance = 0.0f;

    player.teleportTo(
        targetLevel,
        targetPos.getX() + 0.5,
        targetPos.getY(),
        targetPos.getZ() + 0.5,
        yaw,
        player.getXRot()
    );

    targetLevel.playSound(null, targetPos, ModSounds.LINKING_LINK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

    if (targetLevel.dimension() != Level.OVERWORLD) {
      player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }
  }
}
