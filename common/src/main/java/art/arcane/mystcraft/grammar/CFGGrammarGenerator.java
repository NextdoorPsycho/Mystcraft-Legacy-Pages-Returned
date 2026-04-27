package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.util.WeightedItemSelector;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Context-Free Grammar generator for Age creation.
 * Handles rule registration, shortest path calculations, and symbol expansion.
 *
 * Thread-safety: Uses concurrent collections to allow safe access from
 * multiple threads (render thread, server thread during datapack reload).
 */
public final class CFGGrammarGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CFGGrammarGenerator.class);
  // Rule storage - use concurrent collections for thread safety during datapack reload
  private static final Map<ResourceLocation, RankData> ranks = new ConcurrentHashMap<>();
  private static final Map<ResourceLocation, List<CFGRule>> mappings = new ConcurrentHashMap<>();
  private static final Map<ResourceLocation, List<CFGRule>> reverseLookup = new ConcurrentHashMap<>();
  private static volatile Map<ResourceLocation, Map<ResourceLocation, List<List<CFGRule>>>> shortestPaths = null;

  private static volatile boolean isFinalized = false;

  private CFGGrammarGenerator() {
  }

  /**
   * Clears all grammar data to allow a full rebuild (datapack reload).
   */
  public static void reset() {
    ranks.clear();
    mappings.clear();
    reverseLookup.clear();
    shortestPaths = null;
    isFinalized = false;
  }

  /**
   * Removes all rules for a specific parent token.
   * Intended for datapack-driven overrides before grammar finalization.
   */
  public static void removeRulesForParent(ResourceLocation parent) {
    List<CFGRule> rules = mappings.remove(parent);
    if (rules == null || rules.isEmpty()) {
      return;
    }
    ranks.remove(parent);
    for (CFGRule rule : rules) {
      for (ResourceLocation value : rule.values()) {
        List<CFGRule> reverseRules = reverseLookup.get(value);
        if (reverseRules != null) {
          reverseRules.remove(rule);
          if (reverseRules.isEmpty()) {
            reverseLookup.remove(value);
          }
        }
      }
    }
  }

  /**
   * Registers a grammar rule. Must be called before finalization.
   *
   * @param rule The rule to register
   * @throws IllegalStateException if called after grammar is finalized
   */
  public static void registerRule(CFGRule rule) {
    if (isFinalized) {
      throw new IllegalStateException("Cannot register rules after grammar is finalized! " +
          "Register rules before Mystcraft's post-init.");
    }

    // Add to forward mappings - use thread-safe list
    mappings.computeIfAbsent(rule.parent(), k -> new CopyOnWriteArrayList<>()).add(rule);

    // Add to reverse lookup - use thread-safe list
    for (ResourceLocation value : rule.values()) {
      reverseLookup.computeIfAbsent(value, k -> new CopyOnWriteArrayList<>()).add(rule);
    }

    // Track rank data for weighted selection
    if (rule.rank() != null) {
      RankData rankData = ranks.computeIfAbsent(rule.parent(), k -> new RankData());
      synchronized (rankData) {
        while (rankData.rankSizes.size() <= rule.rank()) {
          rankData.rankSizes.add(0);
        }
        rankData.rankSizes.set(rule.rank(), rankData.rankSizes.get(rule.rank()) + 1);
      }
    }
  }

  /**
   * Gets all rules that produce a given token (reverse lookup).
   */
  public static List<CFGRule> getParentRules(ResourceLocation token) {
    List<CFGRule> rules = reverseLookup.get(token);
    if (rules == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(rules);
  }

  /**
   * Gets all rules that expand a given token (forward lookup).
   */
  public static List<CFGRule> getAllRules(ResourceLocation token) {
    List<CFGRule> rules = mappings.get(token);
    if (rules == null) {
      return null;
    }
    return Collections.unmodifiableList(rules);
  }

  /**
   * Gets a random rule for expanding a token using weighted selection.
   */
  public static CFGRule getRandomRule(ResourceLocation token, Random rand) {
    List<CFGRule> rules = mappings.get(token);
    if (rules == null || rules.isEmpty()) {
      return null;
    }
    return WeightedItemSelector.getRandomItem(rand, rules);
  }

  /**
   * Recursively explores/expands a token using random rule selection.
   *
   * @param token The token to expand
   * @param rand  Random number generator
   * @return List of terminal tokens after expansion
   */
  public static List<ResourceLocation> explore(ResourceLocation token, Random rand) {
    List<ResourceLocation> result = new ArrayList<>();
    CFGRule rule = getRandomRule(token, rand);

    if (rule == null) {
      // No rules - token is terminal
      result.add(token);
      return result;
    }

    if (rule.size() == 0) {
      // Epsilon rule - produces nothing
      return result;
    }

    // Recursively expand each produced token
    for (ResourceLocation produced : rule.values()) {
      result.addAll(explore(produced, rand));
    }

    return result;
  }

  /**
   * Gets the shortest connecting paths between two tokens.
   *
   * @param subtreeToken The starting token (child/leaf)
   * @param nodeToken    The target token (parent/root)
   * @return List of paths (each path is a list of rules), or null if no path exists
   */
  public static List<List<CFGRule>> getShortestPaths(ResourceLocation subtreeToken, ResourceLocation nodeToken) {
    // Local reference for thread safety (volatile read once)
    Map<ResourceLocation, Map<ResourceLocation, List<List<CFGRule>>>> localPaths = shortestPaths;
    if (localPaths == null) {
      // Grammar not yet finalized - return empty rather than throwing during reload
      LOGGER.debug("Grammar not yet finalized, returning null for shortest paths");
      return null;
    }

    Map<ResourceLocation, List<List<CFGRule>>> allPaths = localPaths.get(subtreeToken);
    if (allPaths == null) {
      return null;
    }

    List<List<CFGRule>> paths = allPaths.get(nodeToken);
    if (paths == null) {
      return null;
    }

    return Collections.unmodifiableList(paths);
  }

  /**
   * Gets the rank data for a parent token.
   */
  public static RankData getRankData(ResourceLocation parent) {
    return ranks.get(parent);
  }

  /**
   * Builds and finalizes the grammar system.
   * Must be called after all rules are registered.
   */
  public static void buildGrammar() {
    if (isFinalized) {
      LOGGER.warn("Grammar already finalized, skipping rebuild");
      return;
    }

    LOGGER.info("Building grammar with {} forward mappings and {} reverse lookups",
        mappings.size(), reverseLookup.size());

    buildShortestPaths();
    buildRankWeights();
    validateTokenPopulation();

    isFinalized = true;
    LOGGER.info("Grammar finalized successfully");
  }

  /**
   * Builds shortest path lookup tables using BFS.
   */
  private static void buildShortestPaths() {
    long startTime = System.currentTimeMillis();
    LOGGER.info("Starting buildShortestPaths for {} tokens", reverseLookup.size());

    // Build paths into a temporary map, then atomically assign
    Map<ResourceLocation, Map<ResourceLocation, List<List<CFGRule>>>> tempPaths = new ConcurrentHashMap<>();

    int count = 0;
    int total = reverseLookup.size();
    for (ResourceLocation token : reverseLookup.keySet()) {
      if (!tempPaths.containsKey(token)) {
        getOrCalculatePaths(tempPaths, token);
      }
      count++;
      if (count % 50 == 0) {
        LOGGER.debug("buildShortestPaths progress: {}/{}", count, total);
      }
    }

    // Atomic assignment ensures threads see complete data or null
    shortestPaths = tempPaths;

    long endTime = System.currentTimeMillis();
    LOGGER.info("buildShortestPaths completed in {}ms for {} tokens", endTime - startTime, total);
  }

  /**
   * Calculates shortest paths from a token to all reachable parent tokens.
   */
  private static Map<ResourceLocation, List<List<CFGRule>>> getOrCalculatePaths(
      Map<ResourceLocation, Map<ResourceLocation, List<List<CFGRule>>>> pathsMap,
      ResourceLocation token) {

    Map<ResourceLocation, List<List<CFGRule>>> allPaths = pathsMap.get(token);
    if (allPaths != null) {
      return allPaths;
    }

    allPaths = new ConcurrentHashMap<>();

    // Track which nodes we've already queued producers for (at their shortest distance)
    // This prevents exponential re-exploration of the same nodes
    Set<ResourceLocation> exploredFromNode = new HashSet<>();

    // Get all rules that produce this token
    List<CFGRule> producers = reverseLookup.get(token);
    Queue<VisitPair> toVisit = new LinkedList<>();

    if (producers != null) {
      for (CFGRule rule : producers) {
        toVisit.add(new VisitPair(rule.parent(), Collections.singletonList(rule)));
      }
    }

    // BFS to find all paths
    while (!toVisit.isEmpty()) {
      VisitPair elem = toVisit.poll();
      ResourceLocation target = elem.target;

      // Skip if target is the same as starting token (loop)
      if (target.equals(token)) {
        continue;
      }

      List<CFGRule> path = elem.path;
      List<List<CFGRule>> pathsToTarget = allPaths.computeIfAbsent(target, k -> new CopyOnWriteArrayList<>());

      // Check if we already have a shorter path to this target
      if (!pathsToTarget.isEmpty() && pathsToTarget.get(0).size() < path.size()) {
        // This path is longer than existing - skip entirely
        continue;
      }

      // If this is a shorter path, clear existing paths
      if (!pathsToTarget.isEmpty() && pathsToTarget.get(0).size() > path.size()) {
        pathsToTarget.clear();
        exploredFromNode.remove(target); // Allow re-exploration with shorter path
      }

      // Add this path (either first path, or equal-length alternative)
      pathsToTarget.add(path);

      // Only continue BFS from this node if we haven't explored from it yet
      // This prevents exponential blowup from multiple equal-length paths
      if (exploredFromNode.add(target)) {
        List<CFGRule> targetProducers = reverseLookup.get(target);
        if (targetProducers != null) {
          for (CFGRule producer : targetProducers) {
            List<CFGRule> newPath = new ArrayList<>(path);
            newPath.add(producer);
            toVisit.add(new VisitPair(producer.parent(), Collections.unmodifiableList(newPath)));
          }
        }
      }
    }

    pathsMap.put(token, allPaths);
    return allPaths;
  }

  /**
   * Builds weighted selection data based on ranks.
   * Lower ranks are more common, higher ranks are rarer.
   */
  private static void buildRankWeights() {
    final int step = 1;

    for (RankData rankData : ranks.values()) {
      rankData.rankWeights = new ConcurrentHashMap<>();
      int weight = 1;
      int lastTotal = 0;

      // Process from highest rank (rarest) to lowest (common)
      for (int i = rankData.rankSizes.size() - 1; i >= 0; --i) {
        int count = rankData.rankSizes.get(i);
        if (weight != 1 && count > 0) {
          weight = Math.max(weight, lastTotal / count + step);
        }
        rankData.rankWeights.put(i, weight);
        lastTotal = count * weight;
        weight += step;
      }
    }
  }

  /**
   * Checks that terminal grammar tokens (TERRAIN, WEATHER, etc.) have at least
   * one non-epsilon rule registered, meaning at least one symbol maps to them.
   * Logs warnings for any tokens that will always produce epsilon (nothing).
   */
  private static void validateTokenPopulation() {
    ResourceLocation[] terminalTokens = {
        GrammarData.TERRAIN,
        GrammarData.BIOMECONTROLLER,
        GrammarData.BIOME,
        GrammarData.WEATHER,
        GrammarData.LIGHTING,
        GrammarData.EFFECT,
        GrammarData.VISUAL_EFFECT,
        GrammarData.FEATURE_LARGE,
        GrammarData.FEATURE_MEDIUM,
        GrammarData.FEATURE_SMALL,
        GrammarData.BLOCK_SEA
    };

    int unpopulated = 0;
    for (ResourceLocation token : terminalTokens) {
      List<CFGRule> rules = mappings.get(token);
      if (rules == null) {
        LOGGER.warn("[Grammar Validation] Token {} has NO rules registered", token);
        unpopulated++;
        continue;
      }

      // Count non-epsilon rules (rules with at least one child)
      long symbolRules = rules.stream()
          .filter(rule -> !rule.values().isEmpty())
          .count();

      if (symbolRules == 0) {
        LOGGER.warn("[Grammar Validation] Token {} has {} rules but ALL are epsilon " +
            "(no symbols registered) - this token will always produce nothing", token, rules.size());
        unpopulated++;
      } else {
        LOGGER.debug("[Grammar Validation] Token {} has {} symbol rules", token, symbolRules);
      }
    }

    if (unpopulated > 0) {
      LOGGER.warn("[Grammar Validation] {} terminal tokens have no symbols - " +
          "ages may be missing components", unpopulated);
    } else {
      LOGGER.info("[Grammar Validation] All terminal grammar tokens have symbols registered");
    }
  }

  /**
   * Converts a path of rules to a string for debugging.
   */
  public static String pathToString(List<CFGRule> path) {
    StringBuilder out = new StringBuilder();
    for (CFGRule rule : path) {
      if (out.length() > 0) {
        out.append(" -> ");
      }
      out.append(rule.parent());
    }
    return out.toString();
  }

  /**
   * Tests shortest paths between two tokens (for debugging).
   */
  public static void testShortestPaths(ResourceLocation token1, ResourceLocation token2) {
    LOGGER.info("{} -> {}", token1, token2);
    List<List<CFGRule>> paths = getShortestPaths(token1, token2);
    if (paths == null) {
      LOGGER.info("  No path found");
      return;
    }
    for (List<CFGRule> path : paths) {
      LOGGER.info("  Path: {}", pathToString(path));
    }
  }

  /**
   * Checks if the grammar has been finalized.
   */
  public static boolean isFinalized() {
    return isFinalized;
  }

  /**
   * Stores rank sizes and weights for weighted random rule selection.
   * Thread-safe for concurrent access.
   */
  public static class RankData {
    public final List<Integer> rankSizes = new CopyOnWriteArrayList<>();
    public volatile Map<Integer, Integer> rankWeights = null;
  }

  /**
   * Helper class for BFS traversal.
   */
  private record VisitPair(ResourceLocation target, List<CFGRule> path) {
  }

}
