package art.arcane.mystcraft.world;

import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.api.world.logic.*;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation of AgeDirector that collects symbol configurations.
 * Used during Age creation to build world properties.
 *
 * Ported to 1.19.2 API.
 */
public class AgeDirectorImpl implements AgeDirector {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgeDirectorImpl.class);

  private final long seed;
  private final List<Holder<Biome>> biomes = new ArrayList<>();
  private final List<Integer> grassColors = new ArrayList<>();
  private final Set<String> disabledOres = new HashSet<>();
  private final Map<String, Float> oreMultipliers = new HashMap<>();
  // Modifier stacks
  private final Deque<Integer> colorStack = new ArrayDeque<>();
  private final Deque<Float> angleStack = new ArrayDeque<>();
  private final Deque<Float> lengthStack = new ArrayDeque<>();
  private final Deque<Float> phaseStack = new ArrayDeque<>();
  private final Deque<Holder<Biome>> biomeStack = new ArrayDeque<>();
  private final Deque<Integer> gradientStack = new ArrayDeque<>();
  private final List<ITerrainAlteration> terrainAlterations = new ArrayList<>();
  private final List<IChunkProviderFinalization> chunkFinalizers = new ArrayList<>();
  private final List<IPopulate> populateFunctions = new ArrayList<>();
  private final List<ICelestial> celestials = new ArrayList<>();
  private final List<IDynamicColorProvider> dynamicColorProviders = new ArrayList<>();
  private final List<IStaticColorProvider> staticColorProviders = new ArrayList<>();
  private float instability = 0.0f;
  // Terrain
  private String terrainType = "normal";
  private String terrainMixMode = "none";
  private String secondaryTerrainType = "none";
  private int averageGroundLevel = 64;
  private int seaLevel = 63;
  private boolean hasSea = true;
  private BlockState terrainBlock = Blocks.STONE.defaultBlockState();
  private BlockState seaBlock = Blocks.WATER.defaultBlockState();
  private BlockState surfaceBlock = null;
  private BlockState subsurfaceBlock = null;
  // Biomes
  private String biomeController = "native";
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
  private int foliageColor = -1;
  private int waterColor = -1;
  private int cloudColor = -1;
  private int nightSkyColor = -1;
  private boolean skyColorNatural = false;
  private boolean fogColorNatural = false;
  private boolean grassColorNatural = false;
  private boolean foliageColorNatural = false;
  private boolean waterColorNatural = false;
  private boolean cloudColorNatural = false;
  private int horizonColor = -1;
  private boolean horizonColorNatural = false;
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
  private float timescale = 1.0f;
  private boolean meteorsEnabled = false;
  private boolean lightningEnabled = false;
  private boolean scorchedEnabled = false;
  // Ore control
  private boolean oresDisabled = false;
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
  private boolean starFissureExplicit = false;
  private JsonObject starFissureParams = new JsonObject();
  private boolean surfaceLakesEnabled = true;
  private boolean tendrilsEnabled = false;
  private boolean verticalTendrilsEnabled = false;
  private boolean perlinWormsEnabled = false;
  private boolean personalPocket = false;
  // New structures (1.19+)
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
  private boolean igloosEnabled = false;
  private boolean shipwrecksEnabled = false;
  private boolean oceanRuinsEnabled = false;
  private boolean buriedTreasureEnabled = false;
  private boolean netherFossilsEnabled = false;
  // Cave features
  private boolean dripstoneCavesEnabled = false;
  private boolean lushCavesEnabled = false;
  private boolean deepDarkEnabled = false;
  // Gradient colors
  private int sunsetColor = -1;
  // World heights
  private float cloudHeight = 192.0f;
  private float horizonHeight = 0.0f;
  // Micro dimension settings (applied to newly created Ages only)
  private boolean microDimensionsEnabled = false;
  private int microDimensionRadiusChunks = 0;
  private int microDimensionExtraChunks = 1;
  // Registered logic interfaces (for full world generation pipeline)
  private ITerrainGenerator terrainGenerator;
  private IBiomeController biomeControllerImpl;
  private ILightingController lightingControllerImpl;
  private IWeatherController weatherControllerImpl;

  public AgeDirectorImpl(long seed) {
    this.seed = seed;
  }

  private static boolean isValidReplacementBlock(BlockState block) {
    if (block == null) return false;
    if (block.isAir()) return false;
    if (!block.getFluidState().isEmpty()) return false;
    return block.getMaterial().isSolid();
  }

  private static boolean isValidSeaBlock(BlockState block) {
    if (block == null) return false;
    if (block.isAir()) return false;
    return !block.getFluidState().isEmpty() || block.getMaterial().isSolid();
  }

  @Override
  public long getSeed() {
    return seed;
  }

  @Override
  public float getInstability() {
    return instability;
  }

  // --- Terrain ---

  /**
   * Sets the instability value directly.
   */
  public void setInstability(float instability) {
    this.instability = instability;
  }

  @Override
  public void addInstability(float amount) {
    this.instability += amount;
  }

  @Override
  public String getTerrainType() {
    return terrainType;
  }

  @Override
  public void setTerrainType(String type) {
    LOGGER.info("[Director] setTerrainType: {} -> {}", this.terrainType, type);
    this.terrainType = type;
  }

  @Override
  public String getTerrainMixMode() {
    return terrainMixMode;
  }

  @Override
  public void setTerrainMixMode(String mode) {
    LOGGER.info("[Director] setTerrainMixMode: {} -> {}", this.terrainMixMode, mode);
    this.terrainMixMode = mode != null ? mode : "none";
  }

  @Override
  public String getSecondaryTerrainType() {
    return secondaryTerrainType;
  }

  @Override
  public void setSecondaryTerrainType(String type) {
    LOGGER.info("[Director] setSecondaryTerrainType: {} -> {}", this.secondaryTerrainType, type);
    this.secondaryTerrainType = type != null ? type : "none";
  }

  @Override
  public int getAverageGroundLevel() {
    return averageGroundLevel;
  }

  @Override
  public void setAverageGroundLevel(int level) {
    this.averageGroundLevel = level;
  }

  @Override
  public int getSeaLevel() {
    return seaLevel;
  }

  @Override
  public void setSeaLevel(int level) {
    this.seaLevel = level;
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
  public BlockState getTerrainBlock() {
    return terrainBlock;
  }

  @Override
  public void setTerrainBlock(BlockState block) {
    if (!isValidReplacementBlock(block)) {
      LOGGER.warn("[Director] Ignoring non-solid terrain block: {}", block);
      return;
    }
    LOGGER.info("[Director] setTerrainBlock: {} -> {}", this.terrainBlock, block);
    this.terrainBlock = block;
  }

  @Override
  public BlockState getSeaBlock() {
    return seaBlock;
  }

  @Override
  public void setSeaBlock(BlockState block) {
    if (!isValidSeaBlock(block)) {
      LOGGER.warn("[Director] Ignoring invalid sea block: {}", block);
      return;
    }
    LOGGER.info("[Director] setSeaBlock: {} -> {}", this.seaBlock, block);
    this.seaBlock = block;
  }

  public BlockState getSurfaceBlock() {
    return surfaceBlock;
  }

  public void setSurfaceBlock(BlockState block) {
    if (!isValidReplacementBlock(block)) {
      LOGGER.warn("[Director] Ignoring non-solid surface block: {}", block);
      return;
    }
    this.surfaceBlock = block;
  }

  public BlockState getSubsurfaceBlock() {
    return subsurfaceBlock;
  }

  public void setSubsurfaceBlock(BlockState block) {
    if (!isValidReplacementBlock(block)) {
      LOGGER.warn("[Director] Ignoring non-solid subsurface block: {}", block);
      return;
    }
    this.subsurfaceBlock = block;
  }

  // --- Biomes ---

  @Override
  public String getBiomeController() {
    return biomeController;
  }

  @Override
  public void setBiomeController(String type) {
    this.biomeController = type;
  }

  @Override
  public void addBiome(Holder<Biome> biome) {
    biomes.add(biome);
  }

  @Override
  public List<Holder<Biome>> getBiomes() {
    return biomes;
  }

  // --- Celestials ---

  public boolean isSunVisible() {
    return sunVisible;
  }

  @Override
  public void setSunVisible(boolean visible) {
    this.sunVisible = visible;
  }

  public boolean isMoonVisible() {
    return moonVisible;
  }

  @Override
  public void setMoonVisible(boolean visible) {
    this.moonVisible = visible;
  }

  @Override
  public void setStarsVisible(boolean visible) {
    this.starsVisible = visible;
  }

  public boolean areStarsVisible() {
    return starsVisible;
  }

  public String getStarType() {
    return starType;
  }

  @Override
  public void setStarType(String type) {
    this.starType = type;
  }

  // --- Weather ---

  @Override
  public String getWeatherType() {
    return weatherType;
  }

  @Override
  public void setWeatherType(String type) {
    this.weatherType = type;
  }

  // --- Lighting ---

  @Override
  public String getLightingType() {
    return lightingType;
  }

  @Override
  public void setLightingType(String type) {
    this.lightingType = type;
  }

  // --- Colors ---

  @Override
  public int getSkyColor() {
    return skyColor;
  }

  @Override
  public void setSkyColor(int color) {
    this.skyColor = color;
  }

  @Override
  public int getFogColor() {
    return fogColor;
  }

  @Override
  public void setFogColor(int color) {
    this.fogColor = color;
  }

  @Override
  public int getGrassColor() {
    return grassColors.isEmpty() ? -1 : grassColors.get(0);
  }

  @Override
  public void setGrassColor(int color) {
    if (color != -1) {
      this.grassColors.add(color);
    }
  }

  @Override
  public List<Integer> getGrassColors() {
    return Collections.unmodifiableList(grassColors);
  }

  /**
   * Clears all grass colors from the palette.
   */
  public void clearGrassColors() {
    grassColors.clear();
  }

  @Override
  public int getFoliageColor() {
    return foliageColor;
  }

  @Override
  public void setFoliageColor(int color) {
    this.foliageColor = color;
  }

  @Override
  public int getWaterColor() {
    return waterColor;
  }

  @Override
  public void setWaterColor(int color) {
    this.waterColor = color;
  }

  @Override
  public int getCloudColor() {
    return cloudColor;
  }

  @Override
  public void setCloudColor(int color) {
    this.cloudColor = color;
  }

  @Override
  public int getNightSkyColor() {
    return nightSkyColor;
  }

  @Override
  public void setNightSkyColor(int color) {
    this.nightSkyColor = color;
  }

  public boolean isSkyColorNatural() {
    return skyColorNatural;
  }

  @Override
  public void setSkyColorNatural(boolean natural) {
    this.skyColorNatural = natural;
  }

  public boolean isFogColorNatural() {
    return fogColorNatural;
  }

  @Override
  public void setFogColorNatural(boolean natural) {
    this.fogColorNatural = natural;
  }

  public boolean isGrassColorNatural() {
    return grassColorNatural;
  }

  @Override
  public void setGrassColorNatural(boolean natural) {
    this.grassColorNatural = natural;
    if (natural) {
      this.grassColors.clear();
    }
  }

  public boolean isFoliageColorNatural() {
    return foliageColorNatural;
  }

  @Override
  public void setFoliageColorNatural(boolean natural) {
    this.foliageColorNatural = natural;
  }

  public boolean isWaterColorNatural() {
    return waterColorNatural;
  }

  @Override
  public void setWaterColorNatural(boolean natural) {
    this.waterColorNatural = natural;
  }

  public boolean isCloudColorNatural() {
    return cloudColorNatural;
  }

  @Override
  public void setCloudColorNatural(boolean natural) {
    this.cloudColorNatural = natural;
  }

  @Override
  public int getHorizonColor() {
    return horizonColor;
  }

  @Override
  public void setHorizonColor(int color) {
    this.horizonColor = color;
  }

  public boolean isHorizonColorNatural() {
    return horizonColorNatural;
  }

  @Override
  public void setHorizonColorNatural(boolean natural) {
    this.horizonColorNatural = natural;
  }

  // --- Features ---

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

  // --- Structures ---

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

  // --- Environment ---

  public boolean isAcceleratedEnabled() {
    return acceleratedEnabled;
  }

  @Override
  public void setAcceleratedEnabled(boolean enabled) {
    this.acceleratedEnabled = enabled;
  }

  @Override
  public float getTimescale() {
    return timescale;
  }

  @Override
  public void setTimescale(float scale) {
    this.timescale = scale;
  }

  @Override
  public void setMeteorsEnabled(boolean enabled) {
    this.meteorsEnabled = enabled;
  }

  public boolean areMeteorsEnabled() {
    return meteorsEnabled;
  }

  public boolean isLightningEnabled() {
    return lightningEnabled;
  }

  @Override
  public void setLightningEnabled(boolean enabled) {
    this.lightningEnabled = enabled;
  }

  public boolean isScorchedEnabled() {
    return scorchedEnabled;
  }

  @Override
  public void setScorchedEnabled(boolean enabled) {
    this.scorchedEnabled = enabled;
  }

  // --- Additional Features ---

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
  public void setOresDisabled(boolean disabled) {
    this.oresDisabled = disabled;
  }

  @Override
  public boolean areOresDisabled() {
    return oresDisabled;
  }

  @Override
  public void setOreDisabled(String oreType, boolean disabled) {
    if (disabled) {
      disabledOres.add(oreType);
    } else {
      disabledOres.remove(oreType);
    }
  }

  @Override
  public boolean isOreDisabled(String oreType) {
    return oresDisabled || disabledOres.contains(oreType);
  }

  @Override
  public Set<String> getDisabledOres() {
    return Collections.unmodifiableSet(disabledOres);
  }

  @Override
  public void setOreMultiplier(String oreType, float multiplier) {
    oreMultipliers.put(oreType, multiplier);
  }

  @Override
  public float getOreMultiplier(String oreType) {
    return oreMultipliers.getOrDefault(oreType, 1.0f);
  }

  @Override
  public Map<String, Float> getOreMultipliers() {
    return Collections.unmodifiableMap(oreMultipliers);
  }

  @Override
  public void setExplosionsEnabled(boolean enabled) {
    this.explosionsEnabled = enabled;
  }

  public boolean areExplosionsEnabled() {
    return explosionsEnabled;
  }

  public boolean isHorizonHidden() {
    return horizonHidden;
  }

  @Override
  public void setHorizonHidden(boolean hidden) {
    this.horizonHidden = hidden;
  }

  // --- World Heights ---

  @Override
  public float getCloudHeight() {
    return cloudHeight;
  }

  @Override
  public void setCloudHeight(float height) {
    this.cloudHeight = height;
  }

  @Override
  public float getHorizonHeight() {
    return horizonHeight;
  }

  @Override
  public void setHorizonHeight(float height) {
    this.horizonHeight = height;
  }

  public boolean isMicroDimensionsEnabled() {
    return microDimensionsEnabled;
  }

  public int getMicroDimensionRadiusChunks() {
    return microDimensionRadiusChunks;
  }

  public int getMicroDimensionExtraChunks() {
    return microDimensionExtraChunks;
  }

  public void setMicroDimensions(boolean enabled, int radiusChunks, int extraChunks) {
    this.microDimensionsEnabled = enabled;
    this.microDimensionRadiusChunks = Math.max(0, radiusChunks);
    this.microDimensionExtraChunks = Math.max(0, extraChunks);
  }

  @Override
  public void setHugeTreesEnabled(boolean enabled) {
    this.hugeTreesEnabled = enabled;
  }

  public boolean areHugeTreesEnabled() {
    return hugeTreesEnabled;
  }

  public boolean isNetherFortEnabled() {
    return netherFortEnabled;
  }

  @Override
  public void setNetherFortEnabled(boolean enabled) {
    this.netherFortEnabled = enabled;
  }

  @Override
  public void setObelisksEnabled(boolean enabled) {
    this.obelisksEnabled = enabled;
  }

  public boolean areObelisksEnabled() {
    return obelisksEnabled;
  }

  public boolean isPvPEnabled() {
    return pvpEnabled;
  }

  @Override
  public void setPvPEnabled(boolean enabled) {
    this.pvpEnabled = enabled;
  }

  public boolean isRainbowEnabled() {
    return rainbowEnabled;
  }

  @Override
  public void setRainbowEnabled(boolean enabled) {
    this.rainbowEnabled = enabled;
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

  public boolean isStarFissureEnabled() {
    return starFissureEnabled;
  }

  @Override
  public void setStarFissureEnabled(boolean enabled) {
    this.starFissureEnabled = enabled;
  }

  @Override
  public boolean isStarFissureExplicit() {
    return starFissureExplicit;
  }

  @Override
  public void setStarFissureExplicit(boolean explicit) {
    this.starFissureExplicit = explicit;
  }

  @Override
  public JsonObject getStarFissureParams() {
    return starFissureParams;
  }

  @Override
  public void setStarFissureParams(JsonObject params) {
    if (params == null) {
      this.starFissureParams = new JsonObject();
      return;
    }
    this.starFissureParams = params;
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

  @Override
  public void setVerticalTendrilsEnabled(boolean enabled) {
    this.verticalTendrilsEnabled = enabled;
  }

  public boolean areVerticalTendrilsEnabled() {
    return verticalTendrilsEnabled;
  }

  @Override
  public void setPerlinWormsEnabled(boolean enabled) {
    this.perlinWormsEnabled = enabled;
  }

  public boolean arePerlinWormsEnabled() {
    return perlinWormsEnabled;
  }

  public boolean isPersonalPocket() {
    return personalPocket;
  }

  public void setPersonalPocket(boolean personalPocket) {
    this.personalPocket = personalPocket;
  }

  // --- Modifiers ---

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

  // --- Additional Structures ---

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

  @Override
  public void setIgloosEnabled(boolean enabled) {
    this.igloosEnabled = enabled;
  }

  public boolean areIgloosEnabled() {
    return igloosEnabled;
  }

  @Override
  public void setShipwrecksEnabled(boolean enabled) {
    this.shipwrecksEnabled = enabled;
  }

  public boolean areShipwrecksEnabled() {
    return shipwrecksEnabled;
  }

  @Override
  public void setOceanRuinsEnabled(boolean enabled) {
    this.oceanRuinsEnabled = enabled;
  }

  public boolean areOceanRuinsEnabled() {
    return oceanRuinsEnabled;
  }

  public boolean isBuriedTreasureEnabled() {
    return buriedTreasureEnabled;
  }

  @Override
  public void setBuriedTreasureEnabled(boolean enabled) {
    this.buriedTreasureEnabled = enabled;
  }

  @Override
  public void setNetherFossilsEnabled(boolean enabled) {
    this.netherFossilsEnabled = enabled;
  }

  public boolean areNetherFossilsEnabled() {
    return netherFossilsEnabled;
  }

  // --- Cave Features ---

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

  public boolean isDeepDarkEnabled() {
    return deepDarkEnabled;
  }

  @Override
  public void setDeepDarkEnabled(boolean enabled) {
    this.deepDarkEnabled = enabled;
  }

  // --- Gradient Colors ---

  @Override
  public int getSunsetColor() {
    return sunsetColor;
  }

  @Override
  public void setSunsetColor(int color) {
    this.sunsetColor = color;
  }

  @Override
  public void pushGradient(int color) {
    gradientStack.push(color);
  }

  @Override
  public int popGradient() {
    return gradientStack.isEmpty() ? -1 : gradientStack.pop();
  }

  // --- Interface Registration ---

  @Override
  public void registerInterface(ITerrainGenerator generator) {
    if (this.terrainGenerator != null) {
      LOGGER.info("[Director] Replacing terrain generator: {} -> {} (+10 instability)",
          this.terrainGenerator.getType(), generator != null ? generator.getType() : "null");
      addInstability(10.0f);
    } else {
      LOGGER.info("[Director] Registered terrain generator: {}",
          generator != null ? generator.getType() : "null");
    }
    this.terrainGenerator = generator;
    if (generator != null) {
      this.terrainType = generator.getType();
    }
  }

  @Override
  public void registerInterface(IBiomeController controller) {
    if (this.biomeControllerImpl != null) {
      LOGGER.info("[Director] Replacing biome controller: {} -> {} (+10 instability)",
          this.biomeControllerImpl.getType(), controller != null ? controller.getType() : "null");
      addInstability(10.0f);
    } else {
      LOGGER.info("[Director] Registered biome controller: {}",
          controller != null ? controller.getType() : "null");
    }
    this.biomeControllerImpl = controller;
    if (controller != null) {
      this.biomeController = controller.getType();
    }
  }

  @Override
  public void registerInterface(ITerrainAlteration alteration) {
    if (alteration != null) {
      LOGGER.info("[Director] Registered terrain alteration: {} (priority: {})",
          alteration.getClass().getSimpleName(), alteration.getPriority());
      terrainAlterations.add(alteration);
      terrainAlterations.sort(Comparator.comparingInt(ITerrainAlteration::getPriority));
    }
  }

  @Override
  public void registerInterface(IChunkProviderFinalization finalizer) {
    if (finalizer != null) {
      LOGGER.info("[Director] Registered chunk finalizer: {}", finalizer.getClass().getSimpleName());
      chunkFinalizers.add(finalizer);
    }
  }

  @Override
  public void registerInterface(IPopulate populate) {
    if (populate != null) {
      String newId = populate.getIdentifier();
      boolean isDuplicate = populateFunctions.stream()
          .anyMatch(existing -> existing.getIdentifier().equals(newId));

      if (isDuplicate) {
        LOGGER.debug("[Director] Skipping duplicate populator: {}", newId);
        return;
      }
      LOGGER.info("[Director] Registered populator: {}", newId);
      populateFunctions.add(populate);
    }
  }

  @Override
  public void registerInterface(ILightingController controller) {
    if (this.lightingControllerImpl != null) {
      LOGGER.info("[Director] Replacing lighting controller: {} -> {} (+5 instability)",
          this.lightingControllerImpl.getType(), controller != null ? controller.getType() : "null");
      addInstability(5.0f);
    } else {
      LOGGER.info("[Director] Registered lighting controller: {}",
          controller != null ? controller.getType() : "null");
    }
    this.lightingControllerImpl = controller;
    if (controller != null) {
      this.lightingType = controller.getType();
    }
  }

  @Override
  public void registerInterface(IWeatherController controller) {
    if (this.weatherControllerImpl != null) {
      LOGGER.info("[Director] Replacing weather controller: {} -> {}",
          this.weatherControllerImpl.getType(), controller != null ? controller.getType() : "null");
    } else {
      LOGGER.info("[Director] Registered weather controller: {}",
          controller != null ? controller.getType() : "null");
    }
    this.weatherControllerImpl = controller;
    if (controller != null) {
      this.weatherType = controller.getType();
    }
  }

  @Override
  public void registerInterface(ICelestial celestial) {
    if (celestial != null) {
      LOGGER.info("[Director] Registered celestial: {} (total: {})",
          celestial.getClass().getSimpleName(), celestials.size() + 1);
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

  // --- Color Provider Registration ---

  @Override
  public void registerInterface(IDynamicColorProvider provider) {
    if (provider != null) {
      // Check for duplicate identifiers
      String newId = provider.getIdentifier();
      boolean isDuplicate = dynamicColorProviders.stream()
          .anyMatch(existing -> existing.getIdentifier().equals(newId));

      if (!isDuplicate) {
        dynamicColorProviders.add(provider);
      }
    }
  }

  @Override
  public void registerInterface(IStaticColorProvider provider) {
    if (provider != null) {
      // Check for duplicate identifiers
      String newId = provider.getIdentifier();
      boolean isDuplicate = staticColorProviders.stream()
          .anyMatch(existing -> existing.getIdentifier().equals(newId));

      if (!isDuplicate) {
        staticColorProviders.add(provider);
      }
    }
  }

  @Override
  public List<IDynamicColorProvider> getDynamicColorProviders() {
    return dynamicColorProviders;
  }

  @Override
  public List<IStaticColorProvider> getStaticColorProviders() {
    return staticColorProviders;
  }
}
