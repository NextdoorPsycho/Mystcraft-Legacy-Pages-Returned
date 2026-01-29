package art.arcane.mystcraft.world.gen.terrain.script;

import net.minecraft.util.RandomSource;

/**
 * Multi-octave Perlin sampler.
 */
public class PerlinOctaves {

  private final PerlinNoise[] octaves;

  public PerlinOctaves(RandomSource random, int octaveCount) {
    int count = Math.max(1, octaveCount);
    this.octaves = new PerlinNoise[count];
    for (int i = 0; i < count; i++) {
      this.octaves[i] = new PerlinNoise(random);
    }
  }

  public double sample2D(double x, double z, double lacunarity, double gain) {
    double sum = 0.0D;
    double amp = 1.0D;
    double freq = 1.0D;
    for (PerlinNoise octave : octaves) {
      sum += octave.noise(x * freq, 0.0D, z * freq) * amp;
      freq *= lacunarity;
      amp *= gain;
    }
    return sum;
  }

  public double sample3D(double x, double y, double z, double lacunarity, double gain) {
    double sum = 0.0D;
    double amp = 1.0D;
    double freq = 1.0D;
    for (PerlinNoise octave : octaves) {
      sum += octave.noise(x * freq, y * freq, z * freq) * amp;
      freq *= lacunarity;
      amp *= gain;
    }
    return sum;
  }
}
