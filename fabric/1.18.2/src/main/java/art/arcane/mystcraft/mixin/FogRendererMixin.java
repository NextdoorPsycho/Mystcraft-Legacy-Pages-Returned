package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hooks fog color and density for Mystcraft Ages (1.20.1).
 * Equivalent of Forge's ViewportEvent.ComputeFogColor and ViewportEvent.RenderFog.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

  private static final Set<Integer> LOGGED_FOG_AGES = Collections.newSetFromMap(new ConcurrentHashMap<>());

  @Inject(method = "setupColor", at = @At("TAIL"))
  private static void mystcraft$modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, CallbackInfo ci) {
    int ageUID = getCurrentAgeUID();
    if (ageUID < 0) return;

    int fogColor = ClientAgeDataCache.getFogColor(ageUID);
    float r = RenderSystem.getShaderFogColor()[0];
    float g = RenderSystem.getShaderFogColor()[1];
    float b = RenderSystem.getShaderFogColor()[2];

    if (fogColor != -1) {
      r = ((fogColor >> 16) & 0xFF) / 255.0f;
      g = ((fogColor >> 8) & 0xFF) / 255.0f;
      b = (fogColor & 0xFF) / 255.0f;
    }

    String lightingType = ClientAgeDataCache.getLightingType(ageUID);
    switch (lightingType) {
      case "bright" -> {
        r = Math.min(1.0f, r * 1.2f);
        g = Math.min(1.0f, g * 1.2f);
        b = Math.min(1.0f, b * 1.2f);
      }
      case "dark" -> {
        r *= 0.5f;
        g *= 0.5f;
        b *= 0.5f;
      }
    }

    if (fogColor != -1 || !"normal".equals(lightingType)) {
      RenderSystem.setShaderFogColor(r, g, b);
    }

    if (LOGGED_FOG_AGES.add(ageUID)) {
      Mystcraft.LOGGER.info("[FogRender] Age {}: fogColor=0x{}, lighting={}, applied R={} G={} B={}",
          ageUID,
          fogColor != -1 ? Integer.toHexString(fogColor) : "none",
          lightingType,
          String.format("%.3f", r),
          String.format("%.3f", g),
          String.format("%.3f", b));
    }
  }

  @Inject(method = "setupFog", at = @At("TAIL"))
  private static void mystcraft$modifyFogDensity(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog, float partialTick, CallbackInfo ci) {
    int ageUID = getCurrentAgeUID();
    if (ageUID < 0) return;

    String lightingType = ClientAgeDataCache.getLightingType(ageUID);
    if ("dark".equals(lightingType) && fogMode == FogRenderer.FogMode.FOG_SKY) {
      float start = RenderSystem.getShaderFogStart();
      float end = RenderSystem.getShaderFogEnd();
      RenderSystem.setShaderFogStart(start * 0.5f);
      RenderSystem.setShaderFogEnd(end * 0.7f);
    }
  }

  private static int getCurrentAgeUID() {
    return AgeColorUtils.getCurrentAgeUID();
  }
}
