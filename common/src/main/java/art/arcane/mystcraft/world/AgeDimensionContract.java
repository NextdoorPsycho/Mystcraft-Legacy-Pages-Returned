package art.arcane.mystcraft.world;

import net.minecraft.world.level.dimension.DimensionType;

/**
 * Immutable vertical contract shared by an Age's dimension type and chunk
 * generator. A mismatch is rejected before a level can write partial chunks.
 */
public record AgeDimensionContract(int minY, int height, int logicalHeight) {

  public AgeDimensionContract {
    if (height <= 0 || height % 16 != 0) {
      throw new IllegalArgumentException("Dimension height must be a positive multiple of 16");
    }
    if (minY % 16 != 0) {
      throw new IllegalArgumentException("Dimension minimum Y must be a multiple of 16");
    }
    if (logicalHeight < 0 || logicalHeight > height) {
      throw new IllegalArgumentException("Logical height must be within the dimension height");
    }
    Math.addExact(minY, height);
  }

  public static AgeDimensionContract from(DimensionType dimensionType) {
    return new AgeDimensionContract(
        dimensionType.minY(),
        dimensionType.height(),
        dimensionType.logicalHeight()
    );
  }

  public void requireGeneratorCompatible(String ageDescription, int generatorMinY, int generatorHeight) {
    if (minY == generatorMinY && height == generatorHeight) {
      return;
    }
    throw new IllegalStateException(
        ageDescription + " requires dimension range " + minY + ".." + Math.addExact(minY, height)
            + ", but its generator reports " + generatorMinY + ".."
            + Math.addExact(generatorMinY, generatorHeight)
            + ". Age creation was refused before any chunk writes."
    );
  }
}
