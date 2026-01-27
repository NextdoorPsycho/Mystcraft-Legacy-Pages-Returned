package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that causes random explosions.
 * Scales with instability: nearly silent at low values, dangerous at high.
 */
public class EffectExplosion implements IEnvironmentalEffect {

    private static final float BASE_CHANCE = 0.0005f;
    private static final int RANGE = 32;
    private static final float MIN_POWER = 1.0f;
    private static final float MAX_POWER = 2.0f;

    @Override
    public void tick(ServerLevel level, LevelChunk chunk, float instability) {
        // Destructive: ramps from near-zero at instability 30 to full at 100
        float intensity = Math.max(0.0f, Math.min((instability - 30.0f) / 70.0f, 1.0f));
        if (intensity <= 0.0f) return;

        if (level.random.nextFloat() >= BASE_CHANCE * intensity) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        // Pick a random player as reference point
        ServerPlayer target = players.get(level.random.nextInt(players.size()));

        // Random position near the player
        int x = target.getBlockX() + level.random.nextIntBetweenInclusive(-RANGE, RANGE);
        int z = target.getBlockZ() + level.random.nextIntBetweenInclusive(-RANGE, RANGE);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

        // Don't explode too close to the player
        BlockPos pos = new BlockPos(x, y, z);
        if (target.blockPosition().distSqr(pos) < 64) { // 8 blocks min distance
            return;
        }

        // Power scales with intensity
        float power = MIN_POWER + (MAX_POWER - MIN_POWER) * intensity;

        // Create explosion
        level.explode(null, x + 0.5, y + 0.5, z + 0.5,
                power, Level.ExplosionInteraction.BLOCK);
    }
}
