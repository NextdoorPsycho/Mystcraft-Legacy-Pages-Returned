package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Renders a rainbow arc in the sky for Ages with the Rainbow symbol enabled.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public class RainbowRenderer {

  private static final int[] RAINBOW_COLORS = {
      0xFFFF0000, // Red
      0xFFFF7F00, // Orange
      0xFFFFFF00, // Yellow
      0xFF00FF00, // Green
      0xFF0000FF, // Blue
      0xFF4B0082, // Indigo
      0xFF9400D3  // Violet
  };

  private static final float RAINBOW_RADIUS = 0.4f;
  private static final float RAINBOW_WIDTH = 0.08f;
  private static final int ARC_SEGMENTS = 64;

  @SubscribeEvent
  public static void onRenderLevelStage(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
      return;
    }

    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;

    int ageUID = getCurrentAgeUID();
    if (ageUID < 0) return;

    if (!ClientAgeDataCache.isRainbowEnabled(ageUID)) {
      return;
    }

    float gameTime = mc.level.getGameTime() + event.getPartialTick();
    float rainbowAlpha = calculateRainbowAlpha(mc, gameTime);

    if (rainbowAlpha <= 0.01f) {
      return;
    }

    renderRainbow(event.getPoseStack(), event.getCamera(), gameTime, rainbowAlpha);
  }

  private static float calculateRainbowAlpha(Minecraft mc, float gameTime) {
    float dayTime = mc.level.getTimeOfDay(1.0f);
    float sunAngle = dayTime * (float) Math.PI * 2.0f;
    float sunHeight = Mth.cos(sunAngle);

    if (sunHeight < 0) {
      return 0.0f;
    }

    float optimalHeight = 0.3f;
    float heightFactor = 1.0f - Math.abs(sunHeight - optimalHeight) / 0.7f;
    heightFactor = Mth.clamp(heightFactor, 0.0f, 1.0f);

    float pulse = 0.9f + 0.1f * Mth.sin(gameTime * 0.02f);

    return heightFactor * pulse * 0.7f;
  }

  private static void renderRainbow(PoseStack poseStack, Camera camera, float gameTime, float alpha) {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.depthMask(false);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);
    RenderSystem.disableCull();

    poseStack.pushPose();

    Vec3 cameraPos = camera.getPosition();
    Minecraft mc = Minecraft.getInstance();
    float dayTime = mc.level.getTimeOfDay(1.0f);

    float sunAngle = dayTime * 360.0f;
    float rainbowAngle = sunAngle + 180.0f;

    poseStack.mulPose(Axis.YP.rotationDegrees(-rainbowAngle));

    float tilt = 20.0f + Mth.sin(dayTime * (float) Math.PI * 2.0f) * 10.0f;
    poseStack.mulPose(Axis.XP.rotationDegrees(tilt));

    Matrix4f matrix = poseStack.last().pose();

    Tesselator tesselator = Tesselator.getInstance();
    BufferBuilder buffer = tesselator.getBuilder();

    float currentRadius = RAINBOW_RADIUS;
    for (int colorIndex = 0; colorIndex < RAINBOW_COLORS.length; colorIndex++) {
      int color = RAINBOW_COLORS[colorIndex];

      float r = ((color >> 16) & 0xFF) / 255.0f;
      float g = ((color >> 8) & 0xFF) / 255.0f;
      float b = (color & 0xFF) / 255.0f;

      float outerRadius = currentRadius;
      float innerRadius = currentRadius - RAINBOW_WIDTH;

      buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i <= ARC_SEGMENTS; i++) {
        float angle = (float) Math.PI * i / ARC_SEGMENTS;
        float cosAngle = Mth.cos(angle);
        float sinAngle = Mth.sin(angle);

        float edgeFade = Mth.sin(angle);
        float segmentAlpha = alpha * edgeFade;

        float outerX = cosAngle * outerRadius * 100.0f;
        float outerY = sinAngle * outerRadius * 100.0f;
        buffer.vertex(matrix, outerX, outerY, 0)
            .color(r, g, b, segmentAlpha * 0.8f)
            .endVertex();

        float innerX = cosAngle * innerRadius * 100.0f;
        float innerY = sinAngle * innerRadius * 100.0f;
        buffer.vertex(matrix, innerX, innerY, 0)
            .color(r, g, b, segmentAlpha)
            .endVertex();
      }

      BufferUploader.drawWithShader(buffer.end());

      currentRadius = innerRadius;
    }

    poseStack.popPose();

    RenderSystem.enableCull();
    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
  }

  private static int getCurrentAgeUID() {
    return AgeColorUtils.getCurrentAgeUID();
  }
}
