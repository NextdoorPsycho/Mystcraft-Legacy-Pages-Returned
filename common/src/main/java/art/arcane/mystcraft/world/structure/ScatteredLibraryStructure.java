package art.arcane.mystcraft.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

/**
 * Scattered Library structure - small standalone library buildings in the
 * overworld. More common and smaller than the Abandoned Library.
 */
public class ScatteredLibraryStructure extends Structure {

  public static final Codec<ScatteredLibraryStructure> CODEC = RecordCodecBuilder.<ScatteredLibraryStructure>mapCodec(instance ->
      instance.group(
          settingsCodec(instance),
          StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
          ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
          Codec.intRange(0, 7).fieldOf("size").forGetter(structure -> structure.maxDepth),
          HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
          Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
          Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
      ).apply(instance, ScatteredLibraryStructure::new)
  ).codec();

  private final Holder<StructureTemplatePool> startPool;
  private final Optional<ResourceLocation> startJigsawName;
  private final int maxDepth;
  private final HeightProvider startHeight;
  private final Optional<Heightmap.Types> projectStartToHeightmap;
  private final int maxDistanceFromCenter;

  public ScatteredLibraryStructure(
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
    if (!checkLocation(context)) {
      return Optional.empty();
    }

    return JigsawPlacement.addPieces(
        context,
        this.startPool,
        this.startJigsawName,
        this.maxDepth,
        BlockPos.ZERO,
        false,
        this.projectStartToHeightmap,
        this.maxDistanceFromCenter
    );
  }

  private boolean checkLocation(GenerationContext context) {
    ChunkPos chunkPos = context.chunkPos();
    int centerX = chunkPos.getMiddleBlockX();
    int centerZ = chunkPos.getMiddleBlockZ();

    int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
        centerX, centerZ,
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );

    if (surfaceY < 55 || surfaceY > 120) {
      return false;
    }

    int corner1 = context.chunkGenerator().getFirstOccupiedHeight(
        centerX - 4, centerZ - 4,
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );
    int corner2 = context.chunkGenerator().getFirstOccupiedHeight(
        centerX + 4, centerZ + 4,
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );

    int heightDiff = Math.abs(corner1 - corner2);
    return heightDiff <= 6;
  }

  @Override
  public StructureType<?> type() {
    return ModStructures.SCATTERED_LIBRARY.get();
  }
}
