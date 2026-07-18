package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.PocketHeadSyncPacket;
import art.arcane.mystcraft.util.PocketHeadUtils;
import art.arcane.mystcraft.world.AgeData;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side helper to send head palette data to the server. Retries until the
 * player's skin texture is available.
 */
public final class PocketHeadClientSync {

  private static final int MAX_ATTEMPTS = 200;
  private static boolean pending = false;
  private static int attempts = 0;

  private PocketHeadClientSync() {
  }

  public static void requestSend() {
    pending = true;
    attempts = 0;
  }

  public static void reset() {
    pending = false;
    attempts = 0;
  }

  public static void tick() {
    if (!pending) {
      return;
    }
    if (trySend()) {
      pending = false;
      return;
    }
    attempts++;
    if (attempts >= MAX_ATTEMPTS) {
      pending = false;
    }
  }

  public static boolean trySend() {
    Map<AgeData.PocketHeadFace, List<String>> blocks = buildHeadBlocks();
    if (blocks == null) {
      return false;
    }
    MystcraftNetwork.sendToServer(new PocketHeadSyncPacket(blocks));
    Mystcraft.LOGGER.debug("[PocketHead] Sent client head palette to server");
    return true;
  }

  @Nullable
  private static Map<AgeData.PocketHeadFace, List<String>> buildHeadBlocks() {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
    if (player == null) {
      return null;
    }
    try (NativeImage skin = getSkinImage(mc, player)) {
      if (skin == null) {
        return null;
      }
      Map<AgeData.PocketHeadFace, int[]> facePixels = extractHeadFaces(skin);
      return PocketHeadUtils.mapFacesToBlocksFromPixels(facePixels);
    }
  }

  @Nullable
  private static NativeImage getSkinImage(Minecraft mc, LocalPlayer player) {
    if (!player.isSkinLoaded()) {
      return null;
    }
    ResourceLocation skinLoc = player.getSkinTextureLocation();
    AbstractTexture texture = mc.getTextureManager().getTexture(skinLoc);
    NativeImage downloaded = new NativeImage(64, 64, false);
    try {
      texture.bind();
      downloaded.downloadTexture(0, false);
      return downloaded;
    } catch (RuntimeException e) {
      downloaded.close();
    }
    Optional<Resource> resource = mc.getResourceManager().getResource(skinLoc);
    if (resource.isEmpty()) {
      return null;
    }
    try (InputStream in = resource.get().open()) {
      return NativeImage.read(in);
    } catch (Exception e) {
      return null;
    }
  }

  private static Map<AgeData.PocketHeadFace, int[]> extractHeadFaces(NativeImage skin) {
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

  private static int[] extractFace(NativeImage skin, int baseX, int baseY, int overlayX, int overlayY, boolean hasOverlay) {
    int[] pixels = new int[64];
    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        int base = getARGB(skin, baseX + x, baseY + y);
        int color = base;
        if (hasOverlay) {
          int overlay = getARGB(skin, overlayX + x, overlayY + y);
          int alpha = (overlay >> 24) & 0xFF;
          if (alpha > 0) {
            color = blend(base, overlay, alpha);
          }
        }
        pixels[y * 8 + x] = color & 0xFFFFFF;
      }
    }
    return pixels;
  }

  private static int getARGB(NativeImage image, int x, int y) {
    int abgr = image.getPixelRGBA(x, y);
    int a = (abgr >> 24) & 0xFF;
    int b = (abgr >> 16) & 0xFF;
    int g = (abgr >> 8) & 0xFF;
    int r = abgr & 0xFF;
    return (a << 24) | (r << 16) | (g << 8) | b;
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
}
