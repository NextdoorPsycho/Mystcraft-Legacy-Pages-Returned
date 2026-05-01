package art.arcane.mystcraft.world.gen.terrain;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.world.gen.terrain.script.ScriptExpression;
import art.arcane.mystcraft.world.gen.terrain.script.ScriptExpressionParser;
import art.arcane.mystcraft.world.gen.terrain.script.ScriptRuntime;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Terrain generator backed by a datapack-defined script expression.
 */
public class ScriptedTerrainGenerator extends TerrainGeneratorBase {

  private static final int WORLD_MIN_Y = -64;

  private final ScriptExpression densityExpression;
  private final ScriptRuntime runtime;
  private final double xScale;
  private final double yScale;
  private final double zScale;
  private final String name;

  public ScriptedTerrainGenerator(AgeDirector controller, long seed, ScriptExpression densityExpression,
                                  double xScale, double yScale, double zScale, long seedOffset, String name) {
    super(controller, seed);
    this.densityExpression = densityExpression;
    this.xScale = xScale;
    this.yScale = yScale;
    this.zScale = zScale;
    this.name = name != null ? name : "";
    this.runtime = new ScriptRuntime(seed ^ seedOffset);
  }

  public static ScriptedTerrainGenerator fromJson(AgeDirector director, long seed, JsonObject json) {
    JsonObject params = json == null ? new JsonObject() : json;
    if (!params.has("density")) {
      throw new IllegalArgumentException("Scripted terrain generator requires a 'density' expression");
    }
    ScriptExpression density = ScriptExpressionParser.parse(params.get("density"));
    double xScale = GsonHelper.getAsDouble(params, "x_scale", 1.0D);
    double yScale = GsonHelper.getAsDouble(params, "y_scale", 1.0D);
    double zScale = GsonHelper.getAsDouble(params, "z_scale", 1.0D);
    long seedOffset = GsonHelper.getAsLong(params, "seed_offset", 0L);
    String name = GsonHelper.getAsString(params, "name", "");
    ScriptedTerrainGenerator generator =
        new ScriptedTerrainGenerator(director, seed, density, xScale, yScale, zScale, seedOffset, name);
    if (params.has("params") && params.get("params").isJsonObject()) {
      JsonObject values = params.getAsJsonObject("params");
      for (var entry : values.entrySet()) {
        if (entry.getValue().isJsonPrimitive()) {
          try {
            generator.runtime.setParam(entry.getKey(), entry.getValue().getAsDouble());
          } catch (Exception ignored) {
          }
        }
      }
    }
    return generator;
  }

  @Override
  protected double[] initializeNoiseField(double[] field, int x, int y, int z,
                                          int xSize, int ySize, int zSize) {
    int total = xSize * ySize * zSize;
    if (field == null || field.length != total) {
      field = new double[total];
    }

    int index = 0;
    int baseGridX = x;
    int baseGridY = y;
    int baseGridZ = z;

    for (int zz = 0; zz < zSize; zz++) {
      int gridZ = baseGridZ + zz;
      double worldZ = gridZ * XZ_STEP;
      for (int xx = 0; xx < xSize; xx++) {
        int gridX = baseGridX + xx;
        double worldX = gridX * XZ_STEP;
        for (int yy = 0; yy < ySize; yy++) {
          int gridY = baseGridY + yy;
          double worldY = WORLD_MIN_Y + gridY * Y_STEP;
          double density = densityExpression.eval(runtime, worldX * xScale, worldY * yScale, worldZ * zScale);
          field[index++] = density;
        }
      }
    }

    return field;
  }

  @Override
  public void generateTerrain(int chunkX, int chunkZ, net.minecraft.world.level.chunk.ChunkAccess chunk,
                              RandomSource random) {

    BlockState terrain = controller.getTerrainBlock();
    BlockState sea = controller.getSeaBlock();
    if (terrain != null) {
      this.terrainBlock = terrain;
    }
    if (sea != null) {
      this.seaBlock = sea;
    }
    super.generateTerrain(chunkX, chunkZ, chunk, random);
  }

  @Override
  public String getType() {
    return name == null || name.isBlank() ? "scripted" : "scripted:" + name;
  }
}
