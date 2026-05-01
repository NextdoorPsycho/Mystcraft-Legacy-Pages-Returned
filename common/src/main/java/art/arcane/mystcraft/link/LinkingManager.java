package art.arcane.mystcraft.link;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.instability.InstabilityManager;
import art.arcane.mystcraft.network.LinkEffectPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ChunkStatusCompat;
import art.arcane.mystcraft.util.MystcraftChunkLeases;
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
import net.minecraft.world.entity.Mob;
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
import java.util.List;

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
        player.sendSystemMessage(Component.literal("You cannot leave this Age."));
        return LinkResult.PERMISSION_DENIED;
      }

      int targetUID = dimId;
      if (targetUID > 0 && !permissions.canEnter(player, targetUID)) {
        fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
            LinkEvent.Failed.FailureReason.PERMISSION_DENIED, "You cannot enter this Age");
        player.sendSystemMessage(Component.literal("You cannot enter this Age."));
        return LinkResult.PERMISSION_DENIED;
      }
    }

    boolean isIntraLink = sourceLevel.dimension().equals(targetLevel.dimension());
    boolean allowIntra = LinkOptions.getFlag(linkData, LinkFlags.INTRA_LINKING);

    if (isIntraLink && !allowIntra) {
      Mystcraft.LOGGER.debug("Intra-dimensional link attempted without modifier");
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

    targetLevel = alterEvent.getTargetLevel();
    targetVec = alterEvent.getTargetPosition();
    targetYaw = alterEvent.getTargetYaw();

    LinkEvent.Start startEvent = new LinkEvent.Start(entity, linkData, sourceDimension, sourcePos,
        targetLevel, targetVec, targetYaw);

    if (startEvent.isCancelled()) {
      fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
          LinkEvent.Failed.FailureReason.START_CANCELLED, "Link start was cancelled");
      return LinkResult.START_CANCELLED;
    }

    playLinkSound(sourceLevel, sourcePos, linkData, true);
    sendLinkEffect(sourceLevel, sourcePos, LinkEffectPacket.LinkEffectType.DEPARTURE);

    List<Entity> followers = null;
    if (LinkOptions.getFlag(linkData, LinkFlags.FOLLOWING)) {
      followers = getFollowingEntities(entity);
    }

    List<PassengerData> passengers = collectPassengers(entity);

    if (LinkOptions.getFlag(linkData, LinkFlags.DISARM) && entity instanceof ServerPlayer player) {
      disarmEntity(player);
    }

    Vec3 momentum = entity.getDeltaMovement();

    if (entity instanceof ServerPlayer player) {
      int targetAgeUID = AgeDimensionFactory.getAgeUID(targetLevel.dimension());
      if (targetAgeUID > 0) {
        CompoundTag returnData = new CompoundTag();
        LinkOptions.setDimensionUID(returnData, getDimensionUID(sourceLevel));
        LinkOptions.setSpawn(returnData, sourcePos);
        LinkOptions.setSpawnYaw(returnData, player.getYRot());
        AgeReturnData.get(server).setReturnLink(player.getUUID(), targetAgeUID, returnData);
      }
    }

    teleportEntity(entity, targetLevel, targetVec, targetYaw);

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

    restorePassengers(entity, targetLevel, passengers, targetVec);

    if (followers != null && !followers.isEmpty()) {
      teleportFollowers(entity, followers, targetLevel, targetVec, linkData);
    }

    BlockPos arrivalPos = BlockPos.containing(targetVec);
    playLinkSound(targetLevel, arrivalPos, linkData, false);
    sendLinkEffect(targetLevel, arrivalPos, LinkEffectPacket.LinkEffectType.ARRIVAL);

    LinkEvent.End endEvent = new LinkEvent.End(entity, linkData, sourceDimension, sourcePos,
        targetLevel, targetVec);

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

  }

  private static List<PassengerData> collectPassengers(Entity root) {
    List<PassengerData> passengers = new ArrayList<>();
    collectPassengersRecursive(root, root.position(), passengers);
    return passengers;
  }

  private static void collectPassengersRecursive(Entity entity, Vec3 rootPos, List<PassengerData> list) {
    for (Entity passenger : entity.getPassengers()) {
      Vec3 offset = passenger.position().subtract(rootPos);
      list.add(new PassengerData(passenger, offset));
      collectPassengersRecursive(passenger, rootPos, list);
    }
  }

  private static void restorePassengers(Entity root, ServerLevel level, List<PassengerData> passengers, Vec3 rootPos) {

    root.ejectPassengers();

    for (PassengerData data : passengers) {
      Entity passenger = data.passenger;
      Vec3 passengerPos = rootPos.add(data.offset);

      teleportEntity(passenger, level, passengerPos, passenger.getYRot());
    }

  }

  private static void teleportFollowers(Entity source, List<Entity> followers, ServerLevel targetLevel,
                                        Vec3 targetVec, CompoundTag linkData) {
    for (Entity follower : followers) {
      if (follower == source) continue;

      Vec3 offset = follower.position().subtract(source.position());
      Vec3 followerTarget = targetVec.add(offset);

      Vec3 followerMomentum = follower.getDeltaMovement();

      List<PassengerData> followerPassengers = collectPassengers(follower);

      teleportEntity(follower, targetLevel, followerTarget, follower.getYRot());

      if (LinkOptions.getFlag(linkData, LinkFlags.MAINTAIN_MOMENTUM)) {
        follower.setDeltaMovement(followerMomentum);
        if (follower instanceof ServerPlayer followerPlayer && followerPlayer.connection != null) {
          followerPlayer.connection.send(
              new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                  followerPlayer.getId(), followerMomentum));
        } else {
          follower.hurtMarked = true;
        }
      }

      restorePassengers(follower, targetLevel, followerPassengers, followerTarget);
    }

    BlockPos targetPos = BlockPos.containing(targetVec);
    playSound(targetLevel, targetPos, ModSounds.LINKING_FOLLOWING.get(), 1.0f, 1.0f);
    sendLinkEffect(targetLevel, targetPos, LinkEffectPacket.LinkEffectType.FOLLOWING);
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

  private static void teleportEntity(Entity entity, ServerLevel targetLevel, Vec3 targetPos, float yaw) {
    if (entity instanceof ServerPlayer player) {
      ServerPlayerTeleport.teleport(player, targetLevel, targetPos.x, targetPos.y, targetPos.z, yaw, player.getXRot());
    } else {
      entity.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, null, yaw, entity.getXRot());
    }
  }

  private static List<Entity> getFollowingEntities(Entity source) {
    List<Entity> followers = new ArrayList<>();
    double radius = 3.0;

    Entity rootVehicle = source.getRootVehicle();
    if (rootVehicle != source) {
      followers.add(rootVehicle);

      addAllPassengers(rootVehicle, followers);
    }

    AABB area = new AABB(
        source.getX() - radius, source.getY() - radius, source.getZ() - radius,
        source.getX() + radius, source.getY() + radius, source.getZ() + radius
    );

    List<Entity> nearbyEntities = source.level().getEntities(source, area, e -> {
      if (e == source || followers.contains(e)) return false;

      if (e instanceof LivingEntity) {

        if (source instanceof Player player && e instanceof Mob mob) {

          if (mob.getLeashHolder() == player) {
            return true;
          }
        }
        return true;
      }
      return false;
    });

    followers.addAll(nearbyEntities);
    return followers;
  }

  private static void addAllPassengers(Entity entity, List<Entity> list) {
    for (Entity passenger : entity.getPassengers()) {
      if (!list.contains(passenger)) {
        list.add(passenger);
        addAllPassengers(passenger, list);
      }
    }
  }

  private static void disarmEntity(ServerPlayer player) {

    if (!player.getMainHandItem().isEmpty()) {
      player.drop(player.getMainHandItem().copy(), false);
      player.getMainHandItem().setCount(0);
    }

    playSound(player.serverLevel(), player.blockPosition(), ModSounds.LINKING_DISARM.get(), 1.0f, 1.0f);
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
    TOO_UNSTABLE
  }

  /**
   * Link flags that can be applied to modify linking behavior.
   */
  public static class LinkFlags {
    public static final String FOLLOWING = "following";
    public static final String DISARM = "disarm";
    public static final String INTRA_LINKING = "intralinking";
    public static final String RELATIVE = "relative";
    public static final String GENERATE_PLATFORM = "generateplatform";
    public static final String MAINTAIN_MOMENTUM = "maintainmomentum";
  }

  private record PassengerData(Entity passenger, Vec3 offset) {
  }
}
