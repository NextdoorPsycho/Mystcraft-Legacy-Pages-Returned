package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.world.gen.feature.MapGenCavesMyst;
import art.arcane.mystcraft.world.gen.feature.MapGenFloatingIslands;
import art.arcane.mystcraft.world.gen.feature.MapGenRavineMyst;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of datapack terrain alteration factories.
 */
public final class TerrainAlterationRegistry {

    private static final Map<ResourceLocation, TerrainAlterationFactory> FACTORIES = new HashMap<>();

    private TerrainAlterationRegistry() {}

    public static void register(ResourceLocation id, TerrainAlterationFactory factory) {
        if (id == null || factory == null) return;
        TerrainAlterationFactory existing = FACTORIES.putIfAbsent(id, factory);
        if (existing != null) {
            Mystcraft.LOGGER.warn("[TerrainAlterationRegistry] Duplicate alteration factory {} ignored", id);
        }
    }

    public static ITerrainAlteration create(ResourceLocation id, JsonObject json, long seed) {
        TerrainAlterationFactory factory = FACTORIES.get(id);
        if (factory == null) {
            Mystcraft.LOGGER.warn("[TerrainAlterationRegistry] Unknown terrain alteration {}", id);
            return null;
        }
        return factory.create(seed, json);
    }

    public static Map<ResourceLocation, TerrainAlterationFactory> getAll() {
        return Collections.unmodifiableMap(FACTORIES);
    }

    public static void registerDefaults() {
        register(myst("caves"), TerrainAlterationRegistry::createCaves);
        register(myst("ravines"), TerrainAlterationRegistry::createRavines);
        register(myst("floating_islands"), TerrainAlterationRegistry::createFloatingIslands);
    }

    private static ITerrainAlteration createCaves(long seed, JsonObject json) {
        int rate = GsonHelper.getAsInt(json, "rate", 15);
        int size = GsonHelper.getAsInt(json, "size", 40);
        BlockState fill = resolveBlockState(GsonHelper.getAsString(json, "fill_block", "minecraft:air"), Blocks.AIR);
        return new MapGenCavesMyst(seed, rate, size, fill);
    }

    private static ITerrainAlteration createRavines(long seed, JsonObject json) {
        int rate = GsonHelper.getAsInt(json, "rate", 50);
        BlockState fill = resolveBlockState(GsonHelper.getAsString(json, "fill_block", "minecraft:air"), Blocks.AIR);
        return new MapGenRavineMyst(seed, rate, fill);
    }

    private static ITerrainAlteration createFloatingIslands(long seed, JsonObject json) {
        int density = GsonHelper.getAsInt(json, "density", 10);
        BlockState structure = resolveBlockState(GsonHelper.getAsString(json, "structure_block", "minecraft:stone"), Blocks.STONE);
        BlockState surface = resolveBlockState(GsonHelper.getAsString(json, "surface_block", "minecraft:grass_block"), Blocks.GRASS_BLOCK);
        return new MapGenFloatingIslands(seed, density, structure, surface);
    }

    private static BlockState resolveBlockState(String raw, Block fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback != null ? fallback.defaultBlockState() : null;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            return fallback != null ? fallback.defaultBlockState() : null;
        }
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return fallback != null ? fallback.defaultBlockState() : null;
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block.defaultBlockState();
    }

    private static ResourceLocation myst(String path) {
        return new ResourceLocation(Mystcraft.MOD_ID, path);
    }
}
