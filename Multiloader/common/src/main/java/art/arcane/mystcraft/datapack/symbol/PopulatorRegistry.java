package art.arcane.mystcraft.datapack.symbol;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.world.gen.populate.*;
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
 * Registry of datapack-populator factories.
 */
public final class PopulatorRegistry {

    private static final Map<ResourceLocation, PopulatorFactory> FACTORIES = new HashMap<>();

    private PopulatorRegistry() {}

    public static void register(ResourceLocation id, PopulatorFactory factory) {
        if (id == null || factory == null) return;
        PopulatorFactory existing = FACTORIES.putIfAbsent(id, factory);
        if (existing != null) {
            Mystcraft.LOGGER.warn("[PopulatorRegistry] Duplicate populator factory {} ignored", id);
        }
    }

    public static IPopulate create(ResourceLocation id, JsonObject json, long seed) {
        PopulatorFactory factory = FACTORIES.get(id);
        if (factory == null) {
            Mystcraft.LOGGER.warn("[PopulatorRegistry] Unknown populator {}", id);
            return null;
        }
        return factory.create(seed, json);
    }

    public static Map<ResourceLocation, PopulatorFactory> getAll() {
        return Collections.unmodifiableMap(FACTORIES);
    }

    public static void registerDefaults() {
        register(myst("dense_ores"), (seed, json) -> new DenseOresPopulator(seed));
        register(myst("huge_trees"), (seed, json) -> new HugeTreePopulator(seed));
        register(myst("deep_lakes"), (seed, json) -> new DeepLakesPopulator(seed));
        register(myst("surface_lakes"), (seed, json) -> new SurfaceLakesPopulator(seed));
        register(myst("spikes"), (seed, json) -> new SpikesPopulator(seed));
        register(myst("spheres"), (seed, json) -> new SpheresPopulator(seed));
        register(myst("tendrils"), (seed, json) -> new TendrilsPopulator(seed));
        register(myst("vertical_tendrils"), (seed, json) -> new VerticalTendrilsPopulator(seed));
        register(myst("perlin_worms"), (seed, json) -> new PerlinWormsPopulator(seed));
        register(myst("dripstone_caves"), (seed, json) -> new DripstoneCavesPopulator(seed));
        register(myst("lush_caves"), (seed, json) -> new LushCavesPopulator(seed));
        register(myst("deep_dark"), (seed, json) -> new DeepDarkPopulator(seed));
        register(myst("star_fissure"), (seed, json) -> new StarFissurePopulator(seed));

        register(myst("villages"), (seed, json) -> new VillagesPopulator(seed));
        register(myst("dungeons"), (seed, json) -> new DungeonPopulator(seed));
        register(myst("mineshafts"), (seed, json) -> new MineshaftsPopulator(seed));
        register(myst("strongholds"), (seed, json) -> new StrongholdsPopulator(seed));
        register(myst("nether_fortress"), (seed, json) -> new NetherFortressPopulator(seed));
        register(myst("pillager_outposts"), (seed, json) -> new PillagerOutpostsPopulator(seed));
        register(myst("ruined_portals"), (seed, json) -> new RuinedPortalsPopulator(seed));
        register(myst("ocean_monuments"), (seed, json) -> new OceanMonumentsPopulator(seed));
        register(myst("witch_huts"), (seed, json) -> new WitchHutsPopulator(seed));
        register(myst("desert_temples"), (seed, json) -> new DesertTemplesPopulator(seed));
        register(myst("jungle_temples"), (seed, json) -> new JungleTemplesPopulator(seed));
        register(myst("woodland_mansions"), (seed, json) -> new WoodlandMansionsPopulator(seed));
        register(myst("trail_ruins"), (seed, json) -> new TrailRuinsPopulator(seed));
        register(myst("ancient_cities"), (seed, json) -> new AncientCitiesPopulator(seed));
        register(myst("bastion_remnants"), (seed, json) -> new BastionRemnantsPopulator(seed));
        register(myst("end_cities"), (seed, json) -> new EndCitiesPopulator(seed));
        register(myst("igloos"), (seed, json) -> new IglooPopulator(seed));
        register(myst("shipwrecks"), (seed, json) -> new ShipwreckPopulator(seed));
        register(myst("ocean_ruins"), (seed, json) -> new OceanRuinsPopulator(seed));
        register(myst("buried_treasure"), (seed, json) -> new BuriedTreasurePopulator(seed));
        register(myst("nether_fossils"), (seed, json) -> new NetherFossilPopulator(seed));

        register(myst("biome_decoration"), (seed, json) -> new BiomeDecorationPopulator(seed));
        register(myst("standard_ores"), (seed, json) -> new StandardOresPopulator(seed));

        register(myst("single_ore"), PopulatorRegistry::createSingleOrePopulator);
    }

    private static IPopulate createSingleOrePopulator(long seed, JsonObject json) {
        String oreRaw = GsonHelper.getAsString(json, "ore_block");
        String deepslateRaw = GsonHelper.getAsString(json, "deepslate_block", "");
        int veinSize = GsonHelper.getAsInt(json, "vein_size", 8);
        int veinsPerChunk = GsonHelper.getAsInt(json, "veins_per_chunk", 4);
        int minY = GsonHelper.getAsInt(json, "min_y", -64);
        int maxY = GsonHelper.getAsInt(json, "max_y", 64);
        String identifier = GsonHelper.getAsString(json, "identifier", "single_ore");

        BlockState oreBlock = resolveBlockState(oreRaw, Blocks.STONE);
        BlockState deepslateBlock = resolveBlockState(deepslateRaw, null);
        if (deepslateBlock == null) {
            deepslateBlock = oreBlock;
        }

        return new SingleOrePopulator(oreBlock, deepslateBlock, veinSize, veinsPerChunk, minY, maxY, identifier);
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
