package art.arcane.mystcraft.world;

import art.arcane.mystcraft.api.world.AgeDirector;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

    // Modifier stacks
    private final Deque<Integer> colorStack = new ArrayDeque<>();
    private final Deque<Float> angleStack = new ArrayDeque<>();
    private final Deque<Float> lengthStack = new ArrayDeque<>();
    private final Deque<Float> phaseStack = new ArrayDeque<>();

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
}
