package art.arcane.mystcraft.api.instability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Interface for environmental effects caused by instability.
 * Effects tick each world tick in loaded chunks.
 */
public interface IEnvironmentalEffect {

    /**
     * Called each tick while the effect is active.
     *
     * @param level The server level
     * @param chunk The chunk being processed
     */
    void tick(ServerLevel level, LevelChunk chunk);
}
