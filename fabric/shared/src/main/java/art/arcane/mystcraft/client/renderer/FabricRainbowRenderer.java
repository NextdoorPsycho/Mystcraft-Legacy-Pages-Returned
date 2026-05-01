package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renders rainbow arcs in the sky for Ages with the Rainbow symbol.
 */
public final class FabricRainbowRenderer {

  private static final int[] RAINBOW_COLORS = {
      0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00,
      0xFF0000FF, 0xFF4B0082, 0xFF9400D3
  };

  private static final float RAINBOW_RADIUS = 0.4f;
  private static final float RAINBOW_WIDTH = 0.08f;
  private static final int ARC_SEGMENTS = 64;

  private FabricRainbowRenderer() {
  }

  public static void register() {
    WorldRenderEvents.END.register(FabricRainbowRenderer::onRenderWorld);
  }

  private static void onRenderWorld(WorldRenderContext context) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;

    int ageUID = AgeColorUtils.getCurrentAgeUID();
    if (ageUID < 0) return;
    if (!ClientAgeDataCache.isRainbowEnabled(ageUID)) return;

    float gameTime = mc.level.getGameTime() + context.tickDelta();
    float rainbowAlpha = calculateRainbowAlpha(mc, gameTime);
    if (rainbowAlpha <= 0.01f) return;

    PoseStack poseStack = context.matrixStack();
    renderRainbow(poseStack, context.camera(), gameTime, rainbowAlpha);
  }

  private static float calculateRainbowAlpha(Minecraft mc, float gameTime) {
    float dayTime = mc.level.getTimeOfDay(1.0f);
    float sunAngle = dayTime * (float) Math.PI * 2.0f;
    float sunHeight = Mth.cos(sunAngle);
    if (sunHeight < 0) return 0.0f;

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

        buffer.vertex(matrix, cosAngle * outerRadius * 100.0f, sinAngle * outerRadius * 100.0f, 0)
            .color(r, g, b, segmentAlpha * 0.8f).endVertex();
        buffer.vertex(matrix, cosAngle * innerRadius * 100.0f, sinAngle * innerRadius * 100.0f, 0)
            .color(r, g, b, segmentAlpha).endVertex();
      }
      BufferUploader.drawWithShader(buffer.end());
      currentRadius = innerRadius;
    }

    poseStack.popPose();
    RenderSystem.enableCull();
    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
  }
}
