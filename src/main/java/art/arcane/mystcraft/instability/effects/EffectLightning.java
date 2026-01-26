package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that spawns random lightning strikes.
 */
public class EffectLightning implements IEnvironmentalEffect {

    private static final float BASE_CHANCE = 0.0003f;
    private static final int RANGE = 64;

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
        double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, (int) x, (int) z);

        // Create lightning bolt
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(x, y, z);
            lightning.setVisualOnly(false);
            level.addFreshEntity(lightning);
        }
    }
}
