package art.arcane.mystcraft.client.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Manages drawable D'ni words for symbol rendering.
 * Words are mapped by name to their visual components.
 * Unknown words get procedurally generated components based on their name hash.
 */
public class DrawableWordManager {

    private static final Map<String, DrawableWord> words = new HashMap<>();

    /**
     * Gets an unmodifiable view of all registered words.
     */
    public static Map<String, DrawableWord> getWords() {
        return Collections.unmodifiableMap(words);
    }

    /**
     * Registers a word with its drawable representation.
     * @param name The word name (case-insensitive)
     * @param word The drawable word
     */
    public static void registerWord(String name, DrawableWord word) {
        if (name == null || word == null) return;
        name = name.toLowerCase();
        if (words.containsKey(name)) return;
        words.put(name, word);
    }

    /**
     * Registers a word with the given components.
     * @param name The word name
     * @param components The component indices
     */
    public static void registerWord(String name, Integer... components) {
        if (name == null || components == null) return;
        registerWord(name, new DrawableWord(components));
    }

    /**
     * Gets a drawable word by name.
     * If the word is not registered, a procedurally generated one is created.
     * @param name The word name
     * @return The drawable word, or null if name is null
     */
    public static DrawableWord getDrawableWord(String name) {
        if (name == null) return null;

        String key = name.toLowerCase();
        DrawableWord word = words.get(key);

        if (word == null) {
            // Generate procedural word based on name hash
            word = generateWord(key);
            words.put(key, word);
        }

        return word;
    }

    /**
     * Generates a procedural word based on the name's hash.
     * This ensures consistent visuals for unknown words.
     */
    private static DrawableWord generateWord(String name) {
        DrawableWord word = new DrawableWord();
        Random rand = new Random(name.hashCode());

        // Generate 3-12 components
        int maxComponentIndex = 20;
        int count = rand.nextInt(10) + 3;

        // Special case for easter-related words
        if (name.startsWith("easter")) {
            count = 4;
            maxComponentIndex = 8;
        }

        for (int i = 0; i < count; i++) {
            // Components 4-23 (avoiding the first few special slots)
            word.components().add(rand.nextInt(maxComponentIndex) + 4);
        }

        return word;
    }

    /**
     * Initializes built-in D'ni words.
     * Called during client setup.
     */
    public static void initialize() {
        // Register common D'ni words with their visual components
        // These are based on the original Mystcraft word definitions

        // Basic structural words
        registerWord("system", 4, 5);
        registerWord("motion", 6, 7);
        registerWord("cycle", 4, 8);
        registerWord("form", 9, 10);
        registerWord("force", 11, 12);
        registerWord("change", 13, 14);
        registerWord("flow", 15, 16);
        registerWord("order", 17, 18);
        registerWord("chaos", 19, 20);
        registerWord("time", 21, 22);
        registerWord("space", 23, 4);
        registerWord("energy", 5, 6, 7);
        registerWord("matter", 8, 9, 10);
        registerWord("void", 11, 12, 13);

        // Nature words
        registerWord("terrain", 14, 15);
        registerWord("sky", 16, 17);
        registerWord("water", 18, 19);
        registerWord("fire", 20, 21);
        registerWord("earth", 22, 23);
        registerWord("air", 4, 5);
        registerWord("life", 6, 7, 8);
        registerWord("death", 9, 10, 11);
        registerWord("growth", 12, 13);
        registerWord("decay", 14, 15);
        registerWord("light", 16, 17);
        registerWord("dark", 18, 19);
        registerWord("color", 20, 21, 22);

        // Celestial words
        registerWord("celestial", 4, 5, 6);
        registerWord("sun", 7, 8);
        registerWord("moon", 9, 10);
        registerWord("star", 11, 12);
        registerWord("image", 13, 14);
        registerWord("stimulate", 15, 16);
        registerWord("reflect", 17, 18);
        registerWord("radiate", 19, 20);

        // Weather words
        registerWord("weather", 21, 22);
        registerWord("storm", 23, 4);
        registerWord("rain", 5, 6);
        registerWord("snow", 7, 8);
        registerWord("wind", 9, 10);
        registerWord("cloud", 11, 12);
        registerWord("lightning", 13, 14, 15);
        registerWord("thunder", 16, 17);

        // Biome words
        registerWord("biome", 18, 19);
        registerWord("forest", 20, 21);
        registerWord("desert", 22, 23);
        registerWord("ocean", 4, 5);
        registerWord("mountain", 6, 7);
        registerWord("plains", 8, 9);
        registerWord("swamp", 10, 11);
        registerWord("jungle", 12, 13);
        registerWord("tundra", 14, 15);
        registerWord("cave", 16, 17);

        // Modifiers
        registerWord("large", 18, 19);
        registerWord("small", 20, 21);
        registerWord("dense", 22, 23);
        registerWord("sparse", 4, 5);
        registerWord("bright", 6, 7);
        registerWord("dim", 8, 9);
        registerWord("fast", 10, 11);
        registerWord("slow", 12, 13);
        registerWord("normal", 14, 15);
        registerWord("extreme", 16, 17, 18);

        // Structure words
        registerWord("village", 19, 20);
        registerWord("ruin", 21, 22);
        registerWord("portal", 23, 4, 5);
        registerWord("tower", 6, 7);
        registerWord("dungeon", 8, 9);
        registerWord("library", 10, 11, 12);

        // Color words
        registerWord("red", 13, 14);
        registerWord("blue", 15, 16);
        registerWord("green", 17, 18);
        registerWord("yellow", 19, 20);
        registerWord("orange", 21, 22);
        registerWord("purple", 23, 4);
        registerWord("white", 5, 6);
        registerWord("black", 7, 8);
        registerWord("gray", 9, 10);
        registerWord("brown", 11, 12);
        registerWord("pink", 13, 14);
        registerWord("cyan", 15, 16);

        // Abstract concepts
        registerWord("random", 17, 18, 19);
        registerWord("gradient", 20, 21, 22);
        registerWord("standard", 23, 4);
        registerWord("special", 5, 6, 7);
        registerWord("instability", 8, 9, 10, 11);
        registerWord("stability", 12, 13, 14);
    }
}
