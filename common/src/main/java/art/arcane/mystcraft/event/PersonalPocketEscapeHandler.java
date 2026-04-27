package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.util.MystcraftChunkLeases;
import art.arcane.mystcraft.util.ServerPlayerTeleport;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.PersonalPocketData;
import art.arcane.mystcraft.world.PersonalPocketDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player escape from personal pocket dimensions.
 * <p>
 * When players cross personal-pocket boundaries, they are teleported back to the spawn platform.
 * Death in the pocket also triggers teleportation back to the entry point.
 */
public final class PersonalPocketEscapeHandler {

  private static final int PROXY_CLEANUP_INTERVAL_TICKS = 20;
  private static final Map<UUID, WeakReference<ServerPlayer>> MOCK_OWNER_REFERENCES = new ConcurrentHashMap<>();

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
    return returnToEntryPoint(player, true);
  }

  /**
   * Checks if player has crossed any boundary of the personal pocket.
   * Called every tick for players in personal pockets.
   * <p>
   * Boundaries are driven by the configured personal-pocket dimensions.
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
    ServerPlayerTeleport.teleport(
        player,
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
   * Records the origin body proxy before the player enters their personal pocket.
   *
   * @return true when the proxy was spawned and stored
   */
  public static boolean spawnOrReplaceProxy(ServerPlayer player, CompoundTag returnLink) {
    if (!(player.level() instanceof ServerLevel sourceLevel)) {
      return false;
    }
    MinecraftServer server = sourceLevel.getServer();
    PersonalPocketData data = PersonalPocketData.get(server);
    removeActiveProxy(server, player.getUUID());
    rememberMockOwner(player);
    MystcraftChunkLeases.leaseReturnWindow(sourceLevel, player.blockPosition());

    PersonalPocketProxyEntity proxy = new PersonalPocketProxyEntity(sourceLevel);
    proxy.setOwner(player.getGameProfile(), returnLink);
    proxy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    proxy.setDeltaMovement(0.0D, 0.0D, 0.0D);

    if (!sourceLevel.addFreshEntity(proxy)) {
      Mystcraft.LOGGER.warn("[PersonalPocket] Failed to spawn body proxy for {}", player.getGameProfile().getName());
      return false;
    }

    data.setActiveProxy(player.getUUID(), new PersonalPocketData.ProxyState(
        player.getGameProfile().getName(),
        LinkingManager.getDimensionUID(sourceLevel),
        player.blockPosition(),
        player.getYRot(),
        player.getXRot(),
        proxy.getUUID(),
        returnLink
    ));
    return true;
  }

  /**
   * Removes the currently tracked proxy for a player, if any.
   */
  public static void removeActiveProxy(MinecraftServer server, UUID playerId) {
    PersonalPocketData data = PersonalPocketData.get(server);
    PersonalPocketData.ProxyState state = data.getActiveProxy(playerId);
    if (state != null) {
      PersonalPocketProxyEntity proxy = findProxy(server, state);
      if (proxy != null) {
        proxy.discard();
      }
    }
    data.clearActiveProxy(playerId);
    MOCK_OWNER_REFERENCES.remove(playerId);
  }

  /**
   * Teleports player back to their entry point and removes their origin proxy.
   * Used by book return, death return, and proxy-hit rip-out.
   */
  public static boolean returnToEntryPoint(ServerPlayer player, boolean restoreHealth) {
    if (!(player.level() instanceof ServerLevel pocketLevel) || !PersonalPocketDimension.isPersonalPocket(pocketLevel)) {
      return false;
    }

    MinecraftServer server = pocketLevel.getServer();
    PersonalPocketData data = PersonalPocketData.get(server);
    PersonalPocketData.ProxyState proxyState = data.getActiveProxy(player.getUUID());
    CompoundTag link = data.getReturnLink(player.getUUID());
    if (link == null && proxyState != null) {
      link = proxyState.returnLink();
    }

    PersonalPocketProxyEntity proxy = proxyState == null ? null : findProxy(server, proxyState);
    ReturnTarget target = resolveReturnTarget(server, player, link, proxyState, proxy);
    removeActiveProxy(server, player.getUUID());

    if (restoreHealth) {
      player.setHealth(player.getMaxHealth());
      player.setRemainingFireTicks(0);
    }

    player.setDeltaMovement(0.0, 0.0, 0.0);
    player.fallDistance = 0.0f;

    ServerPlayerTeleport.teleport(
        player,
        target.level(),
        target.position().getX() + 0.5,
        target.position().getY(),
        target.position().getZ() + 0.5,
        target.yaw(),
        target.pitch()
    );

    target.level().playSound(null, target.position(), ModSounds.LINKING_LINK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

    if (target.level().dimension() != Level.OVERWORLD) {
      ServerPlayerTeleport.syncAbilitiesIfConnected(player);
    }
    return true;
  }

  /**
   * Rips the owner out of their personal pocket when the origin proxy is damaged.
   */
  public static boolean ripOwnerOutFromProxy(PersonalPocketProxyEntity proxy, DamageSource source) {
    if (!(proxy.level() instanceof ServerLevel level)) {
      return false;
    }
    UUID ownerId = proxy.getOwnerId();
    if (ownerId == null) {
      proxy.discard();
      return true;
    }

    MinecraftServer server = level.getServer();
    ServerPlayer owner = findOwnerForProxy(server, ownerId);
    if (owner == null || !isOwnerInTheirPocket(owner)) {
      proxy.discard();
      removeActiveProxy(server, ownerId);
      return true;
    }

    returnToEntryPoint(owner, false);
    return true;
  }

  /**
   * Keeps proxy state coherent when players log in or change dimensions.
   */
  public static void syncProxyForPlayer(ServerPlayer player) {
    MinecraftServer server = player.server;
    if (isOwnerInTheirPocket(player)) {
      rememberMockOwner(player);
      ensureProxyPresent(server, player.getUUID());
      return;
    }
    removeActiveProxy(server, player.getUUID());
  }

  /**
   * Periodic cleanup for stale or missing origin proxies.
   */
  public static void tickProxyCleanup(ServerLevel level) {
    if (level != level.getServer().overworld()) {
      return;
    }
    if (level.getGameTime() % PROXY_CLEANUP_INTERVAL_TICKS != 0) {
      return;
    }

    MinecraftServer server = level.getServer();
    PersonalPocketData data = PersonalPocketData.get(server);
    for (UUID ownerId : data.getActiveProxyOwners()) {
      ServerPlayer owner = findOwnerForProxy(server, ownerId);
      if (owner == null) {
        continue;
      }
      if (isOwnerInTheirPocket(owner)) {
        ensureProxyPresent(server, ownerId);
      } else {
        removeActiveProxy(server, ownerId);
      }
    }
  }

  private static void ensureProxyPresent(MinecraftServer server, UUID playerId) {
    PersonalPocketData data = PersonalPocketData.get(server);
    PersonalPocketData.ProxyState state = data.getActiveProxy(playerId);
    if (state == null || findProxy(server, state) != null) {
      if (state != null) {
        ServerLevel loadedLevel = LinkingManager.findLoadedDimensionByUID(server, state.dimensionUid());
        if (loadedLevel != null) {
          MystcraftChunkLeases.leaseReturnWindow(loadedLevel, state.position());
        }
      }
      return;
    }

    ServerLevel sourceLevel = LinkingManager.findDimensionByUID(server, state.dimensionUid());
    if (sourceLevel == null) {
      Mystcraft.LOGGER.warn("[PersonalPocket] Cannot respawn missing proxy for {}; source UID {} is unavailable",
          playerId, state.dimensionUid());
      return;
    }
    MystcraftChunkLeases.leaseReturnWindow(sourceLevel, state.position());

    PersonalPocketProxyEntity proxy = new PersonalPocketProxyEntity(sourceLevel);
    ServerPlayer owner = server.getPlayerList().getPlayer(playerId);
    if (owner != null) {
      proxy.setOwner(owner.getGameProfile(), state.returnLink());
    } else {
      proxy.setOwner(playerId, state.ownerName(), state.returnLink());
    }
    BlockPos position = state.position();
    proxy.moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, state.yaw(), state.pitch());
    proxy.setDeltaMovement(0.0D, 0.0D, 0.0D);

    if (sourceLevel.addFreshEntity(proxy)) {
      data.setActiveProxy(playerId, new PersonalPocketData.ProxyState(
          state.ownerName(),
          state.dimensionUid(),
          state.position(),
          state.yaw(),
          state.pitch(),
          proxy.getUUID(),
          state.returnLink()
      ));
    }
  }

  private static PersonalPocketProxyEntity findProxy(MinecraftServer server, PersonalPocketData.ProxyState state) {
    ServerLevel level = LinkingManager.findLoadedDimensionByUID(server, state.dimensionUid());
    if (level == null) {
      return null;
    }
    Entity entity = level.getEntity(state.proxyId());
    if (entity instanceof PersonalPocketProxyEntity proxy) {
      return proxy;
    }
    return null;
  }

  private static void rememberMockOwner(ServerPlayer player) {
    if (player.connection == null) {
      MOCK_OWNER_REFERENCES.put(player.getUUID(), new WeakReference<>(player));
    }
  }

  private static ServerPlayer findOwnerForProxy(MinecraftServer server, UUID ownerId) {
    ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
    if (owner != null) {
      return owner;
    }
    WeakReference<ServerPlayer> reference = MOCK_OWNER_REFERENCES.get(ownerId);
    ServerPlayer remembered = reference == null ? null : reference.get();
    if (remembered != null && remembered.server == server && remembered.connection == null) {
      return remembered;
    }
    MOCK_OWNER_REFERENCES.remove(ownerId);
    return null;
  }

  private static boolean isOwnerInTheirPocket(ServerPlayer player) {
    if (!(player.level() instanceof ServerLevel level) || !PersonalPocketDimension.isPersonalPocket(level)) {
      return false;
    }
    AgeData ageData = AgeData.getIfPresent(level);
    return ageData == null || ageData.getAgeUID() == PersonalPocketDimension.getPersonalAgeUid(player.getUUID());
  }

  private static ReturnTarget resolveReturnTarget(MinecraftServer server, ServerPlayer player, CompoundTag link,
                                                  PersonalPocketData.ProxyState proxyState,
                                                  PersonalPocketProxyEntity proxy) {
    ServerLevel targetLevel = server.overworld();
    BlockPos targetPos = targetLevel.getSharedSpawnPos();
    float yaw = player.getYRot();
    float pitch = player.getXRot();

    if (proxy != null && proxy.level() instanceof ServerLevel proxyLevel) {
      return new ReturnTarget(proxyLevel, proxy.blockPosition(), proxy.getYRot(), proxy.getXRot());
    }

    if (proxyState != null) {
      ServerLevel proxyLevel = LinkingManager.findDimensionByUID(server, proxyState.dimensionUid());
      if (proxyLevel != null && proxyState.dimensionUid() != PersonalPocketDimension.getPersonalAgeUid(player.getUUID())) {
        targetLevel = proxyLevel;
        targetPos = proxyState.position();
        yaw = proxyState.yaw();
        pitch = proxyState.pitch();
        return new ReturnTarget(targetLevel, targetPos, yaw, pitch);
      }
    }

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

    return new ReturnTarget(targetLevel, targetPos, yaw, pitch);
  }

  private record ReturnTarget(ServerLevel level, BlockPos position, float yaw, float pitch) {
  }
}
