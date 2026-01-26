package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Environmental effect that causes blocks to crumble (turn to air or gravel).
 */
public class EffectCrumble implements IEnvironmentalEffect {

    private static final float BASE_CHANCE = 0.0005f;

    @Override
    public void tick(ServerLevel level, LevelChunk chunk) {
        if (level.random.nextFloat() >= BASE_CHANCE) {
            return;
        }

        // Pick a random position in the chunk
        int x = chunk.getPos().getMinBlockX() + level.random.nextInt(16);
        int z = chunk.getPos().getMinBlockZ() + level.random.nextInt(16);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

        // Try a few times to find a crumble-able block
        for (int attempt = 0; attempt < 5; attempt++) {
            BlockPos pos = new BlockPos(x, y - attempt, z);
            BlockState state = level.getBlockState(pos);

            // Skip air, bedrock, and other protected blocks
            if (state.isAir() ||
                state.is(Blocks.BEDROCK) ||
                state.is(Blocks.WATER) ||
                state.is(Blocks.LAVA)) {
                continue;
            }

            // Determine crumble result based on block type
            BlockState result = getCrumbleResult(state, level);
            if (result != null) {
                level.setBlock(pos, result, 3);
                return;
            }
        }
    }

    /**
     * Determines what a block crumbles into.
     */
    private BlockState getCrumbleResult(BlockState original, ServerLevel level) {
        // Stone-like blocks crumble to gravel or air
        if (original.is(Blocks.STONE) ||
            original.is(Blocks.COBBLESTONE) ||
            original.is(Blocks.ANDESITE) ||
            original.is(Blocks.DIORITE) ||
            original.is(Blocks.GRANITE)) {
            return level.random.nextBoolean() ?
                    Blocks.GRAVEL.defaultBlockState() :
                    Blocks.AIR.defaultBlockState();
        }

        // Dirt/grass crumbles to air
        if (original.is(Blocks.DIRT) ||
            original.is(Blocks.GRASS_BLOCK) ||
            original.is(Blocks.COARSE_DIRT)) {
            return Blocks.AIR.defaultBlockState();
        }

        // Gravel/sand falls through (becomes air)
        if (original.is(Blocks.GRAVEL) || original.is(Blocks.SAND)) {
            return Blocks.AIR.defaultBlockState();
        }

        // Wood crumbles to air
        if (original.is(net.minecraft.tags.BlockTags.LOGS) ||
            original.is(net.minecraft.tags.BlockTags.PLANKS)) {
            return Blocks.AIR.defaultBlockState();
        }

        // Default: no crumble
        return null;
    }
}
