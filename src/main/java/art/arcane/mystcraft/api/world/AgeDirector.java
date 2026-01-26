package art.arcane.mystcraft.api.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Age Director receives symbol logic registrations and builds an Age's properties.
 * Symbols register their effects with this interface during Age creation.
 */
public interface AgeDirector {

    /**
     * Gets the world seed for this Age.
     * @return The seed
     */
    long getSeed();

    /**
     * Gets the current instability value for this Age.
     * @return The instability value
     */
    float getInstability();

    /**
     * Adds to the instability value.
     * @param amount Amount to add
     */
    void addInstability(float amount);

    // ========================= Terrain =========================

    /**
     * Sets the terrain generator type.
     * @param type The terrain type identifier
     */
    void setTerrainType(String type);

    /**
     * Gets the terrain generator type.
     * @return The terrain type
     */
    String getTerrainType();

    /**
     * Sets the average ground level for the Age.
     * @param level The Y level
     */
    void setAverageGroundLevel(int level);

    /**
     * Gets the average ground level.
     * @return The Y level
     */
    int getAverageGroundLevel();

    /**
     * Sets the sea level for the Age.
     * @param level The Y level
     */
    void setSeaLevel(int level);

    /**
     * Gets the sea level.
     * @return The Y level
     */
    int getSeaLevel();

    /**
     * Enables or disables the sea.
     * @param hasSea true to enable sea
     */
    void setHasSea(boolean hasSea);

    /**
     * Checks if the Age has a sea.
     * @return true if sea is enabled
     */
    boolean hasSea();

    /**
     * Sets the primary terrain block.
     * @param block The block state
     */
    void setTerrainBlock(BlockState block);

    /**
     * Gets the primary terrain block.
     * @return The block state
     */
    BlockState getTerrainBlock();

    /**
     * Sets the sea/fluid block.
     * @param block The block state
     */
    void setSeaBlock(BlockState block);

    /**
     * Gets the sea/fluid block.
     * @return The block state
     */
    BlockState getSeaBlock();

    // ========================= Biomes =========================

    /**
     * Sets the biome controller type.
     * @param type The controller type identifier
     */
    void setBiomeController(String type);

    /**
     * Gets the biome controller type.
     * @return The controller type
     */
    String getBiomeController();

    /**
     * Adds a biome to the Age's biome list.
     * @param biome The biome to add
     */
    void addBiome(Holder<Biome> biome);

    /**
     * Gets all registered biomes for this Age.
     * @return List of biomes
     */
    List<Holder<Biome>> getBiomes();

    // ========================= Celestials =========================

    /**
     * Sets whether the sun is visible.
     * @param visible true to show sun
     */
    void setSunVisible(boolean visible);

    /**
     * Sets whether the moon is visible.
     * @param visible true to show moon
     */
    void setMoonVisible(boolean visible);

    /**
     * Sets whether stars are visible.
     * @param visible true to show stars
     */
    void setStarsVisible(boolean visible);

    /**
     * Sets the star type (normal, twinkle, end, etc.)
     * @param type The star type
     */
    void setStarType(String type);

    // ========================= Weather =========================

    /**
     * Sets the weather type.
     * @param type The weather type (normal, always, off, rain, snow, storm)
     */
    void setWeatherType(String type);

    /**
     * Gets the weather type.
     * @return The weather type
     */
    String getWeatherType();

    // ========================= Lighting =========================

    /**
     * Sets the lighting type.
     * @param type The lighting type (normal, bright, dark)
     */
    void setLightingType(String type);

    /**
     * Gets the lighting type.
     * @return The lighting type
     */
    String getLightingType();

    // ========================= Colors =========================

    /**
     * Sets the sky color.
     * @param color The RGB color
     */
    void setSkyColor(int color);

    /**
     * Gets the sky color.
     * @return The RGB color, or -1 for default
     */
    int getSkyColor();

    /**
     * Sets the fog color.
     * @param color The RGB color
     */
    void setFogColor(int color);

    /**
     * Gets the fog color.
     * @return The RGB color, or -1 for default
     */
    int getFogColor();

    /**
     * Sets the grass color.
     * @param color The RGB color
     */
    void setGrassColor(int color);

    /**
     * Gets the grass color.
     * @return The RGB color, or -1 for default
     */
    int getGrassColor();

    /**
     * Sets the foliage color.
     * @param color The RGB color
     */
    void setFoliageColor(int color);

    /**
     * Gets the foliage color.
     * @return The RGB color, or -1 for default
     */
    int getFoliageColor();

    /**
     * Sets the water color.
     * @param color The RGB color
     */
    void setWaterColor(int color);

    /**
     * Gets the water color.
     * @return The RGB color, or -1 for default
     */
    int getWaterColor();

    // ========================= Features =========================

    /**
     * Enables or disables caves.
     * @param enabled true to enable
     */
    void setCavesEnabled(boolean enabled);

    /**
     * Enables or disables ravines.
     * @param enabled true to enable
     */
    void setRavinesEnabled(boolean enabled);

    /**
     * Enables or disables floating islands.
     * @param enabled true to enable
     */
    void setFloatingIslandsEnabled(boolean enabled);

    /**
     * Enables or disables skylands terrain.
     * @param enabled true to enable
     */
    void setSkylandsEnabled(boolean enabled);

    // ========================= Structures =========================

    /**
     * Enables or disables villages.
     * @param enabled true to enable
     */
    void setVillagesEnabled(boolean enabled);

    /**
     * Enables or disables dungeons.
     * @param enabled true to enable
     */
    void setDungeonsEnabled(boolean enabled);

    /**
     * Enables or disables mineshafts.
     * @param enabled true to enable
     */
    void setMineshaftsEnabled(boolean enabled);

    /**
     * Enables or disables strongholds.
     * @param enabled true to enable
     */
    void setStrongholdsEnabled(boolean enabled);

    // ========================= Environment =========================

    /**
     * Enables or disables accelerated time effects.
     * @param enabled true to enable
     */
    void setAcceleratedEnabled(boolean enabled);

    /**
     * Enables or disables meteor spawning.
     * @param enabled true to enable
     */
    void setMeteorsEnabled(boolean enabled);

    /**
     * Enables or disables lightning strikes.
     * @param enabled true to enable
     */
    void setLightningEnabled(boolean enabled);

    /**
     * Enables or disables scorched earth effects.
     * @param enabled true to enable
     */
    void setScorchedEnabled(boolean enabled);

    // ========================= Modifiers =========================

    /**
     * Pushes a color modifier onto the stack.
     * @param color The color value
     */
    void pushColor(int color);

    /**
     * Pops a color modifier from the stack.
     * @return The color value, or -1 if empty
     */
    int popColor();

    /**
     * Pushes an angle modifier onto the stack.
     * @param angle The angle in degrees
     */
    void pushAngle(float angle);

    /**
     * Pops an angle modifier from the stack.
     * @return The angle, or 0 if empty
     */
    float popAngle();

    /**
     * Pushes a length modifier onto the stack.
     * @param length The length multiplier
     */
    void pushLength(float length);

    /**
     * Pops a length modifier from the stack.
     * @return The length, or 1.0 if empty
     */
    float popLength();

    /**
     * Pushes a phase modifier onto the stack.
     * @param phase The phase offset
     */
    void pushPhase(float phase);

    /**
     * Pops a phase modifier from the stack.
     * @return The phase, or 0 if empty
     */
    float popPhase();
}
