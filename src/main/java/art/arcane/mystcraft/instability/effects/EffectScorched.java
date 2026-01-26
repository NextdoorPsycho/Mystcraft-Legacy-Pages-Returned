package art.arcane.mystcraft.instability.effects;

import art.arcane.mystcraft.api.instability.IEnvironmentalEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Environmental effect that causes random fires (scorched earth).
 */
public class EffectScorched implements IEnvironmentalEffect {

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

        BlockPos pos = new BlockPos(x, y, z);
        BlockState stateBelow = level.getBlockState(pos.below());

        // Only place fire if there's a solid block below and air at position
        if (level.getBlockState(pos).isAir() && stateBelow.isSolidRender(level, pos.below())) {
            // Check if fire can survive here
            if (BaseFireBlock.canBePlacedAt(level, pos, net.minecraft.core.Direction.UP)) {
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
}
