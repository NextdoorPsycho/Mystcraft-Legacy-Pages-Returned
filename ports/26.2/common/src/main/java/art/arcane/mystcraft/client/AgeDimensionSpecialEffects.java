package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.util.ColorUtils;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Applies synchronized Age visuals to Minecraft 26.2's extracted level state.
 *
 * <p>26.2 removed {@code DimensionSpecialEffects} and immediate-mode sky
 * callbacks. Keeping all policy here makes both the sky/cloud extractor mixin
 * and fog mixin small, deterministic, and free of GPU state mutations.</p>
 */
public final class AgeDimensionSpecialEffects {

  private static final Set<Integer> LOGGED_SKY_AGES =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  private static final Set<Integer> LOGGED_FOG_AGES =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  private AgeDimensionSpecialEffects() {
  }

  /** Applies sky, celestial, horizon, and cloud values after vanilla extraction. */
  public static void applyLevelState(ClientLevel level, float partialTick,
                                     LevelRenderState levelState) {
    int ageUid = AgeColorUtils.getCurrentAgeUID();
    if (ageUid < 0) {
      return;
    }

    SkyRenderState sky = levelState.skyRenderState;
    // Mystcraft's old registered effect always used a normal sky, including
    // generated dimension types that otherwise report no vanilla skybox.
    sky.skybox = DimensionType.Skybox.OVERWORLD;
    float dayTime = Mth.positiveModulo(sky.sunAngle / (float) (Math.PI * 2.0), 1.0F);

    int skyColor = ClientAgeDataCache.getSkyColor(ageUid);
    int nightSkyColor = ClientAgeDataCache.getNightSkyColor(ageUid);
    if (skyColor != -1 || nightSkyColor != -1) {
      int daytimeColor = skyColor != -1
          ? scaleRgb(skyColor, ColorUtils.calculateDayFactor(dayTime))
          : sky.skyColor;
      if (nightSkyColor != -1) {
        // The modern star mesh has no per-dimension tint. Folding the tint into
        // the night sky retains the authored palette without a custom GPU pass.
        float nightBlend = Mth.clamp(sky.starBrightness, 0.0F, 1.0F) * 0.72F;
        sky.skyColor = ARGB.srgbLerp(
            nightBlend, ARGB.opaque(daytimeColor), ARGB.opaque(nightSkyColor));
      } else {
        sky.skyColor = ARGB.opaque(daytimeColor);
      }
    }

    int sunsetColor = ClientAgeDataCache.getSunsetColor(ageUid);
    if (sunsetColor != -1) {
      sky.sunriseAndSunsetColor = sunriseOrSunsetColor(
          sunsetColor, dayTime);
    }

    boolean horizonHidden = ClientAgeDataCache.isHorizonHidden(ageUid);
    sky.shouldRenderDarkDisc |= horizonHidden;

    int cloudColor = ClientAgeDataCache.getCloudColor(ageUid);
    if (cloudColor != -1) {
      levelState.cloudColor = tintClouds(levelState.cloudColor, cloudColor);
    }
    levelState.cloudHeight = ClientAgeDataCache.getCloudHeight(ageUid);

    if (LOGGED_SKY_AGES.add(ageUid)) {
      Mystcraft.LOGGER.info(
          "[SkyRender] Age {}: sky=0x{}, night=0x{}, sunset=0x{}, cloud=0x{}, cloudHeight={}, horizonHidden={}",
          ageUid,
          hexOrNone(skyColor),
          hexOrNone(nightSkyColor),
          hexOrNone(sunsetColor),
          hexOrNone(cloudColor),
          levelState.cloudHeight,
          horizonHidden);
    }
  }

  /** Applies custom Age fog after the vanilla environments populate FogData. */
  public static void applyFog(FogData fog) {
    int ageUid = AgeColorUtils.getCurrentAgeUID();
    if (ageUid < 0) {
      return;
    }

    int fogColor = ClientAgeDataCache.getFogColor(ageUid);
    if (fogColor != -1) {
      fog.color.set(
          ColorUtils.getRed(fogColor) / 255.0F,
          ColorUtils.getGreen(fogColor) / 255.0F,
          ColorUtils.getBlue(fogColor) / 255.0F,
          1.0F);
    }

    String lightingType = ClientAgeDataCache.getLightingType(ageUid);
    switch (lightingType) {
      case "bright" -> {
        fog.color.x = Math.min(1.0F, fog.color.x * 1.2F);
        fog.color.y = Math.min(1.0F, fog.color.y * 1.2F);
        fog.color.z = Math.min(1.0F, fog.color.z * 1.2F);
      }
      case "dark" -> {
        fog.color.x *= 0.5F;
        fog.color.y *= 0.5F;
        fog.color.z *= 0.5F;
        fog.environmentalStart *= 0.5F;
        fog.environmentalEnd *= 0.7F;
        fog.renderDistanceStart *= 0.5F;
        fog.renderDistanceEnd *= 0.7F;
        fog.skyEnd *= 0.7F;
        fog.cloudEnd *= 0.7F;
      }
      default -> {
        // Normal lighting uses the vanilla distances and brightness.
      }
    }

    if (LOGGED_FOG_AGES.add(ageUid)) {
      Mystcraft.LOGGER.info(
          "[FogRender] Age {}: fog=0x{}, lighting={}, rgb=({}, {}, {})",
          ageUid,
          hexOrNone(fogColor),
          lightingType,
          String.format("%.3f", fog.color.x),
          String.format("%.3f", fog.color.y),
          String.format("%.3f", fog.color.z));
    }
  }

  private static int sunriseOrSunsetColor(int rgb, float dayTime) {
    float cosine = Mth.cos(dayTime * (float) (Math.PI * 2.0));
    if (cosine < -0.4F || cosine > 0.4F) {
      return 0;
    }
    float phase = cosine / 0.4F * 0.5F + 0.5F;
    float alpha = 1.0F - (1.0F - Mth.sin(phase * (float) Math.PI)) * 0.99F;
    alpha *= alpha;
    return ARGB.color(Math.round(alpha * 255.0F), rgb);
  }

  private static int scaleRgb(int rgb, float scale) {
    int red = Mth.clamp(Math.round(ColorUtils.getRed(rgb) * scale), 0, 255);
    int green = Mth.clamp(Math.round(ColorUtils.getGreen(rgb) * scale), 0, 255);
    int blue = Mth.clamp(Math.round(ColorUtils.getBlue(rgb) * scale), 0, 255);
    return ARGB.color(red, green, blue);
  }

  private static int tintClouds(int vanillaColor, int customRgb) {
    float vanillaBrightness =
        (ARGB.red(vanillaColor) + ARGB.green(vanillaColor) + ARGB.blue(vanillaColor))
            / (255.0F * 3.0F);
    int red = Mth.clamp(Math.round(ColorUtils.getRed(customRgb) * vanillaBrightness), 0, 255);
    int green = Mth.clamp(Math.round(ColorUtils.getGreen(customRgb) * vanillaBrightness), 0, 255);
    int blue = Mth.clamp(Math.round(ColorUtils.getBlue(customRgb) * vanillaBrightness), 0, 255);
    return ARGB.color(ARGB.alpha(vanillaColor), red, green, blue);
  }

  private static String hexOrNone(int color) {
    return color == -1 ? "none" : Integer.toHexString(color);
  }
}
