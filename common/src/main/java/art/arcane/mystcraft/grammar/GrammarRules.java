package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Defines all base grammar rules for the Mystcraft CFG system. Rules follow the
 * pattern: Parent -> Child1 Child2 ... [rank]
 */
public final class GrammarRules {

  public static final ResourceLocation ROOT = asMyst("age");
  public static final ResourceLocation FEATURE_LARGE_EXT = asMyst("feature_large_ext");
  public static final ResourceLocation FEATURE_MEDIUM_EXT = asMyst("feature_medium_ext");
  public static final ResourceLocation FEATURE_SMALL_EXT = asMyst("feature_small_ext");
  public static final ResourceLocation ANGLE_EXT = asMyst("angle_ext");
  public static final ResourceLocation PERIOD_EXT = asMyst("period_ext");
  public static final ResourceLocation PHASE_EXT = asMyst("phase_ext");
  public static final ResourceLocation COLOR_EXT = asMyst("color_ext");
  public static final ResourceLocation GRADIENT_EXT = asMyst("gradient_ext");
  public static final ResourceLocation SUNSET_EXT = asMyst("sunset_ext");

  public static final ResourceLocation BLOCK_NONSOLID = asMyst("block_nonsolid");
  private static final Logger LOGGER = LoggerFactory.getLogger(GrammarRules.class);

  private static final ResourceLocation BIOME_GEN = asMyst("biomes_adv");
  private static final ResourceLocation BIOME_EXT = asMyst("biomes_ext");
  private static final ResourceLocation VISUAL_EFFECT_GEN = asMyst("visuals_adv");
  private static final ResourceLocation VISUAL_EFFECT_EXT = asMyst("visuals_ext");
  private static final ResourceLocation FEATURE_LARGE_GEN = asMyst("feature_large_adv");
  private static final ResourceLocation FEATURE_MEDIUM_GEN = asMyst("feature_medium_adv");
  private static final ResourceLocation FEATURE_SMALL_GEN = asMyst("feature_small_adv");
  private static final ResourceLocation EFFECT_GEN = asMyst("effects_adv");
  private static final ResourceLocation EFFECT_EXT = asMyst("effects_ext");

  private static final ResourceLocation ANGLE_GEN = asMyst("angle_adv");
  private static final ResourceLocation PERIOD_GEN = asMyst("period_adv");
  private static final ResourceLocation PHASE_GEN = asMyst("phase_adv");
  private static final ResourceLocation COLOR_GEN = asMyst("color_adv");
  private static final ResourceLocation GRADIENT_GEN = asMyst("gradient_adv");

  private GrammarRules() {
  }

  /**
   * Initializes all grammar rules. Call this during mod initialization.
   */
  public static void initialize() {
    LOGGER.info("Initializing Mystcraft grammar rules");
    registerBaseRules();
    CFGGrammarGenerator.buildGrammar();
    LOGGER.info("Grammar rules initialized and built");
  }

  /**
   * Registers base grammar rules without finalizing the grammar. Use this for
   * datapack reloads where additional rules are added later.
   */
  public static void registerBaseRules() {
    LOGGER.info("Registering Mystcraft base grammar rules");

    registerRule(buildRule(0, ROOT,
        GrammarData.TERRAIN,
        GrammarData.BIOMECONTROLLER,
        GrammarData.BIOME_LIST,
        GrammarData.WEATHER,
        GrammarData.LIGHTING,
        GrammarData.BLOCK_SEA,
        asMyst("spawning0"),
        asMyst("visuals0"),
        asMyst("feature_smalls0"),
        asMyst("feature_mediums0"),
        asMyst("feature_larges0"),
        asMyst("effects0")
    ));

    registerRule(buildRule(10, asMyst("spawning0")));

    registerRule(buildRule(1, GrammarData.BIOME_LIST, BIOME_GEN));
    registerRule(buildRule(2, BIOME_GEN, BIOME_GEN, GrammarData.BIOME));
    registerRule(buildRule(3, BIOME_GEN, GrammarData.BIOME));
    registerRule(buildRule(null, GrammarData.BIOME_LIST, BIOME_EXT, GrammarData.BIOME));
    registerRule(buildRule(null, BIOME_EXT, BIOME_EXT, GrammarData.BIOME_LIST));
    registerRule(buildRule(1, BIOME_EXT));

    registerRule(buildRule(1, asMyst("visuals0"), VISUAL_EFFECT_GEN));
    registerRule(buildRule(3, VISUAL_EFFECT_GEN, VISUAL_EFFECT_GEN, GrammarData.VISUAL_EFFECT));
    registerRule(buildRule(2, VISUAL_EFFECT_GEN, GrammarData.VISUAL_EFFECT));
    registerRule(buildRule(null, asMyst("visuals0"), VISUAL_EFFECT_EXT, GrammarData.VISUAL_EFFECT));
    registerRule(buildRule(null, VISUAL_EFFECT_EXT, VISUAL_EFFECT_EXT, GrammarData.VISUAL_EFFECT));
    registerRule(buildRule(1, VISUAL_EFFECT_EXT));
    registerRule(buildRule(1, GrammarData.VISUAL_EFFECT));

    registerRule(buildRule(1, asMyst("feature_larges0"), FEATURE_LARGE_GEN));
    registerRule(buildRule(2, FEATURE_LARGE_GEN, FEATURE_LARGE_GEN, GrammarData.FEATURE_LARGE));
    registerRule(buildRule(2, FEATURE_LARGE_GEN, GrammarData.FEATURE_LARGE));
    registerRule(buildRule(null, asMyst("feature_larges0"), FEATURE_LARGE_EXT, GrammarData.FEATURE_LARGE));
    registerRule(buildRule(null, FEATURE_LARGE_EXT, FEATURE_LARGE_EXT, GrammarData.FEATURE_LARGE));
    registerRule(buildRule(1, FEATURE_LARGE_EXT));
    registerRule(buildRule(2, GrammarData.FEATURE_LARGE));

    registerRule(buildRule(1, asMyst("feature_mediums0"), FEATURE_MEDIUM_GEN));
    registerRule(buildRule(2, FEATURE_MEDIUM_GEN, FEATURE_MEDIUM_GEN, GrammarData.FEATURE_MEDIUM));
    registerRule(buildRule(3, FEATURE_MEDIUM_GEN, GrammarData.FEATURE_MEDIUM));
    registerRule(buildRule(null, asMyst("feature_mediums0"), FEATURE_MEDIUM_EXT, GrammarData.FEATURE_MEDIUM));
    registerRule(buildRule(null, FEATURE_MEDIUM_EXT, FEATURE_MEDIUM_EXT, GrammarData.FEATURE_MEDIUM));
    registerRule(buildRule(1, FEATURE_MEDIUM_EXT));
    registerRule(buildRule(2, GrammarData.FEATURE_MEDIUM));

    registerRule(buildRule(1, asMyst("feature_smalls0"), FEATURE_SMALL_GEN));
    registerRule(buildRule(2, FEATURE_SMALL_GEN, FEATURE_SMALL_GEN, GrammarData.FEATURE_SMALL));
    registerRule(buildRule(4, FEATURE_SMALL_GEN, GrammarData.FEATURE_SMALL));
    registerRule(buildRule(null, asMyst("feature_smalls0"), FEATURE_SMALL_EXT, GrammarData.FEATURE_SMALL));
    registerRule(buildRule(null, FEATURE_SMALL_EXT, FEATURE_SMALL_EXT, GrammarData.FEATURE_SMALL));
    registerRule(buildRule(1, FEATURE_SMALL_EXT));
    registerRule(buildRule(2, GrammarData.FEATURE_SMALL));

    registerRule(buildRule(1, asMyst("effects0"), EFFECT_GEN));
    registerRule(buildRule(3, EFFECT_GEN, EFFECT_GEN, GrammarData.EFFECT));
    registerRule(buildRule(2, EFFECT_GEN, GrammarData.EFFECT));
    registerRule(buildRule(null, asMyst("effects0"), EFFECT_EXT, GrammarData.EFFECT));
    registerRule(buildRule(null, EFFECT_EXT, EFFECT_EXT, GrammarData.EFFECT));
    registerRule(buildRule(1, EFFECT_EXT));
    registerRule(buildRule(1, GrammarData.EFFECT));

    registerRule(buildRule(2, GrammarData.SUNSET_UNCOMMON));
    registerRule(buildRule(3, GrammarData.SUNSET_UNCOMMON, GrammarData.SUNSET));
    registerRule(buildRule(1, GrammarData.SUNSET));
    registerRule(buildRule(null, SUNSET_EXT, GrammarData.SUNSET));
    registerRule(buildRule(1, SUNSET_EXT));

    registerRule(buildRule(1, GrammarData.ANGLE_SEQ, ANGLE_GEN));
    registerRule(buildRule(2, ANGLE_GEN, ANGLE_GEN, GrammarData.ANGLE_BASIC));
    registerRule(buildRule(3, ANGLE_GEN, GrammarData.ANGLE_BASIC));
    registerRule(buildRule(null, GrammarData.ANGLE_SEQ, ANGLE_EXT, GrammarData.ANGLE_BASIC));
    registerRule(buildRule(null, ANGLE_EXT, GrammarData.ANGLE_SEQ));
    registerRule(buildRule(1, ANGLE_EXT));

    registerRule(buildRule(1, GrammarData.PERIOD_SEQ, PERIOD_GEN));
    registerRule(buildRule(2, PERIOD_GEN, PERIOD_GEN, GrammarData.PERIOD_BASIC));
    registerRule(buildRule(3, PERIOD_GEN, GrammarData.PERIOD_BASIC));
    registerRule(buildRule(null, GrammarData.PERIOD_SEQ, PERIOD_EXT, GrammarData.PERIOD_BASIC));
    registerRule(buildRule(null, PERIOD_EXT, GrammarData.PERIOD_SEQ));
    registerRule(buildRule(1, PERIOD_EXT));

    registerRule(buildRule(1, GrammarData.PHASE_SEQ, PHASE_GEN));
    registerRule(buildRule(2, PHASE_GEN, PHASE_GEN, GrammarData.PHASE_BASIC));
    registerRule(buildRule(3, PHASE_GEN, GrammarData.PHASE_BASIC));
    registerRule(buildRule(null, GrammarData.PHASE_SEQ, PHASE_EXT, GrammarData.PHASE_BASIC));
    registerRule(buildRule(null, PHASE_EXT, GrammarData.PHASE_SEQ));
    registerRule(buildRule(1, PHASE_EXT));

    registerRule(buildRule(1, GrammarData.COLOR_SEQ, COLOR_GEN));
    registerRule(buildRule(2, COLOR_GEN, COLOR_GEN, GrammarData.COLOR_BASIC));
    registerRule(buildRule(3, COLOR_GEN, GrammarData.COLOR_BASIC));
    registerRule(buildRule(null, GrammarData.COLOR_SEQ, COLOR_EXT, GrammarData.COLOR_BASIC));
    registerRule(buildRule(null, COLOR_EXT, GrammarData.COLOR_SEQ));
    registerRule(buildRule(1, COLOR_EXT));

    registerRule(buildRule(1, GrammarData.GRADIENT_SEQ, GRADIENT_GEN));
    registerRule(buildRule(2, GRADIENT_GEN, GRADIENT_GEN, GrammarData.GRADIENT_BASIC));
    registerRule(buildRule(2, GRADIENT_GEN, GrammarData.GRADIENT_BASIC));
    registerRule(buildRule(null, GrammarData.GRADIENT_SEQ, GRADIENT_EXT, GrammarData.GRADIENT_BASIC));
    registerRule(buildRule(null, GRADIENT_EXT, GrammarData.GRADIENT_SEQ));
    registerRule(buildRule(1, GRADIENT_EXT));

    registerRule(buildRule(0, GrammarData.BLOCK_TERRAIN));
    registerRule(buildRule(0, GrammarData.BLOCK_SOLID));
    registerRule(buildRule(0, GrammarData.BLOCK_STRUCTURE));
    registerRule(buildRule(0, GrammarData.BLOCK_ORGANIC));
    registerRule(buildRule(0, GrammarData.BLOCK_CRYSTAL));
    registerRule(buildRule(0, GrammarData.BLOCK_SEA));
    registerRule(buildRule(0, GrammarData.BLOCK_FLUID));
    registerRule(buildRule(0, GrammarData.BLOCK_GAS));
    registerRule(buildRule(0, GrammarData.BLOCK_ANY));

    registerRule(buildRule(1, BLOCK_NONSOLID, GrammarData.BLOCK_FLUID));
    registerRule(buildRule(2, BLOCK_NONSOLID, GrammarData.BLOCK_GAS));

  }

  private static void registerRule(CFGRule rule) {
    CFGGrammarGenerator.registerRule(rule);
  }

  private static ResourceLocation asMyst(String path) {
    return new ResourceLocation(Mystcraft.MOD_ID, path);
  }

  private static CFGRule buildRule(Integer rank, ResourceLocation parent, ResourceLocation... children) {
    List<ResourceLocation> values = Arrays.asList(children);
    return new CFGRule(parent, values, rank);
  }
}
