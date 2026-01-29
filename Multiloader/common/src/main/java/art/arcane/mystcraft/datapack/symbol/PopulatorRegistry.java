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

  private PopulatorRegistry() {
  }

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
    register(myst("dense_ores"), (seed, json) -> new DenseOresPopulator(seed, json));
    register(myst("huge_trees"), (seed, json) -> new HugeTreePopulator(seed, json));
    register(myst("deep_lakes"), (seed, json) -> new DeepLakesPopulator(seed, json));
    register(myst("surface_lakes"), (seed, json) -> new SurfaceLakesPopulator(seed, json));
    register(myst("spikes"), (seed, json) -> new SpikesPopulator(seed, json));
    register(myst("spheres"), (seed, json) -> new SpheresPopulator(seed, json));
    register(myst("tendrils"), (seed, json) -> new TendrilsPopulator(seed, json));
    register(myst("vertical_tendrils"), (seed, json) -> new VerticalTendrilsPopulator(seed, json));
    register(myst("perlin_worms"), (seed, json) -> new PerlinWormsPopulator(seed, json));
    register(myst("block_shuffle_terrain"), (seed, json) -> new BlockShufflePopulator(seed, BlockShufflePopulator.Mode.TERRAIN, json));
    register(myst("block_shuffle_global"), (seed, json) -> new BlockShufflePopulator(seed, BlockShufflePopulator.Mode.GLOBAL, json));
    register(myst("dripstone_caves"), (seed, json) -> new DripstoneCavesPopulator(seed, json));
    register(myst("lush_caves"), (seed, json) -> new LushCavesPopulator(seed, json));
    register(myst("deep_dark"), (seed, json) -> new DeepDarkPopulator(seed, json));
    register(myst("star_fissure"), (seed, json) -> new StarFissurePopulator(seed, json));

    register(myst("villages"), (seed, json) -> new VillagesPopulator(seed, json));
    register(myst("dungeons"), (seed, json) -> new DungeonPopulator(seed, json));
    register(myst("mineshafts"), (seed, json) -> new MineshaftsPopulator(seed, json));
    register(myst("strongholds"), (seed, json) -> new StrongholdsPopulator(seed, json));
    register(myst("nether_fortress"), (seed, json) -> new NetherFortressPopulator(seed, json));
    register(myst("pillager_outposts"), (seed, json) -> new PillagerOutpostsPopulator(seed, json));
    register(myst("ruined_portals"), (seed, json) -> new RuinedPortalsPopulator(seed, json));
    register(myst("ocean_monuments"), (seed, json) -> new OceanMonumentsPopulator(seed, json));
    register(myst("witch_huts"), (seed, json) -> new WitchHutsPopulator(seed, json));
    register(myst("desert_temples"), (seed, json) -> new DesertTemplesPopulator(seed, json));
    register(myst("jungle_temples"), (seed, json) -> new JungleTemplesPopulator(seed, json));
    register(myst("woodland_mansions"), (seed, json) -> new WoodlandMansionsPopulator(seed, json));
    register(myst("trail_ruins"), (seed, json) -> new TrailRuinsPopulator(seed, json));
    register(myst("ancient_cities"), (seed, json) -> new AncientCitiesPopulator(seed, json));
    register(myst("bastion_remnants"), (seed, json) -> new BastionRemnantsPopulator(seed, json));
    register(myst("end_cities"), (seed, json) -> new EndCitiesPopulator(seed, json));
    register(myst("igloos"), (seed, json) -> new IglooPopulator(seed, json));
    register(myst("shipwrecks"), (seed, json) -> new ShipwreckPopulator(seed, json));
    register(myst("ocean_ruins"), (seed, json) -> new OceanRuinsPopulator(seed, json));
    register(myst("buried_treasure"), (seed, json) -> new BuriedTreasurePopulator(seed, json));
    register(myst("nether_fossils"), (seed, json) -> new NetherFossilPopulator(seed, json));

    register(myst("biome_decoration"), (seed, json) -> new BiomeDecorationPopulator(seed, json));
    register(myst("standard_ores"), (seed, json) -> new StandardOresPopulator(seed, json));

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
