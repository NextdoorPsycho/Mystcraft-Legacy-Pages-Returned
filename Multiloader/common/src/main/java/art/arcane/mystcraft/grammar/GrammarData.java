package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Constants for all grammar tokens used in the CFG system.
 * These tokens define the structure of Age generation rules.
 */
public final class GrammarData {

  /**
   * Generates a single Biome
   */
  public static final ResourceLocation BIOME = asMyst("biome");

  // Biome tokens
  /**
   * Generates a number of Biomes (minimum 1)
   */
  public static final ResourceLocation BIOME_LIST = asMyst("biomes");
  /**
   * Generates a Biome Controller
   */
  public static final ResourceLocation BIOMECONTROLLER = asMyst("biome_controller");
  /**
   * Generates a Lighting Controller
   */
  public static final ResourceLocation LIGHTING = asMyst("lighting");

  // Core controllers
  /**
   * Generates a Weather Controller
   */
  public static final ResourceLocation WEATHER = asMyst("weather");
  /**
   * Generates a Base Terrain Generator
   */
  public static final ResourceLocation TERRAIN = asMyst("terrain_gen");
  /**
   * Generates a Visual Effect like Sky or Fog Color
   */
  public static final ResourceLocation VISUAL_EFFECT = asMyst("visual");

  // Visual effects
  /**
   * Generates a small world feature
   */
  public static final ResourceLocation FEATURE_SMALL = asMyst("feature_small");

  // Feature tokens
  /**
   * Generates a medium world feature
   */
  public static final ResourceLocation FEATURE_MEDIUM = asMyst("feature_medium");
  /**
   * Generates a large world feature
   */
  public static final ResourceLocation FEATURE_LARGE = asMyst("feature_large");
  /**
   * Generates a world Effect, like Accelerated
   */
  public static final ResourceLocation EFFECT = asMyst("effect");

  // World effects
  /**
   * Generates a Sun
   */
  public static final ResourceLocation SUN = asMyst("sun");

  // Celestial tokens
  /**
   * Generates a Moon
   */
  public static final ResourceLocation MOON = asMyst("moon");
  /**
   * Generates a Starfield
   */
  public static final ResourceLocation STARFIELD = asMyst("starfield");
  /**
   * Generates a Doodad (decorative celestial object)
   */
  public static final ResourceLocation DOODAD = asMyst("doodad");
  /**
   * Generates a Block Modifier which is a valid Terrain Block
   */
  public static final ResourceLocation BLOCK_TERRAIN = asMyst("block_terrain");

  // Block category tokens
  /**
   * Generates a Block Modifier which is a valid Solid Block
   */
  public static final ResourceLocation BLOCK_SOLID = asMyst("block_solid");
  /**
   * Generates a Block Modifier which is a valid Structure Block
   */
  public static final ResourceLocation BLOCK_STRUCTURE = asMyst("block_structure");
  /**
   * Generates a Block Modifier which is a valid Organic Block
   */
  public static final ResourceLocation BLOCK_ORGANIC = asMyst("block_organic");
  /**
   * Generates a Block Modifier which is a valid Crystal Block
   */
  public static final ResourceLocation BLOCK_CRYSTAL = asMyst("block_crystal");
  /**
   * Generates a Block Modifier which is a valid Sea Block
   */
  public static final ResourceLocation BLOCK_SEA = asMyst("block_sea");
  /**
   * Generates a Block Modifier which is a valid Fluid Block
   */
  public static final ResourceLocation BLOCK_FLUID = asMyst("block_fluid");
  /**
   * Generates a Block Modifier which is a valid Gas Block
   */
  public static final ResourceLocation BLOCK_GAS = asMyst("block_gas");
  /**
   * Generates any Block Modifier
   */
  public static final ResourceLocation BLOCK_ANY = asMyst("block_any");
  /**
   * Generates a Sunset modifier about 20% of the time
   */
  public static final ResourceLocation SUNSET_UNCOMMON = asMyst("sunset_uncommon");

  // Sunset modifiers
  /**
   * Generates a Sunset modifier
   */
  public static final ResourceLocation SUNSET = asMyst("sunset");
  /**
   * Generates an Angle sequence
   */
  public static final ResourceLocation ANGLE_SEQ = asMyst("angle");

  // Modifier sequence tokens (for celestials)
  /**
   * Generates a Period sequence
   */
  public static final ResourceLocation PERIOD_SEQ = asMyst("period");
  /**
   * Generates a Phase sequence
   */
  public static final ResourceLocation PHASE_SEQ = asMyst("phase");
  /**
   * Generates a Color sequence
   */
  public static final ResourceLocation COLOR_SEQ = asMyst("color");
  /**
   * Generates a Gradient sequence
   */
  public static final ResourceLocation GRADIENT_SEQ = asMyst("gradient");
  /**
   * Generates a singular Angle value
   */
  public static final ResourceLocation ANGLE_BASIC = asMyst("angle_basic");

  // Basic modifier tokens (singular values)
  /**
   * Generates a singular Period value
   */
  public static final ResourceLocation PERIOD_BASIC = asMyst("period_basic");
  /**
   * Generates a singular Phase value
   */
  public static final ResourceLocation PHASE_BASIC = asMyst("phase_basic");
  /**
   * Generates a singular Color
   */
  public static final ResourceLocation COLOR_BASIC = asMyst("color_basic");
  /**
   * Generates a singular Gradient
   */
  public static final ResourceLocation GRADIENT_BASIC = asMyst("gradient_basic");
  private GrammarData() {
  }

  private static ResourceLocation asMyst(String path) {
    return new ResourceLocation(Mystcraft.MOD_ID, path);
  }
}
