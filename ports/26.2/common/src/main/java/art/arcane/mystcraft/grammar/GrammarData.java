package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.Identifier;

/**
 * Constants for all grammar tokens used in the CFG system. These tokens define
 * the structure of Age generation rules.
 */
public final class GrammarData {

  /**
   * Generates a single Biome
   */
  public static final Identifier BIOME = asMyst("biome");

  /**
   * Generates a number of Biomes (minimum 1)
   */
  public static final Identifier BIOME_LIST = asMyst("biomes");
  /**
   * Generates a Biome Controller
   */
  public static final Identifier BIOMECONTROLLER = asMyst("biome_controller");
  /**
   * Generates a Lighting Controller
   */
  public static final Identifier LIGHTING = asMyst("lighting");

  /**
   * Generates a Weather Controller
   */
  public static final Identifier WEATHER = asMyst("weather");
  /**
   * Generates a Base Terrain Generator
   */
  public static final Identifier TERRAIN = asMyst("terrain_gen");
  /**
   * Generates a Visual Effect like Sky or Fog Color
   */
  public static final Identifier VISUAL_EFFECT = asMyst("visual");

  /**
   * Generates a small world feature
   */
  public static final Identifier FEATURE_SMALL = asMyst("feature_small");

  /**
   * Generates a medium world feature
   */
  public static final Identifier FEATURE_MEDIUM = asMyst("feature_medium");
  /**
   * Generates a large world feature
   */
  public static final Identifier FEATURE_LARGE = asMyst("feature_large");
  /**
   * Generates a world Effect, like Accelerated
   */
  public static final Identifier EFFECT = asMyst("effect");

  /**
   * Generates a Block Modifier which is a valid Terrain Block
   */
  public static final Identifier BLOCK_TERRAIN = asMyst("block_terrain");

  /**
   * Generates a Block Modifier which is a valid Solid Block
   */
  public static final Identifier BLOCK_SOLID = asMyst("block_solid");
  /**
   * Generates a Block Modifier which is a valid Structure Block
   */
  public static final Identifier BLOCK_STRUCTURE = asMyst("block_structure");
  /**
   * Generates a Block Modifier which is a valid Organic Block
   */
  public static final Identifier BLOCK_ORGANIC = asMyst("block_organic");
  /**
   * Generates a Block Modifier which is a valid Crystal Block
   */
  public static final Identifier BLOCK_CRYSTAL = asMyst("block_crystal");
  /**
   * Generates a Block Modifier which is a valid Sea Block
   */
  public static final Identifier BLOCK_SEA = asMyst("block_sea");
  /**
   * Generates a Block Modifier which is a valid Fluid Block
   */
  public static final Identifier BLOCK_FLUID = asMyst("block_fluid");
  /**
   * Generates a Block Modifier which is a valid Gas Block
   */
  public static final Identifier BLOCK_GAS = asMyst("block_gas");
  /**
   * Generates any Block Modifier
   */
  public static final Identifier BLOCK_ANY = asMyst("block_any");
  /**
   * Generates a Sunset modifier about 20% of the time
   */
  public static final Identifier SUNSET_UNCOMMON = asMyst("sunset_uncommon");

  /**
   * Generates a Sunset modifier
   */
  public static final Identifier SUNSET = asMyst("sunset");
  /**
   * Generates an Angle sequence
   */
  public static final Identifier ANGLE_SEQ = asMyst("angle");

  /**
   * Generates a Period sequence
   */
  public static final Identifier PERIOD_SEQ = asMyst("period");
  /**
   * Generates a Phase sequence
   */
  public static final Identifier PHASE_SEQ = asMyst("phase");
  /**
   * Generates a Color sequence
   */
  public static final Identifier COLOR_SEQ = asMyst("color");
  /**
   * Generates a Gradient sequence
   */
  public static final Identifier GRADIENT_SEQ = asMyst("gradient");
  /**
   * Generates a singular Angle value
   */
  public static final Identifier ANGLE_BASIC = asMyst("angle_basic");

  /**
   * Generates a singular Period value
   */
  public static final Identifier PERIOD_BASIC = asMyst("period_basic");
  /**
   * Generates a singular Phase value
   */
  public static final Identifier PHASE_BASIC = asMyst("phase_basic");
  /**
   * Generates a singular Color
   */
  public static final Identifier COLOR_BASIC = asMyst("color_basic");
  /**
   * Generates a singular Gradient
   */
  public static final Identifier GRADIENT_BASIC = asMyst("gradient_basic");

  private GrammarData() {
  }

  private static Identifier asMyst(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }
}
