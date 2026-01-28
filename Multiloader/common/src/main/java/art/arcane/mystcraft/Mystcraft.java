package art.arcane.mystcraft;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Common entry point for Mystcraft across all platforms.
 * Platform-specific entry points call into this class.
 */
public final class Mystcraft {
    public static final String MOD_ID = "mystcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private static MinecraftServer currentServer;

    private Mystcraft() {}

    /**
     * Sets the current server instance. Called by platform entry points on server start/stop.
     */
    public static void setCurrentServer(@Nullable MinecraftServer server) {
        currentServer = server;
    }

    /**
     * Gets the current server instance, or null if no server is running.
     */
    @Nullable
    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    /** Called by platform entry points after registry setup is complete. */
    public static void init() {
        LOGGER.info("[Mystcraft] Common initialization complete");
    }

    /** Called during common setup phase (deferred work). */
    public static void commonSetup() {
        LOGGER.info("[Mystcraft] Common setup");

        // Initialize ink effects registry
        art.arcane.mystcraft.data.InkEffects.init();

        // Register all built-in symbols
        art.arcane.mystcraft.symbol.ModSymbols.registerAll();

        // Freeze symbol registry to prevent late registration
        art.arcane.mystcraft.symbol.SymbolRegistry.freeze();

        // Initialize grammar rules for CFG-based Age generation
        art.arcane.mystcraft.grammar.GrammarRules.initialize();

        // Initialize instability providers and decks
        art.arcane.mystcraft.instability.InstabilityData.initialize();
    }
}
