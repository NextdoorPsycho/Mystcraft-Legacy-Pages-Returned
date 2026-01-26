package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.entity.MeteorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that spawns falling meteors.
 */
public class EffectMeteor implements IEnvironmentalEffect {

    private static final float BASE_CHANCE = 0.0001f;
    private static final int RANGE = 48;

    @Override
    public void tick(ServerLevel level, LevelChunk chunk) {
        if (level.random.nextFloat() >= BASE_CHANCE) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        // Pick a random player as reference point
        ServerPlayer target = players.get(level.random.nextInt(players.size()));

        // Random position near the player
        double x = target.getX() + level.random.nextIntBetweenInclusive(-RANGE, RANGE);
        double z = target.getZ() + level.random.nextIntBetweenInclusive(-RANGE, RANGE);
        double y = Math.min(target.getY() + 100 + level.random.nextInt(50), level.getMaxBuildHeight() - 1);

        // Random size 1-3
        int size = 1 + level.random.nextInt(3);

        // Create meteor
        MeteorEntity meteor = new MeteorEntity(level, x, y, z, size);
        level.addFreshEntity(meteor);
    }
}
