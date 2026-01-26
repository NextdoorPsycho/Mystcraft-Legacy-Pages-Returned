package art.arcane.mystcraft.world;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.api.world.logic.IBiomeController;
import art.arcane.mystcraft.api.world.logic.ICelestial;
import art.arcane.mystcraft.api.world.logic.IChunkProviderFinalization;
import art.arcane.mystcraft.api.world.logic.ILightingController;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.api.world.logic.ITerrainGenerator;
import art.arcane.mystcraft.api.world.logic.IWeatherController;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Implementation of AgeDirector that collects symbol configurations.
 * Used during Age creation to build world properties.
 */
public class AgeDirectorImpl implements AgeDirector {

    private final long seed;
    private float instability = 0.0f;

    // Terrain
    private String terrainType = "normal";
    private int averageGroundLevel = 64;
    private int seaLevel = 63;
    private boolean hasSea = true;
    private BlockState terrainBlock = Blocks.STONE.defaultBlockState();
    private BlockState seaBlock = Blocks.WATER.defaultBlockState();

    // Biomes
    private String biomeController = "native";
    private final List<Holder<Biome>> biomes = new ArrayList<>();

    // Celestials
    private boolean sunVisible = true;
    private boolean moonVisible = true;
    private boolean starsVisible = true;
    private String starType = "normal";

    // Weather
    private String weatherType = "normal";

    // Lighting
    private String lightingType = "normal";

    // Colors (-1 means use default)
    private int skyColor = -1;
    private int fogColor = -1;
    private int grassColor = -1;
    private int foliageColor = -1;
    private int waterColor = -1;
    private int cloudColor = -1;
    private int nightSkyColor = -1;
    private boolean skyColorNatural = true;
    private boolean fogColorNatural = true;
    private boolean grassColorNatural = true;
    private boolean foliageColorNatural = true;
    private boolean waterColorNatural = true;
    private boolean cloudColorNatural = true;

    // Features
    private boolean cavesEnabled = true;
    private boolean ravinesEnabled = true;
    private boolean floatingIslandsEnabled = false;
    private boolean skylandsEnabled = false;

    // Structures
    private boolean villagesEnabled = true;
    private boolean dungeonsEnabled = true;
    private boolean mineshaftsEnabled = true;
    private boolean strongholdsEnabled = true;

    // Environment
    private boolean acceleratedEnabled = false;
    private boolean meteorsEnabled = false;
    private boolean lightningEnabled = false;
    private boolean scorchedEnabled = false;

    // Additional features/structures
    private boolean crystalsEnabled = false;
    private boolean deepLakesEnabled = true;
    private boolean denseOresEnabled = false;
    private boolean explosionsEnabled = false;
    private boolean horizonHidden = false;
    private boolean hugeTreesEnabled = false;
    private boolean netherFortEnabled = false;
    private boolean obelisksEnabled = false;
    private boolean pvpEnabled = true;
    private boolean rainbowEnabled = false;
    private boolean spheresEnabled = false;
    private boolean spikesEnabled = false;
    private boolean starFissureEnabled = false;
    private boolean surfaceLakesEnabled = true;
    private boolean tendrilsEnabled = false;

    // New structures (1.20+)
    private boolean pillagerOutpostsEnabled = false;
    private boolean ruinedPortalsEnabled = false;
    private boolean ancientCitiesEnabled = false;
    private boolean trailRuinsEnabled = false;
    private boolean oceanMonumentsEnabled = false;
    private boolean witchHutsEnabled = false;
    private boolean desertTemplesEnabled = false;
    private boolean jungleTemplesEnabled = false;
    private boolean woodlandMansionsEnabled = false;
    private boolean endCitiesEnabled = false;
    private boolean bastionRemnantsEnabled = false;

    // Cave features
    private boolean dripstoneCavesEnabled = false;
    private boolean lushCavesEnabled = false;
    private boolean deepDarkEnabled = false;

    // Gradient colors
    private int sunsetColor = -1;

    // Modifier stacks
    private final Deque<Integer> colorStack = new ArrayDeque<>();
    private final Deque<Float> angleStack = new ArrayDeque<>();
    private final Deque<Float> lengthStack = new ArrayDeque<>();
    private final Deque<Float> phaseStack = new ArrayDeque<>();
    private final Deque<Holder<Biome>> biomeStack = new ArrayDeque<>();
    private final Deque<Integer> gradientStack = new ArrayDeque<>();

    // Registered logic interfaces (for full world generation pipeline)
    private ITerrainGenerator terrainGenerator;
    private IBiomeController biomeControllerImpl;
    private ILightingController lightingControllerImpl;
    private IWeatherController weatherControllerImpl;
    private final List<ITerrainAlteration> terrainAlterations = new ArrayList<>();
    private final List<IChunkProviderFinalization> chunkFinalizers = new ArrayList<>();
    private final List<IPopulate> populateFunctions = new ArrayList<>();
    private final List<ICelestial> celestials = new ArrayList<>();

    public AgeDirectorImpl() {
        this(0L);
    }

    public AgeDirectorImpl(long seed) {
        this.seed = seed;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public float getInstability() {
        return instability;
    }

    @Override
    public void addInstability(float amount) {
        this.instability += amount;
    }

    /**
     * Sets the instability value directly.
     */
    public void setInstability(float instability) {
        this.instability = instability;
    }

    // ========================= Terrain =========================

    @Override
    public void setTerrainType(String type) {
        this.terrainType = type;
    }

    @Override
    public String getTerrainType() {
        return terrainType;
    }

    @Override
    public void setAverageGroundLevel(int level) {
        this.averageGroundLevel = level;
    }

    @Override
    public int getAverageGroundLevel() {
        return averageGroundLevel;
    }

    @Override
    public void setSeaLevel(int level) {
        this.seaLevel = level;
    }

    @Override
    public int getSeaLevel() {
        return seaLevel;
    }

    @Override
    public void setHasSea(boolean hasSea) {
        this.hasSea = hasSea;
    }

    @Override
    public boolean hasSea() {
        return hasSea;
    }

    @Override
    public void setTerrainBlock(BlockState block) {
        this.terrainBlock = block;
    }

    @Override
    public BlockState getTerrainBlock() {
        return terrainBlock;
    }

    @Override
    public void setSeaBlock(BlockState block) {
        this.seaBlock = block;
    }

    @Override
    public BlockState getSeaBlock() {
        return seaBlock;
    }

    // ========================= Biomes =========================

    @Override
    public void setBiomeController(String type) {
        this.biomeController = type;
    }

    @Override
    public String getBiomeController() {
        return biomeController;
    }

    @Override
    public void addBiome(Holder<Biome> biome) {
        biomes.add(biome);
    }

    @Override
    public List<Holder<Biome>> getBiomes() {
        return biomes;
    }

    // ========================= Celestials =========================

    @Override
    public void setSunVisible(boolean visible) {
        this.sunVisible = visible;
    }

    public boolean isSunVisible() {
        return sunVisible;
    }

    @Override
    public void setMoonVisible(boolean visible) {
        this.moonVisible = visible;
    }

    public boolean isMoonVisible() {
        return moonVisible;
    }

    @Override
    public void setStarsVisible(boolean visible) {
        this.starsVisible = visible;
    }

    public boolean areStarsVisible() {
        return starsVisible;
    }

    @Override
    public void setStarType(String type) {
        this.starType = type;
    }

    public String getStarType() {
        return starType;
    }

    // ========================= Weather =========================

    @Override
    public void setWeatherType(String type) {
        this.weatherType = type;
    }

    @Override
    public String getWeatherType() {
        return weatherType;
    }

    // ========================= Lighting =========================

    @Override
    public void setLightingType(String type) {
        this.lightingType = type;
    }

    @Override
    public String getLightingType() {
        return lightingType;
    }

    // ========================= Colors =========================

    @Override
    public void setSkyColor(int color) {
        this.skyColor = color;
    }

    @Override
    public int getSkyColor() {
        return skyColor;
    }

    @Override
    public void setFogColor(int color) {
        this.fogColor = color;
    }

    @Override
    public int getFogColor() {
        return fogColor;
    }

    @Override
    public void setGrassColor(int color) {
        this.grassColor = color;
    }

    @Override
    public int getGrassColor() {
        return grassColor;
    }

    @Override
    public void setFoliageColor(int color) {
        this.foliageColor = color;
    }

    @Override
    public int getFoliageColor() {
        return foliageColor;
    }

    @Override
    public void setWaterColor(int color) {
        this.waterColor = color;
    }

    @Override
    public int getWaterColor() {
        return waterColor;
    }

    @Override
    public void setCloudColor(int color) {
        this.cloudColor = color;
    }

    @Override
    public int getCloudColor() {
        return cloudColor;
    }

    @Override
    public void setNightSkyColor(int color) {
        this.nightSkyColor = color;
    }

    @Override
    public int getNightSkyColor() {
        return nightSkyColor;
    }

    @Override
    public void setSkyColorNatural(boolean natural) {
        this.skyColorNatural = natural;
    }

    public boolean isSkyColorNatural() {
        return skyColorNatural;
    }

    @Override
    public void setFogColorNatural(boolean natural) {
        this.fogColorNatural = natural;
    }

    public boolean isFogColorNatural() {
        return fogColorNatural;
    }

    @Override
    public void setGrassColorNatural(boolean natural) {
        this.grassColorNatural = natural;
    }

    public boolean isGrassColorNatural() {
        return grassColorNatural;
    }

    @Override
    public void setFoliageColorNatural(boolean natural) {
        this.foliageColorNatural = natural;
    }

    public boolean isFoliageColorNatural() {
        return foliageColorNatural;
    }

    @Override
    public void setWaterColorNatural(boolean natural) {
        this.waterColorNatural = natural;
    }

    public boolean isWaterColorNatural() {
        return waterColorNatural;
    }

    @Override
    public void setCloudColorNatural(boolean natural) {
        this.cloudColorNatural = natural;
    }

    public boolean isCloudColorNatural() {
        return cloudColorNatural;
    }

    // ========================= Features =========================

    @Override
    public void setCavesEnabled(boolean enabled) {
        this.cavesEnabled = enabled;
    }

    public boolean areCavesEnabled() {
        return cavesEnabled;
    }

    @Override
    public void setRavinesEnabled(boolean enabled) {
        this.ravinesEnabled = enabled;
    }

    public boolean areRavinesEnabled() {
        return ravinesEnabled;
    }

    @Override
    public void setFloatingIslandsEnabled(boolean enabled) {
        this.floatingIslandsEnabled = enabled;
    }

    public boolean areFloatingIslandsEnabled() {
        return floatingIslandsEnabled;
    }

    @Override
    public void setSkylandsEnabled(boolean enabled) {
        this.skylandsEnabled = enabled;
    }

    public boolean areSkylandsEnabled() {
        return skylandsEnabled;
    }

    // ========================= Structures =========================

    @Override
    public void setVillagesEnabled(boolean enabled) {
        this.villagesEnabled = enabled;
    }

    public boolean areVillagesEnabled() {
        return villagesEnabled;
    }

    @Override
    public void setDungeonsEnabled(boolean enabled) {
        this.dungeonsEnabled = enabled;
    }

    public boolean areDungeonsEnabled() {
        return dungeonsEnabled;
    }

    @Override
    public void setMineshaftsEnabled(boolean enabled) {
        this.mineshaftsEnabled = enabled;
    }

    public boolean areMineshaftsEnabled() {
        return mineshaftsEnabled;
    }

    @Override
    public void setStrongholdsEnabled(boolean enabled) {
        this.strongholdsEnabled = enabled;
    }

    public boolean areStrongholdsEnabled() {
        return strongholdsEnabled;
    }

    // ========================= Environment =========================

    @Override
    public void setAcceleratedEnabled(boolean enabled) {
        this.acceleratedEnabled = enabled;
    }

    public boolean isAcceleratedEnabled() {
        return acceleratedEnabled;
    }

    @Override
    public void setMeteorsEnabled(boolean enabled) {
        this.meteorsEnabled = enabled;
    }

    public boolean areMeteorsEnabled() {
        return meteorsEnabled;
    }

    @Override
    public void setLightningEnabled(boolean enabled) {
        this.lightningEnabled = enabled;
    }

    public boolean isLightningEnabled() {
        return lightningEnabled;
    }

    @Override
    public void setScorchedEnabled(boolean enabled) {
        this.scorchedEnabled = enabled;
    }

    public boolean isScorchedEnabled() {
        return scorchedEnabled;
    }

    // ========================= Additional Features =========================

    @Override
    public void setCrystalsEnabled(boolean enabled) {
        this.crystalsEnabled = enabled;
    }

    public boolean areCrystalsEnabled() {
        return crystalsEnabled;
    }

    @Override
    public void setDeepLakesEnabled(boolean enabled) {
        this.deepLakesEnabled = enabled;
    }

    public boolean areDeepLakesEnabled() {
        return deepLakesEnabled;
    }

    @Override
    public void setDenseOresEnabled(boolean enabled) {
        this.denseOresEnabled = enabled;
    }

    public boolean areDenseOresEnabled() {
        return denseOresEnabled;
    }

    @Override
    public void setExplosionsEnabled(boolean enabled) {
        this.explosionsEnabled = enabled;
    }

    public boolean areExplosionsEnabled() {
        return explosionsEnabled;
    }

    @Override
    public void setHorizonHidden(boolean hidden) {
        this.horizonHidden = hidden;
    }

    public boolean isHorizonHidden() {
        return horizonHidden;
    }

    @Override
    public void setHugeTreesEnabled(boolean enabled) {
        this.hugeTreesEnabled = enabled;
    }

    public boolean areHugeTreesEnabled() {
        return hugeTreesEnabled;
    }

    @Override
    public void setNetherFortEnabled(boolean enabled) {
        this.netherFortEnabled = enabled;
    }

    public boolean isNetherFortEnabled() {
        return netherFortEnabled;
    }

    @Override
    public void setObelisksEnabled(boolean enabled) {
        this.obelisksEnabled = enabled;
    }

    public boolean areObelisksEnabled() {
        return obelisksEnabled;
    }

    @Override
    public void setPvPEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
    }

    public boolean isPvPEnabled() {
        return pvpEnabled;
    }

    @Override
    public void setRainbowEnabled(boolean enabled) {
        this.rainbowEnabled = enabled;
    }

    public boolean isRainbowEnabled() {
        return rainbowEnabled;
    }

    @Override
    public void setSpheresEnabled(boolean enabled) {
        this.spheresEnabled = enabled;
    }

    public boolean areSpheresEnabled() {
        return spheresEnabled;
    }

    @Override
    public void setSpikesEnabled(boolean enabled) {
        this.spikesEnabled = enabled;
    }

    public boolean areSpikesEnabled() {
        return spikesEnabled;
    }

    @Override
    public void setStarFissureEnabled(boolean enabled) {
        this.starFissureEnabled = enabled;
    }

    public boolean isStarFissureEnabled() {
        return starFissureEnabled;
    }

    @Override
    public void setSurfaceLakesEnabled(boolean enabled) {
        this.surfaceLakesEnabled = enabled;
    }

    public boolean areSurfaceLakesEnabled() {
        return surfaceLakesEnabled;
    }

    @Override
    public void setTendrilsEnabled(boolean enabled) {
        this.tendrilsEnabled = enabled;
    }

    public boolean areTendrilsEnabled() {
        return tendrilsEnabled;
    }

    // ========================= Modifiers =========================

    @Override
    public void pushColor(int color) {
        colorStack.push(color);
    }

    @Override
    public int popColor() {
        return colorStack.isEmpty() ? -1 : colorStack.pop();
    }

    @Override
    public void pushAngle(float angle) {
        angleStack.push(angle);
    }

    @Override
    public float popAngle() {
        return angleStack.isEmpty() ? 0.0f : angleStack.pop();
    }

    @Override
    public void pushLength(float length) {
        lengthStack.push(length);
    }

    @Override
    public float popLength() {
        return lengthStack.isEmpty() ? 1.0f : lengthStack.pop();
    }

    @Override
    public void pushPhase(float phase) {
        phaseStack.push(phase);
    }

    @Override
    public float popPhase() {
        return phaseStack.isEmpty() ? 0.0f : phaseStack.pop();
    }

    @Override
    public void clearModifiers() {
        colorStack.clear();
        angleStack.clear();
        lengthStack.clear();
        phaseStack.clear();
        biomeStack.clear();
    }

    @Override
    public void pushBiome(Holder<Biome> biome) {
        if (biome != null) {
            biomeStack.push(biome);
        }
    }

    @Override
    public Holder<Biome> popBiome() {
        return biomeStack.isEmpty() ? null : biomeStack.pop();
    }

    // ========================= Additional Structures =========================

    @Override
    public void setPillagerOutpostsEnabled(boolean enabled) {
        this.pillagerOutpostsEnabled = enabled;
    }

    public boolean arePillagerOutpostsEnabled() {
        return pillagerOutpostsEnabled;
    }

    @Override
    public void setRuinedPortalsEnabled(boolean enabled) {
        this.ruinedPortalsEnabled = enabled;
    }

    public boolean areRuinedPortalsEnabled() {
        return ruinedPortalsEnabled;
    }

    @Override
    public void setAncientCitiesEnabled(boolean enabled) {
        this.ancientCitiesEnabled = enabled;
    }

    public boolean areAncientCitiesEnabled() {
        return ancientCitiesEnabled;
    }

    @Override
    public void setTrailRuinsEnabled(boolean enabled) {
        this.trailRuinsEnabled = enabled;
    }

    public boolean areTrailRuinsEnabled() {
        return trailRuinsEnabled;
    }

    @Override
    public void setOceanMonumentsEnabled(boolean enabled) {
        this.oceanMonumentsEnabled = enabled;
    }

    public boolean areOceanMonumentsEnabled() {
        return oceanMonumentsEnabled;
    }

    @Override
    public void setWitchHutsEnabled(boolean enabled) {
        this.witchHutsEnabled = enabled;
    }

    public boolean areWitchHutsEnabled() {
        return witchHutsEnabled;
    }

    @Override
    public void setDesertTemplesEnabled(boolean enabled) {
        this.desertTemplesEnabled = enabled;
    }

    public boolean areDesertTemplesEnabled() {
        return desertTemplesEnabled;
    }

    @Override
    public void setJungleTemplesEnabled(boolean enabled) {
        this.jungleTemplesEnabled = enabled;
    }

    public boolean areJungleTemplesEnabled() {
        return jungleTemplesEnabled;
    }

    @Override
    public void setWoodlandMansionsEnabled(boolean enabled) {
        this.woodlandMansionsEnabled = enabled;
    }

    public boolean areWoodlandMansionsEnabled() {
        return woodlandMansionsEnabled;
    }

    @Override
    public void setEndCitiesEnabled(boolean enabled) {
        this.endCitiesEnabled = enabled;
    }

    public boolean areEndCitiesEnabled() {
        return endCitiesEnabled;
    }

    @Override
    public void setBastionRemnantsEnabled(boolean enabled) {
        this.bastionRemnantsEnabled = enabled;
    }

    public boolean areBastionRemnantsEnabled() {
        return bastionRemnantsEnabled;
    }

    // ========================= Cave Features =========================

    @Override
    public void setDripstoneCavesEnabled(boolean enabled) {
        this.dripstoneCavesEnabled = enabled;
    }

    public boolean areDripstoneCavesEnabled() {
        return dripstoneCavesEnabled;
    }

    @Override
    public void setLushCavesEnabled(boolean enabled) {
        this.lushCavesEnabled = enabled;
    }

    public boolean areLushCavesEnabled() {
        return lushCavesEnabled;
    }

    @Override
    public void setDeepDarkEnabled(boolean enabled) {
        this.deepDarkEnabled = enabled;
    }

    public boolean isDeepDarkEnabled() {
        return deepDarkEnabled;
    }

    // ========================= Gradient Colors =========================

    @Override
    public void setSunsetColor(int color) {
        this.sunsetColor = color;
    }

    @Override
    public int getSunsetColor() {
        return sunsetColor;
    }

    @Override
    public void pushGradient(int color) {
        gradientStack.push(color);
    }

    @Override
    public int popGradient() {
        return gradientStack.isEmpty() ? -1 : gradientStack.pop();
    }

    // ========================= Interface Registration =========================

    @Override
    public void registerInterface(ITerrainGenerator generator) {
        if (this.terrainGenerator != null) {
            addInstability(10.0f); // Penalty for replacing terrain generator
        }
        this.terrainGenerator = generator;
        // Also sync the terrain type string for backwards compatibility
        if (generator != null) {
            this.terrainType = generator.getType();
        }
    }

    @Override
    public void registerInterface(IBiomeController controller) {
        if (this.biomeControllerImpl != null) {
            addInstability(10.0f); // Penalty for replacing biome controller
        }
        this.biomeControllerImpl = controller;
        // Also sync the biome controller type string for backwards compatibility
        if (controller != null) {
            this.biomeController = controller.getType();
        }
    }

    @Override
    public void registerInterface(ITerrainAlteration alteration) {
        if (alteration != null) {
            terrainAlterations.add(alteration);
            // Sort by priority (lower = earlier)
            terrainAlterations.sort(Comparator.comparingInt(ITerrainAlteration::getPriority));
        }
    }

    @Override
    public void registerInterface(IChunkProviderFinalization finalizer) {
        if (finalizer != null) {
            chunkFinalizers.add(finalizer);
        }
    }

    @Override
    public void registerInterface(IPopulate populate) {
        if (populate != null) {
            // Check if a populator with the same identifier already exists
            String newId = populate.getIdentifier();
            boolean isDuplicate = populateFunctions.stream()
                    .anyMatch(existing -> existing.getIdentifier().equals(newId));

            if (isDuplicate) {
                // Already have this populator type, skip registration
                return;
            }
            populateFunctions.add(populate);
        }
    }

    @Override
    public void registerInterface(ILightingController controller) {
        if (this.lightingControllerImpl != null) {
            addInstability(5.0f); // Penalty for replacing lighting controller
        }
        this.lightingControllerImpl = controller;
        // Also sync the lighting type string for backwards compatibility
        if (controller != null) {
            this.lightingType = controller.getType();
        }
    }

    @Override
    public void registerInterface(IWeatherController controller) {
        if (this.weatherControllerImpl != null) {
            addInstability(5.0f); // Penalty for replacing weather controller
        }
        this.weatherControllerImpl = controller;
        // Also sync the weather type string for backwards compatibility
        if (controller != null) {
            this.weatherType = controller.getType();
        }
    }

    @Override
    public void registerInterface(ICelestial celestial) {
        if (celestial != null) {
            celestials.add(celestial);
        }
    }

    @Override
    public ITerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    @Override
    public IBiomeController getBiomeControllerImpl() {
        return biomeControllerImpl;
    }

    @Override
    public List<ITerrainAlteration> getTerrainAlterations() {
        return terrainAlterations;
    }

    @Override
    public List<IChunkProviderFinalization> getChunkFinalizers() {
        return chunkFinalizers;
    }

    @Override
    public List<IPopulate> getPopulateFunctions() {
        return populateFunctions;
    }

    @Override
    public ILightingController getLightingController() {
        return lightingControllerImpl;
    }

    @Override
    public IWeatherController getWeatherController() {
        return weatherControllerImpl;
    }

    @Override
    public List<ICelestial> getCelestials() {
        return celestials;
    }
}
