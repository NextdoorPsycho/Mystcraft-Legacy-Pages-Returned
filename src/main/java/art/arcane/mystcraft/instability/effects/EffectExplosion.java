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
 */
public class EffectExplosion implements IEnvironmentalEffect {

    private static final float BASE_CHANCE = 0.0002f;
    private static final int RANGE = 32;
    private static final float EXPLOSION_POWER = 2.0f;

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
        int x = target.getBlockX() + level.random.nextIntBetweenInclusive(-RANGE, RANGE);
        int z = target.getBlockZ() + level.random.nextIntBetweenInclusive(-RANGE, RANGE);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

        // Don't explode too close to the player
        BlockPos pos = new BlockPos(x, y, z);
        if (target.blockPosition().distSqr(pos) < 64) { // 8 blocks min distance
            return;
        }

        // Create explosion
        level.explode(null, x + 0.5, y + 0.5, z + 0.5,
                EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
    }
}
