package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Environmental effect that applies a potion effect to players.
 */
public class EffectPotion implements IEnvironmentalEffect {

    private final int level;
    private final boolean global;
    private final MobEffect effect;
    private final int duration;

    private static final float BASE_CHANCE = 0.001f;

    /**
     * Creates a potion effect.
     *
     * @param level    The effect level (affects frequency)
     * @param global   If true, affects all players; if false, only nearby players
     * @param effect   The mob effect to apply
     * @param duration The duration in ticks
     */
    public EffectPotion(int level, boolean global, MobEffect effect, int duration) {
        this.level = level;
        this.global = global;
        this.effect = effect;
        this.duration = duration;
    }

    @Override
    public void tick(ServerLevel level, LevelChunk chunk, float instability) {
        // Potions only activate at instability 50+ (eating deck gate). Scale from there.
        float intensity = Math.max(0.0f, Math.min((instability - 50.0f) / 50.0f, 1.0f));
        if (intensity <= 0.0f) return;
        float chance = BASE_CHANCE * this.level * intensity;
        if (level.random.nextFloat() >= chance) {
            return;
        }

        List<ServerPlayer> players;
        if (global) {
            players = level.players();
        } else {
            // Only affect players in or near this chunk
            players = level.players().stream()
                    .filter(p -> isNearChunk(p, chunk))
                    .toList();
        }

        if (players.isEmpty()) {
            return;
        }

        // Apply effect to a random player
        ServerPlayer target = players.get(level.random.nextInt(players.size()));
        target.addEffect(new MobEffectInstance(effect, duration, 0));
    }

    private boolean isNearChunk(ServerPlayer player, LevelChunk chunk) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int playerChunkX = player.getBlockX() >> 4;
        int playerChunkZ = player.getBlockZ() >> 4;

        return Math.abs(chunkX - playerChunkX) <= 2 && Math.abs(chunkZ - playerChunkZ) <= 2;
    }
}
