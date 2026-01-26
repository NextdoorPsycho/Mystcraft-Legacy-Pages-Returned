package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.world.logic.ICelestial;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.Collections;
import java.util.List;

/**
 * Packet sent from server to client to sync Age data.
 * Used to keep clients informed about Age properties.
 */
public record SyncAgeDataPacket(int ageUID, CompoundTag data) {

    public static void encode(SyncAgeDataPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.ageUID);
        buf.writeNbt(packet.data);
    }

    public static SyncAgeDataPacket decode(FriendlyByteBuf buf) {
        int ageUID = buf.readVarInt();
        CompoundTag data = buf.readNbt();
        return new SyncAgeDataPacket(ageUID, data);
    }

    public static void handle(SyncAgeDataPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle on client
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            // Store the age data in client-side cache
            ClientAgeDataCache.setAgeData(packet.ageUID, packet.data);

            // Debug logging to track sync flow
            CompoundTag config = packet.data.contains("AgeConfig") ? packet.data.getCompound("AgeConfig") : null;
            int configKeys = config != null ? config.getAllKeys().size() : 0;
            float instability = packet.data.contains("Instability") ? packet.data.getFloat("Instability") : 0;
            Mystcraft.LOGGER.info("Received Age data for UID {} - {} config keys, instability: {}",
                    packet.ageUID, configKeys, instability);

            // Log specific color values for debugging
            if (config != null) {
                if (config.contains("GrassColor")) {
                    Mystcraft.LOGGER.debug("  GrassColor: 0x{}", Integer.toHexString(config.getInt("GrassColor")));
                }
                if (config.contains("FoliageColor")) {
                    Mystcraft.LOGGER.debug("  FoliageColor: 0x{}", Integer.toHexString(config.getInt("FoliageColor")));
                }
                if (config.contains("WaterColor")) {
                    Mystcraft.LOGGER.debug("  WaterColor: 0x{}", Integer.toHexString(config.getInt("WaterColor")));
                }
                if (config.contains("SkyColor")) {
                    Mystcraft.LOGGER.debug("  SkyColor: 0x{}", Integer.toHexString(config.getInt("SkyColor")));
                }
                if (config.contains("FogColor")) {
                    Mystcraft.LOGGER.debug("  FogColor: 0x{}", Integer.toHexString(config.getInt("FogColor")));
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Client-side cache for Age data.
     * Stores synced Age information for rendering and display.
     */
    public static class ClientAgeDataCache {
        private static final java.util.Map<Integer, CompoundTag> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

        public static void setAgeData(int ageUID, CompoundTag data) {
            if (data != null) {
                CACHE.put(ageUID, data);
            } else {
                CACHE.remove(ageUID);
            }
        }

        public static CompoundTag getAgeData(int ageUID) {
            return CACHE.get(ageUID);
        }

        public static void clear() {
            CACHE.clear();
        }

        public static String getAgeName(int ageUID) {
            CompoundTag data = CACHE.get(ageUID);
            if (data != null && data.contains("AgeName")) {
                return data.getString("AgeName");
            }
            return "Age " + ageUID;
        }

        public static float getInstability(int ageUID) {
            CompoundTag data = CACHE.get(ageUID);
            if (data != null && data.contains("Instability")) {
                return data.getFloat("Instability");
            }
            return 0.0f;
        }

        // ========================= Rendering Configuration Getters =========================

        private static CompoundTag getConfig(int ageUID) {
            CompoundTag data = CACHE.get(ageUID);
            if (data != null && data.contains("AgeConfig")) {
                return data.getCompound("AgeConfig");
            }
            return null;
        }

        public static int getSkyColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("SkyColor") ? config.getInt("SkyColor") : -1;
        }

        public static int getFogColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("FogColor") ? config.getInt("FogColor") : -1;
        }

        public static int getGrassColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("GrassColor") ? config.getInt("GrassColor") : -1;
        }

        public static int getFoliageColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("FoliageColor") ? config.getInt("FoliageColor") : -1;
        }

        public static int getWaterColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("WaterColor") ? config.getInt("WaterColor") : -1;
        }

        public static int getCloudColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("CloudColor") ? config.getInt("CloudColor") : -1;
        }

        public static int getNightSkyColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("NightSkyColor") ? config.getInt("NightSkyColor") : -1;
        }

        public static boolean isSunVisible(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config == null || !config.contains("SunVisible") || config.getBoolean("SunVisible");
        }

        public static boolean isMoonVisible(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config == null || !config.contains("MoonVisible") || config.getBoolean("MoonVisible");
        }

        public static boolean areStarsVisible(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config == null || !config.contains("StarsVisible") || config.getBoolean("StarsVisible");
        }

        public static String getStarType(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            if (config != null && config.contains("StarType")) {
                String type = config.getString("StarType");
                return type.isEmpty() ? "normal" : type;
            }
            return "normal";
        }

        public static String getLightingType(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            if (config != null && config.contains("LightingType")) {
                String type = config.getString("LightingType");
                return type.isEmpty() ? "normal" : type;
            }
            return "normal";
        }

        public static boolean isHorizonHidden(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.getBoolean("HorizonHidden");
        }

        public static boolean isRainbowEnabled(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.getBoolean("RainbowEnabled");
        }

        public static boolean areMeteorsEnabled(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.getBoolean("MeteorsEnabled");
        }

        public static boolean isLightningEnabled(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.getBoolean("LightningEnabled");
        }

        public static boolean areExplosionsEnabled(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.getBoolean("ExplosionsEnabled");
        }

        public static int getSunsetColor(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("SunsetColor") ? config.getInt("SunsetColor") : -1;
        }

        public static float getCloudHeight(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("CloudHeight") ? config.getFloat("CloudHeight") : 192.0f;
        }

        public static float getHorizonHeight(int ageUID) {
            CompoundTag config = getConfig(ageUID);
            return config != null && config.contains("HorizonHeight") ? config.getFloat("HorizonHeight") : 0.0f;
        }

        /**
         * Gets the list of celestial objects for the age.
         * Currently returns empty list - celestials are not synced via packets yet.
         * This will be populated when custom celestial symbols are implemented.
         *
         * @param ageUID The age UID
         * @return List of celestials (may be empty)
         */
        public static List<ICelestial> getCelestials(int ageUID) {
            // TODO: Implement celestial serialization/deserialization when celestial symbols are added
            // For now, return empty list to use default sun/moon rendering
            return Collections.emptyList();
        }
    }
}
