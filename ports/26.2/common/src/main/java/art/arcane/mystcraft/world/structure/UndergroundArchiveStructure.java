package art.arcane.mystcraft.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.Optional;

/**
 * Underground Archive structure that spawns deep underground. Contains rare
 * symbols, ancient D'ni artifacts, and instability effects.
 */
public class UndergroundArchiveStructure extends Structure {

  public static final Codec<UndergroundArchiveStructure> CODEC = RecordCodecBuilder.<UndergroundArchiveStructure>mapCodec(instance ->
      instance.group(
          settingsCodec(instance),
          StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
          Identifier.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
          Codec.intRange(0, 7).fieldOf("size").forGetter(structure -> structure.maxDepth),
          HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
          Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
      ).apply(instance, UndergroundArchiveStructure::new)
  ).codec();

  private final Holder<StructureTemplatePool> startPool;
  private final Optional<Identifier> startJigsawName;
  private final int maxDepth;
  private final HeightProvider startHeight;
  private final int maxDistanceFromCenter;

  public UndergroundArchiveStructure(
      StructureSettings settings,
      Holder<StructureTemplatePool> startPool,
      Optional<Identifier> startJigsawName,
      int maxDepth,
      HeightProvider startHeight,
      int maxDistanceFromCenter
  ) {
    super(settings);
    this.startPool = startPool;
    this.startJigsawName = startJigsawName;
    this.maxDepth = maxDepth;
    this.startHeight = startHeight;
    this.maxDistanceFromCenter = maxDistanceFromCenter;
  }

  @Override
  public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {

    if (!checkUndergroundLocation(context)) {
      return Optional.empty();
    }

    ChunkPos chunkPos = context.chunkPos();
    WorldGenerationContext worldGenContext = new WorldGenerationContext(
        context.chunkGenerator(), context.heightAccessor());
    int y = this.startHeight.sample(context.random(), worldGenContext);

    BlockPos blockPos = new BlockPos(
        chunkPos.getMiddleBlockX(),
        y,
        chunkPos.getMiddleBlockZ()
    );

    return JigsawPlacement.addPieces(
        context,
        this.startPool,
        this.startJigsawName,
        this.maxDepth,
        blockPos,
        false,
        Optional.empty(),
        new net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.MaxDistance(this.maxDistanceFromCenter),
        net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup.EMPTY,
        net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.DEFAULT_DIMENSION_PADDING,
        net.minecraft.world.level.levelgen.structure.structures.JigsawStructure.DEFAULT_LIQUID_SETTINGS
    );
  }

  private boolean checkUndergroundLocation(GenerationContext context) {
    ChunkPos chunkPos = context.chunkPos();

    int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
        chunkPos.getMiddleBlockX(),
        chunkPos.getMiddleBlockZ(),
        Heightmap.Types.WORLD_SURFACE_WG,
        context.heightAccessor(),
        context.randomState()
    );

    return surfaceY > 30;
  }

  @Override
  public StructureType<?> type() {
    return ModStructures.UNDERGROUND_ARCHIVE.get();
  }
}
