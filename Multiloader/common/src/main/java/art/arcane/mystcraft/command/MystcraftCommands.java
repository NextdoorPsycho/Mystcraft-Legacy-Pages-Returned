package art.arcane.mystcraft.command;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.instability.InstabilityController;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.AgeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Mystcraft commands for managing ages and symbols.
 * All command logic is platform-independent (Brigadier is vanilla).
 */
public class MystcraftCommands {

    private static final SuggestionProvider<CommandSourceStack> SYMBOL_SUGGESTIONS = (context, builder) -> {
        Collection<IAgeSymbol> symbols = SymbolRegistry.getAll();
        Set<ResourceLocation> symbolIds = new java.util.HashSet<>();
        for (IAgeSymbol symbol : symbols) {
            symbolIds.add(symbol.getRegistryName());
        }
        return SharedSuggestionProvider.suggestResource(symbolIds, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(AgePresets.PRESET_NAMES, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AGE_SUGGESTIONS = (context, builder) -> {
        AgeManager ageManager = AgeManager.get(context.getSource().getServer());
        java.util.List<String> ageIds = new java.util.ArrayList<>();
        for (Integer uid : ageManager.getAllAgeUIDs()) {
            ageIds.add(String.valueOf(uid));
        }
        return SharedSuggestionProvider.suggest(ageIds, builder);
    };

    /**
     * Registers all Mystcraft commands with the given dispatcher.
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mystcraft")
                .then(Commands.literal("age")
                        .then(Commands.literal("list")
                                .requires(source -> source.hasPermission(0))
                                .executes(MystcraftCommands::listAges))
                        .then(Commands.literal("tp")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("ageId", IntegerArgumentType.integer(0))
                                        .suggests(AGE_SUGGESTIONS)
                                        .executes(MystcraftCommands::teleportToAge)))
                        .then(Commands.literal("info")
                                .requires(source -> source.hasPermission(0))
                                .then(Commands.argument("ageId", IntegerArgumentType.integer(0))
                                        .suggests(AGE_SUGGESTIONS)
                                        .executes(MystcraftCommands::ageInfo)))
                        .then(Commands.literal("create")
                                .requires(source -> source.hasPermission(2))
                                .executes(MystcraftCommands::createAge)))
                .then(Commands.literal("symbols")
                        .requires(source -> source.hasPermission(0))
                        .executes(MystcraftCommands::listSymbols)
                        .then(Commands.literal("info")
                                .then(Commands.argument("symbolId", StringArgumentType.string())
                                        .suggests(SYMBOL_SUGGESTIONS)
                                        .executes(MystcraftCommands::symbolInfo))))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("instability")
                                .executes(MystcraftCommands::debugInstability)))
                .then(Commands.literal("give")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("randombook")
                                .executes(context -> giveRandomBook(context, 5))
                                .then(Commands.argument("symbolCount", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> giveRandomBook(context,
                                                IntegerArgumentType.getInteger(context, "symbolCount")))))
                        .then(Commands.literal("agebook")
                                .then(Commands.argument("ageId", IntegerArgumentType.integer(1))
                                        .suggests(AGE_SUGGESTIONS)
                                        .executes(context -> giveAgebook(context))))
                        .then(Commands.literal("preset")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests(PRESET_SUGGESTIONS)
                                        .executes(MystcraftCommands::givePresetBook))))
                // Instability toggle command
                .then(Commands.literal("instability")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("toggle")
                                .executes(MystcraftCommands::toggleInstabilityCurrent)
                                .then(Commands.argument("ageId", IntegerArgumentType.integer(1))
                                        .suggests(AGE_SUGGESTIONS)
                                        .executes(MystcraftCommands::toggleInstabilityAge)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(MystcraftCommands::toggleInstabilityAgeExplicit))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1000.0f))
                                        .executes(MystcraftCommands::setInstabilityCurrent)
                                        .then(Commands.argument("ageId", IntegerArgumentType.integer(1))
                                                .suggests(AGE_SUGGESTIONS)
                                                .executes(MystcraftCommands::setInstabilityAge)))))
                // Chunk regeneration command
                .then(Commands.literal("regen")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> regenChunks(context, 1))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 8))
                                .executes(context -> regenChunks(context,
                                        IntegerArgumentType.getInteger(context, "radius")))))
                // Time control command
                .then(Commands.literal("time")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(MystcraftCommands::setTime)))
                        .then(Commands.literal("add")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer())
                                        .executes(MystcraftCommands::addTime))))
                // Weather control command
                .then(Commands.literal("weather")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("clear")
                                .executes(context -> setWeather(context, "clear")))
                        .then(Commands.literal("rain")
                                .executes(context -> setWeather(context, "rain")))
                        .then(Commands.literal("thunder")
                                .executes(context -> setWeather(context, "thunder"))))
                // Spawn meteor command
                .then(Commands.literal("spawn")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("meteor")
                                .executes(context -> spawnMeteor(context, 2))
                                .then(Commands.argument("scale", IntegerArgumentType.integer(1, 10))
                                        .executes(context -> spawnMeteor(context,
                                                IntegerArgumentType.getInteger(context, "scale"))))))
                // Where am I command - check if current dim is Mystcraft
                .then(Commands.literal("where")
                        .requires(source -> source.hasPermission(0))
                        .executes(MystcraftCommands::whereAmI))
                // Reprofile instability command
                .then(Commands.literal("reprofile")
                        .requires(source -> source.hasPermission(2))
                        .executes(MystcraftCommands::reprofileCurrent)
                        .then(Commands.argument("ageId", IntegerArgumentType.integer(1))
                                .suggests(AGE_SUGGESTIONS)
                                .executes(MystcraftCommands::reprofileAge)))
        );
    }

    /**
     * Lists all known ages.
     */
    private static int listAges(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        AgeManager ageManager = AgeManager.get(source.getServer());
        java.util.List<Integer> ageIds = new java.util.ArrayList<>();
        for (Integer uid : ageManager.getAllAgeUIDs()) {
            ageIds.add(uid);
        }

        if (ageIds.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No ages have been created yet."), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Known Ages ==="), false);
        for (Integer ageId : ageIds) {
            ResourceLocation dimLoc = ageManager.getDimension(ageId);
            // Try to get the name from AgeData if the level is loaded
            String ageName = "Unnamed";
            ServerLevel level = ageManager.getAgeLevel(source.getServer(), ageId);
            if (level != null) {
                var ageData = art.arcane.mystcraft.world.AgeData.getIfPresent(level);
                if (ageData != null && ageData.getAgeName() != null) {
                    ageName = ageData.getAgeName();
                }
            }
            final String finalName = ageName;
            source.sendSuccess(() -> Component.literal(String.format(
                    "  Age %d: %s (%s)", ageId, finalName, dimLoc
            )), false);
        }

        return ageIds.size();
    }

    /**
     * Teleports the player to a specific age.
     */
    private static int teleportToAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        int ageId = IntegerArgumentType.getInteger(context, "ageId");

        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), ageId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Failed to access age " + ageId));
            return 0;
        }

        BlockPos spawn = AgeDimensionFactory.getAgeSpawn(ageLevel);
        player.teleportTo(ageLevel, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());

        source.sendSuccess(() -> Component.literal("Teleported to Age " + ageId), true);
        return 1;
    }

    /**
     * Shows information about a specific age.
     */
    private static int ageInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int ageId = IntegerArgumentType.getInteger(context, "ageId");

        AgeManager ageManager = AgeManager.get(source.getServer());
        ResourceLocation dimLoc = ageManager.getDimension(ageId);

        if (dimLoc == null) {
            source.sendFailure(Component.literal("Age " + ageId + " not found."));
            return 0;
        }

        // Try to get the name from AgeData
        String ageName = "Unnamed";
        ServerLevel level = ageManager.getAgeLevel(source.getServer(), ageId);
        if (level != null) {
            var ageData = art.arcane.mystcraft.world.AgeData.getIfPresent(level);
            if (ageData != null && ageData.getAgeName() != null) {
                ageName = ageData.getAgeName();
            }
        }

        final String finalName = ageName;
        source.sendSuccess(() -> Component.literal("=== Age " + ageId + " ==="), false);
        source.sendSuccess(() -> Component.literal("  Name: " + finalName), false);
        source.sendSuccess(() -> Component.literal("  Dimension: " + dimLoc), false);

        // Check if currently loaded (use the level we already fetched)
        if (level != null) {
            source.sendSuccess(() -> Component.literal("  Status: Loaded"), false);
            source.sendSuccess(() -> Component.literal("  Players: " + level.players().size()), false);
        } else {
            source.sendSuccess(() -> Component.literal("  Status: Not loaded"), false);
        }

        return 1;
    }

    /**
     * Creates a new empty age.
     */
    private static int createAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        AgeManager ageManager = AgeManager.get(source.getServer());
        int newAgeId = ageManager.allocateUID();

        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), newAgeId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Failed to create new age."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Created Age " + newAgeId), true);
        return 1;
    }

    /**
     * Lists all registered symbols.
     */
    private static int listSymbols(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<IAgeSymbol> allSymbols = SymbolRegistry.getAll();

        source.sendSuccess(() -> Component.literal("=== Registered Symbols (" + allSymbols.size() + ") ==="), false);

        // Group by category
        java.util.Map<String, java.util.List<ResourceLocation>> byCategory = new java.util.HashMap<>();
        for (IAgeSymbol symbol : allSymbols) {
            String category = symbol.getCategory().name().toLowerCase();
            byCategory.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(symbol.getRegistryName());
        }

        for (var entry : byCategory.entrySet()) {
            source.sendSuccess(() -> Component.literal("  " + entry.getKey() + ": " + entry.getValue().size()), false);
        }

        return allSymbols.size();
    }

    /**
     * Shows information about a specific symbol.
     */
    private static int symbolInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String symbolIdStr = StringArgumentType.getString(context, "symbolId");
        ResourceLocation symbolId = new ResourceLocation(symbolIdStr);

        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
        if (symbol == null) {
            source.sendFailure(Component.literal("Symbol not found: " + symbolIdStr));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Symbol: " + symbolId + " ==="), false);
        source.sendSuccess(() -> Component.literal("  Category: " + symbol.getCategory()), false);
        source.sendSuccess(() -> Component.literal("  Cost: " + symbol.getInstabilityCost()), false);

        return 1;
    }

    /**
     * Debug command to show instability in current dimension.
     */
    private static int debugInstability(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        var ageData = art.arcane.mystcraft.world.AgeData.getIfPresent((ServerLevel) level);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== Age Instability ==="), false);
        source.sendSuccess(() -> Component.literal("  Current: " + String.format("%.2f", ageData.getInstability())), false);
        source.sendSuccess(() -> Component.literal("  UID: " + ageData.getAgeUID()), false);

        return 1;
    }

    /**
     * Shows the player whether they are in a Mystcraft dimension and its ID.
     */
    private static int whereAmI(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();
        ResourceLocation dimId = level.dimension().location();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendSuccess(() -> Component.literal("You are NOT in a Mystcraft age."), false);
            source.sendSuccess(() -> Component.literal("  Dimension: " + dimId), false);
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent((ServerLevel) level);
        int ageUID = ageData != null ? ageData.getAgeUID() : -1;
        String ageName = ageData != null && ageData.getAgeName() != null ? ageData.getAgeName() : "Unnamed";
        float instability = ageData != null ? ageData.getInstability() : 0;

        source.sendSuccess(() -> Component.literal("=== Mystcraft Age ==="), false);
        source.sendSuccess(() -> Component.literal("  Age UID: " + ageUID), false);
        source.sendSuccess(() -> Component.literal("  Name: " + ageName), false);
        source.sendSuccess(() -> Component.literal("  Dimension ID: " + dimId), false);
        source.sendSuccess(() -> Component.literal("  Instability: " + String.format("%.2f", instability)), false);

        if (ageData != null) {
            List<ItemStack> pages = ageData.getPages();
            int symbolCount = 0;
            for (ItemStack page : pages) {
                if (art.arcane.mystcraft.data.Page.getSymbol(page) != null) {
                    symbolCount++;
                }
            }
            int finalSymbolCount = symbolCount;
            source.sendSuccess(() -> Component.literal("  Pages: " + pages.size() + " (" + finalSymbolCount + " symbols)"), false);
        }

        return 1;
    }

    // Celestial variant symbol IDs for random book generation
    private static final String[] RANDOM_SUN_VARIANTS = {
            "mystcraft:sun", "mystcraft:sun_large", "mystcraft:sun_small",
            "mystcraft:sun_fast", "mystcraft:sun_slow", "mystcraft:sun_dark"
    };
    private static final String[] RANDOM_MOON_VARIANTS = {
            "mystcraft:moon", "mystcraft:moon_large", "mystcraft:moon_small",
            "mystcraft:moon_full", "mystcraft:moon_fast", "mystcraft:moon_slow"
    };
    private static final String[] RANDOM_STAR_VARIANTS = {
            "mystcraft:stars", "mystcraft:stars_dense", "mystcraft:stars_sparse"
    };
    private static final String[] RANDOM_GRADIENT_SYMBOLS = {
            "mystcraft:gradient_sunset", "mystcraft:gradient_dawn",
            "mystcraft:gradient_dusk", "mystcraft:gradient_aurora",
            "mystcraft:gradient_blood_sky"
    };
    private static final String[] RANDOM_CLOUD_MODIFIERS = {
            "mystcraft:clouds_low", "mystcraft:clouds_high", "mystcraft:clouds_none"
    };
    private static final String[] RANDOM_HORIZON_MODIFIERS = {
            "mystcraft:horizon_low", "mystcraft:horizon_high"
    };

    /**
     * Gives the player a random descriptive book with a link panel and random symbols.
     */
    private static int giveRandomBook(CommandContext<CommandSourceStack> context, int symbolCount) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        RandomSource random = player.getRandom();

        // Create the agebook item
        ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());

        // Build pages list - start with link panel
        List<ItemStack> pages = new ArrayList<>();
        pages.add(Page.createLinkPage());
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();

        int budget = symbolCount;

        // --- Core symbols: terrain, biome controller, biome ---

        IAgeSymbol terrain = pickWeightedTerrain(random);
        if (terrain != null && budget > 0 && addSymbolPage(pages, seen, terrain)) {
            budget--;
        }

        IAgeSymbol controller = pickUniformFromCategory(SymbolCategory.BIOME_CONTROLLER, random);
        if (controller != null && budget > 0 && addSymbolPage(pages, seen, controller)) {
            budget--;
        }

        if (budget > 0) {
            IAgeSymbol biome = pickWeightedBiome(random);
            if (biome != null && addSymbolPage(pages, seen, biome)) {
                budget--;
            }
        }

        // Optional second biome for variety (avoid oceans)
        if (budget > 0 && random.nextFloat() < 0.5f) {
            IAgeSymbol biome = pickWeightedBiome(random);
            if (biome != null && addSymbolPage(pages, seen, biome)) {
                budget--;
            }
        }

        // --- Celestials: pick a sun, moon, and stars variant ---

        if (budget > 0 && addRandomSymbolFromPool(pages, seen, RANDOM_SUN_VARIANTS, random)) budget--;
        if (budget > 0 && addRandomSymbolFromPool(pages, seen, RANDOM_MOON_VARIANTS, random)) budget--;
        if (budget > 0 && addRandomSymbolFromPool(pages, seen, RANDOM_STAR_VARIANTS, random)) budget--;

        // --- Gradient: 50% chance to include one ---

        if (budget > 0 && random.nextFloat() < 0.35f) {
            if (addRandomSymbolFromPool(pages, seen, RANDOM_GRADIENT_SYMBOLS, random)) {
                budget--;
            }
        }

        // --- Cloud modifier: 30% chance ---

        if (budget > 0 && random.nextFloat() < 0.35f) {
            if (addRandomSymbolFromPool(pages, seen, RANDOM_CLOUD_MODIFIERS, random)) {
                budget--;
            }
        }

        // --- Horizon modifier: 20% chance ---

        if (budget > 0 && random.nextFloat() < 0.15f) {
            if (addRandomSymbolFromPool(pages, seen, RANDOM_HORIZON_MODIFIERS, random)) {
                budget--;
            }
        }

        // --- Weather: include one ---

        if (budget > 0) {
            IAgeSymbol weather = pickUniformFromCategory(SymbolCategory.WEATHER, random);
            if (weather != null && addSymbolPage(pages, seen, weather)) {
                budget--;
            }
        }

        // --- Lighting: rarely dark, others even ---
        if (budget > 0) {
            IAgeSymbol lighting = pickWeightedFromCategory(SymbolCategory.LIGHTING, random, MystcraftCommands::lightingWeight);
            if (lighting != null && addSymbolPage(pages, seen, lighting)) {
                budget--;
            }
        }

        // --- Big features: 0-3 weighted 0,0,1,1,1,2,2,3,3 ---
        int featureLargeCount = pickCountWeighted(random, new int[]{0, 0, 1, 1, 1, 2, 2, 3, 3});
        budget = addCategorySymbols(pages, seen, SymbolCategory.FEATURE_LARGE, random, featureLargeCount, budget,
                MystcraftCommands::featureLargeWeight);

        // --- Medium/Small features: random ---
        int featureMediumCount = random.nextInt(3);
        budget = addCategorySymbols(pages, seen, SymbolCategory.FEATURE_MEDIUM, random, featureMediumCount, budget, null);
        int featureSmallCount = random.nextInt(3);
        budget = addCategorySymbols(pages, seen, SymbolCategory.FEATURE_SMALL, random, featureSmallCount, budget, null);

        // --- Structures: frequent and random ---
        int structureCount = pickCountWeighted(random, new int[]{1, 1, 2, 2, 3, 3, 4});
        budget = addCategorySymbols(pages, seen, SymbolCategory.STRUCTURE, random, structureCount, budget, null);

        // --- Environment: random, low lightning/meteors ---
        int environmentCount = random.nextInt(3);
        budget = addCategorySymbols(pages, seen, SymbolCategory.ENVIRONMENT, random, environmentCount, budget, MystcraftCommands::environmentWeight);

        // --- Visual effects: common but not too diverse ---
        int visualCount = pickCountWeighted(random, new int[]{1, 1, 2});
        budget = addCategorySymbols(pages, seen, SymbolCategory.VISUAL_EFFECT, random, visualCount, budget, null);

        // --- Ores: random, rarely no ore ---
        int oreCount = pickCountWeighted(random, new int[]{0, 1, 1, 2});
        budget = addOreSymbols(pages, seen, random, oreCount, budget);

        // --- Special symbols: random ---
        int specialCount = random.nextInt(2);
        budget = addCategorySymbols(pages, seen, SymbolCategory.SPECIAL, random, specialCount, budget, null);

        // --- Terrain block modifier: frequently weird ---
        if (budget > 0 && random.nextFloat() < 0.6f) {
            IAgeSymbol modifier = pickRandomTerrainBlock(random);
            if (modifier != null && addSymbolPage(pages, seen, modifier)) {
                budget--;
            }
        }

        // --- Fill remaining slots with random weighted symbols ---

        for (int i = 0; i < budget && i < 50; i++) {
            IAgeSymbol symbol = SymbolRegistry.getRandomWeighted(random);
            if (symbol != null) {
                if (addSymbolPage(pages, seen, symbol)) {
                    budget--;
                }
            }
        }

        // Generate a random age name
        String[] prefixes = {
                "Mysterious", "Ancient", "Lost", "Hidden", "Eternal",
                "Twilight", "Crystal", "Shadow", "Golden", "Silver",
                "Crimson", "Jade", "Sapphire", "Amber", "Emerald",
                "Copper", "Aurora", "Midnight", "Ivory", "Cobalt"
        };
        String[] suffixes = {
                "Realm", "World", "Domain", "Land", "Age",
                "Dimension", "Expanse", "Haven", "Void", "Sanctuary",
                "Horizon", "Depths", "Reach", "Wastes", "Pinnacle"
        };
        String ageName = prefixes[random.nextInt(prefixes.length)] + " " + suffixes[random.nextInt(suffixes.length)];

        // Create the book with pages
        AgebookItem.create(agebook, player, pages, ageName);

        // Give to player
        if (!player.getInventory().add(agebook)) {
            player.drop(agebook, false);
        }

        int totalSymbols = pages.size() - 1; // Exclude link panel
        source.sendSuccess(() -> Component.literal("Created random Descriptive Book '" + ageName + "' with " + totalSymbols + " symbols"), true);

        return 1;
    }

    /**
     * Picks a random symbol ID from the pool and adds it as a page if it exists in the registry.
     */
    private static void addRandomSymbolFromPool(List<ItemStack> pages, String[] pool, RandomSource random) {
        String chosen = pool[random.nextInt(pool.length)];
        ResourceLocation id = new ResourceLocation(chosen);
        if (SymbolRegistry.get(id) != null) {
            pages.add(Page.createSymbolPage(id));
        }
    }

    private static boolean addRandomSymbolFromPool(List<ItemStack> pages, java.util.Set<ResourceLocation> seen,
                                                   String[] pool, RandomSource random) {
        String chosen = pool[random.nextInt(pool.length)];
        ResourceLocation id = new ResourceLocation(chosen);
        IAgeSymbol symbol = SymbolRegistry.get(id);
        if (symbol == null) return false;
        return addSymbolPage(pages, seen, symbol);
    }

    private static boolean addSymbolPage(List<ItemStack> pages, java.util.Set<ResourceLocation> seen, IAgeSymbol symbol) {
        if (symbol == null) return false;
        ResourceLocation id = symbol.getRegistryName();
        if (!symbol.canDuplicate() && seen.contains(id)) return false;
        pages.add(Page.createSymbolPage(id));
        seen.add(id);
        return true;
    }

    private static IAgeSymbol pickUniformFromCategory(SymbolCategory category, RandomSource random) {
        List<IAgeSymbol> list = SymbolRegistry.getByCategory(category);
        List<IAgeSymbol> filtered = new ArrayList<>();
        for (IAgeSymbol symbol : list) {
            if (!symbol.allowInRandomGeneration()) continue;
            if (SymbolRegistry.isBlacklisted(symbol.getRegistryName())) continue;
            filtered.add(symbol);
        }
        if (filtered.isEmpty()) return null;
        return filtered.get(random.nextInt(filtered.size()));
    }

    private static IAgeSymbol pickWeightedFromCategory(SymbolCategory category, RandomSource random,
                                                       java.util.function.ToIntFunction<IAgeSymbol> weightFn) {
        List<IAgeSymbol> list = SymbolRegistry.getByCategory(category);
        List<IAgeSymbol> weighted = new ArrayList<>();
        for (IAgeSymbol symbol : list) {
            if (!symbol.allowInRandomGeneration()) continue;
            if (SymbolRegistry.isBlacklisted(symbol.getRegistryName())) continue;
            int weight = Math.max(0, weightFn.applyAsInt(symbol));
            for (int i = 0; i < weight; i++) {
                weighted.add(symbol);
            }
        }
        if (weighted.isEmpty()) return null;
        return weighted.get(random.nextInt(weighted.size()));
    }

    private static IAgeSymbol pickWeightedTerrain(RandomSource random) {
        List<IAgeSymbol> list = SymbolRegistry.getByCategory(SymbolCategory.TERRAIN);
        List<IAgeSymbol> weighted = new ArrayList<>();
        for (IAgeSymbol symbol : list) {
            if (!symbol.allowInRandomGeneration()) continue;
            if (SymbolRegistry.isBlacklisted(symbol.getRegistryName())) continue;
            int weight = terrainWeight(symbol);
            for (int i = 0; i < weight; i++) {
                weighted.add(symbol);
            }
        }
        if (weighted.isEmpty()) return null;
        return weighted.get(random.nextInt(weighted.size()));
    }

    private static IAgeSymbol pickWeightedBiome(RandomSource random) {
        List<IAgeSymbol> list = SymbolRegistry.getByCategory(SymbolCategory.BIOME);
        List<IAgeSymbol> weighted = new ArrayList<>();
        for (IAgeSymbol symbol : list) {
            if (!symbol.allowInRandomGeneration()) continue;
            if (SymbolRegistry.isBlacklisted(symbol.getRegistryName())) continue;
            int weight = biomeWeight(symbol);
            for (int i = 0; i < weight; i++) {
                weighted.add(symbol);
            }
        }
        if (weighted.isEmpty()) return null;
        return weighted.get(random.nextInt(weighted.size()));
    }

    private static int biomeWeight(IAgeSymbol symbol) {
        String path = symbol.getRegistryName().getPath();
        if (path.contains("ocean")) return 1;
        if (path.contains("nether") || path.contains("crimson") || path.contains("warped")
                || path.contains("basalt") || path.contains("soul")) return 1;
        if (path.contains("end")) return 1;
        return 5;
    }

    private static int terrainWeight(IAgeSymbol symbol) {
        String path = symbol.getRegistryName().getPath();
        if (path.contains("normal")) return 0; // avoid boring overworld-like in random books
        if (path.contains("void")) return 1; // void rare
        return 2; // even otherwise
    }

    private static int lightingWeight(IAgeSymbol symbol) {
        String path = symbol.getRegistryName().getPath();
        if (path.contains("dark")) return 1;
        return 3;
    }

    private static int environmentWeight(IAgeSymbol symbol) {
        String path = symbol.getRegistryName().getPath();
        if (path.contains("lightning") || path.contains("meteors")) return 1;
        return 3;
    }

    private static int featureLargeWeight(IAgeSymbol symbol) {
        String path = symbol.getRegistryName().getPath();
        if (path.startsWith("floating_islands") || path.equals("skylands")) return 1;
        return 3;
    }

    private static int pickCountWeighted(RandomSource random, int[] options) {
        return options[random.nextInt(options.length)];
    }

    private static int addCategorySymbols(List<ItemStack> pages, java.util.Set<ResourceLocation> seen,
                                          SymbolCategory category, RandomSource random, int count, int budget,
                                          java.util.function.ToIntFunction<IAgeSymbol> weightFn) {
        if (count <= 0 || budget <= 0) return budget;
        List<IAgeSymbol> pool = SymbolRegistry.getByCategory(category);
        List<IAgeSymbol> candidates = new ArrayList<>();
        for (IAgeSymbol symbol : pool) {
            if (!symbol.allowInRandomGeneration()) continue;
            if (SymbolRegistry.isBlacklisted(symbol.getRegistryName())) continue;
            candidates.add(symbol);
        }
        if (candidates.isEmpty()) return budget;
        for (int i = 0; i < count && budget > 0; i++) {
            IAgeSymbol pick = (weightFn == null)
                    ? candidates.get(random.nextInt(candidates.size()))
                    : pickWeightedFromCategory(category, random, weightFn);
            if (pick != null && addSymbolPage(pages, seen, pick)) {
                budget--;
            }
        }
        return budget;
    }

    private static int addOreSymbols(List<ItemStack> pages, java.util.Set<ResourceLocation> seen,
                                     RandomSource random, int count, int budget) {
        if (count <= 0 || budget <= 0) return budget;
        List<IAgeSymbol> pool = new ArrayList<>();
        for (IAgeSymbol symbol : SymbolRegistry.getByCategory(SymbolCategory.FEATURE_MEDIUM)) {
            String path = symbol.getRegistryName().getPath();
            if (path.startsWith("extra_") || path.startsWith("no_") || path.equals("dense_ores")) {
                pool.add(symbol);
            }
        }
        if (pool.isEmpty()) return budget;
        for (int i = 0; i < count && budget > 0; i++) {
            IAgeSymbol pick = pickWeightedOre(pool, random);
            if (pick != null && addSymbolPage(pages, seen, pick)) {
                budget--;
            }
        }
        return budget;
    }

    private static IAgeSymbol pickWeightedOre(List<IAgeSymbol> pool, RandomSource random) {
        List<IAgeSymbol> weighted = new ArrayList<>();
        for (IAgeSymbol symbol : pool) {
            String path = symbol.getRegistryName().getPath();
            int weight = (path.equals("no_ores") || path.startsWith("no_")) ? 1 : 3;
            for (int i = 0; i < weight; i++) {
                weighted.add(symbol);
            }
        }
        if (weighted.isEmpty()) return null;
        return weighted.get(random.nextInt(weighted.size()));
    }

    private static IAgeSymbol pickFromPool(String[] pool, int[] weights, RandomSource random) {
        int total = 0;
        for (int w : weights) total += w;
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < pool.length; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return SymbolRegistry.get(new ResourceLocation(pool[i]));
            }
        }
        return SymbolRegistry.get(new ResourceLocation(pool[0]));
    }

    private static IAgeSymbol pickRandomTerrainBlock(RandomSource random) {
        List<IAgeSymbol> pool = new ArrayList<>();
        for (IAgeSymbol symbol : SymbolRegistry.getByCategory(SymbolCategory.MODIFIER)) {
            if (!symbol.getRegistryName().getPath().startsWith("block_minecraft_")) continue;
            pool.add(symbol);
        }
        if (pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * Gives the player an Agebook linked to an existing age.
     */
    private static int giveAgebook(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        int ageId = IntegerArgumentType.getInteger(context, "ageId");

        AgeManager ageManager = AgeManager.get(source.getServer());
        ResourceLocation dimLoc = ageManager.getDimension(ageId);

        if (dimLoc == null) {
            source.sendFailure(Component.literal("Age " + ageId + " does not exist. Use '/mystcraft age create' to create a new age first, or '/mystcraft give randombook' to create a book that generates a new age on first use."));
            return 0;
        }

        // Get or create the age dimension to ensure it's loaded
        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), ageId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Failed to load age " + ageId));
            return 0;
        }

        // Get age data for name and spawn info
        art.arcane.mystcraft.world.AgeData ageData = art.arcane.mystcraft.world.AgeData.getIfPresent(ageLevel);
        String ageName = ageData != null && ageData.getAgeName() != null ? ageData.getAgeName() : "Age " + ageId;
        BlockPos spawn = AgeDimensionFactory.getAgeSpawn(ageLevel);

        // Create the agebook item
        ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());
        agebook.setTag(new net.minecraft.nbt.CompoundTag());

        // Set up the book with the age's information
        art.arcane.mystcraft.data.LinkOptions.setDimensionUID(agebook.getTag(), ageId);
        art.arcane.mystcraft.data.LinkOptions.setSpawn(agebook.getTag(), spawn);
        art.arcane.mystcraft.data.LinkOptions.setDisplayName(agebook.getTag(), ageName);

        // Copy pages from age data if available
        if (ageData != null) {
            List<ItemStack> pages = ageData.getPages();
            if (pages != null && !pages.isEmpty()) {
                AgebookItem item = (AgebookItem) agebook.getItem();
                item.addPages(agebook, pages);
            }
        }

        // Give to player
        if (!player.getInventory().add(agebook)) {
            player.drop(agebook, false);
        }

        source.sendSuccess(() -> Component.literal("Created Descriptive Book for Age " + ageId + " ('" + ageName + "')"), true);
        return 1;
    }

    // --- Preset Book Command ---

    /**
     * Gives the player a preset descriptive book with curated symbols.
     */
    private static int givePresetBook(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String presetName = StringArgumentType.getString(context, "name");

        AgePresets.Preset preset = AgePresets.getPreset(presetName);
        if (preset == null) {
            source.sendFailure(Component.literal("Unknown preset: " + presetName));
            source.sendFailure(Component.literal("Available: " + String.join(", ", AgePresets.PRESET_NAMES)));
            return 0;
        }

        RandomSource random = player.getRandom();

        // Build pages
        List<ItemStack> pages = new ArrayList<>();
        pages.add(Page.createLinkPage());

        // Add fixed symbols
        for (String symbolId : preset.fixedSymbols) {
            ResourceLocation id = new ResourceLocation(symbolId);
            if (SymbolRegistry.get(id) != null) {
                pages.add(Page.createSymbolPage(id));
            } else {
                Mystcraft.LOGGER.warn("[Preset] Unknown fixed symbol: {}", symbolId);
            }
        }

        // Add random picks from each pool
        for (AgePresets.RandomPool pool : preset.randomPools) {
            List<String> available = new ArrayList<>(pool.options);
            Collections.shuffle(available, new java.util.Random(random.nextLong()));
            int count = Math.min(pool.pickCount, available.size());
            for (int i = 0; i < count; i++) {
                ResourceLocation id = new ResourceLocation(available.get(i));
                if (SymbolRegistry.get(id) != null) {
                    pages.add(Page.createSymbolPage(id));
                } else {
                    Mystcraft.LOGGER.warn("[Preset] Unknown pool symbol: {}", available.get(i));
                }
            }
        }

        // Create the book
        ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());
        AgebookItem.create(agebook, player, pages, preset.displayName);

        if (!player.getInventory().add(agebook)) {
            player.drop(agebook, false);
        }

        int symbolCount = pages.size() - 1;
        source.sendSuccess(() -> Component.literal("Created preset book '" + preset.displayName + "' with " + symbolCount + " symbols"), true);
        return 1;
    }

    // --- Instability Toggle Commands ---

    /**
     * Toggles instability for the current Age.
     */
    private static int toggleInstabilityCurrent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent((ServerLevel) level);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found."));
            return 0;
        }

        // Toggle: if instability > 0, set to 0, otherwise restore default
        float current = ageData.getInstability();
        if (current > 0) {
            ageData.setInstability(0);
            source.sendSuccess(() -> Component.literal("Instability disabled for current Age"), true);
        } else {
            ageData.setInstability(50.0f); // Default instability value
            source.sendSuccess(() -> Component.literal("Instability enabled for current Age (set to 50)"), true);
        }

        return 1;
    }

    /**
     * Toggles instability for a specific Age (toggle mode).
     */
    private static int toggleInstabilityAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int ageId = IntegerArgumentType.getInteger(context, "ageId");

        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), ageId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Age " + ageId + " not found."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent(ageLevel);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found for Age " + ageId + "."));
            return 0;
        }

        // Toggle: if instability > 0, set to 0, otherwise restore default
        float current = ageData.getInstability();
        if (current > 0) {
            ageData.setInstability(0);
            source.sendSuccess(() -> Component.literal("Instability disabled for Age " + ageId), true);
        } else {
            ageData.setInstability(50.0f);
            source.sendSuccess(() -> Component.literal("Instability enabled for Age " + ageId + " (set to 50)"), true);
        }

        return 1;
    }

    /**
     * Sets instability for a specific Age (explicit enable/disable).
     */
    private static int toggleInstabilityAgeExplicit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int ageId = IntegerArgumentType.getInteger(context, "ageId");
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), ageId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Age " + ageId + " not found."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent(ageLevel);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found for Age " + ageId + "."));
            return 0;
        }

        if (enabled) {
            if (ageData.getInstability() <= 0) {
                ageData.setInstability(50.0f);
            }
            source.sendSuccess(() -> Component.literal("Instability enabled for Age " + ageId), true);
        } else {
            ageData.setInstability(0);
            source.sendSuccess(() -> Component.literal("Instability disabled for Age " + ageId), true);
        }

        return 1;
    }

    // --- Set Instability Commands ---

    /**
     * Sets instability to a specific value for the current Age.
     */
    private static int setInstabilityCurrent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Level level = player.level();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent((ServerLevel) level);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found."));
            return 0;
        }

        float value = FloatArgumentType.getFloat(context, "value");
        float oldValue = ageData.getInstability();
        ageData.setInstability(value);

        source.sendSuccess(() -> Component.literal(String.format(
                "Instability: %.2f -> %.2f", oldValue, value)), true);
        return 1;
    }

    /**
     * Sets instability to a specific value for a specified Age.
     */
    private static int setInstabilityAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        float value = FloatArgumentType.getFloat(context, "value");
        int ageId = IntegerArgumentType.getInteger(context, "ageId");

        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), ageId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Age " + ageId + " not found."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent(ageLevel);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found for Age " + ageId + "."));
            return 0;
        }

        float oldValue = ageData.getInstability();
        ageData.setInstability(value);

        source.sendSuccess(() -> Component.literal(String.format(
                "Age %d instability: %.2f -> %.2f", ageId, oldValue, value)), true);
        return 1;
    }

    // --- Chunk Regeneration Command ---

    /**
     * Regenerates chunks around the player by clearing and forcing reload.
     */
    private static int regenChunks(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age. Chunk regeneration only works in Ages."));
            return 0;
        }

        BlockPos playerPos = player.blockPosition();
        ChunkPos centerChunk = new ChunkPos(playerPos);

        int regenerated = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int surfaceY = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                                chunkPos.getBlockAt(x, 0, z)).getY();
                        for (int y = 1; y < Math.min(surfaceY + 10, level.getMaxBuildHeight()); y++) {
                            BlockPos pos = chunkPos.getBlockAt(x, y, z);
                            if (!level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                                level.removeBlock(pos, false);
                            }
                        }
                    }
                }
                regenerated++;
            }
        }

        int finalRegenerated = regenerated;
        source.sendSuccess(() -> Component.literal("Cleared " + finalRegenerated + " chunks"), true);
        source.sendSuccess(() -> Component.literal("Note: For full regeneration, re-enter the Age"), false);

        return regenerated;
    }

    // --- Time Control Commands ---

    /**
     * Sets the time in the current Age.
     */
    private static int setTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        level.setDayTime(ticks);

        source.sendSuccess(() -> Component.literal("Set time to " + ticks + " ticks"), true);
        return 1;
    }

    /**
     * Adds time in the current Age.
     */
    private static int addTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        long newTime = level.getDayTime() + ticks;
        level.setDayTime(newTime);

        source.sendSuccess(() -> Component.literal("Added " + ticks + " ticks (new time: " + newTime + ")"), true);
        return 1;
    }

    // --- Weather Control Command ---

    /**
     * Sets the weather in the current Age.
     */
    private static int setWeather(CommandContext<CommandSourceStack> context, String weatherType) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        int duration = 6000; // Default duration: 5 minutes

        switch (weatherType) {
            case "clear" -> {
                level.setWeatherParameters(duration, 0, false, false);
                source.sendSuccess(() -> Component.literal("Weather set to clear"), true);
            }
            case "rain" -> {
                level.setWeatherParameters(0, duration, true, false);
                source.sendSuccess(() -> Component.literal("Weather set to rain"), true);
            }
            case "thunder" -> {
                level.setWeatherParameters(0, duration, true, true);
                source.sendSuccess(() -> Component.literal("Weather set to thunder"), true);
            }
            default -> {
                source.sendFailure(Component.literal("Unknown weather type: " + weatherType));
                return 0;
            }
        }

        return 1;
    }

    // --- Spawn Meteor Command ---

    /**
     * Spawns a meteor at the player's look position.
     */
    private static int spawnMeteor(CommandContext<CommandSourceStack> context, int scale) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        // Get the look position (raytrace)
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(50)); // 50 blocks max range

        HitResult hitResult = level.clip(new net.minecraft.world.level.ClipContext(
                eyePos, targetPos,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));

        Vec3 spawnPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Spawn above the hit position
            spawnPos = hitResult.getLocation().add(0, 30 + scale * 5, 0);
        } else {
            // Spawn at max range, high in the sky
            spawnPos = targetPos.add(0, 50, 0);
        }

        MeteorEntity meteor = new MeteorEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, scale);
        level.addFreshEntity(meteor);

        source.sendSuccess(() -> Component.literal("Spawned meteor (size " + scale + ") at " +
                String.format("%.1f, %.1f, %.1f", spawnPos.x, spawnPos.y, spawnPos.z)), true);
        return 1;
    }

    // --- Reprofile Commands ---

    /**
     * Recalculates instability profile for the current Age.
     */
    private static int reprofileCurrent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
            source.sendFailure(Component.literal("Not in a Mystcraft age."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent(level);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found."));
            return 0;
        }

        return reprofileAge(source, ageData, level, ageData.getAgeUID());
    }

    /**
     * Recalculates instability profile for a specific Age.
     */
    private static int reprofileAge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int ageId = IntegerArgumentType.getInteger(context, "ageId");

        ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(source.getServer(), ageId);
        if (ageLevel == null) {
            source.sendFailure(Component.literal("Age " + ageId + " not found."));
            return 0;
        }

        AgeData ageData = AgeData.getIfPresent(ageLevel);
        if (ageData == null) {
            source.sendFailure(Component.literal("No age data found for Age " + ageId + "."));
            return 0;
        }

        return reprofileAge(source, ageData, ageLevel, ageId);
    }

    /**
     * Common reprofile logic.
     */
    private static int reprofileAge(CommandSourceStack source, AgeData ageData, ServerLevel level, int ageId) {
        float oldInstability = ageData.getInstability();

        // Recalculate instability from pages using AgeBuilder
        List<ItemStack> pages = ageData.getPages();
        if (pages.isEmpty()) {
            source.sendFailure(Component.literal("Age has no pages to reprofile."));
            return 0;
        }

        // Extract symbols from pages and rebuild
        List<ResourceLocation> symbols = new java.util.ArrayList<>();
        for (ItemStack page : pages) {
            ResourceLocation symbol = art.arcane.mystcraft.data.Page.getSymbol(page);
            if (symbol != null) {
                symbols.add(symbol);
            }
        }

        // Create new director and calculate instability
        AgeDirectorImpl director = new AgeDirectorImpl(level.getSeed());
        float calculatedInstability = 0;
        for (ResourceLocation symbolId : symbols) {
            art.arcane.mystcraft.api.symbol.IAgeSymbol symbol = art.arcane.mystcraft.symbol.SymbolRegistry.get(symbolId);
            if (symbol != null) {
                calculatedInstability += symbol.getInstabilityCost();
            }
        }

        // Create final copy for lambda
        final float newInstability = calculatedInstability;

        // Update instability
        ageData.setInstability(newInstability);

        // Clear saved deck orders to force reshuffling
        ageData.clearDeckOrders();

        source.sendSuccess(() -> Component.literal(String.format(
                "Reprofiled Age %d: instability %.2f -> %.2f", ageId, oldInstability, newInstability)), true);

        return 1;
    }
}
