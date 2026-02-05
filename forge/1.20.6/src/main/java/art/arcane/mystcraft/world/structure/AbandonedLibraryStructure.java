package art.arcane.mystcraft.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;

import java.util.Optional;

/**
 * Abandoned Library structure that spawns in the Overworld.
 * Contains bookshelves, lecterns with symbol pages, and Mystcraft artifacts.
 */
public class AbandonedLibraryStructure extends Structure {

  public static final MapCodec<AbandonedLibraryStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
          settingsCodec(instance),
          StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
          ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
          Codec.intRange(0, 7).fieldOf("size").forGetter(structure -> structure.maxDepth),
          HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
          Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
          Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
      ).apply(instance, AbandonedLibraryStructure::new)
  );

  private final Holder<StructureTemplatePool> startPool;
  private final Optional<ResourceLocation> startJigsawName;
  private final int maxDepth;
  private final HeightProvider startHeight;
  private final Optional<Heightmap.Types> projectStartToHeightmap;
  private final int maxDistanceFromCenter;

  public AbandonedLibraryStructure(
      StructureSettings settings,
      Holder<StructureTemplatePool> startPool,
      Optional<ResourceLocation> startJigsawName,
      int maxDepth,
      HeightProvider startHeight,
      Optional<Heightmap.Types> projectStartToHeightmap,
      int maxDistanceFromCenter
  ) {
    super(settings);
    this.startPool = startPool;
    this.startJigsawName = startJigsawName;
    this.maxDepth = maxDepth;
    this.startHeight = startHeight;
    this.projectStartToHeightmap = projectStartToHeightmap;
    this.maxDistanceFromCenter = maxDistanceFromCenter;
  }

  @Override
  public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
    // Check if the chunk position is valid for generation
    if (!checkLocation(context)) {
      return Optional.empty();
    }

    // Use jigsaw placement for structure generation
    return JigsawPlacement.addPieces(
        context,
        this.startPool,
        this.startJigsawName,
        this.maxDepth,
        BlockPos.ZERO,
        false,
        this.projectStartToHeightmap,
        this.maxDistanceFromCenter,
        PoolAliasLookup.EMPTY
    );
  }

  /**
   * Checks if the location is suitable for structure generation.
   */
  private boolean checkLocation(GenerationContext context) {
    ChunkPos chunkPos = context.chunkPos();
    WorldgenRandom random = context.random();

    // Get terrain height at the chunk center
    int centerX = chunkPos.getMiddleBlockX();
    int centerZ = chunkPos.getMiddleBlockZ();

    int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
        centerX, centerZ,
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );

    // Don't generate if too high or too low
    if (surfaceY < 60 || surfaceY > 100) {
      return false;
    }

    // Check for relatively flat terrain
    int corner1 = context.chunkGenerator().getFirstOccupiedHeight(
        centerX - 8, centerZ - 8,
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );
    int corner2 = context.chunkGenerator().getFirstOccupiedHeight(
        centerX + 8, centerZ + 8,
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );

    int heightDiff = Math.abs(corner1 - corner2);
    return heightDiff <= 4; // Relatively flat
  }

  @Override
  public StructureType<?> type() {
    return ModStructures.ABANDONED_LIBRARY.get();
  }
}
