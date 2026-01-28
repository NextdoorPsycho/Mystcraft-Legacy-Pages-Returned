package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic biome symbols that are generated from the biome registry.
 * Each biome in the game gets a corresponding symbol that can be used
 * to add that biome to an Age's biome list.
 */
public final class BiomeSymbols {

    private BiomeSymbols() {}

    // Map of biome resource locations to their symbols
    private static final Map<ResourceLocation, BiomeSymbol> BIOME_SYMBOLS = new HashMap<>();

    // Water-related biomes get higher card ranks (less likely to appear in random generation)
    private static final Set<ResourceKey<Biome>> OCEAN_BIOMES = Set.of(
            Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
            Biomes.COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN,
            Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_FROZEN_OCEAN
    );

    private static final Set<ResourceKey<Biome>> SHORE_BIOMES = Set.of(
            Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE
    );

    /**
     * Registers all vanilla biome symbols.
     * These are registered at mod load time with static biome references.
     */
    public static void register() {
        // Register common vanilla biomes
        registerVanillaBiome(Biomes.PLAINS, "Plains", 64);
        registerVanillaBiome(Biomes.FOREST, "Forest", 70);
        registerVanillaBiome(Biomes.TAIGA, "Taiga", 68);
        registerVanillaBiome(Biomes.DESERT, "Desert", 64);
        registerVanillaBiome(Biomes.SWAMP, "Swamp", 62);
        registerVanillaBiome(Biomes.JUNGLE, "Jungle", 70);
        registerVanillaBiome(Biomes.SNOWY_PLAINS, "Snowy Plains", 64);
        registerVanillaBiome(Biomes.SAVANNA, "Savanna", 68);
        registerVanillaBiome(Biomes.BADLANDS, "Badlands", 80);
        registerVanillaBiome(Biomes.DARK_FOREST, "Dark Forest", 70);
        registerVanillaBiome(Biomes.BIRCH_FOREST, "Birch Forest", 70);
        registerVanillaBiome(Biomes.FLOWER_FOREST, "Flower Forest", 70);
        registerVanillaBiome(Biomes.MEADOW, "Meadow", 100);
        registerVanillaBiome(Biomes.BEACH, "Beach", 63);
        registerVanillaBiome(Biomes.OCEAN, "Ocean", 45);
        registerVanillaBiome(Biomes.DEEP_OCEAN, "Deep Ocean", 30);
        registerVanillaBiome(Biomes.RIVER, "River", 58);
        registerVanillaBiome(Biomes.MUSHROOM_FIELDS, "Mushroom Fields", 64);
        registerVanillaBiome(Biomes.WINDSWEPT_HILLS, "Windswept Hills", 100);
        registerVanillaBiome(Biomes.WINDSWEPT_FOREST, "Windswept Forest", 100);
        registerVanillaBiome(Biomes.STONY_PEAKS, "Stony Peaks", 140);
        registerVanillaBiome(Biomes.FROZEN_PEAKS, "Frozen Peaks", 140);
        registerVanillaBiome(Biomes.JAGGED_PEAKS, "Jagged Peaks", 160);
        registerVanillaBiome(Biomes.SNOWY_SLOPES, "Snowy Slopes", 120);
        registerVanillaBiome(Biomes.GROVE, "Grove", 100);
        registerVanillaBiome(Biomes.CHERRY_GROVE, "Cherry Grove", 100);
        registerVanillaBiome(Biomes.OLD_GROWTH_PINE_TAIGA, "Old Growth Pine Taiga", 70);
        registerVanillaBiome(Biomes.OLD_GROWTH_SPRUCE_TAIGA, "Old Growth Spruce Taiga", 70);
        registerVanillaBiome(Biomes.OLD_GROWTH_BIRCH_FOREST, "Old Growth Birch Forest", 70);
        registerVanillaBiome(Biomes.SPARSE_JUNGLE, "Sparse Jungle", 70);
        registerVanillaBiome(Biomes.BAMBOO_JUNGLE, "Bamboo Jungle", 70);
        registerVanillaBiome(Biomes.MANGROVE_SWAMP, "Mangrove Swamp", 62);
        registerVanillaBiome(Biomes.WARM_OCEAN, "Warm Ocean", 45);
        registerVanillaBiome(Biomes.LUKEWARM_OCEAN, "Lukewarm Ocean", 45);
        registerVanillaBiome(Biomes.COLD_OCEAN, "Cold Ocean", 45);
        registerVanillaBiome(Biomes.FROZEN_OCEAN, "Frozen Ocean", 45);
        registerVanillaBiome(Biomes.DEEP_LUKEWARM_OCEAN, "Deep Lukewarm Ocean", 30);
        registerVanillaBiome(Biomes.DEEP_COLD_OCEAN, "Deep Cold Ocean", 30);
        registerVanillaBiome(Biomes.DEEP_FROZEN_OCEAN, "Deep Frozen Ocean", 30);
        registerVanillaBiome(Biomes.SNOWY_BEACH, "Snowy Beach", 63);
        registerVanillaBiome(Biomes.STONY_SHORE, "Stony Shore", 68);
        registerVanillaBiome(Biomes.FROZEN_RIVER, "Frozen River", 58);
        registerVanillaBiome(Biomes.SNOWY_TAIGA, "Snowy Taiga", 68);
        registerVanillaBiome(Biomes.ICE_SPIKES, "Ice Spikes", 64);
        registerVanillaBiome(Biomes.SUNFLOWER_PLAINS, "Sunflower Plains", 64);
        registerVanillaBiome(Biomes.WOODED_BADLANDS, "Wooded Badlands", 80);
        registerVanillaBiome(Biomes.ERODED_BADLANDS, "Eroded Badlands", 80);
        registerVanillaBiome(Biomes.SAVANNA_PLATEAU, "Savanna Plateau", 100);
        registerVanillaBiome(Biomes.WINDSWEPT_SAVANNA, "Windswept Savanna", 100);
        registerVanillaBiome(Biomes.DRIPSTONE_CAVES, "Dripstone Caves", 40);
        registerVanillaBiome(Biomes.LUSH_CAVES, "Lush Caves", 40);
        registerVanillaBiome(Biomes.DEEP_DARK, "Deep Dark", -20);

        // Nether biomes
        registerVanillaBiome(Biomes.NETHER_WASTES, "Nether Wastes", 64);
        registerVanillaBiome(Biomes.CRIMSON_FOREST, "Crimson Forest", 64);
        registerVanillaBiome(Biomes.WARPED_FOREST, "Warped Forest", 64);
        registerVanillaBiome(Biomes.SOUL_SAND_VALLEY, "Soul Sand Valley", 64);
        registerVanillaBiome(Biomes.BASALT_DELTAS, "Basalt Deltas", 64);

        // End biomes
        registerVanillaBiome(Biomes.THE_END, "The End", 64);
        registerVanillaBiome(Biomes.END_HIGHLANDS, "End Highlands", 64);
        registerVanillaBiome(Biomes.END_MIDLANDS, "End Midlands", 64);
        registerVanillaBiome(Biomes.END_BARRENS, "End Barrens", 64);
        registerVanillaBiome(Biomes.SMALL_END_ISLANDS, "Small End Islands", 64);

        Mystcraft.LOGGER.info("Registered {} biome symbols", BIOME_SYMBOLS.size());
    }

    private static void registerVanillaBiome(ResourceKey<Biome> biomeKey, String displayName, int groundLevel) {
        ResourceLocation biomeId = biomeKey.location();
        String symbolId = "biome_" + biomeId.getPath();

        BiomeSymbol symbol = new BiomeSymbol(
                SymbolRegistry.mystcraftId(symbolId),
                biomeKey,
                displayName,
                groundLevel
        );
        SymbolRegistry.register(symbol);
        BIOME_SYMBOLS.put(biomeId, symbol);
    }

    /**
     * A symbol representing a specific biome.
     */
    public static class BiomeSymbol extends SymbolBase {
        private final ResourceKey<Biome> biomeKey;
        private final String displayName;
        private final int groundLevel;

        public BiomeSymbol(ResourceLocation id, ResourceKey<Biome> biomeKey, String displayName, int groundLevel) {
            super(id, SymbolCategory.BIOME);
            this.biomeKey = biomeKey;
            this.displayName = displayName;
            this.groundLevel = groundLevel;
            setCardRank(cardRankForBiome(biomeKey));
            setInstabilityCost(0.0f);
            setPoem("Nature", "Nurture", "Encourage", displayName.split(" ")[0]);
        }

        private static int cardRankForBiome(ResourceKey<Biome> key) {
            if (OCEAN_BIOMES.contains(key)) {
                return 4;
            }
            if (SHORE_BIOMES.contains(key)) {
                return 3;
            }
            return 1;
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            // Get the biome holder from the server registry
            MinecraftServer server = Mystcraft.getCurrentServer();
            if (server != null) {
                server.registryAccess().registry(Registries.BIOME).ifPresent(registry -> {
                    Holder<Biome> biomeHolder = registry.getHolderOrThrow(biomeKey);
                    director.pushBiome(biomeHolder);
                    director.addBiome(biomeHolder);
                    director.setAverageGroundLevel(groundLevel);
                });
            }
        }

        public ResourceKey<Biome> getBiomeKey() {
            return biomeKey;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
