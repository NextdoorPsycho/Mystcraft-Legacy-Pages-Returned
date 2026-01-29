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
 * Prevents death in personal pocket dimensions and returns players to their entry point.
 */
public final class PersonalPocketEscapeHandler {

    private PersonalPocketEscapeHandler() {
    }

    public static boolean handleDeath(ServerPlayer player, DamageSource source) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!PersonalPocketDimension.isPersonalPocket(level)) {
            return false;
        }
        teleportBack(player, level);
        return true;
    }

    public static boolean handleVoidFall(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!PersonalPocketDimension.isPersonalPocket(level)) {
            return false;
        }
        int minY = level.getMinBuildHeight();
        if (player.getY() > (double) (minY - 5)) {
            return false;
        }
        teleportBack(player, level);
        return true;
    }

    private static void teleportBack(ServerPlayer player, ServerLevel pocketLevel) {
        MinecraftServer server = pocketLevel.getServer();
        CompoundTag link = PersonalPocketData.get(server).getReturnLink(player.getUUID());

        ServerLevel targetLevel = server.overworld();
        BlockPos targetPos = targetLevel.getSharedSpawnPos();
        float yaw = player.getYRot();

        if (link != null) {
            Integer uid = LinkOptions.getDimensionUID(link);
            BlockPos spawn = LinkOptions.getSpawn(link);
            float linkYaw = LinkOptions.getSpawnYaw(link);
            if (uid != null && spawn != null && uid != art.arcane.mystcraft.world.PersonalPocketDimension.getPersonalAgeUid(player.getUUID())) {
                ServerLevel found = LinkingManager.findDimensionByUID(server, uid);
                if (found != null) {
                    targetLevel = found;
                    targetPos = spawn;
                    yaw = linkYaw;
                }
            }
        }

        player.setHealth(player.getMaxHealth());
        player.setRemainingFireTicks(0);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        player.teleportTo(targetLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, yaw, player.getXRot());
        targetLevel.playSound(null, targetPos, ModSounds.LINKING_LINK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        if (targetLevel.dimension() != Level.OVERWORLD) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        }
    }
}
