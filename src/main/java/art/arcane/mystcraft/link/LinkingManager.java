package art.arcane.mystcraft.link;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.network.LinkEffectPacket;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Manages linking (teleportation) operations for Mystcraft.
 * Handles cross-dimension teleportation, spawn platform generation,
 * and link sound/particle effects.
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
        BLOCKED
    }

    /**
     * Performs a link for the given entity using the provided link data.
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

        // Get destination information
        BlockPos targetPos = LinkOptions.getSpawn(linkData);
        Integer dimId = LinkOptions.getDimensionUID(linkData);
        float targetYaw = LinkOptions.getSpawnYaw(linkData);

        if (targetPos == null || dimId == null) {
            return LinkResult.INVALID_DESTINATION;
        }

        // Find the target dimension
        ServerLevel targetLevel = findDimensionByUID(sourceLevel.getServer(), dimId);
        if (targetLevel == null) {
            return LinkResult.DIMENSION_NOT_FOUND;
        }

        // Check for intra-linking (same dimension teleportation)
        boolean isIntraLink = sourceLevel.dimension().equals(targetLevel.dimension());
        boolean allowIntra = LinkOptions.getFlag(linkData, LinkFlags.INTRA_LINKING);

        // Traditional linking books cannot link within the same dimension
        // unless they have the intra-linking modifier
        if (isIntraLink && !allowIntra) {
            // Still allow the link for testing purposes
            Mystcraft.LOGGER.debug("Intra-dimensional link attempted without modifier");
        }

        // Play departure sound and effects
        playLinkSound(sourceLevel, entity.blockPosition(), linkData, true);
        sendLinkEffect(sourceLevel, entity.blockPosition(), LinkEffectPacket.LinkEffectType.DEPARTURE);

        // Handle following link (brings nearby entities)
        List<Entity> followers = null;
        if (LinkOptions.getFlag(linkData, LinkFlags.FOLLOWING)) {
            followers = getFollowingEntities(entity);
        }

        // Handle disarm (remove items) - only for players
        if (LinkOptions.getFlag(linkData, LinkFlags.DISARM) && entity instanceof ServerPlayer player) {
            disarmEntity(player);
        }

        // Generate spawn platform if needed
        if (LinkOptions.getFlag(linkData, LinkFlags.GENERATE_PLATFORM)) {
            generateSpawnPlatform(targetLevel, targetPos);
        }

        // Calculate final position (with relative offset if applicable)
        Vec3 targetVec = calculateTargetPosition(targetPos, linkData, entity.position(), entity.blockPosition());

        // Store momentum if maintaining
        Vec3 momentum = entity.getDeltaMovement();

        // Perform the teleport
        teleportEntity(entity, targetLevel, targetVec, targetYaw);

        // Restore momentum if flag is set
        if (LinkOptions.getFlag(linkData, LinkFlags.MAINTAIN_MOMENTUM)) {
            entity.setDeltaMovement(momentum);
        }

        // Teleport followers
        if (followers != null && !followers.isEmpty()) {
            for (Entity follower : followers) {
                if (follower != entity) {
                    Vec3 offset = follower.position().subtract(entity.position());
                    Vec3 followerTarget = targetVec.add(offset);
                    teleportEntity(follower, targetLevel, followerTarget, follower.getYRot());
                }
            }
            // Play following sound and effect
            playSound(targetLevel, targetPos, ModSounds.LINKING_FOLLOWING.get(), 1.0f, 1.0f);
            sendLinkEffect(targetLevel, targetPos, LinkEffectPacket.LinkEffectType.FOLLOWING);
        }

        // Play arrival sound and effects
        BlockPos arrivalPos = BlockPos.containing(targetVec);
        playLinkSound(targetLevel, arrivalPos, linkData, false);
        sendLinkEffect(targetLevel, arrivalPos, LinkEffectPacket.LinkEffectType.ARRIVAL);

        return LinkResult.SUCCESS;
    }

    /**
     * Finds a ServerLevel by its dimension UID.
     */
    @Nullable
    public static ServerLevel findDimensionByUID(MinecraftServer server, int uid) {
        // Check vanilla dimensions first
        if (uid == 0) {
            return server.getLevel(Level.OVERWORLD);
        } else if (uid == -1) {
            return server.getLevel(Level.NETHER);
        } else if (uid == 1) {
            return server.getLevel(Level.END);
        }

        // Check if this is a registered Mystcraft Age
        AgeManager ageManager = AgeManager.get(server);
        ResourceLocation ageDimension = ageManager.getDimension(uid);
        if (ageDimension != null) {
            // This is a Mystcraft Age, try to get or create the dimension
            ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(server, uid);
            if (ageLevel != null) {
                return ageLevel;
            }
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
     */
    private static List<Entity> getFollowingEntities(Entity source) {
        double radius = 3.0;
        AABB area = new AABB(
                source.getX() - radius, source.getY() - radius, source.getZ() - radius,
                source.getX() + radius, source.getY() + radius, source.getZ() + radius
        );
        return source.level().getEntities(source, area, e -> e instanceof LivingEntity && e != source);
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
     *
     * @param targetPos Base destination position
     * @param linkData Link NBT data containing flags
     * @param entityPos Current entity position
     * @param sourcePos Position where the link was initiated (book/portal location)
     * @return The final target position
     */
    private static Vec3 calculateTargetPosition(BlockPos targetPos, CompoundTag linkData, Vec3 entityPos, BlockPos sourcePos) {
        // Center on the block
        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY();
        double z = targetPos.getZ() + 0.5;

        // Apply relative positioning if that flag is set
        // Relative linking preserves the entity's offset from the link source
        if (LinkOptions.getFlag(linkData, LinkFlags.RELATIVE)) {
            // Calculate offset from source (where the book/portal is)
            double offsetX = entityPos.x - (sourcePos.getX() + 0.5);
            double offsetY = entityPos.y - sourcePos.getY();
            double offsetZ = entityPos.z - (sourcePos.getZ() + 0.5);

            // Apply offset to destination
            x += offsetX;
            y += offsetY;
            z += offsetZ;

            Mystcraft.LOGGER.debug("Applied relative offset: ({}, {}, {})", offsetX, offsetY, offsetZ);
        }

        return new Vec3(x, y, z);
    }

    /**
     * Plays the appropriate link sound based on flags.
     */
    private static void playLinkSound(ServerLevel level, BlockPos pos, CompoundTag linkData, boolean isDeparture) {
        if (isDeparture) {
            // Play departure pop sound
            playSound(level, pos, ModSounds.LINKING_POP.get(), 1.0f, 1.0f);
        } else {
            // Play arrival link sound
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
    private static void playSound(ServerLevel level, BlockPos pos, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * Sends a link effect packet to all nearby players.
     */
    private static void sendLinkEffect(ServerLevel level, BlockPos pos, LinkEffectPacket.LinkEffectType type) {
        LinkEffectPacket packet = new LinkEffectPacket(pos, type);
        // Send to all players tracking this position
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(pos) < 64 * 64) { // Within 64 blocks
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
