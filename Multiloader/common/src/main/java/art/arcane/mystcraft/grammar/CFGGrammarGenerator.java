package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.util.WeightedItemSelector;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Context-Free Grammar generator for Age creation.
 * Handles rule registration, shortest path calculations, and symbol expansion.
 */
public final class CFGGrammarGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFGGrammarGenerator.class);

    /**
     * Stores rank sizes and weights for weighted random rule selection.
     */
    public static class RankData {
        public final List<Integer> rankSizes = new ArrayList<>();
        public Map<Integer, Integer> rankWeights = null;
    }

    // Rule storage
    private static final Map<ResourceLocation, RankData> ranks = new HashMap<>();
    private static final Map<ResourceLocation, List<CFGRule>> mappings = new HashMap<>();
    private static final Map<ResourceLocation, List<CFGRule>> reverseLookup = new HashMap<>();
    private static Map<ResourceLocation, Map<ResourceLocation, List<List<CFGRule>>>> shortestPaths = null;

    private static boolean isFinalized = false;
    private static final boolean profilePathBuilder = false;

    private CFGGrammarGenerator() {}

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
            for (ResourceLocation value : rule.getValues()) {
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

        // Add to forward mappings
        mappings.computeIfAbsent(rule.getParent(), k -> new ArrayList<>()).add(rule);

        // Add to reverse lookup
        for (ResourceLocation value : rule.getValues()) {
            reverseLookup.computeIfAbsent(value, k -> new ArrayList<>()).add(rule);
        }

        // Track rank data for weighted selection
        if (rule.getRank() != null) {
            RankData rankData = ranks.computeIfAbsent(rule.getParent(), k -> new RankData());
            while (rankData.rankSizes.size() <= rule.getRank()) {
                rankData.rankSizes.add(0);
            }
            rankData.rankSizes.set(rule.getRank(), rankData.rankSizes.get(rule.getRank()) + 1);
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
        for (ResourceLocation produced : rule.getValues()) {
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
        if (shortestPaths == null) {
            throw new IllegalStateException("Grammar must be finalized before using shortest paths!");
        }

        Map<ResourceLocation, List<List<CFGRule>>> allPaths = shortestPaths.get(subtreeToken);
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
        if (profilePathBuilder) {
            LOGGER.info("Starting buildShortestPaths");
        }

        shortestPaths = new HashMap<>();

        for (ResourceLocation token : reverseLookup.keySet()) {
            if (!shortestPaths.containsKey(token)) {
                getOrCalculatePaths(shortestPaths, token);
            }
        }

        long endTime = System.currentTimeMillis();
        if (profilePathBuilder) {
            LOGGER.info("buildShortestPaths execution time: {}ms", endTime - startTime);
        }
    }

    /**
     * Helper class for BFS traversal.
     */
    private static class VisitPair {
        final ResourceLocation target;
        final List<CFGRule> path;

        VisitPair(ResourceLocation target, List<CFGRule> path) {
            this.target = target;
            this.path = path;
        }
    }

    /**
     * Calculates shortest paths from a token to all reachable parent tokens.
     */
    private static Map<ResourceLocation, List<List<CFGRule>>> getOrCalculatePaths(
            Map<ResourceLocation, Map<ResourceLocation, List<List<CFGRule>>>> shortestPaths,
            ResourceLocation token) {

        Map<ResourceLocation, List<List<CFGRule>>> allPaths = shortestPaths.get(token);
        if (allPaths != null) {
            return allPaths;
        }

        allPaths = new HashMap<>();

        // Get all rules that produce this token
        List<CFGRule> producers = reverseLookup.get(token);
        Queue<VisitPair> toVisit = new LinkedList<>();

        if (producers != null) {
            for (CFGRule rule : producers) {
                toVisit.add(new VisitPair(rule.getParent(), Collections.singletonList(rule)));
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
            List<List<CFGRule>> pathsToTarget = allPaths.computeIfAbsent(target, k -> new ArrayList<>());

            // Check if this path is shorter or equal to existing paths
            if (!pathsToTarget.isEmpty()) {
                if (pathsToTarget.get(0).size() > path.size()) {
                    // Found shorter path - clear existing
                    pathsToTarget.clear();
                }
            }

            if (pathsToTarget.isEmpty() || pathsToTarget.get(0).size() == path.size()) {
                pathsToTarget.add(path);

                // Continue BFS from target's producers
                List<CFGRule> targetProducers = reverseLookup.get(target);
                if (targetProducers != null) {
                    for (CFGRule producer : targetProducers) {
                        List<CFGRule> newPath = new ArrayList<>(path);
                        newPath.add(producer);
                        toVisit.add(new VisitPair(producer.getParent(), Collections.unmodifiableList(newPath)));
                    }
                }
            }
        }

        shortestPaths.put(token, allPaths);
        return allPaths;
    }

    /**
     * Builds weighted selection data based on ranks.
     * Lower ranks are more common, higher ranks are rarer.
     */
    private static void buildRankWeights() {
        final int step = 1;

        for (RankData rankData : ranks.values()) {
            rankData.rankWeights = new HashMap<>();
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
                GrammarData.SUN,
                GrammarData.MOON,
                GrammarData.STARFIELD,
                GrammarData.EFFECT,
                GrammarData.VISUAL_EFFECT,
                GrammarData.FEATURE_LARGE,
                GrammarData.FEATURE_MEDIUM,
                GrammarData.FEATURE_SMALL,
                GrammarData.BLOCK_SEA,
                GrammarData.DOODAD
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
                    .filter(rule -> !rule.getValues().isEmpty())
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
            out.append(rule.getParent());
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

}
