package art.arcane.mystcraft.api.world.logic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for terrain generators that create the base terrain shape for an Age.
 * Implementations fill chunks with solid terrain blocks based on noise algorithms.
 */
public interface ITerrainGenerator {

    /**
     * Generates the base terrain for a chunk.
     * This fills the chunk with terrain blocks, sea blocks, and air based on the
     * generator's algorithm (noise-based, flat, void, etc.)
     *
     * @param chunkX The chunk X coordinate
     * @param chunkZ The chunk Z coordinate
     * @param chunk The chunk to fill with terrain
     * @param random The random source seeded for this chunk
     */
    void generateTerrain(int chunkX, int chunkZ, ChunkAccess chunk, RandomSource random);

    /**
     * Sets the primary terrain block (replaces stone).
     * @param block The block state to use for solid terrain
     */
    void setTerrainBlock(BlockState block);

    /**
     * Gets the primary terrain block.
     * @return The terrain block state
     */
    BlockState getTerrainBlock();

    /**
     * Sets the sea/fluid block.
     * @param block The block state to use for fluids
     */
    void setSeaBlock(BlockState block);

    /**
     * Gets the sea/fluid block.
     * @return The sea block state
     */
    BlockState getSeaBlock();

    /**
     * Gets the terrain generator type identifier.
     * @return The type name (e.g., "normal", "flat", "void")
     */
    String getType();
}
