package art.arcane.mystcraft.world.gen.populate;

import art.arcane.mystcraft.api.world.logic.IPopulate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Tendrils populator that generates vine-like terrain formations.
 * Used when the Tendrils symbol is applied to an age.
 * Generates winding columns of blocks extending from ceiling or ground.
 */
public class TendrilsPopulator implements IPopulate {

    private final long seed;

    private static final int TENDRILS_PER_CHUNK = 4;
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 25;
    private static final float CEILING_CHANCE = 0.3f;

    public TendrilsPopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(ServerLevel world, RandomSource random, BlockPos chunkPos) {
        int chunkX = chunkPos.getX();
        int chunkZ = chunkPos.getZ();

        for (int i = 0; i < TENDRILS_PER_CHUNK; i++) {
            int x = chunkX + random.nextInt(16);
            int z = chunkZ + random.nextInt(16);

            // Determine if tendril hangs from ceiling or grows from ground
            boolean fromCeiling = random.nextFloat() < CEILING_CHANCE;

            int y;
            if (fromCeiling) {
                // Find a suitable ceiling position (look for air below solid blocks)
                y = findCeilingPosition(world, x, z, random);
                if (y == -1) {
                    continue; // No suitable ceiling found
                }
            } else {
                // Ground tendrils start at surface
                y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            }

            BlockPos startPos = new BlockPos(x, y, z);

            // Check if valid location
            if (!canStartTendril(world, startPos, fromCeiling)) {
                continue;
            }

            // Generate tendril
            generateTendril(world, random, startPos, fromCeiling);
        }
    }

    private int findCeilingPosition(ServerLevel world, int x, int z, RandomSource random) {
        // Look for a suitable ceiling between y=80 and y=200
        for (int attempt = 0; attempt < 10; attempt++) {
            int y = 80 + random.nextInt(120);
            BlockPos checkPos = new BlockPos(x, y, z);

            // Check if there's a solid block with air below
            if (world.getBlockState(checkPos).isSolid() &&
                    world.getBlockState(checkPos.below()).isAir()) {
                return y;
            }
        }
        return -1; // No suitable position found
    }

    private boolean canStartTendril(ServerLevel world, BlockPos pos, boolean fromCeiling) {
        if (fromCeiling) {
            // Need solid block above and air below
            return world.getBlockState(pos).isSolid() &&
                   world.getBlockState(pos.below()).isAir();
        } else {
            // Need solid ground
            BlockState ground = world.getBlockState(pos.below());
            return ground.isSolid() && !ground.is(BlockTags.LEAVES);
        }
    }

    private void generateTendril(ServerLevel world, RandomSource random, BlockPos startPos, boolean fromCeiling) {
        // Choose tendril material
        BlockState tendrilBlock = getTendrilMaterial(random);
        BlockState decorationBlock = getDecorationBlock(tendrilBlock, random);

        int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);

        // Direction multiplier (down for ceiling, up for ground)
        int direction = fromCeiling ? -1 : 1;

        // Track current position with floating point for smooth curves
        double currentX = startPos.getX();
        double currentY = startPos.getY();
        double currentZ = startPos.getZ();

        // Random curve parameters
        double curvatureX = (random.nextDouble() - 0.5) * 0.3;
        double curvatureZ = (random.nextDouble() - 0.5) * 0.3;

        // Thickness of the tendril
        int baseThickness = 1 + random.nextInt(2);

        for (int segment = 0; segment < length; segment++) {
            float progress = (float) segment / length;

            // Update curve direction occasionally
            if (segment % 5 == 0) {
                curvatureX += (random.nextDouble() - 0.5) * 0.15;
                curvatureZ += (random.nextDouble() - 0.5) * 0.15;

                // Clamp curvature
                curvatureX = Math.max(-0.4, Math.min(0.4, curvatureX));
                curvatureZ = Math.max(-0.4, Math.min(0.4, curvatureZ));
            }

            // Move in the curved direction
            currentX += curvatureX;
            currentY += direction;
            currentZ += curvatureZ;

            // Thickness decreases toward the tip
            int thickness = (int) Math.max(1, baseThickness * (1.0f - progress * 0.7f));

            // Place blocks in a small cross-section
            for (int dx = -thickness; dx <= thickness; dx++) {
                for (int dz = -thickness; dz <= thickness; dz++) {
                    int distance = Math.abs(dx) + Math.abs(dz);
                    if (distance <= thickness) {
                        BlockPos tendrilPos = new BlockPos(
                                (int) Math.floor(currentX + dx),
                                (int) Math.floor(currentY),
                                (int) Math.floor(currentZ + dz)
                        );

                        // Place tendril block
                        if (shouldPlaceTendrilBlock(world, tendrilPos)) {
                            world.setBlock(tendrilPos, tendrilBlock, 2);

                            // Add decorations occasionally
                            if (segment > 3 && random.nextInt(8) == 0) {
                                addTendrilDecoration(world, random, tendrilPos, decorationBlock, fromCeiling);
                            }
                        }
                    }
                }
            }

            // Early termination if we hit something solid
            BlockPos checkPos = new BlockPos(
                    (int) Math.floor(currentX),
                    (int) Math.floor(currentY),
                    (int) Math.floor(currentZ)
            );

            if (fromCeiling && checkPos.getY() < startPos.getY() - length) {
                break; // Went too far down
            } else if (!fromCeiling && checkPos.getY() > startPos.getY() + length) {
                break; // Went too far up
            }

            // Stop if we hit a solid obstacle
            if (segment > 5) {
                BlockPos nextPos = new BlockPos(
                        (int) Math.floor(currentX),
                        (int) Math.floor(currentY + direction),
                        (int) Math.floor(currentZ)
                );
                if (world.getBlockState(nextPos).isSolid() && !world.getBlockState(nextPos).is(tendrilBlock.getBlock())) {
                    break;
                }
            }
        }
    }

    private boolean shouldPlaceTendrilBlock(ServerLevel world, BlockPos pos) {
        BlockState existing = world.getBlockState(pos);
        return existing.isAir() ||
               existing.is(BlockTags.LEAVES) ||
               existing.is(Blocks.SNOW) ||
               existing.is(Blocks.VINE) ||
               existing.is(Blocks.WATER);
    }

    private BlockState getTendrilMaterial(RandomSource random) {
        int choice = random.nextInt(8);
        return switch (choice) {
            case 0 -> Blocks.STONE.defaultBlockState();
            case 1 -> Blocks.COBBLESTONE.defaultBlockState();
            case 2 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 3 -> Blocks.ANDESITE.defaultBlockState();
            case 4 -> Blocks.DRIPSTONE_BLOCK.defaultBlockState();
            case 5 -> Blocks.BASALT.defaultBlockState();
            case 6 -> Blocks.BLACKSTONE.defaultBlockState();
            default -> Blocks.STONE_BRICKS.defaultBlockState();
        };
    }

    private BlockState getDecorationBlock(BlockState baseBlock, RandomSource random) {
        // Choose decoration that matches the tendril material
        if (random.nextInt(3) == 0) {
            int choice = random.nextInt(5);
            return switch (choice) {
                case 0 -> Blocks.GLOWSTONE.defaultBlockState();
                case 1 -> Blocks.SHROOMLIGHT.defaultBlockState();
                case 2 -> Blocks.SEA_LANTERN.defaultBlockState();
                case 3 -> Blocks.OCHRE_FROGLIGHT.defaultBlockState();
                default -> Blocks.LANTERN.defaultBlockState();
            };
        }
        return baseBlock;
    }

    private void addTendrilDecoration(ServerLevel world, RandomSource random, BlockPos pos,
                                      BlockState decorationBlock, boolean fromCeiling) {
        // Add small protrusions or light sources
        int[] directions = {0, 1, 2, 3}; // N, S, E, W
        int direction = directions[random.nextInt(4)];

        int dx = (direction == 0) ? 1 : (direction == 1) ? -1 : 0;
        int dz = (direction == 2) ? 1 : (direction == 3) ? -1 : 0;

        BlockPos decorPos = pos.offset(dx, 0, dz);
        if (world.getBlockState(decorPos).isAir()) {
            world.setBlock(decorPos, decorationBlock, 2);
        }
    }

    @Override
    public String getIdentifier() {
        return "mystcraft:tendrils";
    }
}
