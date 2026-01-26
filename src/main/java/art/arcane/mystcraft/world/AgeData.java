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

    private int ageUID;
    private UUID ageUUID;
    private String ageName = "";
    private final List<String> authors = new ArrayList<>();
    private final List<ItemStack> pages = new ArrayList<>();
    private float instability = 0.0f;
    private long createdTime;
    private boolean spawnSet = false;
    private int spawnX, spawnY, spawnZ;

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

    public void clearSpawn() {
        this.spawnSet = false;
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
}
