package art.arcane.mystcraft.link;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for all Mystcraft linking events.
 * These events are created during the linking process to allow inspection
 * and cancellation of linking operations.
 */
public abstract class LinkEvent {

    protected final Entity entity;
    protected final CompoundTag linkData;
    protected final ResourceKey<Level> sourceDimension;
    protected final BlockPos sourcePos;

    protected LinkEvent(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension, BlockPos sourcePos) {
        this.entity = entity;
        this.linkData = linkData;
        this.sourceDimension = sourceDimension;
        this.sourcePos = sourcePos;
    }

    /**
     * Gets the entity being linked.
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Gets the link data NBT containing destination and flags.
     */
    public CompoundTag getLinkData() {
        return linkData;
    }

    /**
     * Gets the dimension the entity is linking from.
     */
    public ResourceKey<Level> getSourceDimension() {
        return sourceDimension;
    }

    /**
     * Gets the position the entity is linking from.
     */
    public BlockPos getSourcePos() {
        return sourcePos;
    }

    // --- Event Subtypes ---

    /**
     * Fired before any link processing begins.
     * Cancel this event to prevent the link entirely.
     * This is the first event in the link chain.
     */
    public static class Allow extends LinkEvent {
        private String cancelReason = "";
        private boolean cancelled = false;

        public Allow(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension, BlockPos sourcePos) {
            super(entity, linkData, sourceDimension, sourcePos);
        }

        /**
         * Sets a reason for canceling the link (shown to player).
         */
        public void setCancelReason(String reason) {
            this.cancelReason = reason;
        }

        /**
         * Gets the reason for cancellation.
         */
        public String getCancelReason() {
            return cancelReason;
        }

        /**
         * Cancels this event, preventing the link.
         */
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        /**
         * Returns whether this event has been cancelled.
         */
        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * Fired after Allow but before teleportation.
     * Allows modification of the destination position and rotation.
     * Cannot cancel the link at this stage.
     */
    public static class Alter extends LinkEvent {
        private Vec3 targetPosition;
        private float targetYaw;
        private ServerLevel targetLevel;

        public Alter(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension, BlockPos sourcePos,
                     ServerLevel targetLevel, Vec3 targetPosition, float targetYaw) {
            super(entity, linkData, sourceDimension, sourcePos);
            this.targetLevel = targetLevel;
            this.targetPosition = targetPosition;
            this.targetYaw = targetYaw;
        }

        /**
         * Gets the target dimension.
         */
        public ServerLevel getTargetLevel() {
            return targetLevel;
        }

        /**
         * Sets the target dimension.
         */
        public void setTargetLevel(ServerLevel level) {
            this.targetLevel = level;
        }

        /**
         * Gets the target position.
         */
        public Vec3 getTargetPosition() {
            return targetPosition;
        }

        /**
         * Sets the target position.
         */
        public void setTargetPosition(Vec3 position) {
            this.targetPosition = position;
        }

        /**
         * Gets the target yaw rotation.
         */
        public float getTargetYaw() {
            return targetYaw;
        }

        /**
         * Sets the target yaw rotation.
         */
        public void setTargetYaw(float yaw) {
            this.targetYaw = yaw;
        }
    }

    /**
     * Fired immediately before the teleportation occurs.
     * Last chance to read state before the entity moves.
     * Canceling this event will abort the teleport.
     */
    public static class Start extends LinkEvent {
        private final ServerLevel targetLevel;
        private final Vec3 targetPosition;
        private final float targetYaw;
        private boolean cancelled = false;

        public Start(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension, BlockPos sourcePos,
                     ServerLevel targetLevel, Vec3 targetPosition, float targetYaw) {
            super(entity, linkData, sourceDimension, sourcePos);
            this.targetLevel = targetLevel;
            this.targetPosition = targetPosition;
            this.targetYaw = targetYaw;
        }

        /**
         * Gets the target dimension.
         */
        public ServerLevel getTargetLevel() {
            return targetLevel;
        }

        /**
         * Gets the final target position.
         */
        public Vec3 getTargetPosition() {
            return targetPosition;
        }

        /**
         * Gets the final target yaw.
         */
        public float getTargetYaw() {
            return targetYaw;
        }

        /**
         * Cancels this event, aborting the teleport.
         */
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        /**
         * Returns whether this event has been cancelled.
         */
        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * Fired after the entity has been successfully teleported.
     * The entity is now in the target dimension at the target position.
     * Cannot be canceled.
     */
    public static class End extends LinkEvent {
        private final ServerLevel targetLevel;
        private final Vec3 targetPosition;

        public End(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension, BlockPos sourcePos,
                   ServerLevel targetLevel, Vec3 targetPosition) {
            super(entity, linkData, sourceDimension, sourcePos);
            this.targetLevel = targetLevel;
            this.targetPosition = targetPosition;
        }

        /**
         * Gets the dimension the entity arrived in.
         */
        public ServerLevel getTargetLevel() {
            return targetLevel;
        }

        /**
         * Gets the position the entity arrived at.
         */
        public Vec3 getTargetPosition() {
            return targetPosition;
        }
    }

    /**
     * Fired if the link fails for any reason.
     * Contains information about why the link failed.
     */
    public static class Failed extends LinkEvent {
        private final FailureReason reason;
        private final String message;

        public Failed(Entity entity, CompoundTag linkData, ResourceKey<Level> sourceDimension, BlockPos sourcePos,
                      FailureReason reason, String message) {
            super(entity, linkData, sourceDimension, sourcePos);
            this.reason = reason;
            this.message = message;
        }

        /**
         * Gets the reason for failure.
         */
        public FailureReason getReason() {
            return reason;
        }

        /**
         * Gets a human-readable failure message.
         */
        public String getMessage() {
            return message;
        }

        public enum FailureReason {
            CANCELLED,           // Cancelled by Allow event
            INVALID_DESTINATION, // No valid destination in link data
            DIMENSION_NOT_FOUND, // Target dimension doesn't exist
            PERMISSION_DENIED,   // Player lacks permission
            START_CANCELLED,     // Cancelled by Start event
            TELEPORT_FAILED,     // Minecraft teleportation failed
            UNKNOWN              // Unknown error
        }
    }
}
