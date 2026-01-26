package art.arcane.mystcraft.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Utility methods for entity operations.
 * Ported from legacy Mystcraft EntityUtils.
 */
public final class EntityUtils {

    private EntityUtils() {
    }

    /**
     * Performs a ray trace from the player's eye position in their look direction.
     * This is similar to the old Item.rayTrace method.
     *
     * @param level      The world
     * @param player     The player to ray trace from
     * @param useLiquids Whether to hit liquid blocks
     * @return The block hit result, or null if nothing was hit
     */
    public static BlockHitResult getPlayerPOVHitResult(Level level, Player player, boolean useLiquids) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        Vec3 eyePos = player.getEyePosition();

        float f2 = Mth.cos(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = Mth.sin(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -Mth.cos(-pitch * ((float) Math.PI / 180F));
        float f5 = Mth.sin(-pitch * ((float) Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;

        double reach = 5.0D;
        if (player instanceof ServerPlayer serverPlayer) {
            reach = serverPlayer.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
        }

        Vec3 endPos = eyePos.add(f6 * reach, f5 * reach, f7 * reach);

        ClipContext.Fluid fluidMode = useLiquids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        return level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, fluidMode, player));
    }

    /**
     * Gets the block the player is looking at within reach distance.
     *
     * @param level  The world
     * @param player The player
     * @return The block hit result
     */
    public static BlockHitResult getTargetBlock(Level level, Player player) {
        return getPlayerPOVHitResult(level, player, false);
    }

    /**
     * Gets the block the player is looking at, including liquids.
     *
     * @param level  The world
     * @param player The player
     * @return The block hit result
     */
    public static BlockHitResult getTargetBlockIncludingLiquids(Level level, Player player) {
        return getPlayerPOVHitResult(level, player, true);
    }

    /**
     * Calculates the distance between two entities.
     */
    public static double getDistanceBetween(net.minecraft.world.entity.Entity entity1, net.minecraft.world.entity.Entity entity2) {
        return entity1.distanceTo(entity2);
    }

    /**
     * Calculates the squared distance between two entities.
     * More efficient than getDistanceBetween when only comparing distances.
     */
    public static double getDistanceSqBetween(net.minecraft.world.entity.Entity entity1, net.minecraft.world.entity.Entity entity2) {
        double dx = entity1.getX() - entity2.getX();
        double dy = entity1.getY() - entity2.getY();
        double dz = entity1.getZ() - entity2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Checks if an entity is within a certain distance of a position.
     */
    public static boolean isWithinDistance(net.minecraft.world.entity.Entity entity, double x, double y, double z, double distance) {
        double dx = entity.getX() - x;
        double dy = entity.getY() - y;
        double dz = entity.getZ() - z;
        return dx * dx + dy * dy + dz * dz <= distance * distance;
    }

    /**
     * Gets the look vector for an entity.
     */
    public static Vec3 getLookVector(net.minecraft.world.entity.Entity entity) {
        return entity.getLookAngle();
    }

    /**
     * Applies knockback to an entity from a source position.
     */
    public static void knockbackFrom(net.minecraft.world.entity.Entity entity, double x, double z, double strength) {
        double dx = entity.getX() - x;
        double dz = entity.getZ() - z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) {
            entity.setDeltaMovement(
                    entity.getDeltaMovement().add(
                            dx / dist * strength,
                            0.1D,
                            dz / dist * strength
                    )
            );
        }
    }
}
