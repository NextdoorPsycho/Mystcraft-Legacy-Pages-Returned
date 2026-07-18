package art.arcane.mystcraft.util;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.AgeData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Resolves and caches player-head texture data for personal pocket proxy rendering.
 */
public final class PocketHeadUtils {

  private static final int FACE_SIZE = 8;
  private static final int FACE_PIXELS = FACE_SIZE * FACE_SIZE;

  private static final int SKIN_WIDTH = 64;
  private static final int SKIN_HEIGHT_LEGACY = 32;
  private static final int SKIN_HEIGHT_MODERN = 64;

  private static final PaletteEntry[] PALETTE = new PaletteEntry[]{
      new PaletteEntry("minecraft:white_wool", 0xF9FFFE),
      new PaletteEntry("minecraft:orange_wool", 0xF9801D),
      new PaletteEntry("minecraft:magenta_wool", 0xC74EBD),
      new PaletteEntry("minecraft:light_blue_wool", 0x3AB3DA),
      new PaletteEntry("minecraft:yellow_wool", 0xFED83D),
      new PaletteEntry("minecraft:lime_wool", 0x80C71F),
      new PaletteEntry("minecraft:pink_wool", 0xF38BAA),
      new PaletteEntry("minecraft:gray_wool", 0x474F52),
      new PaletteEntry("minecraft:light_gray_wool", 0x9D9D97),
      new PaletteEntry("minecraft:cyan_wool", 0x169C9C),
      new PaletteEntry("minecraft:purple_wool", 0x8932B8),
      new PaletteEntry("minecraft:blue_wool", 0x3C44AA),
      new PaletteEntry("minecraft:brown_wool", 0x835432),
      new PaletteEntry("minecraft:green_wool", 0x5E7C16),
      new PaletteEntry("minecraft:red_wool", 0xB02E26),
      new PaletteEntry("minecraft:black_wool", 0x1D1D21)
  };

  private static final Set<String> ALLOWED_HEAD_BLOCK_IDS;

  static {
    Set<String> allowed = new HashSet<>();
    for (PaletteEntry entry : PALETTE) {
      allowed.add(entry.blockId);
    }
    ALLOWED_HEAD_BLOCK_IDS = Collections.unmodifiableSet(allowed);
  }

  private PocketHeadUtils() {
  }

  public static Map<AgeData.PocketHeadFace, List<String>> buildPocketHeadBlocks(MinecraftServer server, UUID owner) {
    GameProfile profile = resolveProfile(server, owner);
    String skinUrl = null;

    if (profile != null) {
      skinUrl = getSkinUrl(server, profile);
    }

    if (skinUrl == null) {
      Mystcraft.LOGGER.debug("[PocketHead] Local resolution failed, trying Mojang API for UUID {}", owner);
      skinUrl = fetchSkinUrlFromMojangApiByUuid(owner);
    }

    if (skinUrl == null) {
      Mystcraft.LOGGER.warn("[PocketHead] No skin URL for {}", owner);
      return null;
    }

    BufferedImage skin = downloadSkin(skinUrl);
    if (skin == null) {
      Mystcraft.LOGGER.warn("[PocketHead] Failed to download skin for {}", owner);
      return null;
    }

    Map<AgeData.PocketHeadFace, int[]> facePixels = extractHeadFaces(skin);
    if (facePixels == null) {
      return null;
    }
    return mapFacesToBlocks(facePixels);
  }

  public static Map<AgeData.PocketHeadFace, List<String>> buildPocketHeadBlocksByName(MinecraftServer server, String name) {

    GameProfile profile = resolveProfileByName(server, name);
    String skinUrl = null;

    if (profile != null) {
      skinUrl = getSkinUrl(server, profile);
    }

    if (skinUrl == null) {
      Mystcraft.LOGGER.debug("[PocketHead] Local resolution failed, trying Mojang API for {}", name);
      skinUrl = fetchSkinUrlFromMojangApi(name);
    }

    if (skinUrl == null) {
      Mystcraft.LOGGER.warn("[PocketHead] No skin URL for {}", name);
      return null;
    }

    BufferedImage skin = downloadSkin(skinUrl);
    if (skin == null) {
      Mystcraft.LOGGER.warn("[PocketHead] Failed to download skin for {}", name);
      return null;
    }

    Map<AgeData.PocketHeadFace, int[]> facePixels = extractHeadFaces(skin);
    if (facePixels == null) {
      return null;
    }
    return mapFacesToBlocks(facePixels);
  }

  public static Map<AgeData.PocketHeadFace, List<String>> mapFacesToBlocksFromPixels(Map<AgeData.PocketHeadFace, int[]> facePixels) {
    return mapFacesToBlocks(facePixels);
  }

  public static boolean isValidHeadBlockMap(Map<AgeData.PocketHeadFace, List<String>> blocks) {
    if (blocks == null || blocks.size() != AgeData.PocketHeadFace.values().length) {
      return false;
    }
    for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
      List<String> list = blocks.get(face);
      if (list == null || list.size() != FACE_PIXELS) {
        return false;
      }
      for (String blockId : list) {
        if (!isAllowedHeadBlockId(blockId)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns whether a client-supplied pocket-head pixel is one of the wool
   * blocks produced by this class's skin palette.
   */
  public static boolean isAllowedHeadBlockId(@Nullable String blockId) {
    return blockId != null && ALLOWED_HEAD_BLOCK_IDS.contains(blockId);
  }

  private static GameProfile resolveProfile(MinecraftServer server, UUID owner) {
    GameProfile profile = new GameProfile(owner, "");
    if (server.getProfileCache() != null) {
      Optional<GameProfile> cached = server.getProfileCache().get(owner);
      profile = cached.orElse(profile);
    }
    try {
      GameProfile filled = server.getSessionService().fillProfileProperties(profile, true);
      return filled != null ? filled : profile;
    } catch (Exception e) {
      Mystcraft.LOGGER.debug("[PocketHead] Failed to fill profile properties for {}", owner);
    }
    return profile;
  }

  @Nullable
  private static GameProfile resolveProfileByName(MinecraftServer server, String name) {
    if (server.getProfileCache() == null) {
      return null;
    }
    Optional<GameProfile> cached = server.getProfileCache().get(name);
    if (cached.isEmpty()) {
      return null;
    }
    GameProfile profile = cached.get();
    try {
      GameProfile filled = server.getSessionService().fillProfileProperties(profile, true);
      return filled != null ? filled : profile;
    } catch (Exception e) {
      Mystcraft.LOGGER.debug("[PocketHead] Failed to fill profile properties for {}", name);
    }
    return profile;
  }

  private static String getSkinUrl(MinecraftServer server, GameProfile profile) {
    Property textures = profile.getProperties().get("textures").stream().findFirst().orElse(null);
    if (textures == null) {
      try {
        com.mojang.authlib.minecraft.MinecraftSessionService session = server.getSessionService();
        java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> map =
            session.getTextures(profile, true);
        com.mojang.authlib.minecraft.MinecraftProfileTexture skin =
            map.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
        return skin != null ? skin.getUrl() : null;
      } catch (Exception e) {
        return null;
      }
    }
    try {
      String propValue = getPropertyValue(textures);
      if (propValue == null) {
        return null;
      }
      String decoded = new String(Base64.getDecoder().decode(propValue), StandardCharsets.UTF_8);
      JsonObject root = JsonParser.parseString(decoded).getAsJsonObject();
      JsonObject texturesObj = root.getAsJsonObject("textures");
      if (texturesObj == null || !texturesObj.has("SKIN")) {
        return null;
      }
      JsonObject skin = texturesObj.getAsJsonObject("SKIN");
      JsonElement urlElement = skin.get("url");
      return urlElement != null ? urlElement.getAsString() : null;
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[PocketHead] Failed to parse textures property for {}", profile.getName(), e);
      return null;
    }
  }

  @Nullable
  private static String fetchSkinUrlFromMojangApiByUuid(UUID uuid) {

    String uuidStr = uuid.toString().replace("-", "");
    return fetchSkinUrlFromSessionServer(uuidStr);
  }

  @Nullable
  private static String fetchSkinUrlFromMojangApi(String playerName) {

    String uuid = fetchUuidFromMojangApi(playerName);
    if (uuid == null) {
      Mystcraft.LOGGER.debug("[PocketHead] Could not fetch UUID for player {} from Mojang API", playerName);
      return null;
    }

    return fetchSkinUrlFromSessionServer(uuid);
  }

  @Nullable
  private static String fetchSkinUrlFromSessionServer(String uuid) {
    HttpURLConnection connection = null;
    try {
      String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
      connection = (HttpURLConnection) new URL(profileUrl).openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.connect();

      if (connection.getResponseCode() != 200) {
        Mystcraft.LOGGER.debug("[PocketHead] Session server returned {} for UUID {}", connection.getResponseCode(), uuid);
        return null;
      }

      try (InputStream in = connection.getInputStream()) {
        String response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(response).getAsJsonObject();

        if (!root.has("properties")) {
          return null;
        }

        for (JsonElement prop : root.getAsJsonArray("properties")) {
          JsonObject propObj = prop.getAsJsonObject();
          if ("textures".equals(propObj.get("name").getAsString())) {
            String value = propObj.get("value").getAsString();
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            JsonObject textureRoot = JsonParser.parseString(decoded).getAsJsonObject();
            JsonObject textures = textureRoot.getAsJsonObject("textures");
            if (textures != null && textures.has("SKIN")) {
              JsonObject skin = textures.getAsJsonObject("SKIN");
              JsonElement urlElement = skin.get("url");
              if (urlElement != null) {
                String skinUrl = urlElement.getAsString();
                Mystcraft.LOGGER.debug("[PocketHead] Found skin URL for UUID {}: {}", uuid, skinUrl);
                return skinUrl;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.debug("[PocketHead] Error fetching profile from session server for UUID {}: {}", uuid, e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    return null;
  }

  @Nullable
  private static String fetchUuidFromMojangApi(String playerName) {
    HttpURLConnection connection = null;
    try {
      String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
      connection = (HttpURLConnection) new URL(apiUrl).openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.connect();

      if (connection.getResponseCode() != 200) {
        Mystcraft.LOGGER.debug("[PocketHead] Mojang API returned {} for player {}", connection.getResponseCode(), playerName);
        return null;
      }

      try (InputStream in = connection.getInputStream()) {
        String response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        if (root.has("id")) {
          String uuid = root.get("id").getAsString();
          Mystcraft.LOGGER.debug("[PocketHead] Resolved {} to UUID {}", playerName, uuid);
          return uuid;
        }
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.debug("[PocketHead] Error fetching UUID from Mojang API for {}: {}", playerName, e.getMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    return null;
  }

  private static String getPropertyValue(Property property) {
    return property.getValue();
  }

  private static BufferedImage downloadSkin(String url) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.setInstanceFollowRedirects(true);
      connection.connect();
      try (InputStream in = connection.getInputStream()) {
        return ImageIO.read(in);
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[PocketHead] Error downloading skin from {}", url, e);
      return null;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private static Map<AgeData.PocketHeadFace, int[]> extractHeadFaces(BufferedImage skin) {

    int width = skin.getWidth();
    int height = skin.getHeight();

    if (width != SKIN_WIDTH || (height != SKIN_HEIGHT_LEGACY && height != SKIN_HEIGHT_MODERN)) {
      Mystcraft.LOGGER.warn("[PocketHead] Invalid skin dimensions: {}x{} (expected {}x{} or {}x{})",
          width, height, SKIN_WIDTH, SKIN_HEIGHT_LEGACY, SKIN_WIDTH, SKIN_HEIGHT_MODERN);
      return null;
    }

    Map<AgeData.PocketHeadFace, int[]> faces = new EnumMap<>(AgeData.PocketHeadFace.class);

    faces.put(AgeData.PocketHeadFace.FRONT, extractFace(skin, 8, 8));
    faces.put(AgeData.PocketHeadFace.BACK, extractFace(skin, 24, 8));
    faces.put(AgeData.PocketHeadFace.RIGHT, extractFace(skin, 16, 8));
    faces.put(AgeData.PocketHeadFace.LEFT, extractFace(skin, 0, 8));
    faces.put(AgeData.PocketHeadFace.TOP, extractFace(skin, 8, 0));
    faces.put(AgeData.PocketHeadFace.BOTTOM, extractFace(skin, 16, 0));

    return faces;
  }

  private static int[] extractFace(BufferedImage skin, int startX, int startY) {
    int[] pixels = new int[FACE_PIXELS];
    for (int y = 0; y < FACE_SIZE; y++) {
      for (int x = 0; x < FACE_SIZE; x++) {
        int color = skin.getRGB(startX + x, startY + y);
        pixels[y * FACE_SIZE + x] = color & 0xFFFFFF;
      }
    }
    return pixels;
  }

  private static Map<AgeData.PocketHeadFace, List<String>> mapFacesToBlocks(Map<AgeData.PocketHeadFace, int[]> facePixels) {
    Map<AgeData.PocketHeadFace, List<String>> result = new EnumMap<>(AgeData.PocketHeadFace.class);
    for (Map.Entry<AgeData.PocketHeadFace, int[]> entry : facePixels.entrySet()) {
      int[] pixels = entry.getValue();
      if (pixels == null || pixels.length != FACE_PIXELS) {
        continue;
      }
      List<String> blocks = new ArrayList<>(FACE_PIXELS);
      for (int color : pixels) {
        blocks.add(matchPalette(color));
      }
      result.put(entry.getKey(), blocks);
    }
    return result.size() == AgeData.PocketHeadFace.values().length ? result : null;
  }

  private static String matchPalette(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    int best = Integer.MAX_VALUE;
    String bestId = "minecraft:white_wool";
    for (PaletteEntry entry : PALETTE) {
      int dr = r - entry.r;
      int dg = g - entry.g;
      int db = b - entry.b;
      int dist = dr * dr + dg * dg + db * db;
      if (dist < best) {
        best = dist;
        bestId = entry.blockId;
      }
    }
    return bestId;
  }

  private static final class PaletteEntry {
    private final String blockId;
    private final int r;
    private final int g;
    private final int b;

    private PaletteEntry(String blockId, int rgb) {
      this.blockId = blockId;
      this.r = (rgb >> 16) & 0xFF;
      this.g = (rgb >> 8) & 0xFF;
      this.b = rgb & 0xFF;
    }
  }
}
