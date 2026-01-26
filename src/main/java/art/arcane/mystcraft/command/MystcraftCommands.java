package art.arcane.mystcraft.command;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.Set;

/**
 * Mystcraft commands for managing ages and symbols.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class MystcraftCommands {

    private static final SuggestionProvider<CommandSourceStack> SYMBOL_SUGGESTIONS = (context, builder) -> {
        Collection<IAgeSymbol> symbols = SymbolRegistry.getAll();
        Set<ResourceLocation> symbolIds = new java.util.HashSet<>();
        for (IAgeSymbol symbol : symbols) {
            symbolIds.add(symbol.getRegistryName());
        }
        return SharedSuggestionProvider.suggestResource(symbolIds, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AGE_SUGGESTIONS = (context, builder) -> {
        AgeManager ageManager = AgeManager.get(context.getSource().getServer());
        java.util.List<String> ageIds = new java.util.ArrayList<>();
        for (Integer uid : ageManager.getAllAgeUIDs()) {
            ageIds.add(String.valueOf(uid));
        }
        return SharedSuggestionProvider.suggest(ageIds, builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

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
}
