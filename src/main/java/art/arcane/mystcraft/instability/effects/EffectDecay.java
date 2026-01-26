package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import art.arcane.mystcraft.block.DecayBlock;
import art.arcane.mystcraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Environmental effect that spawns decay blocks.
 */
public class EffectDecay implements IEnvironmentalEffect {

    private final DecayBlock.DecayType decayType;
    private static final float BASE_CHANCE = 0.0003f;

    /**
     * Creates a decay effect.
     *
     * @param decayType The type of decay to spawn
     */
    public EffectDecay(DecayBlock.DecayType decayType) {
        this.decayType = decayType;
    }

    @Override
    public void tick(ServerLevel level, LevelChunk chunk) {
        if (level.random.nextFloat() >= BASE_CHANCE) {
            return;
        }

        // Pick a random position in the chunk
        int x = chunk.getPos().getMinBlockX() + level.random.nextInt(16);
        int z = chunk.getPos().getMinBlockZ() + level.random.nextInt(16);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);

        // Try to find a valid block to replace
        for (int attempt = 0; attempt < 3; attempt++) {
            BlockPos pos = new BlockPos(x, y - attempt, z);
            BlockState state = level.getBlockState(pos);

            // Skip protected blocks
            if (state.isAir() ||
                state.is(Blocks.BEDROCK) ||
                state.is(Blocks.WATER) ||
                state.is(Blocks.LAVA) ||
                state.getBlock() instanceof DecayBlock) {
                continue;
            }

            // Spawn decay
            BlockState decayState = ModBlocks.DECAY.get().defaultBlockState()
                    .setValue(DecayBlock.DECAY_TYPE, decayType);
            level.setBlock(pos, decayState, 3);
            return;
        }
    }
}
