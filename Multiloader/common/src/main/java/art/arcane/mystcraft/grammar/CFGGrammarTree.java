package art.arcane.mystcraft.grammar;

import art.arcane.mystcraft.util.WeightedItemSelector;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Builds derivation trees from grammar rules.
 * Used to parse user-written symbols and fill in missing elements.
 */
public class CFGGrammarTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFGGrammarTree.class);

    /**
     * Represents a node in the grammar derivation tree.
     */
    private static class GrammarNode {
        final ResourceLocation token;
        final boolean isTerminal;
        CFGRule selected = null;
        GrammarNode parent;
        List<GrammarNode> children = new ArrayList<>();
        Integer leftPos;
        Integer rightPos;

        // Non-terminal node
        GrammarNode(ResourceLocation token) {
            this.token = token;
            this.leftPos = null;
            this.rightPos = null;
            this.isTerminal = false;
        }

        // Terminal node with position
        GrammarNode(ResourceLocation token, int pos) {
            this.token = token;
            this.leftPos = pos;
            this.rightPos = pos;
            this.isTerminal = true;
        }

        Integer getLeftPosition() {
            if (leftPos != null) return leftPos;

            // Check children
            for (GrammarNode child : children) {
                Integer pos = child.getLeftPosition();
                if (pos != null && (leftPos == null || leftPos > pos)) {
                    leftPos = pos;
                }
            }

            if (leftPos != null) return leftPos;

            // Check siblings via parent
            if (parent != null) {
                for (int i = 0; i < parent.children.size(); i++) {
                    if (parent.children.get(i).equals(this)) {
                        if (parent.children.size() > i + 1) {
                            leftPos = parent.children.get(i + 1).getLeftPosition();
                        }
                        break;
                    }
                }
            }

            return leftPos;
        }

        Integer getRightPosition() {
            if (rightPos != null) return rightPos;

            // Check children
            for (GrammarNode child : children) {
                Integer pos = child.getRightPosition();
                if (pos != null && (rightPos == null || rightPos < pos)) {
                    rightPos = pos;
                }
            }

            if (rightPos != null) return rightPos;

            // Check siblings via parent
            if (parent != null) {
                for (int i = parent.children.size() - 1; i >= 0; i--) {
                    if (parent.children.get(i).equals(this)) {
                        if (i > 0) {
                            rightPos = parent.children.get(i - 1).getRightPosition();
                        }
                    } else if (rightPos != null) {
                        Integer siblingPos = parent.children.get(i).getRightPosition();
                        if (siblingPos != null && rightPos < siblingPos) {
                            rightPos = siblingPos;
                        }
                    }
                }
            }

            return rightPos;
        }

        void addChild(GrammarNode child) {
            children.add(child);
            child.parent = this;
        }

        void addChild(int index, GrammarNode child) {
            children.add(index, child);
            child.parent = this;
        }

        @Override
        public GrammarNode clone() {
            Queue<NodePair> todo = new LinkedList<>();
            GrammarNode clone = flatClone(this);
            todo.add(new NodePair(this, clone));

            while (!todo.isEmpty()) {
                NodePair current = todo.poll();
                for (GrammarNode child : current.original.children) {
                    GrammarNode childClone = flatClone(child);
                    current.clone.addChild(childClone);
                    todo.add(new NodePair(child, childClone));
                }
            }

            return clone;
        }

        private GrammarNode flatClone(GrammarNode node) {
            GrammarNode clone = node.isTerminal ?
                    new GrammarNode(node.token, node.leftPos) :
                    new GrammarNode(node.token);
            clone.leftPos = node.leftPos;
            clone.rightPos = node.rightPos;
            clone.selected = node.selected;
            return clone;
        }

        @Override
        public String toString() {
            return String.format("%s%s%s (%d)",
                    token,
                    selected != null ? ":" : "",
                    isTerminal ? "*" : "",
                    children.size());
        }

        private static class NodePair {
            final GrammarNode original;
            final GrammarNode clone;

            NodePair(GrammarNode original, GrammarNode clone) {
                this.original = original;
                this.clone = clone;
            }
        }
    }

    private final GrammarNode root;
    private final List<GrammarNode> unexplored = new LinkedList<>();
    private List<GrammarNode> subroots = new ArrayList<>();
    private List<ResourceLocation> terminals;

    /**
     * Creates a new grammar tree with the given root token.
     */
    public CFGGrammarTree(ResourceLocation root) {
        this.root = new GrammarNode(root);
    }

    /**
     * Parses terminal symbols and builds subtrees.
     *
     * @param terminals The original symbol list (in order)
     * @param rand      Random number generator for path selection
     */
    public void parseTerminals(List<ResourceLocation> terminals, Random rand) {
        this.terminals = Collections.unmodifiableList(new ArrayList<>(terminals));

        // Process terminals in reverse order (right to left)
        for (int i = terminals.size(); i > 0; i--) {
            ResourceLocation terminal = terminals.get(i - 1);
            buildSubtree(new GrammarNode(terminal, i - 1), rand);
        }
    }

    /**
     * Merges subtrees into the main tree and returns the expanded symbol list.
     *
     * @param rand Random number generator for expansion choices
     * @return The complete list of symbols after expansion
     */
    public List<ResourceLocation> getExpanded(Random rand) {
        List<ResourceLocation> out = new ArrayList<>();
        if (terminals != null) {
            out.addAll(terminals);
        }

        // Reset unexplored list
        unexplored.clear();
        if (root.selected == null) {
            unexplored.add(root);
        }

        // Expand nodes that have only one possible rule
        for (int i = 0; i < unexplored.size(); i++) {
            List<CFGRule> rules = CFGGrammarGenerator.getAllRules(unexplored.get(i).token);
            if (rules != null && rules.size() == 1) {
                expandUnexploredNode(i--, rules.get(0));
            }
        }

        // Connect all subroots to the main tree
        List<GrammarNode> failedSubs = new ArrayList<>();
        while (!subroots.isEmpty()) {
            if (unexplored.isEmpty()) {
                failedSubs.addAll(subroots);
                break;
            }
            GrammarNode subroot = subroots.remove(0);
            if (!connectSubtreeShortest(subroot, rand)) {
                failedSubs.add(subroot);
            }
        }
        subroots = failedSubs;

        // Determine insertion locations
        Map<Integer, List<ResourceLocation>> insertLeft = new HashMap<>();
        Map<Integer, List<ResourceLocation>> insertRight = new HashMap<>();
        getInsertions(root, insertLeft, insertRight);

        // Insert tokens into the list, expanding non-terminals
        for (int i = out.size() + 1; i > 0; i--) {
            List<ResourceLocation> products = insertRight.get(i - 1);
            if (products != null) {
                for (int j = products.size(); j > 0; j--) {
                    out.addAll(i, CFGGrammarGenerator.explore(products.get(j - 1), rand));
                }
            }
            products = insertLeft.get(i - 1);
            if (products != null) {
                for (int j = products.size(); j > 0; j--) {
                    out.addAll(i - 1, CFGGrammarGenerator.explore(products.get(j - 1), rand));
                }
            }
        }

        unexplored.clear();
        return out;
    }

    /**
     * Recursively collects insertion points for unexpanded nodes.
     */
    private void getInsertions(GrammarNode node,
                               Map<Integer, List<ResourceLocation>> insertLeft,
                               Map<Integer, List<ResourceLocation>> insertRight) {
        for (GrammarNode child : node.children) {
            getInsertions(child, insertLeft, insertRight);
        }

        if (node.children.isEmpty() && node.selected == null && !node.isTerminal) {
            Integer left = node.getLeftPosition();
            if (left == null) {
                Integer right = node.getRightPosition();
                if (right == null) {
                    left = terminals != null ? terminals.size() : 0;
                } else {
                    insertRight.computeIfAbsent(right, k -> new ArrayList<>()).add(node.token);
                    return;
                }
            }
            insertLeft.computeIfAbsent(left, k -> new ArrayList<>()).add(node.token);
        }
    }

    /**
     * Builds a subtree from the provided node.
     */
    private void buildSubtree(GrammarNode subroot, Random rand) {
        // Try to connect to existing unexplored nodes
        for (GrammarNode node : unexplored) {
            List<CFGRule> path = getShortestPath(subroot, node, rand);
            if (path != null) {
                for (CFGRule rule : path) {
                    subroot = reverseExpand(subroot, rule);
                }
                replaceNodeWithTree(node, subroot);
                return;
            }
        }

        // Reverse expand as long as there's only one producing rule
        List<CFGRule> rules = CFGGrammarGenerator.getParentRules(subroot.token);
        while (rules != null && rules.size() == 1) {
            if (rules.get(0).getParent().equals(root.token)) {
                break; // Don't expand to root prematurely
            }
            subroot = reverseExpand(subroot, rules.get(0));
            rules = CFGGrammarGenerator.getParentRules(subroot.token);
        }

        subroots.add(subroot);
        addUnexploredNodes(subroot);
    }

    /**
     * Gets the shortest path between two nodes.
     */
    private List<CFGRule> getShortestPath(GrammarNode subroot, GrammarNode node, Random rand) {
        List<List<CFGRule>> paths = CFGGrammarGenerator.getShortestPaths(subroot.token, node.token);
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        return WeightedItemSelector.getRandomItem(rand, paths);
    }

    /**
     * Attempts to connect a subtree to the main tree using shortest paths.
     */
    private boolean connectSubtreeShortest(GrammarNode subroot, Random rand) {
        List<CFGRule> rules = CFGGrammarGenerator.getParentRules(subroot.token);
        if (rules == null || rules.isEmpty()) {
            return false;
        }

        for (GrammarNode node : unexplored) {
            // Check for direct match
            if (node.token.equals(subroot.token)) {
                replaceNodeWithTree(node, subroot);
                return true;
            }

            // Check for direct connection via rules
            List<CFGRule> options = new ArrayList<>();
            for (CFGRule rule : rules) {
                if (node.token.equals(rule.getParent())) {
                    options.add(rule);
                }
            }
            if (!options.isEmpty() && WeightedItemSelector.getTotalWeight(options) > 0) {
                subroot = reverseExpand(subroot, WeightedItemSelector.getRandomItem(rand, options));
                replaceNodeWithTree(node, subroot);
                return true;
            }

            // Check for path via shortest path algorithm
            List<CFGRule> path = getShortestPath(subroot, node, rand);
            if (path != null) {
                for (CFGRule rule : path) {
                    subroot = reverseExpand(subroot, rule);
                }
                replaceNodeWithTree(node, subroot);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a token already exists in a tree (loop detection).
     */
    private boolean checkForLoop(ResourceLocation parent, GrammarNode subroot) {
        Queue<GrammarNode> nodes = new LinkedList<>();
        nodes.add(subroot);

        while (!nodes.isEmpty()) {
            GrammarNode node = nodes.poll();
            if (node.token.equals(parent)) {
                return true;
            }
            nodes.addAll(node.children);
        }

        return false;
    }

    /**
     * Replaces a node with a subtree.
     */
    private void replaceNodeWithTree(GrammarNode node, GrammarNode subroot) {
        unexplored.remove(node);
        node.selected = subroot.selected;
        node.children = subroot.children;
        for (GrammarNode child : node.children) {
            child.parent = node;
        }
        node.leftPos = null;
        node.rightPos = null;
        addUnexploredNodes(node);
    }

    /**
     * Adds unexplored children of a subtree to the unexplored list.
     */
    private void addUnexploredNodes(GrammarNode subroot) {
        Queue<GrammarNode> nodes = new LinkedList<>();
        nodes.add(subroot);

        while (!nodes.isEmpty()) {
            GrammarNode node = nodes.poll();
            for (GrammarNode child : node.children) {
                if (child.selected != null) {
                    nodes.add(child);
                } else if (!child.isTerminal) {
                    unexplored.add(0, child);
                }
            }
        }
    }

    /**
     * Expands a node using a rule, adding produced tokens as children.
     */
    private void expandUnexploredNode(int index, CFGRule rule) {
        GrammarNode node = unexplored.remove(index);
        node.selected = rule;

        List<ResourceLocation> products = rule.getValues();
        for (int i = products.size(); i > 0; i--) {
            ResourceLocation product = products.get(i - 1);
            GrammarNode newNode = new GrammarNode(product);
            node.addChild(0, newNode);
            unexplored.add(index, newNode);
        }
    }

    /**
     * Reverse expands a node by a rule, building the tree upward.
     */
    private GrammarNode reverseExpand(GrammarNode subroot, CFGRule rule) {
        GrammarNode newRoot = new GrammarNode(rule.getParent());
        newRoot.selected = rule;

        List<ResourceLocation> products = rule.getValues();
        GrammarNode toInsert = subroot;

        for (int i = products.size(); i > 0; i--) {
            ResourceLocation product = products.get(i - 1);
            if (toInsert != null && product.equals(toInsert.token)) {
                newRoot.addChild(0, toInsert);
                toInsert = null;
            } else {
                newRoot.addChild(0, new GrammarNode(product));
            }
        }

        return newRoot;
    }

    /**
     * Prints the tree for debugging.
     */
    public void print() {
        printNode(root, ">");
        LOGGER.info("With {} subtrees", subroots.size());
        for (GrammarNode subroot : subroots) {
            printNode(subroot, "  >");
        }
    }

    private void printNode(GrammarNode node, String prefix) {
        LOGGER.info("{}{}{}{}  {}-{}",
                prefix,
                node.token,
                node.selected != null ? ":" : "",
                node.isTerminal ? "*" : "",
                node.getLeftPosition() != null ? node.getLeftPosition() : "?",
                node.getRightPosition() != null ? node.getRightPosition() : "?");

        for (GrammarNode child : node.children) {
            printNode(child, prefix + "  ");
        }
    }
}
