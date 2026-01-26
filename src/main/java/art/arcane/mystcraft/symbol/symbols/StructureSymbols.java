package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;

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
            director.addInstability(getInstabilityCost());
        }
    }

    // ========================= Overworld Structures =========================

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
        }
    }

    // ========================= Underground Structures =========================

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
            director.addInstability(getInstabilityCost());
        }
    }

    // ========================= Nether Structures =========================

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
            director.addInstability(getInstabilityCost());
        }
    }

    // ========================= End Structures =========================

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
            director.addInstability(getInstabilityCost());
        }
    }
}
