package art.arcane.mystcraft.world;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.datafix.DataFixTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saved data for a Mystcraft Age (dimension).
 * Stores the age's pages, instability, authors, and other metadata.
 */
public class AgeData extends SavedData {

    private static final String DATA_NAME = Mystcraft.MOD_ID + "_age_data";

    private static final String TAG_AGE_UID = "AgeUID";
    private static final String TAG_AGE_UUID = "AgeUUID";
    private static final String TAG_AGE_NAME = "AgeName";
    private static final String TAG_AUTHORS = "Authors";
    private static final String TAG_PAGES = "Pages";
    private static final String TAG_INSTABILITY = "Instability";
    private static final String TAG_CREATED_TIME = "CreatedTime";
    private static final String TAG_SPAWN_SET = "SpawnSet";
    private static final String TAG_SPAWN_X = "SpawnX";
    private static final String TAG_SPAWN_Y = "SpawnY";
    private static final String TAG_SPAWN_Z = "SpawnZ";

    // Age Configuration Tags
    private static final String TAG_CONFIG = "AgeConfig";
    private static final String TAG_WEATHER_TYPE = "WeatherType";
    private static final String TAG_LIGHTING_TYPE = "LightingType";
    private static final String TAG_BIOME_CONTROLLER = "BiomeController";
    private static final String TAG_SKY_COLOR = "SkyColor";
    private static final String TAG_FOG_COLOR = "FogColor";
    private static final String TAG_GRASS_COLOR = "GrassColor";
    private static final String TAG_GRASS_COLORS = "GrassColors";
    private static final String TAG_FOLIAGE_COLOR = "FoliageColor";
    private static final String TAG_WATER_COLOR = "WaterColor";
    private static final String TAG_CLOUD_COLOR = "CloudColor";
    private static final String TAG_NIGHT_SKY_COLOR = "NightSkyColor";
    private static final String TAG_SUN_VISIBLE = "SunVisible";
    private static final String TAG_MOON_VISIBLE = "MoonVisible";
    private static final String TAG_STARS_VISIBLE = "StarsVisible";
    private static final String TAG_STAR_TYPE = "StarType";
    private static final String TAG_PVP_ENABLED = "PvPEnabled";
    private static final String TAG_METEORS_ENABLED = "MeteorsEnabled";
    private static final String TAG_LIGHTNING_ENABLED = "LightningEnabled";
    private static final String TAG_EXPLOSIONS_ENABLED = "ExplosionsEnabled";
    private static final String TAG_ACCELERATED_ENABLED = "AcceleratedEnabled";
    private static final String TAG_SCORCHED_ENABLED = "ScorchedEnabled";
    private static final String TAG_HORIZON_HIDDEN = "HorizonHidden";
    private static final String TAG_DENSE_ORES_ENABLED = "DenseOresEnabled";
    private static final String TAG_HUGE_TREES_ENABLED = "HugeTreesEnabled";
    private static final String TAG_OBELISKS_ENABLED = "ObelisksEnabled";
    private static final String TAG_CRYSTALS_ENABLED = "CrystalsEnabled";
    private static final String TAG_RAINBOW_ENABLED = "RainbowEnabled";
    private static final String TAG_STAR_FISSURE_ENABLED = "StarFissureEnabled";
    private static final String TAG_SPIKES_ENABLED = "SpikesEnabled";
    private static final String TAG_SPHERES_ENABLED = "SpheresEnabled";
    private static final String TAG_TENDRILS_ENABLED = "TendrilsEnabled";
    private static final String TAG_HORIZON_COLOR = "HorizonColor";
    private static final String TAG_SUNSET_COLOR = "SunsetColor";
    private static final String TAG_CLOUD_HEIGHT = "CloudHeight";
    private static final String TAG_HORIZON_HEIGHT = "HorizonHeight";
    private static final String TAG_TERRAIN_MIX_MODE = "TerrainMixMode";
    private static final String TAG_SECONDARY_TERRAIN_TYPE = "SecondaryTerrainType";
    private static final String TAG_TIMESCALE = "Timescale";
    private static final String TAG_DECK_ORDERS = "DeckOrders";

    private int ageUID;
    private UUID ageUUID;
    private String ageName = "";
    private final List<String> authors = new ArrayList<>();
    private final List<ItemStack> pages = new ArrayList<>();
    private float instability = 0.0f;
    private long createdTime;
    private boolean spawnSet = false;
    private int spawnX, spawnY, spawnZ;

    // Age Configuration (from AgeDirector)
    private String weatherType = "normal";
    private String lightingType = "normal";
    private String biomeController = "native";
    private int skyColor = -1;
    private int fogColor = -1;
    private final List<Integer> grassColors = new ArrayList<>();
    private int foliageColor = -1;
    private int waterColor = -1;
    private int cloudColor = -1;
    private int nightSkyColor = -1;
    private int horizonColor = -1;
    private int sunsetColor = -1;
    private boolean sunVisible = true;
    private boolean moonVisible = true;
    private boolean starsVisible = true;
    private String starType = "normal";
    private boolean pvpEnabled = true;
    private boolean meteorsEnabled = false;
    private boolean lightningEnabled = false;
    private boolean explosionsEnabled = false;
    private boolean acceleratedEnabled = false;
    private boolean scorchedEnabled = false;
    private boolean horizonHidden = false;
    private boolean denseOresEnabled = false;
    private boolean hugeTreesEnabled = false;
    private boolean obelisksEnabled = false;
    private boolean crystalsEnabled = false;
    private boolean rainbowEnabled = false;
    private boolean starFissureEnabled = false;
    private boolean spikesEnabled = false;
    private boolean spheresEnabled = false;
    private boolean tendrilsEnabled = false;
    private float timescale = 1.0f;
    private float cloudHeight = 192.0f;
    private float horizonHeight = 0.0f;
    private String terrainMixMode = "none";
    private String secondaryTerrainType = "none";

    // Instability deck order storage (for persistence across sessions)
    private final Map<String, List<String>> deckOrders = new HashMap<>();

    public AgeData() {
        this.ageUUID = UUID.randomUUID();
        this.createdTime = System.currentTimeMillis();
    }

    /**
     * Creates a factory for loading AgeData.
     */
    public static SavedData.Factory<AgeData> factory() {
        return new SavedData.Factory<>(AgeData::new, AgeData::load, DataFixTypes.LEVEL);
    }

    /**
     * Loads the age data from NBT (static factory method).
     */
    public static AgeData load(CompoundTag tag) {
        AgeData data = new AgeData();
        data.loadFromTag(tag);
        return data;
    }

    /**
     * Loads the age data from NBT.
     */
    private void loadFromTag(CompoundTag tag) {
        this.ageUID = tag.getInt(TAG_AGE_UID);
        if (tag.contains(TAG_AGE_UUID)) {
            this.ageUUID = UUID.fromString(tag.getString(TAG_AGE_UUID));
        } else {
            this.ageUUID = UUID.randomUUID();
        }
        this.ageName = tag.getString(TAG_AGE_NAME);
        this.instability = tag.getFloat(TAG_INSTABILITY);
        this.createdTime = tag.getLong(TAG_CREATED_TIME);

        // Load authors
        this.authors.clear();
        ListTag authorsList = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
        for (int i = 0; i < authorsList.size(); i++) {
            this.authors.add(authorsList.getString(i));
        }

        // Load pages
        this.pages.clear();
        ListTag pagesList = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
        for (int i = 0; i < pagesList.size(); i++) {
            ItemStack page = ItemStack.of(pagesList.getCompound(i));
            if (!page.isEmpty()) {
                this.pages.add(page);
            }
        }

        // Load spawn
        this.spawnSet = tag.getBoolean(TAG_SPAWN_SET);
        if (this.spawnSet) {
            this.spawnX = tag.getInt(TAG_SPAWN_X);
            this.spawnY = tag.getInt(TAG_SPAWN_Y);
            this.spawnZ = tag.getInt(TAG_SPAWN_Z);
        }

        // Load age configuration
        if (tag.contains(TAG_CONFIG)) {
            CompoundTag config = tag.getCompound(TAG_CONFIG);
            this.weatherType = config.getString(TAG_WEATHER_TYPE);
            if (this.weatherType.isEmpty()) this.weatherType = "normal";
            this.lightingType = config.getString(TAG_LIGHTING_TYPE);
            if (this.lightingType.isEmpty()) this.lightingType = "normal";
            this.biomeController = config.getString(TAG_BIOME_CONTROLLER);
            if (this.biomeController.isEmpty()) this.biomeController = "native";
            this.skyColor = config.contains(TAG_SKY_COLOR) ? config.getInt(TAG_SKY_COLOR) : -1;
            this.fogColor = config.contains(TAG_FOG_COLOR) ? config.getInt(TAG_FOG_COLOR) : -1;
            this.grassColors.clear();
            if (config.contains(TAG_GRASS_COLORS)) {
                int[] arr = config.getIntArray(TAG_GRASS_COLORS);
                for (int c : arr) {
                    this.grassColors.add(c);
                }
            } else if (config.contains(TAG_GRASS_COLOR)) {
                this.grassColors.add(config.getInt(TAG_GRASS_COLOR));
            }
            this.foliageColor = config.contains(TAG_FOLIAGE_COLOR) ? config.getInt(TAG_FOLIAGE_COLOR) : -1;
            this.waterColor = config.contains(TAG_WATER_COLOR) ? config.getInt(TAG_WATER_COLOR) : -1;
            this.cloudColor = config.contains(TAG_CLOUD_COLOR) ? config.getInt(TAG_CLOUD_COLOR) : -1;
            this.nightSkyColor = config.contains(TAG_NIGHT_SKY_COLOR) ? config.getInt(TAG_NIGHT_SKY_COLOR) : -1;
            this.horizonColor = config.contains(TAG_HORIZON_COLOR) ? config.getInt(TAG_HORIZON_COLOR) : -1;
            this.sunsetColor = config.contains(TAG_SUNSET_COLOR) ? config.getInt(TAG_SUNSET_COLOR) : -1;
            this.sunVisible = !config.contains(TAG_SUN_VISIBLE) || config.getBoolean(TAG_SUN_VISIBLE);
            this.moonVisible = !config.contains(TAG_MOON_VISIBLE) || config.getBoolean(TAG_MOON_VISIBLE);
            this.starsVisible = !config.contains(TAG_STARS_VISIBLE) || config.getBoolean(TAG_STARS_VISIBLE);
            this.starType = config.getString(TAG_STAR_TYPE);
            if (this.starType.isEmpty()) this.starType = "normal";
            this.pvpEnabled = !config.contains(TAG_PVP_ENABLED) || config.getBoolean(TAG_PVP_ENABLED);
            this.meteorsEnabled = config.getBoolean(TAG_METEORS_ENABLED);
            this.lightningEnabled = config.getBoolean(TAG_LIGHTNING_ENABLED);
            this.explosionsEnabled = config.getBoolean(TAG_EXPLOSIONS_ENABLED);
            this.acceleratedEnabled = config.getBoolean(TAG_ACCELERATED_ENABLED);
            this.scorchedEnabled = config.getBoolean(TAG_SCORCHED_ENABLED);
            this.horizonHidden = config.getBoolean(TAG_HORIZON_HIDDEN);
            this.denseOresEnabled = config.getBoolean(TAG_DENSE_ORES_ENABLED);
            this.hugeTreesEnabled = config.getBoolean(TAG_HUGE_TREES_ENABLED);
            this.obelisksEnabled = config.getBoolean(TAG_OBELISKS_ENABLED);
            this.crystalsEnabled = config.getBoolean(TAG_CRYSTALS_ENABLED);
            this.rainbowEnabled = config.getBoolean(TAG_RAINBOW_ENABLED);
            this.starFissureEnabled = config.getBoolean(TAG_STAR_FISSURE_ENABLED);
            this.spikesEnabled = config.getBoolean(TAG_SPIKES_ENABLED);
            this.spheresEnabled = config.getBoolean(TAG_SPHERES_ENABLED);
            this.tendrilsEnabled = config.getBoolean(TAG_TENDRILS_ENABLED);
            this.timescale = config.contains(TAG_TIMESCALE) ? config.getFloat(TAG_TIMESCALE) : 1.0f;
            this.cloudHeight = config.contains(TAG_CLOUD_HEIGHT) ? config.getFloat(TAG_CLOUD_HEIGHT) : 192.0f;
            this.horizonHeight = config.contains(TAG_HORIZON_HEIGHT) ? config.getFloat(TAG_HORIZON_HEIGHT) : 0.0f;
            this.terrainMixMode = config.getString(TAG_TERRAIN_MIX_MODE);
            if (this.terrainMixMode.isEmpty()) this.terrainMixMode = "none";
            this.secondaryTerrainType = config.getString(TAG_SECONDARY_TERRAIN_TYPE);
            if (this.secondaryTerrainType.isEmpty()) this.secondaryTerrainType = "none";
        }

        // Load deck orders
        this.deckOrders.clear();
        if (tag.contains(TAG_DECK_ORDERS)) {
            CompoundTag decksTag = tag.getCompound(TAG_DECK_ORDERS);
            for (String deckName : decksTag.getAllKeys()) {
                ListTag cardsList = decksTag.getList(deckName, Tag.TAG_STRING);
                List<String> cards = new ArrayList<>();
                for (int i = 0; i < cardsList.size(); i++) {
                    cards.add(cardsList.getString(i));
                }
                this.deckOrders.put(deckName, cards);
            }
        }
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        tag.putInt(TAG_AGE_UID, ageUID);
        tag.putString(TAG_AGE_UUID, ageUUID.toString());
        tag.putString(TAG_AGE_NAME, ageName);
        tag.putFloat(TAG_INSTABILITY, instability);
        tag.putLong(TAG_CREATED_TIME, createdTime);

        // Save authors
        ListTag authorsList = new ListTag();
        for (String author : authors) {
            authorsList.add(net.minecraft.nbt.StringTag.valueOf(author));
        }
        tag.put(TAG_AUTHORS, authorsList);

        // Save pages
        ListTag pagesList = new ListTag();
        for (ItemStack page : pages) {
            if (!page.isEmpty()) {
                pagesList.add(page.save(new CompoundTag()));
            }
        }
        tag.put(TAG_PAGES, pagesList);

        // Save spawn
        tag.putBoolean(TAG_SPAWN_SET, spawnSet);
        if (spawnSet) {
            tag.putInt(TAG_SPAWN_X, spawnX);
            tag.putInt(TAG_SPAWN_Y, spawnY);
            tag.putInt(TAG_SPAWN_Z, spawnZ);
        }

        // Save age configuration
        CompoundTag config = new CompoundTag();
        config.putString(TAG_WEATHER_TYPE, weatherType);
        config.putString(TAG_LIGHTING_TYPE, lightingType);
        config.putString(TAG_BIOME_CONTROLLER, biomeController);
        if (skyColor != -1) config.putInt(TAG_SKY_COLOR, skyColor);
        if (fogColor != -1) config.putInt(TAG_FOG_COLOR, fogColor);
        if (!grassColors.isEmpty()) {
            int[] arr = new int[grassColors.size()];
            for (int i = 0; i < grassColors.size(); i++) {
                arr[i] = grassColors.get(i);
            }
            config.putIntArray(TAG_GRASS_COLORS, arr);
        }
        if (foliageColor != -1) config.putInt(TAG_FOLIAGE_COLOR, foliageColor);
        if (waterColor != -1) config.putInt(TAG_WATER_COLOR, waterColor);
        if (cloudColor != -1) config.putInt(TAG_CLOUD_COLOR, cloudColor);
        if (nightSkyColor != -1) config.putInt(TAG_NIGHT_SKY_COLOR, nightSkyColor);
        if (horizonColor != -1) config.putInt(TAG_HORIZON_COLOR, horizonColor);
        if (sunsetColor != -1) config.putInt(TAG_SUNSET_COLOR, sunsetColor);
        config.putBoolean(TAG_SUN_VISIBLE, sunVisible);
        config.putBoolean(TAG_MOON_VISIBLE, moonVisible);
        config.putBoolean(TAG_STARS_VISIBLE, starsVisible);
        config.putString(TAG_STAR_TYPE, starType);
        config.putBoolean(TAG_PVP_ENABLED, pvpEnabled);
        config.putBoolean(TAG_METEORS_ENABLED, meteorsEnabled);
        config.putBoolean(TAG_LIGHTNING_ENABLED, lightningEnabled);
        config.putBoolean(TAG_EXPLOSIONS_ENABLED, explosionsEnabled);
        config.putBoolean(TAG_ACCELERATED_ENABLED, acceleratedEnabled);
        config.putBoolean(TAG_SCORCHED_ENABLED, scorchedEnabled);
        config.putBoolean(TAG_HORIZON_HIDDEN, horizonHidden);
        config.putBoolean(TAG_DENSE_ORES_ENABLED, denseOresEnabled);
        config.putBoolean(TAG_HUGE_TREES_ENABLED, hugeTreesEnabled);
        config.putBoolean(TAG_OBELISKS_ENABLED, obelisksEnabled);
        config.putBoolean(TAG_CRYSTALS_ENABLED, crystalsEnabled);
        config.putBoolean(TAG_RAINBOW_ENABLED, rainbowEnabled);
        config.putBoolean(TAG_STAR_FISSURE_ENABLED, starFissureEnabled);
        config.putBoolean(TAG_SPIKES_ENABLED, spikesEnabled);
        config.putBoolean(TAG_SPHERES_ENABLED, spheresEnabled);
        config.putBoolean(TAG_TENDRILS_ENABLED, tendrilsEnabled);
        config.putFloat(TAG_TIMESCALE, timescale);
        config.putFloat(TAG_CLOUD_HEIGHT, cloudHeight);
        config.putFloat(TAG_HORIZON_HEIGHT, horizonHeight);
        config.putString(TAG_TERRAIN_MIX_MODE, terrainMixMode);
        config.putString(TAG_SECONDARY_TERRAIN_TYPE, secondaryTerrainType);
        tag.put(TAG_CONFIG, config);

        // Save deck orders
        CompoundTag decksTag = new CompoundTag();
        for (Map.Entry<String, List<String>> entry : deckOrders.entrySet()) {
            ListTag cardsList = new ListTag();
            for (String card : entry.getValue()) {
                cardsList.add(net.minecraft.nbt.StringTag.valueOf(card));
            }
            decksTag.put(entry.getKey(), cardsList);
        }
        tag.put(TAG_DECK_ORDERS, decksTag);

        return tag;
    }

    /**
     * Gets the AgeData for a level, creating it if necessary.
     */
    public static AgeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    /**
     * Gets the AgeData for a level, or null if it doesn't exist.
     */
    @Nullable
    public static AgeData getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(factory(), DATA_NAME);
    }

    // Getters and setters

    public int getAgeUID() {
        return ageUID;
    }

    public void setAgeUID(int ageUID) {
        this.ageUID = ageUID;
        setDirty();
    }

    public UUID getAgeUUID() {
        return ageUUID;
    }

    public void setAgeUUID(UUID ageUUID) {
        this.ageUUID = ageUUID;
        setDirty();
    }

    public String getAgeName() {
        return ageName;
    }

    public void setAgeName(String ageName) {
        this.ageName = ageName != null ? ageName : "";
        setDirty();
    }

    public List<String> getAuthors() {
        return new ArrayList<>(authors);
    }

    public void addAuthor(String author) {
        if (author != null && !author.isEmpty() && !authors.contains(author)) {
            authors.add(author);
            setDirty();
        }
    }

    public List<ItemStack> getPages() {
        return new ArrayList<>(pages);
    }

    public void setPages(List<ItemStack> pages) {
        this.pages.clear();
        if (pages != null) {
            for (ItemStack page : pages) {
                if (!page.isEmpty()) {
                    this.pages.add(page.copy());
                }
            }
        }
        setDirty();
    }

    public void addPage(ItemStack page) {
        if (!page.isEmpty()) {
            this.pages.add(page.copy());
            setDirty();
        }
    }

    public float getInstability() {
        return instability;
    }

    public void setInstability(float instability) {
        this.instability = Math.max(0.0f, instability);
        setDirty();
    }

    public void addInstability(float amount) {
        setInstability(this.instability + amount);
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public boolean isSpawnSet() {
        return spawnSet;
    }

    public int getSpawnX() {
        return spawnX;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getSpawnZ() {
        return spawnZ;
    }

    public void setSpawn(int x, int y, int z) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.spawnSet = true;
        setDirty();
    }

    /**
     * Checks if this dimension is a Mystcraft Age.
     */
    public boolean isAge() {
        return ageUID > 0 || !pages.isEmpty();
    }

    /**
     * Gets a formatted display name for this age.
     */
    public String getDisplayName() {
        if (!ageName.isEmpty()) {
            return ageName;
        }
        return "Age " + ageUID;
    }

    /**
     * Gets a formatted authors string.
     */
    public String getAuthorsString() {
        if (authors.isEmpty()) {
            return "Unknown";
        }
        return String.join(", ", authors);
    }

    // --- Age Configuration Getters ---

    public String getWeatherType() { return weatherType; }
    public String getLightingType() { return lightingType; }
    public String getBiomeController() { return biomeController; }
    public int getSkyColor() { return skyColor; }
    public int getFogColor() { return fogColor; }
    public int getGrassColor() { return grassColors.isEmpty() ? -1 : grassColors.get(0); }
    public List<Integer> getGrassColors() { return new ArrayList<>(grassColors); }
    public int getFoliageColor() { return foliageColor; }
    public int getWaterColor() { return waterColor; }
    public int getCloudColor() { return cloudColor; }
    public int getNightSkyColor() { return nightSkyColor; }
    public int getHorizonColor() { return horizonColor; }
    public int getSunsetColor() { return sunsetColor; }
    public boolean isSunVisible() { return sunVisible; }
    public boolean isMoonVisible() { return moonVisible; }
    public boolean areStarsVisible() { return starsVisible; }
    public String getStarType() { return starType; }
    public boolean isPvPEnabled() { return pvpEnabled; }
    public boolean areMeteorsEnabled() { return meteorsEnabled; }
    public boolean isLightningEnabled() { return lightningEnabled; }
    public boolean areExplosionsEnabled() { return explosionsEnabled; }
    public boolean isAcceleratedEnabled() { return acceleratedEnabled; }
    public boolean isScorchedEnabled() { return scorchedEnabled; }
    public boolean isHorizonHidden() { return horizonHidden; }
    public boolean areDenseOresEnabled() { return denseOresEnabled; }
    public boolean areHugeTreesEnabled() { return hugeTreesEnabled; }
    public boolean areObelisksEnabled() { return obelisksEnabled; }
    public boolean areCrystalsEnabled() { return crystalsEnabled; }
    public boolean isRainbowEnabled() { return rainbowEnabled; }
    public boolean isStarFissureEnabled() { return starFissureEnabled; }
    public boolean areSpikesEnabled() { return spikesEnabled; }
    public boolean areSpheresEnabled() { return spheresEnabled; }
    public boolean areTendrilsEnabled() { return tendrilsEnabled; }
    public float getTimescale() { return timescale; }
    public float getCloudHeight() { return cloudHeight; }
    public float getHorizonHeight() { return horizonHeight; }
    public String getTerrainMixMode() { return terrainMixMode; }
    public String getSecondaryTerrainType() { return secondaryTerrainType; }

    /**
     * Copies configuration from an AgeDirectorImpl.
     * Call this after processing symbols to persist the Age configuration.
     */
    public void copyFromDirector(AgeDirectorImpl director) {
        this.weatherType = director.getWeatherType();
        this.lightingType = director.getLightingType();
        this.biomeController = director.getBiomeController();
        this.skyColor = director.getSkyColor();
        this.fogColor = director.getFogColor();
        this.grassColors.clear();
        this.grassColors.addAll(director.getGrassColors());
        this.foliageColor = director.getFoliageColor();
        this.waterColor = director.getWaterColor();
        this.cloudColor = director.getCloudColor();
        this.nightSkyColor = director.getNightSkyColor();
        this.horizonColor = director.getHorizonColor();
        this.sunsetColor = director.getSunsetColor();
        this.sunVisible = director.isSunVisible();
        this.moonVisible = director.isMoonVisible();
        this.starsVisible = director.areStarsVisible();
        this.starType = director.getStarType();
        this.pvpEnabled = director.isPvPEnabled();
        this.meteorsEnabled = director.areMeteorsEnabled();
        this.lightningEnabled = director.isLightningEnabled();
        this.explosionsEnabled = director.areExplosionsEnabled();
        this.acceleratedEnabled = director.isAcceleratedEnabled();
        this.scorchedEnabled = director.isScorchedEnabled();
        this.horizonHidden = director.isHorizonHidden();
        this.denseOresEnabled = director.areDenseOresEnabled();
        this.hugeTreesEnabled = director.areHugeTreesEnabled();
        this.obelisksEnabled = director.areObelisksEnabled();
        this.crystalsEnabled = director.areCrystalsEnabled();
        this.rainbowEnabled = director.isRainbowEnabled();
        this.starFissureEnabled = director.isStarFissureEnabled();
        this.spikesEnabled = director.areSpikesEnabled();
        this.spheresEnabled = director.areSpheresEnabled();
        this.tendrilsEnabled = director.areTendrilsEnabled();
        this.timescale = director.getTimescale();
        this.cloudHeight = director.getCloudHeight();
        this.horizonHeight = director.getHorizonHeight();
        this.terrainMixMode = director.getTerrainMixMode();
        this.secondaryTerrainType = director.getSecondaryTerrainType();
        this.instability = director.getInstability();
        setDirty();

        Mystcraft.LOGGER.info("[AgeData] Age {} colors from director: sky=0x{}, fog=0x{}, grass=0x{}, foliage=0x{}, water=0x{}, cloud=0x{}, nightSky=0x{}, sunset=0x{}",
                ageUID,
                skyColor != -1 ? Integer.toHexString(skyColor) : "none",
                fogColor != -1 ? Integer.toHexString(fogColor) : "none",
                !grassColors.isEmpty() ? grassColors.stream().map(c -> Integer.toHexString(c)).collect(java.util.stream.Collectors.joining(",")) : "none",
                foliageColor != -1 ? Integer.toHexString(foliageColor) : "none",
                waterColor != -1 ? Integer.toHexString(waterColor) : "none",
                cloudColor != -1 ? Integer.toHexString(cloudColor) : "none",
                nightSkyColor != -1 ? Integer.toHexString(nightSkyColor) : "none",
                sunsetColor != -1 ? Integer.toHexString(sunsetColor) : "none");
    }

    // --- Deck Order Methods ---

    /**
     * Gets the saved deck order for a deck.
     *
     * @param deckName The deck name
     * @return The saved card order, or null if not saved
     */
    @Nullable
    public List<String> getSavedDeckOrder(String deckName) {
        List<String> order = deckOrders.get(deckName);
        return order != null ? new ArrayList<>(order) : null;
    }

    /**
     * Saves the deck order for persistence.
     *
     * @param deckName The deck name
     * @param cards    The card order
     */
    public void saveDeckOrder(String deckName, java.util.Collection<String> cards) {
        deckOrders.put(deckName, new ArrayList<>(cards));
        setDirty();
    }

    /**
     * Clears all saved deck orders.
     */
    public void clearDeckOrders() {
        deckOrders.clear();
        setDirty();
    }
}
