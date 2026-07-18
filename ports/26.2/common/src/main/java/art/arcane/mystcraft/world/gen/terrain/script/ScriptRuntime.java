package art.arcane.mystcraft.world.gen.terrain.script;

import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime context for scripted terrain expressions.
 */
public class ScriptRuntime {

  private final long seed;
  private final Map<NoiseKey, PerlinOctaves> noiseCache = new HashMap<>();
  private final Map<String, Double> params = new HashMap<>();

  public ScriptRuntime(long seed) {
    this.seed = seed;
  }

  public long getSeed() {
    return seed;
  }

  public void setParam(String name, double value) {
    if (name != null) {
      params.put(name, value);
    }
  }

  public double getParam(String name, double fallback) {
    if (name == null) return fallback;
    return params.getOrDefault(name, fallback);
  }

  public double noise2(double x, double z, int octaves, double scale, double lacunarity, double gain, long seedOffset) {
    PerlinOctaves oct = getNoise(octaves, seedOffset);
    return oct.sample2D(x * scale, z * scale, lacunarity, gain);
  }

  public double noise3(double x, double y, double z, int octaves, double scale, double lacunarity, double gain, long seedOffset) {
    PerlinOctaves oct = getNoise(octaves, seedOffset);
    return oct.sample3D(x * scale, y * scale, z * scale, lacunarity, gain);
  }

  private PerlinOctaves getNoise(int octaves, long seedOffset) {
    NoiseKey key = new NoiseKey(octaves, seedOffset);
    PerlinOctaves existing = noiseCache.get(key);
    if (existing != null) {
      return existing;
    }
    RandomSource random = RandomSource.create(seed ^ seedOffset);
    PerlinOctaves created = new PerlinOctaves(random, Math.max(1, octaves));
    noiseCache.put(key, created);
    return created;
  }

  private record NoiseKey(int octaves, long seedOffset) {
  }
}
