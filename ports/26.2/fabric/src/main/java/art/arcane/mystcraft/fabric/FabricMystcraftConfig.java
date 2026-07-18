package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.config.MystcraftConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/**
 * Small dependency-free reader for the existing {@code mystcraft-common.toml}
 * file. It intentionally understands only Mystcraft's scalar and string-list
 * settings, which keeps the loader integration deterministic and preserves the
 * configuration path used by the 1.20.1 Fabric build.
 */
public final class FabricMystcraftConfig {

  private static final Path PATH = FabricLoader.getInstance().getConfigDir()
      .resolve("mystcraft-common.toml");
  private static final Map<String, Object> VALUES = new LinkedHashMap<>();
  private static final Map<String, NumericRange> NUMERIC_RANGES = Map.ofEntries(
      Map.entry("general.maxSymbolsPerBook", new NumericRange(-1.0D, 1000.0D)),
      Map.entry("general.microDimensionRadiusChunks", new NumericRange(0.0D, 2048.0D)),
      Map.entry("general.microDimensionExtraChunks", new NumericRange(0.0D, 16.0D)),
      Map.entry("personal_pocket.innerHalfSizeXZ", new NumericRange(2.0D, 4096.0D)),
      Map.entry("personal_pocket.innerHalfSizeY", new NumericRange(2.0D, 4096.0D)),
      Map.entry("personal_pocket.innerThickness", new NumericRange(1.0D, 32.0D)),
      Map.entry("personal_pocket.outerThickness", new NumericRange(1.0D, 32.0D)),
      Map.entry("personal_pocket.centerY", new NumericRange(-2032.0D, 2032.0D)),
      Map.entry("instability.effectMultiplier", new NumericRange(0.0D, 10.0D)),
      Map.entry("instability.maxAllowedInstability", new NumericRange(0.0D, 1000.0D)),
      Map.entry("instability_thresholds.decay", new NumericRange(0.0D, 500.0D)),
      Map.entry("instability_thresholds.transmute", new NumericRange(0.0D, 500.0D)),
      Map.entry("instability_thresholds.lightning", new NumericRange(0.0D, 500.0D)),
      Map.entry("instability_thresholds.meteor", new NumericRange(0.0D, 500.0D)),
      Map.entry("instability_thresholds.poison", new NumericRange(0.0D, 500.0D)),
      Map.entry("instability_thresholds.wither", new NumericRange(0.0D, 500.0D)),
      Map.entry("instability_chances.decay", new NumericRange(0.0D, 1.0D)),
      Map.entry("instability_chances.transmute", new NumericRange(0.0D, 1.0D)),
      Map.entry("instability_chances.lightning", new NumericRange(0.0D, 1.0D)),
      Map.entry("instability_chances.meteor", new NumericRange(0.0D, 1.0D)),
      Map.entry("instability_chances.playerEffect", new NumericRange(0.0D, 1.0D)));
  private static final Set<String> IDENTIFIER_LISTS = Set.of(
      "general.bookBinderCoverItems",
      "general.disabledSymbols",
      "personal_pocket.innerBlockPalette");
  private static final Set<String> IDENTIFIERS = Set.of("personal_pocket.outerBlock");

  private static final List<String> DEFAULT_COVERS = List.of(
      "minecraft:leather",
      "mystcraft:folder");
  private static final List<String> DEFAULT_DISABLED_SYMBOLS = List.of(
      "mystcraft:block_minecraft_coal_block",
      "mystcraft:block_minecraft_copper_block",
      "mystcraft:block_minecraft_diamond_block",
      "mystcraft:block_minecraft_emerald_block",
      "mystcraft:block_minecraft_gold_block",
      "mystcraft:block_minecraft_iron_block",
      "mystcraft:block_minecraft_lapis_block",
      "mystcraft:block_minecraft_netherite_block",
      "mystcraft:block_minecraft_raw_copper_block",
      "mystcraft:block_minecraft_raw_gold_block",
      "mystcraft:block_minecraft_raw_iron_block",
      "mystcraft:block_minecraft_redstone_block",
      "mystcraft:block_minecraft_ancient_debris");
  private static final List<String> DEFAULT_POCKET_PALETTE = List.of(
      "minecraft:oak_planks",
      "minecraft:spruce_planks",
      "minecraft:birch_planks",
      "minecraft:jungle_planks",
      "minecraft:acacia_planks",
      "minecraft:dark_oak_planks",
      "minecraft:mangrove_planks",
      "minecraft:cherry_planks");

  private FabricMystcraftConfig() {
  }

  public static synchronized void load() {
    installDefaults();
    if (!Files.isRegularFile(PATH)) {
      writeDefaultFile();
      return;
    }

    try {
      parse(Files.readAllLines(PATH, StandardCharsets.UTF_8));
      Mystcraft.LOGGER.info("[Mystcraft] Loaded Fabric configuration from {}", PATH);
    } catch (IOException | RuntimeException exception) {
      Mystcraft.LOGGER.error(
          "[Mystcraft] Could not read {}; using safe defaults for this launch",
          PATH,
          exception);
      installDefaults();
    }
  }

  public static void bindCommonConfig() {
    MystcraftConfig.giveGuidebookOnFirstSpawn = bool("general.giveGuidebookOnFirstSpawn");
    MystcraftConfig.maxSymbolsPerBook = integer("general.maxSymbolsPerBook");
    MystcraftConfig.deleteAgesOnStartup = bool("general.deleteAgesOnStartup");
    MystcraftConfig.enablePersonalLinkBooks = bool("general.enablePersonalLinkBooks");
    MystcraftConfig.linkPanelInBoosterPacks = bool("general.linkPanelInBoosterPacks");
    MystcraftConfig.enableBoosterLoot = bool("general.enableBoosterLoot");
    MystcraftConfig.enablePageLoot = bool("general.enablePageLoot");
    MystcraftConfig.bookBinderCoverItems = strings("general.bookBinderCoverItems");
    MystcraftConfig.allowGravityBlocksInAges = bool("general.allowGravityBlocksInAges");
    MystcraftConfig.microDimensionsEnabled = bool("general.microDimensionsEnabled");
    MystcraftConfig.microDimensionRadiusChunks = integer("general.microDimensionRadiusChunks");
    MystcraftConfig.microDimensionExtraChunks = integer("general.microDimensionExtraChunks");
    MystcraftConfig.safeStories = bool("general.safeStories");
    MystcraftConfig.droppedBooksBecomeLivingEntities =
        bool("general.droppedBooksBecomeLivingEntities");
    MystcraftConfig.dropBooksOnRead = bool("general.dropBooksOnRead");
    MystcraftConfig.disabledSymbols = strings("general.disabledSymbols");
    MystcraftConfig.proceduralUiEnabled = bool("general.proceduralUiEnabled");
    MystcraftConfig.proceduralBookCoversEnabled =
        bool("general.proceduralBookCoversEnabled");
    MystcraftConfig.proceduralSymbolPagesEnabled =
        bool("general.proceduralSymbolPagesEnabled");
    MystcraftConfig.proceduralSymbolGlyphsEnabled =
        bool("general.proceduralSymbolGlyphsEnabled");

    MystcraftConfig.pocketInnerHalfSizeXZ = integer("personal_pocket.innerHalfSizeXZ");
    MystcraftConfig.pocketInnerHalfSizeY = integer("personal_pocket.innerHalfSizeY");
    MystcraftConfig.pocketInnerThickness = integer("personal_pocket.innerThickness");
    MystcraftConfig.pocketOuterThickness = integer("personal_pocket.outerThickness");
    MystcraftConfig.pocketCenterY = integer("personal_pocket.centerY");
    MystcraftConfig.pocketInnerBlockPalette = strings("personal_pocket.innerBlockPalette");
    MystcraftConfig.pocketOuterBlock = string("personal_pocket.outerBlock");

    MystcraftConfig.instabilityEnabled = bool("instability.enabled");
    MystcraftConfig.deathEffectsEnabled = bool("instability.deathEffectsEnabled");
    MystcraftConfig.allowUnstableAges = bool("instability.allowUnstableAges");
    MystcraftConfig.instabilityMultiplier = decimal("instability.effectMultiplier");
    MystcraftConfig.maxAllowedInstability = decimal("instability.maxAllowedInstability");
    MystcraftConfig.thresholdDecay = decimal("instability_thresholds.decay");
    MystcraftConfig.thresholdTransmute = decimal("instability_thresholds.transmute");
    MystcraftConfig.thresholdLightning = decimal("instability_thresholds.lightning");
    MystcraftConfig.thresholdMeteor = decimal("instability_thresholds.meteor");
    MystcraftConfig.thresholdPoison = decimal("instability_thresholds.poison");
    MystcraftConfig.thresholdWither = decimal("instability_thresholds.wither");
    MystcraftConfig.chanceDecay = decimal("instability_chances.decay");
    MystcraftConfig.chanceTransmute = decimal("instability_chances.transmute");
    MystcraftConfig.chanceLightning = decimal("instability_chances.lightning");
    MystcraftConfig.chanceMeteor = decimal("instability_chances.meteor");
    MystcraftConfig.chancePlayerEffect = decimal("instability_chances.playerEffect");
  }

  public static boolean deleteAgesOnStartup() {
    return (Boolean) VALUES.get("general.deleteAgesOnStartup");
  }

  private static void installDefaults() {
    VALUES.clear();
    putGeneralDefaults();
    putPocketDefaults();
    putInstabilityDefaults();
  }

  private static void putGeneralDefaults() {
    VALUES.put("general.giveGuidebookOnFirstSpawn", true);
    VALUES.put("general.maxSymbolsPerBook", 50);
    VALUES.put("general.deleteAgesOnStartup", false);
    VALUES.put("general.enablePersonalLinkBooks", true);
    VALUES.put("general.linkPanelInBoosterPacks", true);
    VALUES.put("general.enableBoosterLoot", true);
    VALUES.put("general.enablePageLoot", true);
    VALUES.put("general.bookBinderCoverItems", DEFAULT_COVERS);
    VALUES.put("general.allowGravityBlocksInAges", false);
    VALUES.put("general.microDimensionsEnabled", false);
    VALUES.put("general.microDimensionRadiusChunks", 0);
    VALUES.put("general.microDimensionExtraChunks", 1);
    VALUES.put("general.safeStories", true);
    VALUES.put("general.droppedBooksBecomeLivingEntities", true);
    VALUES.put("general.dropBooksOnRead", true);
    VALUES.put("general.disabledSymbols", DEFAULT_DISABLED_SYMBOLS);
    VALUES.put("general.proceduralUiEnabled", true);
    VALUES.put("general.proceduralBookCoversEnabled", true);
    VALUES.put("general.proceduralSymbolPagesEnabled", true);
    VALUES.put("general.proceduralSymbolGlyphsEnabled", true);
  }

  private static void putPocketDefaults() {
    VALUES.put("personal_pocket.innerHalfSizeXZ", 24);
    VALUES.put("personal_pocket.innerHalfSizeY", 24);
    VALUES.put("personal_pocket.innerThickness", 3);
    VALUES.put("personal_pocket.outerThickness", 5);
    VALUES.put("personal_pocket.centerY", 0);
    VALUES.put("personal_pocket.innerBlockPalette", DEFAULT_POCKET_PALETTE);
    VALUES.put("personal_pocket.outerBlock", "minecraft:bedrock");
  }

  private static void putInstabilityDefaults() {
    VALUES.put("instability.enabled", true);
    VALUES.put("instability.deathEffectsEnabled", true);
    VALUES.put("instability.allowUnstableAges", true);
    VALUES.put("instability.effectMultiplier", 1.0D);
    VALUES.put("instability.maxAllowedInstability", 150.0D);
    VALUES.put("instability_thresholds.decay", 40.0D);
    VALUES.put("instability_thresholds.transmute", 50.0D);
    VALUES.put("instability_thresholds.lightning", 70.0D);
    VALUES.put("instability_thresholds.meteor", 70.0D);
    VALUES.put("instability_thresholds.poison", 80.0D);
    VALUES.put("instability_thresholds.wither", 100.0D);
    VALUES.put("instability_chances.decay", 0.001D);
    VALUES.put("instability_chances.transmute", 0.002D);
    VALUES.put("instability_chances.lightning", 0.0005D);
    VALUES.put("instability_chances.meteor", 0.0002D);
    VALUES.put("instability_chances.playerEffect", 0.0001D);
  }

  private static void parse(List<String> lines) {
    String section = "";
    for (int index = 0; index < lines.size(); index++) {
      String line = stripComment(lines.get(index)).trim();
      if (line.isEmpty()) {
        continue;
      }
      if (line.startsWith("[") && line.endsWith("]")) {
        section = line.substring(1, line.length() - 1).trim();
        continue;
      }
      int equals = line.indexOf('=');
      if (equals < 1) {
        continue;
      }
      String key = section + "." + line.substring(0, equals).trim();
      Object expected = VALUES.get(key);
      if (expected == null) {
        continue;
      }

      String rawValue = line.substring(equals + 1).trim();
      int valueLine = index + 1;
      while (expected instanceof List<?> && !rawValue.contains("]")
          && index + 1 < lines.size()) {
        index++;
        rawValue += " " + stripComment(lines.get(index)).trim();
      }
      try {
        Object value = parseValue(rawValue, expected);
        validateValue(key, value);
        VALUES.put(key, value);
      } catch (IllegalArgumentException exception) {
        Mystcraft.LOGGER.warn(
            "[Mystcraft] Ignoring invalid Fabric config value for {} at line {} ({}); "
                + "keeping the previous/default value",
            key,
            valueLine,
            exception.getMessage());
      }
    }
  }

  private static Object parseValue(String raw, Object expected) {
    if (expected instanceof Boolean) {
      if ("true".equalsIgnoreCase(raw)) {
        return true;
      }
      if ("false".equalsIgnoreCase(raw)) {
        return false;
      }
      throw new IllegalArgumentException("Expected a boolean");
    }
    if (expected instanceof Integer) {
      return Integer.parseInt(raw);
    }
    if (expected instanceof Double) {
      return Double.parseDouble(raw);
    }
    if (expected instanceof List<?>) {
      if (!raw.startsWith("[") || !raw.endsWith("]")) {
        throw new IllegalArgumentException("Expected a string list");
      }
      ArrayList<String> result = new ArrayList<>();
      int cursor = 0;
      while ((cursor = raw.indexOf('"', cursor)) >= 0) {
        int end = raw.indexOf('"', cursor + 1);
        if (end < 0) {
          break;
        }
        result.add(raw.substring(cursor + 1, end));
        cursor = end + 1;
      }
      if (result.isEmpty() && !raw.substring(1, raw.length() - 1).isBlank()) {
        throw new IllegalArgumentException("Expected quoted string list entries");
      }
      return List.copyOf(result);
    }
    if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
      return raw.substring(1, raw.length() - 1);
    }
    throw new IllegalArgumentException("Expected a quoted string");
  }

  private static void validateValue(String key, Object value) {
    NumericRange range = NUMERIC_RANGES.get(key);
    if (range != null && value instanceof Number number) {
      range.validate(number.doubleValue());
    }

    if (IDENTIFIERS.contains(key) && value instanceof String identifier) {
      validateIdentifier(identifier);
    }
    if (IDENTIFIER_LISTS.contains(key) && value instanceof List<?> identifiers) {
      for (Object identifier : identifiers) {
        if (!(identifier instanceof String string)) {
          throw new IllegalArgumentException("Expected a resource identifier string");
        }
        validateIdentifier(string);
      }
    }
  }

  private static void validateIdentifier(String value) {
    if (Identifier.tryParse(value) == null) {
      throw new IllegalArgumentException("Invalid resource identifier: " + value);
    }
  }

  private static String stripComment(String line) {
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char character = line.charAt(index);
      if (character == '"') {
        quoted = !quoted;
      } else if (character == '#' && !quoted) {
        return line.substring(0, index);
      }
    }
    return line;
  }

  private static void writeDefaultFile() {
    String defaults = """
        # Mystcraft Legacy Returned Fabric configuration
        [general]
        giveGuidebookOnFirstSpawn = true
        maxSymbolsPerBook = 50
        deleteAgesOnStartup = false
        enablePersonalLinkBooks = true
        linkPanelInBoosterPacks = true
        enableBoosterLoot = true
        enablePageLoot = true
        bookBinderCoverItems = ["minecraft:leather", "mystcraft:folder"]
        allowGravityBlocksInAges = false
        microDimensionsEnabled = false
        microDimensionRadiusChunks = 0
        microDimensionExtraChunks = 1
        safeStories = true
        droppedBooksBecomeLivingEntities = true
        dropBooksOnRead = true
        disabledSymbols = ["mystcraft:block_minecraft_coal_block", "mystcraft:block_minecraft_copper_block", "mystcraft:block_minecraft_diamond_block", "mystcraft:block_minecraft_emerald_block", "mystcraft:block_minecraft_gold_block", "mystcraft:block_minecraft_iron_block", "mystcraft:block_minecraft_lapis_block", "mystcraft:block_minecraft_netherite_block", "mystcraft:block_minecraft_raw_copper_block", "mystcraft:block_minecraft_raw_gold_block", "mystcraft:block_minecraft_raw_iron_block", "mystcraft:block_minecraft_redstone_block", "mystcraft:block_minecraft_ancient_debris"]
        proceduralUiEnabled = true
        proceduralBookCoversEnabled = true
        proceduralSymbolPagesEnabled = true
        proceduralSymbolGlyphsEnabled = true

        [personal_pocket]
        innerHalfSizeXZ = 24
        innerHalfSizeY = 24
        innerThickness = 3
        outerThickness = 5
        centerY = 0
        innerBlockPalette = ["minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks", "minecraft:mangrove_planks", "minecraft:cherry_planks"]
        outerBlock = "minecraft:bedrock"

        [instability]
        enabled = true
        deathEffectsEnabled = true
        allowUnstableAges = true
        effectMultiplier = 1.0
        maxAllowedInstability = 150.0

        [instability_thresholds]
        decay = 40.0
        transmute = 50.0
        lightning = 70.0
        meteor = 70.0
        poison = 80.0
        wither = 100.0

        [instability_chances]
        decay = 0.001
        transmute = 0.002
        lightning = 0.0005
        meteor = 0.0002
        playerEffect = 0.0001
        """;
    try {
      Files.createDirectories(PATH.getParent());
      Files.writeString(PATH, defaults, StandardCharsets.UTF_8);
      Mystcraft.LOGGER.info("[Mystcraft] Created default Fabric configuration at {}", PATH);
    } catch (IOException exception) {
      Mystcraft.LOGGER.error("[Mystcraft] Could not create default configuration at {}", PATH,
          exception);
    }
  }

  private static Supplier<Boolean> bool(String key) {
    return () -> (Boolean) VALUES.get(key);
  }

  private static Supplier<Integer> integer(String key) {
    return () -> (Integer) VALUES.get(key);
  }

  private static Supplier<Double> decimal(String key) {
    return () -> (Double) VALUES.get(key);
  }

  private static Supplier<String> string(String key) {
    return () -> (String) VALUES.get(key);
  }

  @SuppressWarnings("unchecked")
  private static Supplier<List<String>> strings(String key) {
    return () -> (List<String>) VALUES.get(key);
  }

  private record NumericRange(double minimum, double maximum) {
    private void validate(double value) {
      if (!Double.isFinite(value) || value < minimum || value > maximum) {
        throw new IllegalArgumentException(
            "Expected a finite number from " + minimum + " through " + maximum);
      }
    }
  }
}
