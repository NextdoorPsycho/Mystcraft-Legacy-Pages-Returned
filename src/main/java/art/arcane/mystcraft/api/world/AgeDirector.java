package art.arcane.mystcraft.api.world;

import art.arcane.mystcraft.api.world.logic.IBiomeController;
import art.arcane.mystcraft.api.world.logic.ICelestial;
import art.arcane.mystcraft.api.world.logic.IChunkProviderFinalization;
import art.arcane.mystcraft.api.world.logic.IDynamicColorProvider;
import art.arcane.mystcraft.api.world.logic.ILightingController;
import art.arcane.mystcraft.api.world.logic.IPopulate;
import art.arcane.mystcraft.api.world.logic.IStaticColorProvider;
import art.arcane.mystcraft.api.world.logic.ITerrainAlteration;
import art.arcane.mystcraft.api.world.logic.ITerrainGenerator;
import art.arcane.mystcraft.api.world.logic.IWeatherController;
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

    /**
     * Clears all modifier stacks (color, angle, length, phase).
     * Used by the Clear symbol to reset modifier state.
     */
    void clearModifiers();

    /**
     * Pushes a biome onto the biome modifier stack.
     * @param biome The biome to add
     */
    void pushBiome(Holder<Biome> biome);

    /**
     * Pops a biome from the biome modifier stack.
     * @return The biome, or null if empty
     */
    Holder<Biome> popBiome();

    // ========================= Additional Features =========================

    /**
     * Enables or disables nether fortress generation.
     * @param enabled true to enable
     */
    void setNetherFortEnabled(boolean enabled);

    /**
     * Enables or disables dense ore generation.
     * @param enabled true to enable
     */
    void setDenseOresEnabled(boolean enabled);

    /**
     * Enables or disables huge tree generation.
     * @param enabled true to enable
     */
    void setHugeTreesEnabled(boolean enabled);

    /**
     * Enables or disables deep lakes.
     * @param enabled true to enable
     */
    void setDeepLakesEnabled(boolean enabled);

    /**
     * Enables or disables surface lakes.
     * @param enabled true to enable
     */
    void setSurfaceLakesEnabled(boolean enabled);

    /**
     * Enables or disables spike formations.
     * @param enabled true to enable
     */
    void setSpikesEnabled(boolean enabled);

    /**
     * Enables or disables sphere formations.
     * @param enabled true to enable
     */
    void setSpheresEnabled(boolean enabled);

    /**
     * Enables or disables tendril formations.
     * @param enabled true to enable
     */
    void setTendrilsEnabled(boolean enabled);

    /**
     * Enables or disables crystal formations.
     * @param enabled true to enable
     */
    void setCrystalsEnabled(boolean enabled);

    /**
     * Enables or disables rainbow formations.
     * @param enabled true to enable
     */
    void setRainbowEnabled(boolean enabled);

    /**
     * Enables or disables obelisk formations.
     * @param enabled true to enable
     */
    void setObelisksEnabled(boolean enabled);

    /**
     * Enables or disables star fissure generation.
     * @param enabled true to enable
     */
    void setStarFissureEnabled(boolean enabled);

    /**
     * Enables or disables random explosions.
     * @param enabled true to enable
     */
    void setExplosionsEnabled(boolean enabled);

    /**
     * Enables or disables PvP in this Age.
     * @param enabled true to enable
     */
    void setPvPEnabled(boolean enabled);

    /**
     * Hides or shows the horizon.
     * @param hidden true to hide
     */
    void setHorizonHidden(boolean hidden);

    // ========================= World Heights =========================

    /**
     * Sets the cloud height for this Age.
     * @param height The Y level where clouds appear (default 192.0)
     */
    void setCloudHeight(float height);

    /**
     * Gets the cloud height.
     * @return The cloud height Y level
     */
    float getCloudHeight();

    /**
     * Sets the horizon height for this Age.
     * @param height The Y level of the horizon line (default 0.0)
     */
    void setHorizonHeight(float height);

    /**
     * Gets the horizon height.
     * @return The horizon height Y level
     */
    float getHorizonHeight();

    // ========================= Additional Structures =========================

    /**
     * Enables or disables pillager outpost generation.
     * @param enabled true to enable
     */
    void setPillagerOutpostsEnabled(boolean enabled);

    /**
     * Enables or disables ruined portal generation.
     * @param enabled true to enable
     */
    void setRuinedPortalsEnabled(boolean enabled);

    /**
     * Enables or disables ancient city generation.
     * @param enabled true to enable
     */
    void setAncientCitiesEnabled(boolean enabled);

    /**
     * Enables or disables trail ruins generation.
     * @param enabled true to enable
     */
    void setTrailRuinsEnabled(boolean enabled);

    /**
     * Enables or disables ocean monument generation.
     * @param enabled true to enable
     */
    void setOceanMonumentsEnabled(boolean enabled);

    /**
     * Enables or disables witch hut generation.
     * @param enabled true to enable
     */
    void setWitchHutsEnabled(boolean enabled);

    /**
     * Enables or disables desert temple generation.
     * @param enabled true to enable
     */
    void setDesertTemplesEnabled(boolean enabled);

    /**
     * Enables or disables jungle temple generation.
     * @param enabled true to enable
     */
    void setJungleTemplesEnabled(boolean enabled);

    /**
     * Enables or disables woodland mansion generation.
     * @param enabled true to enable
     */
    void setWoodlandMansionsEnabled(boolean enabled);

    /**
     * Enables or disables end city generation.
     * @param enabled true to enable
     */
    void setEndCitiesEnabled(boolean enabled);

    /**
     * Enables or disables bastion remnant generation.
     * @param enabled true to enable
     */
    void setBastionRemnantsEnabled(boolean enabled);

    // ========================= Cave Features =========================

    /**
     * Enables or disables dripstone cave features.
     * @param enabled true to enable
     */
    void setDripstoneCavesEnabled(boolean enabled);

    /**
     * Enables or disables lush cave features.
     * @param enabled true to enable
     */
    void setLushCavesEnabled(boolean enabled);

    /**
     * Enables or disables deep dark/sculk features.
     * @param enabled true to enable
     */
    void setDeepDarkEnabled(boolean enabled);

    // ========================= Gradient Colors =========================

    /**
     * Sets the sunset/sunrise color gradient.
     * @param color The RGB color
     */
    void setSunsetColor(int color);

    /**
     * Gets the sunset/sunrise color.
     * @return The RGB color, or -1 for default
     */
    int getSunsetColor();

    /**
     * Pushes a gradient color onto the stack.
     * @param color The color value
     */
    void pushGradient(int color);

    /**
     * Pops a gradient color from the stack.
     * @return The color value, or -1 if empty
     */
    int popGradient();

    // ========================= Additional Colors =========================

    /**
     * Sets the cloud color.
     * @param color The RGB color
     */
    void setCloudColor(int color);

    /**
     * Gets the cloud color.
     * @return The RGB color, or -1 for default
     */
    int getCloudColor();

    /**
     * Sets the night sky color.
     * @param color The RGB color
     */
    void setNightSkyColor(int color);

    /**
     * Gets the night sky color.
     * @return The RGB color, or -1 for default
     */
    int getNightSkyColor();

    /**
     * Marks the sky color as natural (biome-dependent).
     * @param natural true for biome-dependent color
     */
    void setSkyColorNatural(boolean natural);

    /**
     * Marks the fog color as natural (biome-dependent).
     * @param natural true for biome-dependent color
     */
    void setFogColorNatural(boolean natural);

    /**
     * Marks the grass color as natural (biome-dependent).
     * @param natural true for biome-dependent color
     */
    void setGrassColorNatural(boolean natural);

    /**
     * Marks the foliage color as natural (biome-dependent).
     * @param natural true for biome-dependent color
     */
    void setFoliageColorNatural(boolean natural);

    /**
     * Marks the water color as natural (biome-dependent).
     * @param natural true for biome-dependent color
     */
    void setWaterColorNatural(boolean natural);

    /**
     * Marks the cloud color as natural (weather-dependent).
     * @param natural true for weather-dependent color
     */
    void setCloudColorNatural(boolean natural);

    // ========================= Interface Registration =========================
    // These methods allow symbols to register actual generation logic objects
    // rather than just configuration strings. This enables the full legacy
    // Mystcraft world generation pipeline.

    /**
     * Registers a terrain generator for this Age.
     * Only one terrain generator can be active. Registering a second one
     * replaces the first and adds instability.
     *
     * @param generator The terrain generator implementation
     */
    void registerInterface(ITerrainGenerator generator);

    /**
     * Registers a biome controller for this Age.
     * Only one biome controller can be active. Registering a second one
     * replaces the first and adds instability.
     *
     * @param controller The biome controller implementation
     */
    void registerInterface(IBiomeController controller);

    /**
     * Registers a terrain alteration for this Age.
     * Multiple alterations can be registered (caves, ravines, floating islands).
     *
     * @param alteration The terrain alteration implementation
     */
    void registerInterface(ITerrainAlteration alteration);

    /**
     * Registers a chunk finalization handler for this Age.
     * Multiple handlers can be registered.
     *
     * @param finalizer The chunk finalization implementation
     */
    void registerInterface(IChunkProviderFinalization finalizer);

    /**
     * Registers a population function for this Age.
     * Multiple population functions can be registered.
     *
     * @param populate The population implementation
     */
    void registerInterface(IPopulate populate);

    /**
     * Registers a lighting controller for this Age.
     * Only one lighting controller can be active.
     *
     * @param controller The lighting controller implementation
     */
    void registerInterface(ILightingController controller);

    /**
     * Registers a weather controller for this Age.
     * Only one weather controller can be active.
     *
     * @param controller The weather controller implementation
     */
    void registerInterface(IWeatherController controller);

    /**
     * Registers a celestial object for this Age.
     * Multiple celestials can be registered (multiple suns, moons, etc.)
     *
     * @param celestial The celestial implementation
     */
    void registerInterface(ICelestial celestial);

    /**
     * Gets the registered terrain generator.
     * @return The terrain generator, or null if none registered
     */
    ITerrainGenerator getTerrainGenerator();

    /**
     * Gets the registered biome controller.
     * @return The biome controller, or null if none registered
     */
    IBiomeController getBiomeControllerImpl();

    /**
     * Gets all registered terrain alterations.
     * @return List of terrain alterations (never null)
     */
    List<ITerrainAlteration> getTerrainAlterations();

    /**
     * Gets all registered chunk finalizers.
     * @return List of chunk finalizers (never null)
     */
    List<IChunkProviderFinalization> getChunkFinalizers();

    /**
     * Gets all registered population functions.
     * @return List of population functions (never null)
     */
    List<IPopulate> getPopulateFunctions();

    /**
     * Gets the registered lighting controller.
     * @return The lighting controller, or null if none registered
     */
    ILightingController getLightingController();

    /**
     * Gets the registered weather controller.
     * @return The weather controller, or null if none registered
     */
    IWeatherController getWeatherController();

    /**
     * Gets all registered celestial objects.
     * @return List of celestials (never null)
     */
    List<ICelestial> getCelestials();

    // ========================= Color Provider Registration =========================

    /**
     * Registers a dynamic color provider for this Age.
     * Multiple providers can be registered per color type and their colors will be averaged.
     *
     * @param provider The color provider implementation
     */
    void registerInterface(IDynamicColorProvider provider);

    /**
     * Registers a static color provider for this Age.
     * Multiple providers can be registered per color type and their colors will be averaged.
     *
     * @param provider The color provider implementation
     */
    void registerInterface(IStaticColorProvider provider);

    /**
     * Gets all registered dynamic color providers.
     * @return List of dynamic color providers (never null)
     */
    List<IDynamicColorProvider> getDynamicColorProviders();

    /**
     * Gets all registered static color providers.
     * @return List of static color providers (never null)
     */
    List<IStaticColorProvider> getStaticColorProviders();
}
