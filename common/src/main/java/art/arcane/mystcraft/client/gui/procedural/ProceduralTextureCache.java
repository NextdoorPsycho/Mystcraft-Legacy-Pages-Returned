package art.arcane.mystcraft.client.gui.procedural;

import art.arcane.mystcraft.Mystcraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LRU cache of dynamically generated UI textures.
 * <p>
 * Generators are keyed by an arbitrary string hash (typically derived from the
 * data the texture depends on, e.g. cover item + page count + ink tint). When
 * an entry is evicted its {@link DynamicTexture} is released so its VRAM can be
 * reclaimed.
 * <p>
 * Capacity is configurable but defaults to 64 — at 256x256 RGBA each texture is
 * ~256 KB, so 64 entries cap VRAM use at ~16 MB.
 */
public final class ProceduralTextureCache {

  private static final int DEFAULT_CAPACITY = 64;
  private static final String NAMESPACE = Mystcraft.MOD_ID;

  private final int capacity;
  private final String prefix;
  private final LinkedHashMap<String, ResourceLocation> cache;

  public ProceduralTextureCache(@NotNull String prefix) {
    this(prefix, DEFAULT_CAPACITY);
  }

  public ProceduralTextureCache(@NotNull String prefix, int capacity) {
    this.prefix = sanitize(prefix);
    this.capacity = Math.max(1, capacity);
    this.cache = new LinkedHashMap<>(this.capacity, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
        if (size() > ProceduralTextureCache.this.capacity) {
          releaseTexture(eldest.getValue());
          return true;
        }
        return false;
      }
    };
  }

  private static TextureManager textureManager() {
    return Minecraft.getInstance().getTextureManager();
  }

  private static void releaseTexture(ResourceLocation loc) {
    try {
      textureManager().release(loc);
    } catch (Exception e) {

      Mystcraft.LOGGER.debug("[ProceduralTextureCache] release({}) failed: {}", loc, e.toString());
    }
  }

  private static String sanitize(@NotNull String raw) {
    StringBuilder sb = new StringBuilder(raw.length());
    String lower = raw.toLowerCase(Locale.ROOT);
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '/' || c == '.' || c == '-') {
        sb.append(c);
      } else {
        sb.append('_');
      }
    }
    return sb.toString();
  }

  /**
   * Convenience: returns the namespace used for registered textures.
   */
  public static String namespace() {
    return NAMESPACE;
  }

  /**
   * Looks up or generates the texture for the given key.
   * <p>
   * If the key is already cached the existing {@link ResourceLocation} is
   * returned. Otherwise {@code generator} is invoked with a fresh
   * {@link NativeImage} of the requested dimensions; the caller is responsible
   * for filling the image's pixels. The image is uploaded as a
   * {@link DynamicTexture} and registered.
   *
   * @param key       cache key (usually a hash of the inputs)
   * @param width     image width in pixels
   * @param height    image height in pixels
   * @param generator callback that draws into the supplied image
   * @return the {@link ResourceLocation} that can be passed to
   * {@link net.minecraft.client.gui.GuiGraphics#blit}
   */
  public synchronized ResourceLocation getOrCreate(@NotNull String key, int width, int height,
                                                   @NotNull Consumer<NativeImage> generator) {
    ResourceLocation cached = cache.get(key);
    if (cached != null) {
      return cached;
    }
    NativeImage image = new NativeImage(width, height, true);
    try {
      generator.accept(image);
    } catch (Throwable t) {
      image.close();
      Mystcraft.LOGGER.error("[ProceduralTextureCache] Generator threw for {}/{}: {}",
          prefix, key, t.toString());
      throw t;
    }
    DynamicTexture texture = new DynamicTexture(image);
    String safeKey = sanitize(prefix + "_" + key);
    ResourceLocation loc = textureManager().register(safeKey, texture);
    cache.put(key, loc);
    return loc;
  }

  /**
   * Clears every cached texture and releases its GPU resources. Call this on
   * resource pack reloads.
   */
  public synchronized void clear() {
    for (Iterator<Map.Entry<String, ResourceLocation>> it = cache.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<String, ResourceLocation> entry = it.next();
      releaseTexture(entry.getValue());
      it.remove();
    }
  }

  /**
   * Returns the current number of cached entries (testing / debug aid).
   */
  public synchronized int size() {
    return cache.size();
  }

  /**
   * Removes a single entry without disturbing the rest of the cache.
   */
  public synchronized void invalidate(@NotNull String key) {
    ResourceLocation removed = cache.remove(key);
    if (removed != null) {
      releaseTexture(removed);
    }
  }
}
