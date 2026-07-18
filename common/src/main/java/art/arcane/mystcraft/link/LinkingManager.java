package art.arcane.mystcraft.link;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.instability.InstabilityManager;
import art.arcane.mystcraft.network.LinkEffectPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ChunkStatusCompat;
import art.arcane.mystcraft.util.MystcraftChunkLeases;
import art.arcane.mystcraft.util.PlayerMessages;
import art.arcane.mystcraft.util.ServerPlayerTeleport;
import art.arcane.mystcraft.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages linking (teleportation) operations for Mystcraft. Handles
 * cross-dimension teleportation, spawn platform generation, link events,
 * permissions, and sound/particle effects.
 */
public final class LinkingManager {

  private LinkingManager() {
  }

  /**
   * Performs a link for the given entity using the provided link data. Fires
   * events and checks permissions throughout the process.
   *
   * @param entity   The entity to teleport
   * @param linkData The NBT data containing link information
   * @return The result of the link operation
   */
  public static LinkResult performLink(@NotNull Entity entity, @Nullable CompoundTag linkData) {
    if (linkData == null) {
      return LinkResult.INVALID_DESTINATION;
    }

    if (!(entity.level() instanceof ServerLevel sourceLevel)) {
      return LinkResult.BLOCKED;
    }

    MinecraftServer server = sourceLevel.getServer();
    ResourceKey<Level> sourceDimension = sourceLevel.dimension();
    BlockPos sourcePos = entity.blockPosition();

    BlockPos targetPos = LinkOptions.getSpawn(linkData);
    Integer dimId = LinkOptions.getDimensionUID(linkData);
    float targetYaw = LinkOptions.getSpawnYaw(linkData);

    if (targetPos == null || dimId == null) {
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.INVALID_DESTINATION, "No valid destination");
      return LinkResult.INVALID_DESTINATION;
    }

    ServerLevel targetLevel = findDimensionByUID(server, dimId);
    if (targetLevel == null) {
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.DIMENSION_NOT_FOUND, "Target dimension not found");
      return LinkResult.DIMENSION_NOT_FOUND;
    }

    boolean isIntraLink = sourceLevel.dimension().equals(targetLevel.dimension());
    if (isIntraLink && !LinkOptions.getFlag(linkData, LinkFlags.INTRA_LINKING)) {
      String message = "This link does not allow intra-dimensional travel";
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.CANCELLED, message);
      if (entity instanceof ServerPlayer player) {
        PlayerMessages.send(player, Component.literal(message + "."), false);
      }
      return LinkResult.BLOCKED;
    }
    if (!isIntraLink && LinkOptions.getFlag(linkData, LinkFlags.INTRA_LINKING_ONLY)) {
      String message = "This link only allows intra-dimensional travel";
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.CANCELLED, message);
      if (entity instanceof ServerPlayer player) {
        PlayerMessages.send(player, Component.literal(message + "."), false);
      }
      return LinkResult.BLOCKED;
    }

    if (AgeDimensionFactory.isMystcraftAge(targetLevel.dimension())) {
      AgeData ageData = AgeData.getIfPresent(targetLevel);
      if (ageData != null) {

        logAgeSymbols(ageData, dimId);
        float instability = ageData.getInstability();
        if (!InstabilityManager.isAgeAllowed(instability)) {
          String rating = InstabilityManager.getInstabilityRating(instability);
          String message = "This Age is too unstable to enter safely. (" + rating + ")";
          fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
              LinkEvent.Failed.FailureReason.CANCELLED, message);
          Mystcraft.LOGGER.info("Link blocked due to instability: {} ({})", instability, rating);
          return LinkResult.TOO_UNSTABLE;
        }
      }
    }

    LinkEvent.Allow allowEvent = new LinkEvent.Allow(entity, linkData, sourceDimension, sourcePos);
    LinkEventBus.post(allowEvent);

    if (allowEvent.isCancelled()) {
      String reason = allowEvent.getCancelReason();
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.CANCELLED, reason.isEmpty() ? "Link was cancelled" : reason);

      if (!reason.isEmpty()) {
        Mystcraft.LOGGER.warn("Link cancelled: {}", reason);
      }
      return LinkResult.CANCELLED;
    }

    if (entity instanceof ServerPlayer player) {
      LinkPermissions permissions = LinkPermissions.get(server);

      int sourceUID = getAgeUID(sourceLevel);
      if (sourceUID > 0 && !permissions.canDepart(player, sourceUID)) {
        fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
            LinkEvent.Failed.FailureReason.PERMISSION_DENIED, "You cannot leave this Age");
        PlayerMessages.send(player, Component.literal("You cannot leave this Age."), false);
        return LinkResult.PERMISSION_DENIED;
      }

      int targetUID = dimId;
      if (targetUID > 0 && !permissions.canEnter(player, targetUID)) {
        fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
            LinkEvent.Failed.FailureReason.PERMISSION_DENIED, "You cannot enter this Age");
        PlayerMessages.send(player, Component.literal("You cannot enter this Age."), false);
        return LinkResult.PERMISSION_DENIED;
      }
    }

    MystcraftChunkLeases.leaseReturnWindow(sourceLevel, sourcePos);

    int destChunkX = targetPos.getX() >> 4;
    int destChunkZ = targetPos.getZ() >> 4;
    ChunkPos destChunkPos = new ChunkPos(destChunkX, destChunkZ);
    targetLevel.getChunkSource().addRegionTicket(
        TicketType.POST_TELEPORT,
        destChunkPos, 1, entity.getId()
    );
    MystcraftChunkLeases.leaseReturnWindow(targetLevel, destChunkPos);

    targetLevel.getChunk(destChunkX, destChunkZ);
    Mystcraft.LOGGER.info("[LinkingManager] Pre-loaded destination chunk [{}, {}] in {}",
        destChunkX, destChunkZ, targetLevel.dimension().location());

    Vec3 targetVec = calculateTargetPosition(targetPos, linkData, entity.position(), sourcePos, targetLevel);

    BlockPos destBlock = BlockPos.containing(targetVec);
    boolean isPersonalPocket = PersonalPocketDimension.isPersonalPocket(targetLevel);

    if (!isPersonalPocket && LinkOptions.getFlag(linkData, LinkFlags.GENERATE_PLATFORM)) {
      generateSpawnPlatform(targetLevel, destBlock);

      targetVec = calculateTargetPosition(targetPos, linkData, entity.position(), sourcePos, targetLevel);
      destBlock = BlockPos.containing(targetVec);
    }

    if (!isPersonalPocket && !hasSolidGround(targetLevel, destBlock)) {
      BlockPos platformSpawn = buildFluidSurfacePlatform(targetLevel, destBlock);
      if (platformSpawn != null) {
        targetVec = new Vec3(platformSpawn.getX() + 0.5, platformSpawn.getY(), platformSpawn.getZ() + 0.5);
        destBlock = platformSpawn;
        Mystcraft.LOGGER.info("[LinkingManager] Built spawn platform over fluid at {}", platformSpawn);
      }
    }

    LinkEvent.Alter alterEvent = new LinkEvent.Alter(entity, linkData, sourceDimension, sourcePos,
        targetLevel, targetVec, targetYaw);
    LinkEventBus.post(alterEvent);

    targetLevel = alterEvent.getTargetLevel();
    targetVec = alterEvent.getTargetPosition();
    targetYaw = alterEvent.getTargetYaw();

    LinkEvent.Start startEvent = new LinkEvent.Start(entity, linkData, sourceDimension, sourcePos,
        targetLevel, targetVec, targetYaw);
    LinkEventBus.post(startEvent);

    if (startEvent.isCancelled()) {
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.START_CANCELLED, "Link start was cancelled");
      return LinkResult.START_CANCELLED;
    }

    playLinkSound(sourceLevel, sourcePos, linkData, true);
    sendLinkEffect(sourceLevel, sourcePos, LinkEffectPacket.LinkEffectType.DEPARTURE);

    boolean follows = LinkOptions.getFlag(linkData, LinkFlags.FOLLOWING);
    RidingGroupData sourceRidingGroup = captureRidingGroup(entity, follows);
    List<FollowerData> followers = null;
    if (follows) {
      followers = captureFollowers(entity, sourceRidingGroup);
    }

    PendingDisarm pendingDisarm = null;
    if (LinkOptions.getFlag(linkData, LinkFlags.DISARM) && entity instanceof ServerPlayer player) {
      pendingDisarm = prepareDisarm(player, sourceLevel);
    }

    Vec3 momentum = entity.getDeltaMovement();

    PendingReturnLink pendingReturnLink = null;
    if (entity instanceof ServerPlayer player) {
      int targetAgeUID = AgeDimensionFactory.getAgeUID(targetLevel.dimension());
      if (targetAgeUID > 0) {
        CompoundTag returnData = new CompoundTag();
        LinkOptions.setDimensionUID(returnData, getDimensionUID(sourceLevel));
        LinkOptions.setSpawn(returnData, sourcePos);
        LinkOptions.setSpawnYaw(returnData, player.getYRot());
        pendingReturnLink = new PendingReturnLink(player.getUUID(), targetAgeUID, returnData);
      }
    }

    TeleportedRidingGroup teleportedSource = teleportRidingGroup(
        sourceRidingGroup, targetLevel, targetVec, targetYaw);
    if (teleportedSource == null) {
      if (pendingDisarm != null) {
        pendingDisarm.rollback();
      }
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.TELEPORT_FAILED, "Entity teleport failed");
      return LinkResult.TELEPORT_FAILED;
    }
    entity = teleportedSource.anchor();

    if (pendingDisarm != null) {
      pendingDisarm.commit();
    }
    if (pendingReturnLink != null) {
      pendingReturnLink.commit(server);
    }

    if (LinkOptions.getFlag(linkData, LinkFlags.MAINTAIN_MOMENTUM)) {
      entity.setDeltaMovement(momentum);
      if (entity instanceof ServerPlayer maintainPlayer && maintainPlayer.connection != null) {
        maintainPlayer.connection.send(
            new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                maintainPlayer.getId(), momentum));
      } else {
        entity.hurtMarked = true;
      }
    }

    if (followers != null && !followers.isEmpty()) {
      teleportFollowers(followers, targetLevel, targetVec, linkData);
    }

    BlockPos arrivalPos = BlockPos.containing(targetVec);
    playLinkSound(targetLevel, arrivalPos, linkData, false);
    sendLinkEffect(targetLevel, arrivalPos, LinkEffectPacket.LinkEffectType.ARRIVAL);

    LinkEvent.End endEvent = new LinkEvent.End(entity, linkData, sourceDimension, sourcePos,
        targetLevel, targetVec);
    LinkEventBus.post(endEvent);

    if (AgeDimensionFactory.isMystcraftAge(targetLevel.dimension()) && entity instanceof ServerPlayer serverPlayer) {
      checkMystDimensionAdvancements(serverPlayer);
    }

    return LinkResult.SUCCESS;
  }

  private static void logAgeSymbols(AgeData ageData, int ageUID) {
    List<ItemStack> pages = ageData.getPages();
    if (pages.isEmpty()) {
      Mystcraft.LOGGER.info("[LinkingManager] Age {} has no pages stored", ageUID);
      return;
    }

    List<String> symbolNames = new ArrayList<>();
    int linkPanelCount = 0;

    for (ItemStack page : pages) {
      if (Page.isLinkPanel(page)) {
        linkPanelCount++;
        continue;
      }
      ResourceLocation symbolId = Page.getSymbol(page);
      if (symbolId != null) {
        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
        if (symbol != null) {
          symbolNames.add(symbolId.getPath() + " [" + symbol.getCategory().name() + "]");
        } else {
          symbolNames.add(symbolId.getPath() + " [MISSING]");
        }
      }
    }

    Mystcraft.LOGGER.debug("========== Age {} Symbol List ==========", ageUID);
    Mystcraft.LOGGER.debug("[Age {}] {} pages total, {} link panel(s), {} symbol(s)",
        ageUID, pages.size(), linkPanelCount, symbolNames.size());
    for (int i = 0; i < symbolNames.size(); i++) {
      Mystcraft.LOGGER.debug("[Age {}]   [{}] {}", ageUID, i + 1, symbolNames.get(i));
    }
    Mystcraft.LOGGER.debug("[Age {}] Instability: {}", ageUID, String.format("%.1f", ageData.getInstability()));
    Mystcraft.LOGGER.debug("========================================");
  }

  private static int getAgeUID(ServerLevel level) {
    return AgeDimensionFactory.getAgeUID(level.dimension());
  }

  private static void fireFailedEvent(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension,
                                      BlockPos sourcePos, LinkEvent.Failed.FailureReason reason, String message) {
    LinkEvent.Failed failedEvent = new LinkEvent.Failed(entity, linkData, sourceDimension, sourcePos, reason, message);
    LinkEventBus.post(failedEvent);
  }

  private static RidingGroupData captureRidingGroup(Entity anchor, boolean includeVehicle) {
    List<RidingEntityData> members = new ArrayList<>();
    Entity groupRoot = includeVehicle ? anchor.getRootVehicle() : anchor;
    collectRidingGroup(groupRoot, null, anchor.position(), members);
    return new RidingGroupData(anchor, members);
  }

  private static void collectRidingGroup(Entity entity, @Nullable Entity parent, Vec3 anchorPosition,
                                         List<RidingEntityData> members) {
    members.add(new RidingEntityData(entity, parent, entity.position().subtract(anchorPosition)));
    for (Entity passenger : entity.getPassengers()) {
      collectRidingGroup(passenger, entity, anchorPosition, members);
    }
  }

  private static List<FollowerData> captureFollowers(Entity source, RidingGroupData sourceGroup) {
    Set<Entity> sourceMembers = Collections.newSetFromMap(new IdentityHashMap<>());
    for (RidingEntityData member : sourceGroup.members()) {
      sourceMembers.add(member.entity());
    }

    double radius = 3.0;
    AABB area = new AABB(
        source.getX() - radius, source.getY() - radius, source.getZ() - radius,
        source.getX() + radius, source.getY() + radius, source.getZ() + radius
    );
    List<Entity> nearbyEntities = source.level().getEntities(
        source, area, candidate -> candidate instanceof LivingEntity && !sourceMembers.contains(candidate));

    Set<Entity> capturedRoots = Collections.newSetFromMap(new IdentityHashMap<>());
    List<FollowerData> followers = new ArrayList<>();
    Vec3 sourcePosition = source.position();
    for (Entity candidate : nearbyEntities) {
      Entity root = candidate.getRootVehicle();
      if (sourceMembers.contains(root) || !capturedRoots.add(root)) {
        continue;
      }
      followers.add(new FollowerData(
          captureRidingGroup(candidate, true),
          candidate.position().subtract(sourcePosition),
          candidate.getDeltaMovement()
      ));
    }
    return followers;
  }

  @Nullable
  private static TeleportedRidingGroup teleportRidingGroup(RidingGroupData group, ServerLevel targetLevel,
                                                            Vec3 anchorTarget, float anchorYaw) {
    List<RidingEntityData> members = group.members();
    Entity groupRoot = members.get(0).entity();
    if (groupRoot.isPassenger()) {
      groupRoot.stopRiding();
    }
    for (int i = members.size() - 1; i >= 0; i--) {
      if (members.get(i).parent() != null) {
        members.get(i).entity().stopRiding();
      }
    }

    Map<Entity, Entity> replacements = new IdentityHashMap<>();
    for (RidingEntityData member : members) {
      Entity original = member.entity();
      Vec3 destination = anchorTarget.add(member.offset());
      float yaw = original == group.anchor() ? anchorYaw : original.getYRot();
      Entity teleported = teleportEntity(original, targetLevel, destination, yaw);
      if (teleported == null) {
        Mystcraft.LOGGER.warn("[LinkingManager] Failed to teleport riding-group entity {}",
            original.getType());
        continue;
      }
      replacements.put(original, teleported);
    }

    for (RidingEntityData member : members) {
      if (member.parent() == null) {
        continue;
      }
      Entity passenger = replacements.get(member.entity());
      Entity vehicle = replacements.get(member.parent());
      if (passenger != null && vehicle != null && !passenger.startRiding(vehicle, true)) {
        Mystcraft.LOGGER.warn("[LinkingManager] Failed to restore passenger {} onto {}",
            passenger.getType(), vehicle.getType());
      }
    }

    Entity teleportedAnchor = replacements.get(group.anchor());
    return teleportedAnchor == null ? null : new TeleportedRidingGroup(teleportedAnchor);
  }

  private static void teleportFollowers(List<FollowerData> followers, ServerLevel targetLevel,
                                        Vec3 targetVec, CompoundTag linkData) {
    boolean teleportedAny = false;
    for (FollowerData followerData : followers) {
      Vec3 followerTarget = targetVec.add(followerData.offset());
      TeleportedRidingGroup teleported = teleportRidingGroup(
          followerData.group(), targetLevel, followerTarget, followerData.group().anchor().getYRot());
      if (teleported == null) {
        continue;
      }
      teleportedAny = true;
      Entity follower = teleported.anchor();

      if (LinkOptions.getFlag(linkData, LinkFlags.MAINTAIN_MOMENTUM)) {
        Vec3 followerMomentum = followerData.momentum();
        follower.setDeltaMovement(followerMomentum);
        if (follower instanceof ServerPlayer followerPlayer && followerPlayer.connection != null) {
          followerPlayer.connection.send(
              new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                  followerPlayer.getId(), followerMomentum));
        } else {
          follower.hurtMarked = true;
        }
      }
    }

    if (teleportedAny) {
      BlockPos targetPos = BlockPos.containing(targetVec);
      playSound(targetLevel, targetPos, ModSounds.LINKING_FOLLOWING.get(), 1.0f, 1.0f);
      sendLinkEffect(targetLevel, targetPos, LinkEffectPacket.LinkEffectType.FOLLOWING);
    }
  }

  /**
   * Finds a ServerLevel by its dimension UID.
   */
  @Nullable
  public static ServerLevel findDimensionByUID(MinecraftServer server, int uid) {
    ServerLevel loaded = findLoadedDimensionByUID(server, uid);
    if (loaded != null) {
      return loaded;
    }

    if (uid > 0) {
      AgeManager ageManager = AgeManager.get(server);
      ResourceLocation ageDimension = ageManager.getDimension(uid);
      if (ageDimension != null) {
        return AgeDimensionFactory.getOrCreateAgeDimension(server, uid);
      }
    }

    if (uid == 1) {
      return server.getLevel(Level.END);
    }

    return null;
  }

  /**
   * Finds a ServerLevel by UID without creating or loading dimensions.
   */
  @Nullable
  public static ServerLevel findLoadedDimensionByUID(MinecraftServer server, int uid) {

    if (uid == 0) {
      return server.getLevel(Level.OVERWORLD);
    } else if (uid == -1) {
      return server.getLevel(Level.NETHER);
    }

    if (uid > 0) {
      AgeManager ageManager = AgeManager.get(server);
      ResourceLocation ageDimension = ageManager.getDimension(uid);
      if (ageDimension != null) {
        return server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ageDimension));
      }
    }

    if (uid == 1) {
      return server.getLevel(Level.END);
    }

    for (ServerLevel level : server.getAllLevels()) {
      if (getDimensionUID(level) == uid) {
        return level;
      }
    }

    return null;
  }

  /**
   * Gets the dimension UID for a level.
   */
  public static int getDimensionUID(Level level) {
    ResourceKey<Level> dimension = level.dimension();
    if (dimension == Level.OVERWORLD) {
      return 0;
    } else if (dimension == Level.NETHER) {
      return -1;
    } else if (dimension == Level.END) {
      return 1;
    }

    if (level instanceof ServerLevel serverLevel) {
      AgeManager ageManager = AgeManager.get(serverLevel);
      int ageUID = ageManager.getAgeUID(dimension);
      if (ageUID > 0) {
        return ageUID;
      }
    }

    return dimension.location().hashCode();
  }

  /**
   * Gets the dimension ResourceKey from a UID.
   */
  @Nullable
  public static ResourceKey<Level> getDimensionKeyFromUID(int uid) {
    if (uid == 0) {
      return Level.OVERWORLD;
    } else if (uid == -1) {
      return Level.NETHER;
    } else if (uid == 1) {
      return Level.END;
    }
    return null;
  }

  @Nullable
  private static Entity teleportEntity(Entity entity, ServerLevel targetLevel, Vec3 targetPos, float yaw) {
    if (entity instanceof ServerPlayer player) {
      ServerPlayerTeleport.teleport(player, targetLevel, targetPos.x, targetPos.y, targetPos.z, yaw, player.getXRot());
      return player;
    }
    boolean changedDimension = entity.level() != targetLevel;
    UUID entityId = entity.getUUID();
    boolean teleported = entity.teleportTo(
        targetLevel, targetPos.x, targetPos.y, targetPos.z, Set.of(), yaw, entity.getXRot());
    if (!teleported) {
      return null;
    }
    if (!changedDimension) {
      return entity;
    }

    Entity replacement = targetLevel.getEntity(entityId);
    if (replacement == null) {
      Mystcraft.LOGGER.error("[LinkingManager] Cross-dimension teleport succeeded but replacement {} was not found",
          entityId);
    }
    return replacement;
  }

  @Nullable
  private static PendingDisarm prepareDisarm(ServerPlayer player, ServerLevel sourceLevel) {
    ItemStack heldItem = player.getMainHandItem();
    if (heldItem.isEmpty()) {
      return null;
    }

    int inventorySlot = player.getInventory().selected;
    ItemStack reservedItem = heldItem.copy();
    Vec3 sourcePosition = player.position();
    player.getInventory().setItem(inventorySlot, ItemStack.EMPTY);
    return new PendingDisarm(sourceLevel, player, inventorySlot, reservedItem, sourcePosition);
  }

  private static void generateSpawnPlatform(ServerLevel level, BlockPos pos) {

    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
      Mystcraft.LOGGER.warn("[LinkingManager] Spawn chunk not loaded at {}, skipping platform generation", pos);
      return;
    }

    BlockState platformBlock = Blocks.STONE.defaultBlockState();

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        BlockPos platformPos = pos.offset(x, -1, z);
        if (level.getBlockState(platformPos).isAir()) {
          level.setBlock(platformPos, platformBlock, 3);
        }
      }
    }

    for (int x = -1; x <= 1; x++) {
      for (int y = 0; y <= 1; y++) {
        for (int z = -1; z <= 1; z++) {
          BlockPos clearPos = pos.offset(x, y, z);
          BlockState state = level.getBlockState(clearPos);
          if (!state.isAir() && state.getDestroySpeed(level, clearPos) >= 0) {
            level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
          }
        }
      }
    }
  }

  private static boolean hasSolidGround(ServerLevel level, BlockPos pos) {
    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
      return true;
    }
    BlockPos below = pos.below();
    BlockState ground = level.getBlockState(below);
    return ground.isSolidRender(level, below);
  }

  @Nullable
  private static BlockPos buildFluidSurfacePlatform(ServerLevel level, BlockPos center) {
    if (!level.hasChunk(center.getX() >> 4, center.getZ() >> 4)) {
      return null;
    }

    int minY = level.getMinBuildHeight();
    int startY = Math.min(level.getMaxBuildHeight() - 1, 128);
    int cx = center.getX();
    int cz = center.getZ();

    for (int y = startY; y > minY; y--) {
      BlockPos pos = new BlockPos(cx, y, cz);
      BlockState state = level.getBlockState(pos);
      BlockState aboveState = level.getBlockState(pos.above());

      boolean isFluid = !state.getFluidState().isEmpty();
      boolean aboveIsClear = aboveState.isAir();

      if (isFluid && aboveIsClear) {

        int platY = y + 1;
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
          for (int dz = -1; dz <= 1; dz++) {
            BlockPos platPos = new BlockPos(cx + dx, platY, cz + dz);
            level.setBlock(platPos, plank, 2);
          }
        }

        for (int dx = -1; dx <= 1; dx++) {
          for (int dy = 1; dy <= 2; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
              BlockPos clearPos = new BlockPos(cx + dx, platY + dy, cz + dz);
              BlockState clearState = level.getBlockState(clearPos);
              if (!clearState.isAir()) {
                level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 2);
              }
            }
          }
        }
        return new BlockPos(cx, platY + 1, cz);
      }
    }

    return null;
  }

  private static Vec3 calculateTargetPosition(BlockPos targetPos, CompoundTag linkData, Vec3 entityPos,
                                              BlockPos sourcePos, ServerLevel targetLevel) {
    double x = targetPos.getX() + 0.5;
    double z = targetPos.getZ() + 0.5;

    int safeY = findSafeY(targetLevel, targetPos.getX(), targetPos.getY(), targetPos.getZ());

    if (LinkOptions.getFlag(linkData, LinkFlags.RELATIVE)) {
      double offsetX = entityPos.x - (sourcePos.getX() + 0.5);
      double offsetY = entityPos.y - sourcePos.getY();
      double offsetZ = entityPos.z - (sourcePos.getZ() + 0.5);

      x += offsetX;
      safeY += (int) offsetY;
      z += offsetZ;

      Mystcraft.LOGGER.debug("Applied relative offset: ({}, {}, {})", offsetX, offsetY, offsetZ);
    }

    return new Vec3(x, safeY, z);
  }

  private static int findSafeY(ServerLevel level, int x, int startY, int z) {
    AgeData ageData = AgeData.getIfPresent(level);
    if (ageData != null && ageData.isPersonalPocket()) {
      return startY;
    }
    int minY = level.getMinBuildHeight();
    int maxY = level.getMaxBuildHeight();

    ChunkAccess chunk = ChunkStatusCompat.getChunk(level, x >> 4, z >> 4, false);

    if (chunk == null) {
      Mystcraft.LOGGER.warn("[LinkingManager] findSafeY: chunk not loaded at ({}, {}), forcing load",
          x >> 4, z >> 4);
      chunk = level.getChunk(x >> 4, z >> 4);
    }

    boolean hasCeiling = hasCeilingTerrain(level);

    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    if (hasCeiling) {

      Mystcraft.LOGGER.debug("Ceiling terrain detected, searching bottom-up for safe spawn");

      for (int y = minY + 1; y < maxY - 1; y++) {
        if (isSafeSpawn(level, pos, x, y, z)) {
          Mystcraft.LOGGER.debug("Found safe spawn inside ceiling terrain at Y={}", y);
          return y;
        }
      }

      if (isSafeSpawn(level, pos, x, startY, z)) {
        return startY;
      }

      Mystcraft.LOGGER.warn("No safe spawn in ceiling terrain at ({}, {}), using startY={}", x, z, startY);
      return startY;
    }

    int hmY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
    int heightmapY = (hmY > minY) ? hmY + 1 : startY;

    if (isSafeSpawn(level, pos, x, heightmapY, z)) {
      Mystcraft.LOGGER.debug("Safe spawn from heightmap at Y={}", heightmapY);
      return heightmapY;
    }

    if (startY != heightmapY && isSafeSpawn(level, pos, x, startY, z)) {
      Mystcraft.LOGGER.debug("Safe spawn from requested Y={}", startY);
      return startY;
    }

    int searchFrom = heightmapY;
    for (int offset = 1; offset < 256; offset++) {
      int upY = searchFrom + offset;
      if (upY < maxY - 1 && isSafeSpawn(level, pos, x, upY, z)) {
        Mystcraft.LOGGER.debug("Found safe spawn at Y={} (searched up from {})", upY, searchFrom);
        return upY;
      }

      int downY = searchFrom - offset;
      if (downY > minY && isSafeSpawn(level, pos, x, downY, z)) {
        Mystcraft.LOGGER.debug("Found safe spawn at Y={} (searched down from {})", downY, searchFrom);
        return downY;
      }
    }

    Mystcraft.LOGGER.warn("Exhaustive search for safe spawn at ({}, {})", x, z);
    for (int y = maxY - 2; y > minY; y--) {
      if (isSafeSpawn(level, pos, x, y, z)) {
        Mystcraft.LOGGER.info("Found safe spawn via full column scan at Y={}", y);
        return y;
      }
    }

    Mystcraft.LOGGER.warn("No safe spawn found at ({}, {}), using heightmap+1 Y={}", x, z, heightmapY);
    return heightmapY;
  }

  private static boolean hasCeilingTerrain(ServerLevel level) {
    ChunkGenerator generator = level.getChunkSource().getGenerator();
    if (generator instanceof art.arcane.mystcraft.world.gen.AgeChunkGenerator ageGen) {
      String type = ageGen.getTerrainType();
      return "nether".equals(type) || "cave".equals(type);
    }

    return level.dimensionType().hasCeiling();
  }

  private static boolean isSafeSpawn(ServerLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
    int minY = level.getMinBuildHeight();
    if (y <= minY || y >= level.getMaxBuildHeight() - 1) {
      return false;
    }

    if (!level.hasChunk(x >> 4, z >> 4)) {
      return false;
    }

    pos.set(x, y - 1, z);
    BlockState ground = level.getBlockState(pos);
    if (!ground.isSolidRender(level, pos)) {
      return false;
    }

    if (ground.is(Blocks.LAVA) || ground.is(Blocks.FIRE) || ground.is(Blocks.SOUL_FIRE)
        || ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.BEDROCK)) {
      return false;
    }

    pos.set(x, y, z);
    BlockState feet = level.getBlockState(pos);
    if (feet.blocksMotion()) {
      return false;
    }
    if (!feet.getFluidState().isEmpty()) {
      return false;
    }
    if (feet.is(Blocks.FIRE) || feet.is(Blocks.SOUL_FIRE)) {
      return false;
    }

    pos.set(x, y + 1, z);
    BlockState head = level.getBlockState(pos);
    if (head.blocksMotion()) {
      return false;
    }
    return head.getFluidState().isEmpty();
  }

  private static void playLinkSound(ServerLevel level, BlockPos pos, CompoundTag linkData, boolean isDeparture) {
    if (isDeparture) {
      playSound(level, pos, ModSounds.LINKING_POP.get(), 1.0f, 1.0f);
    } else {
      boolean isIntra = LinkOptions.getFlag(linkData, LinkFlags.INTRA_LINKING);
      if (isIntra) {
        playSound(level, pos, ModSounds.LINKING_INTRA.get(), 1.0f, 1.0f);
      } else {
        playSound(level, pos, ModSounds.LINKING_LINK.get(), 1.0f, 1.0f);
      }
    }
  }

  private static void playSound(ServerLevel level, BlockPos pos, net.minecraft.sounds.SoundEvent sound,
                                float volume, float pitch) {
    level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
  }

  private static void sendLinkEffect(ServerLevel level, BlockPos pos, LinkEffectPacket.LinkEffectType type) {
    LinkEffectPacket packet = new LinkEffectPacket(pos, type);
    for (ServerPlayer player : level.players()) {
      if (player.blockPosition().distSqr(pos) < 64 * 64) {
        MystcraftNetwork.sendToPlayer(packet, player);
      }
    }
  }

  /**
   * Creates link data for a position in the world.
   */
  public static CompoundTag createLinkData(Level level, BlockPos pos, float yaw) {
    CompoundTag data = new CompoundTag();
    LinkOptions.setSpawn(data, pos);
    LinkOptions.setSpawnYaw(data, yaw);
    LinkOptions.setDimensionUID(data, getDimensionUID(level));
    return data;
  }

  /**
   * Creates link data for a position with a display name.
   */
  public static CompoundTag createLinkData(Level level, BlockPos pos, float yaw, String displayName) {
    CompoundTag data = createLinkData(level, pos, yaw);
    LinkOptions.setDisplayName(data, displayName);
    return data;
  }

  private static void checkMystDimensionAdvancements(ServerPlayer player) {
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      ItemStack itemStack = player.getInventory().getItem(i);
      if (!itemStack.isEmpty() && itemStack.getItem() instanceof art.arcane.mystcraft.item.LinkbookItem) {
        art.arcane.mystcraft.advancements.ModAdvancements.triggerEnterMystDimensionSafe(player);
        return;
      }
    }
    art.arcane.mystcraft.advancements.ModAdvancements.triggerEnterMystDimensionQuinn(player);
  }

  /**
   * Result of a link operation.
   */
  public enum LinkResult {
    SUCCESS,
    INVALID_DESTINATION,
    DIMENSION_NOT_FOUND,
    CANCELLED,
    BLOCKED,
    PERMISSION_DENIED,
    START_CANCELLED,
    TOO_UNSTABLE,
    TELEPORT_FAILED
  }

  private record RidingEntityData(Entity entity, @Nullable Entity parent, Vec3 offset) {
  }

  private record RidingGroupData(Entity anchor, List<RidingEntityData> members) {
  }

  private record TeleportedRidingGroup(Entity anchor) {
  }

  private record FollowerData(RidingGroupData group, Vec3 offset, Vec3 momentum) {
  }

  private record PendingDisarm(
      ServerLevel sourceLevel,
      ServerPlayer player,
      int inventorySlot,
      ItemStack reservedItem,
      Vec3 sourcePosition
  ) {
    private void commit() {
      ItemEntity droppedItem = new ItemEntity(
          sourceLevel,
          sourcePosition.x,
          sourcePosition.y + 0.5,
          sourcePosition.z,
          reservedItem.copy()
      );
      droppedItem.setDefaultPickUpDelay();
      if (sourceLevel.addFreshEntity(droppedItem)) {
        playSound(
            sourceLevel,
            BlockPos.containing(sourcePosition),
            ModSounds.LINKING_DISARM.get(),
            1.0f,
            1.0f
        );
      } else {
        rollback();
      }
    }

    private void rollback() {
      if (player.getInventory().getItem(inventorySlot).isEmpty()) {
        player.getInventory().setItem(inventorySlot, reservedItem);
      } else {
        player.getInventory().placeItemBackInInventory(reservedItem);
      }
    }
  }

  private record PendingReturnLink(UUID playerId, int targetAgeUID, CompoundTag returnData) {
    private void commit(MinecraftServer server) {
      AgeReturnData.get(server).setReturnLink(playerId, targetAgeUID, returnData);
    }
  }
}
