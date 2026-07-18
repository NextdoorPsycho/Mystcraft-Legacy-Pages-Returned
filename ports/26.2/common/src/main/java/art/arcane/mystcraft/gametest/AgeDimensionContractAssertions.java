package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.MystcraftConstants;
import art.arcane.mystcraft.world.AgeDimensionContract;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;

/** Focused vertical-contract checks for normal Ages and personal pockets. */
final class AgeDimensionContractAssertions {
  private static final ResourceKey<DimensionType> NORMAL_TYPE = ResourceKey.create(
      Registries.DIMENSION_TYPE,
      MystcraftConstants.loc("age_normal")
  );
  private static final ResourceKey<DimensionType> PERSONAL_TYPE = ResourceKey.create(
      Registries.DIMENSION_TYPE,
      MystcraftConstants.loc("age_personal")
  );

  private AgeDimensionContractAssertions() {
  }

  static void assertVerticalContracts(GameTestHelper helper) {
    Registry<DimensionType> dimensionTypes = helper.getLevel().registryAccess()
        .lookupOrThrow(Registries.DIMENSION_TYPE);
    AgeDimensionContract normalContract = AgeDimensionContract.from(
        dimensionTypes.getValueOrThrow(NORMAL_TYPE)
    );
    AgeDimensionContract personalContract = AgeDimensionContract.from(
        dimensionTypes.getValueOrThrow(PERSONAL_TYPE)
    );

    assertRange(helper, "normal", normalContract, -64, 384, 384);
    assertRange(helper, "personal", personalContract, -2032, 4064, 4064);

    Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
    Holder<Biome> plains = biomes.getOrThrow(Biomes.PLAINS);
    AgeChunkGenerator normalGenerator = generator(new FixedBiomeSource(plains), "flat");
    AgeChunkGenerator personalGenerator = generator(new FixedBiomeSource(plains), "personal");

    normalContract.requireGeneratorCompatible(
        "normal GameTest",
        normalGenerator.getMinY(),
        normalGenerator.getGenDepth()
    );
    personalContract.requireGeneratorCompatible(
        "personal GameTest",
        personalGenerator.getMinY(),
        personalGenerator.getGenDepth()
    );

    assertMismatchRejected(
        helper,
        "normal dimension with personal generator",
        normalContract,
        personalGenerator
    );
    assertMismatchRejected(
        helper,
        "personal dimension with normal generator",
        personalContract,
        normalGenerator
    );
  }

  private static AgeChunkGenerator generator(FixedBiomeSource biomes, String terrainType) {
    return new AgeChunkGenerator(
        biomes,
        terrainType,
        64,
        63,
        true,
        26L,
        -1,
        "none",
        "none",
        false,
        0,
        0
    );
  }

  private static void assertRange(
      GameTestHelper helper,
      String label,
      AgeDimensionContract contract,
      int expectedMinY,
      int expectedHeight,
      int expectedLogicalHeight
  ) {
    if (contract.minY() != expectedMinY
        || contract.height() != expectedHeight
        || contract.logicalHeight() != expectedLogicalHeight) {
      helper.fail(
          label + " dimension contract was " + contract
              + "; expected minY=" + expectedMinY
              + ", height=" + expectedHeight
              + ", logicalHeight=" + expectedLogicalHeight
      );
    }
  }

  private static void assertMismatchRejected(
      GameTestHelper helper,
      String label,
      AgeDimensionContract contract,
      AgeChunkGenerator generator
  ) {
    try {
      contract.requireGeneratorCompatible(label, generator.getMinY(), generator.getGenDepth());
      helper.fail("Vertical contract accepted mismatched " + label);
    } catch (IllegalStateException expected) {
      if (!expected.getMessage().contains("refused before any chunk writes")) {
        helper.fail("Vertical contract mismatch omitted the fail-closed reason: " + expected.getMessage());
      }
    }
  }
}
