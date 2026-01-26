package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Environmental effect that applies a potion effect to hostile mobs (buffing enemies).
 */
public class EffectPotionEnemy implements IEnvironmentalEffect {

    private final int level;
    private final boolean global;
    private final MobEffect effect;
    private final int duration;

    private static final float BASE_CHANCE = 0.002f;

    /**
     * Creates an enemy buff effect.
     *
     * @param level    The effect level (affects frequency)
     * @param global   If true, affects all mobs; if false, only nearby mobs
     * @param effect   The mob effect to apply
     * @param duration The duration in ticks
     */
    public EffectPotionEnemy(int level, boolean global, MobEffect effect, int duration) {
        this.level = level;
        this.global = global;
        this.effect = effect;
        this.duration = duration;
    }

    @Override
    public void tick(ServerLevel level, LevelChunk chunk) {
        // Calculate chance based on level
        float chance = BASE_CHANCE * this.level;
        if (level.random.nextFloat() >= chance) {
            return;
        }

        // Get mobs in or near the chunk
        AABB area;
        if (global) {
            // Large area around players
            if (level.players().isEmpty()) return;
            var player = level.players().get(0);
            area = new AABB(player.blockPosition()).inflate(128, 64, 128);
        } else {
            // Just this chunk area
            int chunkX = chunk.getPos().getMinBlockX();
            int chunkZ = chunk.getPos().getMinBlockZ();
            area = new AABB(chunkX, level.getMinBuildHeight(), chunkZ,
                    chunkX + 16, level.getMaxBuildHeight(), chunkZ + 16);
        }

        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, area);
        if (mobs.isEmpty()) {
            return;
        }

        // Apply effect to a random mob
        Mob target = mobs.get(level.random.nextInt(mobs.size()));
        target.addEffect(new MobEffectInstance(effect, duration, 0));
    }
}
