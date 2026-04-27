package art.arcane.mystcraft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MystcraftGameTestSuites {

  public static final String CORE = "core";
  public static final String BOOK_TRAVEL = "book_travel";
  public static final String BOOK_CRAFTING = "book_crafting";
  public static final String AGE_CREATION = "age_creation";
  public static final String WORLD_RULES = "world_rules";
  public static final String COMMANDS = "commands";

  private static final String ENV_SELECTION = "MYSTCRAFT_GAMETEST_SUITE";
  private static final String PROPERTY_SELECTION = "mystcraft.gametest.suite";
  private static final Set<String> FAST_SUITES = Set.of(CORE, BOOK_TRAVEL, BOOK_CRAFTING, AGE_CREATION);
  private static final Set<String> ALL_SUITES = Set.of(CORE, BOOK_TRAVEL, BOOK_CRAFTING, AGE_CREATION, WORLD_RULES, COMMANDS);

  private MystcraftGameTestSuites() {
  }

  public static boolean skipUnless(GameTestHelper helper, String suite) {
    if (shouldRun(suite)) {
      return false;
    }
    helper.succeed();
    return true;
  }

  public static boolean shouldRun(String suite) {
    String selection = selection();
    if (selection.isBlank() || "fast".equals(selection)) {
      return FAST_SUITES.contains(suite);
    }
    if ("full".equals(selection) || "all".equals(selection)) {
      return ALL_SUITES.contains(suite);
    }
    return Arrays.stream(selection.split(","))
        .map(String::trim)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet())
        .contains(suite);
  }

  private static String selection() {
    String property = System.getProperty(PROPERTY_SELECTION);
    if (property != null && !property.isBlank()) {
      return property.trim().toLowerCase(Locale.ROOT);
    }
    String env = System.getenv(ENV_SELECTION);
    if (env != null && !env.isBlank()) {
      return env.trim().toLowerCase(Locale.ROOT);
    }
    return "fast";
  }
}
