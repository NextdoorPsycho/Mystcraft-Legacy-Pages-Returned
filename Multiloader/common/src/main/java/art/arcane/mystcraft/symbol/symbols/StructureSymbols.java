package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.populate.BastionRemnantsPopulator;
import art.arcane.mystcraft.world.gen.populate.AncientCitiesPopulator;
import art.arcane.mystcraft.world.gen.populate.DesertTemplesPopulator;
import art.arcane.mystcraft.world.gen.populate.DungeonPopulator;
import art.arcane.mystcraft.world.gen.populate.EndCitiesPopulator;
import art.arcane.mystcraft.world.gen.populate.JungleTemplesPopulator;
import art.arcane.mystcraft.world.gen.populate.MineshaftsPopulator;
import art.arcane.mystcraft.world.gen.populate.OceanMonumentsPopulator;
import art.arcane.mystcraft.world.gen.populate.PillagerOutpostsPopulator;
import art.arcane.mystcraft.world.gen.populate.RuinedPortalsPopulator;
import art.arcane.mystcraft.world.gen.populate.NetherFortressPopulator;
import art.arcane.mystcraft.world.gen.populate.StrongholdsPopulator;
import art.arcane.mystcraft.world.gen.populate.TrailRuinsPopulator;
import art.arcane.mystcraft.world.gen.populate.VillagesPopulator;
import art.arcane.mystcraft.world.gen.populate.WitchHutsPopulator;
import art.arcane.mystcraft.world.gen.populate.WoodlandMansionsPopulator;
import art.arcane.mystcraft.world.gen.populate.IglooPopulator;
import art.arcane.mystcraft.world.gen.populate.ShipwreckPopulator;
import art.arcane.mystcraft.world.gen.populate.OceanRuinsPopulator;
import art.arcane.mystcraft.world.gen.populate.BuriedTreasurePopulator;
import art.arcane.mystcraft.world.gen.populate.NetherFossilPopulator;

/**
 * Structure generation symbols.
 */
public final class StructureSymbols {

    private StructureSymbols() {}

    public static void register() {
        // Classic structures
        SymbolRegistry.register(new Villages());
        SymbolRegistry.register(new Dungeons());
        SymbolRegistry.register(new Mineshafts());
        SymbolRegistry.register(new Strongholds());
        SymbolRegistry.register(new NetherFortress());

        // Overworld structures
        SymbolRegistry.register(new PillagerOutposts());
        SymbolRegistry.register(new RuinedPortals());
        SymbolRegistry.register(new OceanMonuments());
        SymbolRegistry.register(new WitchHuts());
        SymbolRegistry.register(new DesertTemples());
        SymbolRegistry.register(new JungleTemples());
        SymbolRegistry.register(new WoodlandMansions());
        SymbolRegistry.register(new TrailRuins());

        // Underground structures
        SymbolRegistry.register(new AncientCities());

        // Nether structures
        SymbolRegistry.register(new BastionRemnants());

        // End structures
        SymbolRegistry.register(new EndCities());

        // Additional overworld structures
        SymbolRegistry.register(new Igloos());
        SymbolRegistry.register(new Shipwrecks());
        SymbolRegistry.register(new OceanRuins());
        SymbolRegistry.register(new BuriedTreasure());

        // Additional nether structures
        SymbolRegistry.register(new NetherFossils());
    }

    public static class Villages extends SymbolBase {
        public Villages() {
            super(SymbolRegistry.mystcraftId("villages"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Society", "Harmony", "Nurture");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setVillagesEnabled(true);
            director.registerInterface(new VillagesPopulator(seed));
        }
    }

    public static class Dungeons extends SymbolBase {
        public Dungeons() {
            super(SymbolRegistry.mystcraftId("dungeons"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Constraint", "Chain", "Resurrect");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDungeonsEnabled(true);

            // Register the dungeon populator for mob spawner dungeon generation
            director.registerInterface(new DungeonPopulator(seed));
        }
    }

    public static class Mineshafts extends SymbolBase {
        public Mineshafts() {
            super(SymbolRegistry.mystcraftId("mineshafts"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Machine", "Motion", "Tradition");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setMineshaftsEnabled(true);
            director.registerInterface(new MineshaftsPopulator(seed));
        }
    }

    public static class Strongholds extends SymbolBase {
        public Strongholds() {
            super(SymbolRegistry.mystcraftId("strongholds"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Wisdom", "Future", "Honor");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setStrongholdsEnabled(true);
            director.registerInterface(new StrongholdsPopulator(seed));
        }
    }

    public static class NetherFortress extends SymbolBase {
        public NetherFortress() {
            super(SymbolRegistry.mystcraftId("nether_fortress"), SymbolCategory.STRUCTURE);
            setCardRank(4);
            setInstabilityCost(10.0f);
            setPoem("Civilization", "Chaos", "Fire", "Power");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setNetherFortEnabled(true);

            // Register the nether fortress populator for custom structure generation
            director.registerInterface(new NetherFortressPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Overworld Structures ---

    public static class PillagerOutposts extends SymbolBase {
        public PillagerOutposts() {
            super(SymbolRegistry.mystcraftId("pillager_outposts"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(5.0f);
            setPoem("Civilization", "Chaos", "Conflict", "Watch");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setPillagerOutpostsEnabled(true);
            director.registerInterface(new PillagerOutpostsPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class RuinedPortals extends SymbolBase {
        public RuinedPortals() {
            super(SymbolRegistry.mystcraftId("ruined_portals"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(8.0f);
            setPoem("Civilization", "Void", "Gateway", "Decay");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setRuinedPortalsEnabled(true);
            director.registerInterface(new RuinedPortalsPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class OceanMonuments extends SymbolBase {
        public OceanMonuments() {
            super(SymbolRegistry.mystcraftId("ocean_monuments"), SymbolCategory.STRUCTURE);
            setCardRank(4);
            setInstabilityCost(5.0f);
            setPoem("Civilization", "Water", "Guardian", "Treasure");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOceanMonumentsEnabled(true);
            director.registerInterface(new OceanMonumentsPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    public static class WitchHuts extends SymbolBase {
        public WitchHuts() {
            super(SymbolRegistry.mystcraftId("witch_huts"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Civilization", "Magic", "Swamp", "Hermit");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWitchHutsEnabled(true);
            director.addInstability(getInstabilityCost());

            director.registerInterface(new WitchHutsPopulator(seed));
        }
    }

    public static class DesertTemples extends SymbolBase {
        public DesertTemples() {
            super(SymbolRegistry.mystcraftId("desert_temples"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Civilization", "Sand", "Treasure", "Trap");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setDesertTemplesEnabled(true);
            director.addInstability(getInstabilityCost());

            director.registerInterface(new DesertTemplesPopulator(seed));
        }
    }

    public static class JungleTemples extends SymbolBase {
        public JungleTemples() {
            super(SymbolRegistry.mystcraftId("jungle_temples"), SymbolCategory.STRUCTURE);
            setCardRank(3);
            setInstabilityCost(3.0f);
            setPoem("Civilization", "Nature", "Puzzle", "Ancient");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setJungleTemplesEnabled(true);
            director.addInstability(getInstabilityCost());

            director.registerInterface(new JungleTemplesPopulator(seed));
        }
    }

    public static class WoodlandMansions extends SymbolBase {
        public WoodlandMansions() {
            super(SymbolRegistry.mystcraftId("woodland_mansions"), SymbolCategory.STRUCTURE);
            setCardRank(4);
            setInstabilityCost(8.0f);
            setPoem("Civilization", "Dark", "Illager", "Mansion");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setWoodlandMansionsEnabled(true);
            director.addInstability(getInstabilityCost());

            director.registerInterface(new WoodlandMansionsPopulator(seed));
        }
    }

    public static class TrailRuins extends SymbolBase {
        public TrailRuins() {
            super(SymbolRegistry.mystcraftId("trail_ruins"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Past", "Explore", "Artifact");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setTrailRuinsEnabled(true);
            director.registerInterface(new TrailRuinsPopulator(seed));
        }
    }

    // --- Underground Structures ---

    public static class AncientCities extends SymbolBase {
        public AncientCities() {
            super(SymbolRegistry.mystcraftId("ancient_cities"), SymbolCategory.STRUCTURE);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Civilization", "Dark", "Sculk", "Echo");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setAncientCitiesEnabled(true);
            director.registerInterface(new AncientCitiesPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Nether Structures ---

    public static class BastionRemnants extends SymbolBase {
        public BastionRemnants() {
            super(SymbolRegistry.mystcraftId("bastion_remnants"), SymbolCategory.STRUCTURE);
            setCardRank(4);
            setInstabilityCost(12.0f);
            setPoem("Civilization", "Fire", "Piglin", "Gold");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBastionRemnantsEnabled(true);

            // Register the bastion remnants populator for custom structure generation
            director.registerInterface(new BastionRemnantsPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    // --- End Structures ---

    public static class EndCities extends SymbolBase {
        public EndCities() {
            super(SymbolRegistry.mystcraftId("end_cities"), SymbolCategory.STRUCTURE);
            setCardRank(4);
            setInstabilityCost(15.0f);
            setPoem("Civilization", "Void", "Shulker", "Elytra");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setEndCitiesEnabled(true);

            // Register the end cities populator for custom structure generation
            director.registerInterface(new EndCitiesPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }

    // --- Additional Overworld Structures ---

    public static class Igloos extends SymbolBase {
        public Igloos() {
            super(SymbolRegistry.mystcraftId("igloos"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Ice", "Shelter", "Snow");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setIgloosEnabled(true);
            director.registerInterface(new IglooPopulator(seed));
        }
    }

    public static class Shipwrecks extends SymbolBase {
        public Shipwrecks() {
            super(SymbolRegistry.mystcraftId("shipwrecks"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Water", "Journey", "Wreck");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setShipwrecksEnabled(true);
            director.registerInterface(new ShipwreckPopulator(seed));
        }
    }

    public static class OceanRuins extends SymbolBase {
        public OceanRuins() {
            super(SymbolRegistry.mystcraftId("ocean_ruins"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Water", "Past", "Stone");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setOceanRuinsEnabled(true);
            director.registerInterface(new OceanRuinsPopulator(seed));
        }
    }

    public static class BuriedTreasure extends SymbolBase {
        public BuriedTreasure() {
            super(SymbolRegistry.mystcraftId("buried_treasure"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(0.0f);
            setPoem("Civilization", "Earth", "Treasure", "Hidden");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setBuriedTreasureEnabled(true);
            director.registerInterface(new BuriedTreasurePopulator(seed));
        }
    }

    // --- Additional Nether Structures ---

    public static class NetherFossils extends SymbolBase {
        public NetherFossils() {
            super(SymbolRegistry.mystcraftId("nether_fossils"), SymbolCategory.STRUCTURE);
            setCardRank(2);
            setInstabilityCost(3.0f);
            setPoem("Civilization", "Death", "Bone", "Ancient");
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setNetherFossilsEnabled(true);
            director.registerInterface(new NetherFossilPopulator(seed));
            director.addInstability(getInstabilityCost());
        }
    }
}
