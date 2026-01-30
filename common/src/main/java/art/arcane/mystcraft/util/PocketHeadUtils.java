package art.arcane.mystcraft.util;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.world.AgeData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PocketHeadUtils {

  private static final int FACE_SIZE = 8;
  private static final int FACE_PIXELS = FACE_SIZE * FACE_SIZE;

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
      new PaletteEntry("minecraft:black_wool", 0x1D1D21),
      new PaletteEntry("minecraft:oak_planks", 0xC6A86A),
      new PaletteEntry("minecraft:spruce_planks", 0x6B4F2A),
      new PaletteEntry("minecraft:birch_planks", 0xD7CE8D),
      new PaletteEntry("minecraft:jungle_planks", 0xB88757),
      new PaletteEntry("minecraft:acacia_planks", 0xB96A50),
      new PaletteEntry("minecraft:dark_oak_planks", 0x4F3A1D),
      new PaletteEntry("minecraft:mangrove_planks", 0x773F32),
      new PaletteEntry("minecraft:cherry_planks", 0xE8B6B3),
      new PaletteEntry("minecraft:bamboo_planks", 0xD1C04A)
  };

  private PocketHeadUtils() {
  }

  public static Map<AgeData.PocketHeadFace, List<String>> buildPocketHeadBlocks(MinecraftServer server, UUID owner) {
    GameProfile profile = resolveProfile(server, owner);
    if (profile == null) {
      Mystcraft.LOGGER.warn("[PocketHead] Unable to resolve profile for {}", owner);
      return null;
    }

    String skinUrl = getSkinUrl(server, profile);
    if (skinUrl == null) {
      Mystcraft.LOGGER.warn("[PocketHead] No skin URL for {}", profile.getName());
      return null;
    }

    BufferedImage skin = downloadSkin(skinUrl);
    if (skin == null) {
      Mystcraft.LOGGER.warn("[PocketHead] Failed to download skin for {}", profile.getName());
      return null;
    }

    Map<AgeData.PocketHeadFace, int[]> facePixels = extractHeadFaces(skin);
    return mapFacesToBlocks(facePixels);
  }

  private static GameProfile resolveProfile(MinecraftServer server, UUID owner) {
    Optional<GameProfile> cached = server.getProfileCache().get(owner);
    GameProfile profile = cached.orElseGet(() -> new GameProfile(owner, ""));
    try {
      Object sessionService = server.getSessionService();
      java.lang.reflect.Method fill = sessionService.getClass().getMethod("fillProfileProperties", GameProfile.class, boolean.class);
      Object filled = fill.invoke(sessionService, profile, true);
      if (filled instanceof GameProfile filledProfile) {
        return filledProfile;
      }
    } catch (Exception e) {
      Mystcraft.LOGGER.debug("[PocketHead] fillProfileProperties unavailable or failed for {}", owner);
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

  private static String getPropertyValue(Property property) {
    try {
      java.lang.reflect.Method method = property.getClass().getMethod("getValue");
      Object value = method.invoke(property);
      if (value instanceof String str) {
        return str;
      }
    } catch (Exception ignored) {
    }
    try {
      java.lang.reflect.Method method = property.getClass().getMethod("value");
      Object value = method.invoke(property);
      if (value instanceof String str) {
        return str;
      }
    } catch (Exception ignored) {
    }
    try {
      java.lang.reflect.Field field = property.getClass().getDeclaredField("value");
      field.setAccessible(true);
      Object value = field.get(property);
      if (value instanceof String str) {
        return str;
      }
    } catch (Exception ignored) {
    }
    return null;
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
    boolean hasOverlay = skin.getHeight() >= 64;
    Map<AgeData.PocketHeadFace, int[]> faces = new EnumMap<>(AgeData.PocketHeadFace.class);

    faces.put(AgeData.PocketHeadFace.TOP, extractFace(skin, 8, 0, 8, 16, hasOverlay));
    faces.put(AgeData.PocketHeadFace.BOTTOM, extractFace(skin, 16, 0, 16, 16, hasOverlay));
    faces.put(AgeData.PocketHeadFace.LEFT, extractFace(skin, 0, 8, 0, 24, hasOverlay));
    faces.put(AgeData.PocketHeadFace.FRONT, extractFace(skin, 8, 8, 8, 24, hasOverlay));
    faces.put(AgeData.PocketHeadFace.RIGHT, extractFace(skin, 16, 8, 16, 24, hasOverlay));
    faces.put(AgeData.PocketHeadFace.BACK, extractFace(skin, 24, 8, 24, 24, hasOverlay));

    return faces;
  }

  private static int[] extractFace(BufferedImage skin, int baseX, int baseY, int overlayX, int overlayY, boolean hasOverlay) {
    int[] pixels = new int[FACE_PIXELS];
    for (int y = 0; y < FACE_SIZE; y++) {
      for (int x = 0; x < FACE_SIZE; x++) {
        int base = skin.getRGB(baseX + x, baseY + y);
        int color = base;
        if (hasOverlay) {
          int overlay = skin.getRGB(overlayX + x, overlayY + y);
          int alpha = (overlay >> 24) & 0xFF;
          if (alpha > 0) {
            color = blend(base, overlay, alpha);
          }
        }
        pixels[y * FACE_SIZE + x] = color & 0xFFFFFF;
      }
    }
    return pixels;
  }

  private static int blend(int base, int overlay, int alpha) {
    int br = (base >> 16) & 0xFF;
    int bg = (base >> 8) & 0xFF;
    int bb = base & 0xFF;
    int or = (overlay >> 16) & 0xFF;
    int og = (overlay >> 8) & 0xFF;
    int ob = overlay & 0xFF;

    int inv = 255 - alpha;
    int r = (or * alpha + br * inv) / 255;
    int g = (og * alpha + bg * inv) / 255;
    int b = (ob * alpha + bb * inv) / 255;
    return (r << 16) | (g << 8) | b;
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
