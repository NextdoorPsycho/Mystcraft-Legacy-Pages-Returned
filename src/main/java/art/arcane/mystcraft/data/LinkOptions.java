package art.arcane.mystcraft.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Utility class for reading and writing link data to/from NBT.
 * Handles dimension IDs, spawn positions, display names, and link flags.
 */
public class LinkOptions {

    private CompoundTag data;

    public LinkOptions(@Nullable CompoundTag data) {
        if (data != null) {
            this.data = data.copy();
        } else {
            this.data = new CompoundTag();
        }
    }

    @NotNull
    public CompoundTag getTagCompound() {
        return data;
    }

    public LinkOptions copy() {
        return new LinkOptions(this.getTagCompound());
    }

    @NotNull
    public String getDisplayName() {
        return getDisplayName(data);
    }

    @Nullable
    public Integer getDimensionUID() {
        return getDimensionUID(data);
    }

    @Nullable
    public UUID getTargetUUID() {
        return getUUID(data);
    }

    @Nullable
    public BlockPos getSpawn() {
        return getSpawn(data);
    }

    public float getSpawnYaw() {
        return getSpawnYaw(data);
    }

    public boolean getFlag(String flag) {
        return getFlag(data, flag);
    }

    public void setFlag(String flag, boolean value) {
        data = setFlag(data, flag, value);
    }

    public String getProperty(String prop) {
        return getProperty(data, prop);
    }

    public void setProperty(String prop, String value) {
        data = setProperty(data, prop, value);
    }

    public void setDisplayName(@NotNull String displayname) {
        data = setDisplayName(data, displayname);
    }

    public void setDimensionUID(int uid) {
        data = setDimensionUID(data, uid);
    }

    public void setTargetUUID(@Nullable UUID uuid) {
        data = setUUID(data, uuid);
    }

    public void setSpawn(@Nullable BlockPos spawn) {
        data = setSpawn(data, spawn);
    }

    public void setSpawnYaw(float spawnyaw) {
        data = setSpawnYaw(data, spawnyaw);
    }

    // Static utility methods

    public static CompoundTag setDisplayName(@Nullable CompoundTag nbttagcompound, @NotNull String name) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        nbttagcompound.putString("DisplayName", name);
        return nbttagcompound;
    }

    @NotNull
    public static String getDisplayName(@Nullable CompoundTag nbttagcompound) {
        if (nbttagcompound != null && nbttagcompound.contains("DisplayName")) {
            return nbttagcompound.getString("DisplayName");
        }
        if (nbttagcompound != null && nbttagcompound.contains("agename")) {
            return nbttagcompound.getString("agename");
        }
        return "???";
    }

    public static CompoundTag setFlag(CompoundTag nbttagcompound, String flag, boolean val) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        getFlagCompound(nbttagcompound).putBoolean(flag, val);
        return nbttagcompound;
    }

    public static boolean getFlag(CompoundTag nbttagcompound, String flag) {
        if (nbttagcompound != null && getFlagCompound(nbttagcompound).contains(flag)) {
            return getFlagCompound(nbttagcompound).getBoolean(flag);
        }
        return false;
    }

    public static CompoundTag setProperty(CompoundTag nbttagcompound, String flag, String value) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        CompoundTag props = getPropertyCompound(nbttagcompound);
        if (value == null) {
            props.remove(flag);
        } else {
            props.putString(flag, value);
        }
        return nbttagcompound;
    }

    @Nullable
    public static String getProperty(CompoundTag nbttagcompound, String flag) {
        if (nbttagcompound != null && getPropertyCompound(nbttagcompound).contains(flag)) {
            return getPropertyCompound(nbttagcompound).getString(flag);
        }
        return null;
    }

    public static CompoundTag setDimensionUID(CompoundTag nbttagcompound, int uid) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        nbttagcompound.putInt("Dimension", uid);
        return nbttagcompound;
    }

    @Nullable
    public static Integer getDimensionUID(CompoundTag nbttagcompound) {
        if (nbttagcompound != null && nbttagcompound.contains("Dimension")) {
            return nbttagcompound.getInt("Dimension");
        }
        if (nbttagcompound != null && nbttagcompound.contains("AgeUID")) {
            return nbttagcompound.getInt("AgeUID");
        }
        return null;
    }

    public static CompoundTag setUUID(CompoundTag nbttagcompound, @Nullable UUID uuid) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        if (uuid != null) {
            nbttagcompound.putString("TargetUUID", uuid.toString());
        } else {
            nbttagcompound.remove("TargetUUID");
        }
        return nbttagcompound;
    }

    @Nullable
    public static UUID getUUID(CompoundTag nbttagcompound) {
        if (nbttagcompound != null && nbttagcompound.contains("TargetUUID")) {
            return UUID.fromString(nbttagcompound.getString("TargetUUID"));
        }
        return null;
    }

    public static CompoundTag setSpawn(CompoundTag nbttagcompound, @Nullable BlockPos coords) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        if (coords != null) {
            nbttagcompound.putInt("SpawnX", coords.getX());
            nbttagcompound.putInt("SpawnY", coords.getY());
            nbttagcompound.putInt("SpawnZ", coords.getZ());
        }
        return nbttagcompound;
    }

    @Nullable
    public static BlockPos getSpawn(CompoundTag nbttagcompound) {
        if (nbttagcompound != null && nbttagcompound.contains("SpawnX") && nbttagcompound.contains("SpawnY") && nbttagcompound.contains("SpawnZ")) {
            return new BlockPos(nbttagcompound.getInt("SpawnX"), nbttagcompound.getInt("SpawnY"), nbttagcompound.getInt("SpawnZ"));
        }
        return null;
    }

    public static CompoundTag setSpawnYaw(CompoundTag nbttagcompound, float yaw) {
        if (nbttagcompound == null) {
            nbttagcompound = new CompoundTag();
        }
        nbttagcompound.putFloat("SpawnYaw", yaw);
        return nbttagcompound;
    }

    public static float getSpawnYaw(CompoundTag nbttagcompound) {
        if (nbttagcompound != null && nbttagcompound.contains("SpawnYaw")) {
            return nbttagcompound.getFloat("SpawnYaw");
        }
        return 180;
    }

    @NotNull
    private static CompoundTag getFlagCompound(CompoundTag nbttagcompound) {
        if (!nbttagcompound.contains("Flags")) {
            nbttagcompound.put("Flags", new CompoundTag());
        }
        return nbttagcompound.getCompound("Flags");
    }

    @NotNull
    private static CompoundTag getPropertyCompound(CompoundTag nbttagcompound) {
        if (!nbttagcompound.contains("Props")) {
            nbttagcompound.put("Props", new CompoundTag());
        }
        return nbttagcompound.getCompound("Props");
    }
}
