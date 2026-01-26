package art.arcane.mystcraft.link;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.network.LinkEffectPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages linking (teleportation) operations for Mystcraft.
 * Handles cross-dimension teleportation, spawn platform generation,
 * link events, permissions, and sound/particle effects.
 */
public final class LinkingManager {

    private LinkingManager() {
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
        START_CANCELLED
    }

    /**
     * Performs a link for the given entity using the provided link data.
     * Fires events and checks permissions throughout the process.
     *
     * @param entity The entity to teleport
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

        // Get destination information
        BlockPos targetPos = LinkOptions.getSpawn(linkData);
        Integer dimId = LinkOptions.getDimensionUID(linkData);
        float targetYaw = LinkOptions.getSpawnYaw(linkData);

        if (targetPos == null || dimId == null) {
            fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
                    LinkEvent.Failed.FailureReason.INVALID_DESTINATION, "No valid destination");
            return LinkResult.INVALID_DESTINATION;
        }

        // Find the target dimension
        ServerLevel targetLevel = findDimensionByUID(server, dimId);
        if (targetLevel == null) {
            fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
                    LinkEvent.Failed.FailureReason.DIMENSION_NOT_FOUND, "Target dimension not found");
            return LinkResult.DIMENSION_NOT_FOUND;
        }

        // === FIRE ALLOW EVENT ===
        LinkEvent.Allow allowEvent = new LinkEvent.Allow(entity, linkData, sourceDimension, sourcePos);
        MinecraftForge.EVENT_BUS.post(allowEvent);

        if (allowEvent.isCanceled()) {
            String reason = allowEvent.getCancelReason();
            fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
                    LinkEvent.Failed.FailureReason.CANCELLED, reason.isEmpty() ? "Link was cancelled" : reason);

            if (entity instanceof ServerPlayer player && !reason.isEmpty()) {
                player.sendSystemMessage(Component.literal(reason));
            }
            return LinkResult.CANCELLED;
        }

        // === CHECK PERMISSIONS ===
        if (entity instanceof ServerPlayer player) {
            LinkPermissions permissions = LinkPermissions.get(server);

            // Check departure permission from source Age
            int sourceUID = getAgeUID(sourceLevel);
            if (sourceUID > 0 && !permissions.canDepart(player, sourceUID)) {
                fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
                        LinkEvent.Failed.FailureReason.PERMISSION_DENIED, "You cannot leave this Age");
                player.sendSystemMessage(Component.literal("You cannot leave this Age."));
                return LinkResult.PERMISSION_DENIED;
            }

            // Check entry permission to target Age
            int targetUID = dimId;
            if (targetUID > 0 && !permissions.canEnter(player, targetUID)) {
                fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
                        LinkEvent.Failed.FailureReason.PERMISSION_DENIED, "You cannot enter this Age");
                player.sendSystemMessage(Component.literal("You cannot enter this Age."));
                return LinkResult.PERMISSION_DENIED;
            }
        }

        // Check for intra-linking (same dimension teleportation)
        boolean isIntraLink = sourceLevel.dimension().equals(targetLevel.dimension());
        boolean allowIntra = LinkOptions.getFlag(linkData, LinkFlags.INTRA_LINKING);

        if (isIntraLink && !allowIntra) {
            Mystcraft.LOGGER.debug("Intra-dimensional link attempted without modifier");
        }

        // Calculate initial target position
        Vec3 targetVec = calculateTargetPosition(targetPos, linkData, entity.position(), sourcePos, targetLevel);

        // === FIRE ALTER EVENT ===
        LinkEvent.Alter alterEvent = new LinkEvent.Alter(entity, linkData, sourceDimension, sourcePos,
                targetLevel, targetVec, targetYaw);
        MinecraftForge.EVENT_BUS.post(alterEvent);

        // Apply alterations
        targetLevel = alterEvent.getTargetLevel();
        targetVec = alterEvent.getTargetPosition();
        targetYaw = alterEvent.getTargetYaw();

        // === FIRE START EVENT ===
        LinkEvent.Start startEvent = new LinkEvent.Start(entity, linkData, sourceDimension, sourcePos,
                targetLevel, targetVec, targetYaw);
        MinecraftForge.EVENT_BUS.post(startEvent);

        if (startEvent.isCanceled()) {
            fireFailedEvent(entity, linkData, sourceDimension, sourcePos,
                    LinkEvent.Failed.FailureReason.START_CANCELLED, "Link start was cancelled");
            return LinkResult.START_CANCELLED;
        }

        // Play departure sound and effects
        playLinkSound(sourceLevel, sourcePos, linkData, true);
        sendLinkEffect(sourceLevel, sourcePos, LinkEffectPacket.LinkEffectType.DEPARTURE);

        // Handle following link (brings nearby entities)
        List<Entity> followers = null;
        if (LinkOptions.getFlag(linkData, LinkFlags.FOLLOWING)) {
            followers = getFollowingEntities(entity);
        }

        // Collect passengers before teleport
        List<PassengerData> passengers = collectPassengers(entity);

        // Handle disarm (remove items) - only for players
        if (LinkOptions.getFlag(linkData, LinkFlags.DISARM) && entity instanceof ServerPlayer player) {
            disarmEntity(player);
        }

        // Generate spawn platform if needed
        if (LinkOptions.getFlag(linkData, LinkFlags.GENERATE_PLATFORM)) {
            generateSpawnPlatform(targetLevel, BlockPos.containing(targetVec));
        }

        // Store momentum if maintaining
        Vec3 momentum = entity.getDeltaMovement();

        // Perform the teleport
        teleportEntity(entity, targetLevel, targetVec, targetYaw);

        // Restore momentum if flag is set
        if (LinkOptions.getFlag(linkData, LinkFlags.MAINTAIN_MOMENTUM)) {
            entity.setDeltaMovement(momentum);
        }

        // Restore passengers
        restorePassengers(entity, targetLevel, passengers, targetVec);

        // Teleport followers
        if (followers != null && !followers.isEmpty()) {
            teleportFollowers(entity, followers, targetLevel, targetVec, linkData);
        }

        // Play arrival sound and effects
        BlockPos arrivalPos = BlockPos.containing(targetVec);
        playLinkSound(targetLevel, arrivalPos, linkData, false);
        sendLinkEffect(targetLevel, arrivalPos, LinkEffectPacket.LinkEffectType.ARRIVAL);

        // === FIRE END EVENT ===
        LinkEvent.End endEvent = new LinkEvent.End(entity, linkData, sourceDimension, sourcePos,
                targetLevel, targetVec);
        MinecraftForge.EVENT_BUS.post(endEvent);

        return LinkResult.SUCCESS;
    }

    /**
     * Gets the Age UID from a level, or -1 if not a Mystcraft Age.
     */
    private static int getAgeUID(ServerLevel level) {
        if (AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            String path = level.dimension().location().getPath();
            if (path.startsWith("mystcraft_age_")) {
                try {
                    return Integer.parseInt(path.substring("mystcraft_age_".length()));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Fires a Failed event.
     */
    private static void fireFailedEvent(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension,
                                        BlockPos sourcePos, LinkEvent.Failed.FailureReason reason, String message) {
        LinkEvent.Failed failedEvent = new LinkEvent.Failed(entity, linkData, sourceDimension, sourcePos, reason, message);
        MinecraftForge.EVENT_BUS.post(failedEvent);
    }

    /**
     * Data class for storing passenger information during teleport.
     */
    private static class PassengerData {
        final Entity passenger;
        final Vec3 offset;

        PassengerData(Entity passenger, Vec3 offset) {
            this.passenger = passenger;
            this.offset = offset;
        }
    }

    /**
     * Collects all passengers recursively.
     */
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

    /**
     * Restores passengers after teleportation.
     */
    private static void restorePassengers(Entity root, ServerLevel level, List<PassengerData> passengers, Vec3 rootPos) {
        // First, dismount all passengers
        root.ejectPassengers();

        // Teleport each passenger
        for (PassengerData data : passengers) {
            Entity passenger = data.passenger;
            Vec3 passengerPos = rootPos.add(data.offset);

            // Teleport the passenger
            teleportEntity(passenger, level, passengerPos, passenger.getYRot());
        }

        // Re-mount passengers (with a small delay via scheduled tick)
        // This is handled by the entities themselves after teleport
    }

    /**
     * Teleports followers through the link.
     */
    private static void teleportFollowers(Entity source, List<Entity> followers, ServerLevel targetLevel,
                                          Vec3 targetVec, CompoundTag linkData) {
        for (Entity follower : followers) {
            if (follower == source) continue;

            // Calculate offset from source entity
            Vec3 offset = follower.position().subtract(source.position());
            Vec3 followerTarget = targetVec.add(offset);

            // Store follower momentum
            Vec3 followerMomentum = follower.getDeltaMovement();

            // Collect follower's passengers
            List<PassengerData> followerPassengers = collectPassengers(follower);

            // Teleport follower
            teleportEntity(follower, targetLevel, followerTarget, follower.getYRot());

            // Restore momentum if flag is set
            if (LinkOptions.getFlag(linkData, LinkFlags.MAINTAIN_MOMENTUM)) {
                follower.setDeltaMovement(followerMomentum);
            }

            // Restore follower's passengers
            restorePassengers(follower, targetLevel, followerPassengers, followerTarget);
        }

        // Play following sound and effect
        BlockPos targetPos = BlockPos.containing(targetVec);
        playSound(targetLevel, targetPos, ModSounds.LINKING_FOLLOWING.get(), 1.0f, 1.0f);
        sendLinkEffect(targetLevel, targetPos, LinkEffectPacket.LinkEffectType.FOLLOWING);
    }

    /**
     * Finds a ServerLevel by its dimension UID.
     */
    @Nullable
    public static ServerLevel findDimensionByUID(MinecraftServer server, int uid) {
        // Check vanilla dimensions with negative or zero UIDs first
        if (uid == 0) {
            return server.getLevel(Level.OVERWORLD);
        } else if (uid == -1) {
            return server.getLevel(Level.NETHER);
        }

        // For positive UIDs, check Mystcraft Ages BEFORE The End
        // This ensures registered Ages take priority over the End's hardcoded UID 1
        // (handles both new Ages with UID >= 2 and any legacy Ages with UID 1)
        if (uid > 0) {
            AgeManager ageManager = AgeManager.get(server);
            ResourceLocation ageDimension = ageManager.getDimension(uid);
            if (ageDimension != null) {
                // This is a Mystcraft Age, try to get or create the dimension
                ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(server, uid);
                if (ageLevel != null) {
                    return ageLevel;
                }
            }
        }

        // Only now check for The End (uid == 1 but not a registered Age)
        if (uid == 1) {
            return server.getLevel(Level.END);
        }

        // Search for other custom dimensions by hash
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

        // Check if this is a Mystcraft Age
        if (level instanceof ServerLevel serverLevel) {
            AgeManager ageManager = AgeManager.get(serverLevel);
            int ageUID = ageManager.getAgeUID(dimension);
            if (ageUID > 0) {
                return ageUID;
            }
        }

        // For other custom dimensions, use a hash-based ID
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

    /**
     * Teleports an entity to the target position in the target level.
     */
    private static void teleportEntity(Entity entity, ServerLevel targetLevel, Vec3 targetPos, float yaw) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, yaw, player.getXRot());
        } else {
            entity.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, null, yaw, entity.getXRot());
        }
    }

    /**
     * Gets entities that should follow through the link.
     * Enhanced to include mounts, vehicles, and nearby tamed animals.
     */
    private static List<Entity> getFollowingEntities(Entity source) {
        List<Entity> followers = new ArrayList<>();
        double radius = 3.0;

        // Get the root vehicle if source is riding something
        Entity rootVehicle = source.getRootVehicle();
        if (rootVehicle != source) {
            followers.add(rootVehicle);
            // Add all passengers of the vehicle
            addAllPassengers(rootVehicle, followers);
        }

        // Get nearby living entities
        AABB area = new AABB(
                source.getX() - radius, source.getY() - radius, source.getZ() - radius,
                source.getX() + radius, source.getY() + radius, source.getZ() + radius
        );

        List<Entity> nearbyEntities = source.level().getEntities(source, area, e -> {
            if (e == source || followers.contains(e)) return false;

            // Include living entities
            if (e instanceof LivingEntity) {
                // Include tamed mobs owned by the source player
                if (source instanceof Player player && e instanceof Mob mob) {
                    // Check if mob is leashed to the player
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

    /**
     * Recursively adds all passengers of an entity.
     */
    private static void addAllPassengers(Entity entity, List<Entity> list) {
        for (Entity passenger : entity.getPassengers()) {
            if (!list.contains(passenger)) {
                list.add(passenger);
                addAllPassengers(passenger, list);
            }
        }
    }

    /**
     * Removes items from a player (disarm effect).
     */
    private static void disarmEntity(ServerPlayer player) {
        // Drop main hand item
        if (!player.getMainHandItem().isEmpty()) {
            player.drop(player.getMainHandItem().copy(), false);
            player.getMainHandItem().setCount(0);
        }
        // Play disarm sound
        playSound(player.serverLevel(), player.blockPosition(), ModSounds.LINKING_DISARM.get(), 1.0f, 1.0f);
    }

    /**
     * Generates a spawn platform at the target location.
     */
    private static void generateSpawnPlatform(ServerLevel level, BlockPos pos) {
        BlockState platformBlock = Blocks.STONE.defaultBlockState();

        // Create a 3x3 platform
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = pos.offset(x, -1, z);
                if (level.getBlockState(platformPos).isAir()) {
                    level.setBlock(platformPos, platformBlock, 3);
                }
            }
        }

        // Clear the 3x3x2 area above the platform for the player
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

    /**
     * Calculates the final target position, applying any modifiers.
     */
    private static Vec3 calculateTargetPosition(BlockPos targetPos, CompoundTag linkData, Vec3 entityPos,
                                                 BlockPos sourcePos, ServerLevel targetLevel) {
        double x = targetPos.getX() + 0.5;
        double z = targetPos.getZ() + 0.5;

        // Find safe Y position
        int safeY = findSafeY(targetLevel, targetPos.getX(), targetPos.getY(), targetPos.getZ());

        // Apply relative positioning if that flag is set
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

    /**
     * Finds a safe Y position for spawning.
     */
    private static int findSafeY(ServerLevel level, int x, int startY, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);

        // First check if we're already at a safe position
        pos.setY(startY - 1);
        BlockState groundState = level.getBlockState(pos);
        if (groundState.isSolidRender(level, pos)) {
            pos.setY(startY);
            BlockState above1 = level.getBlockState(pos);
            pos.setY(startY + 1);
            BlockState above2 = level.getBlockState(pos);
            if (!above1.blocksMotion() && !above2.blocksMotion()) {
                return startY;
            }
        }

        // Search downward for solid ground
        for (int y = startY; y > level.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.isSolidRender(level, pos)) {
                pos.setY(y + 1);
                BlockState above1 = level.getBlockState(pos);
                pos.setY(y + 2);
                BlockState above2 = level.getBlockState(pos);

                if (!above1.blocksMotion() && !above2.blocksMotion()) {
                    Mystcraft.LOGGER.debug("Found safe spawn at Y={} (original Y={})", y + 1, startY);
                    return y + 1;
                }
            }
        }

        Mystcraft.LOGGER.debug("No safe ground found, using original Y={}", startY);
        return startY;
    }

    /**
     * Plays the appropriate link sound based on flags.
     */
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

    /**
     * Helper method to play sounds.
     */
    private static void playSound(ServerLevel level, BlockPos pos, net.minecraft.sounds.SoundEvent sound,
                                   float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * Sends a link effect packet to all nearby players.
     */
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
}
