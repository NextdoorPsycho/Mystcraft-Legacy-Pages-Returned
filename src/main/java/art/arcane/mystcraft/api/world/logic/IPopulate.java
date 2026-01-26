package art.arcane.mystcraft.api.world.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Interface for population functions that add decorations to generated terrain.
 * Examples include trees, flowers, ores, and custom decorations.
 */
public interface IPopulate {

    /**
     * Populates a chunk with decorations.
     * Called during the decoration phase after terrain is fully generated.
     *
     * @param world The server level
     * @param random The random source for this chunk
     * @param chunkPos The position of the chunk being populated (block coordinates of chunk corner)
     */
    void populate(ServerLevel world, RandomSource random, BlockPos chunkPos);

    /**
     * Gets the population function identifier.
     * @return The identifier name
     */
    String getIdentifier();
}
