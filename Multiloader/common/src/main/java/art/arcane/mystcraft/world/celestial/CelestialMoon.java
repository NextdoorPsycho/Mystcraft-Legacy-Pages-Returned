package art.arcane.mystcraft.world.celestial;

import art.arcane.mystcraft.util.ColorUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Moon celestial with procedural craters, atmospheric glow halo, and phase-aware shading.
 * The moon disc is rendered procedurally as a shaded sphere with crater detail,
 * surrounded by a soft atmospheric glow. Dark moons render as lunar eclipses.
 */
public class CelestialMoon extends AbstractCelestial {

  private static final ResourceLocation MOON_PHASES_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
  private static final int GLOW_SEGMENTS = 48;
  private static final int DISC_SEGMENTS = 32;
  private final String identifier;
  private boolean showPhases = true;

  public CelestialMoon(String identifier) {
    super(CelestialType.MOON);
    this.identifier = identifier;
    this.phase = 0.5f;
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  public void setShowPhases(boolean showPhases) {
    this.showPhases = showPhases;
  }

  @Override
  public void render(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime, float rainLevel) {
    if (!visible) return;

    float adjustedTime = (dayTime + phase) / period;
    adjustedTime = adjustedTime - (float) Math.floor(adjustedTime);
    float celestialAngle = adjustedTime * 360.0f;

    long timeMs = System.currentTimeMillis();
    float pulse = 0.97f + 0.03f * (float) Math.sin(timeMs * 0.001);

    float baseR, baseG, baseB;
    if (color != -1) {
      baseR = ColorUtils.getRed(color) / 255.0f;
      baseG = ColorUtils.getGreen(color) / 255.0f;
      baseB = ColorUtils.getBlue(color) / 255.0f;
    } else {
      baseR = 0.85f;
      baseG = 0.88f;
      baseB = 0.95f;
    }

    poseStack.pushPose();
    poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
    poseStack.mulPose(Axis.XP.rotationDegrees(celestialAngle));

    if (dark) {
      renderLunarEclipse(poseStack, level, timeMs, pulse);
    } else {
      renderAtmosphericGlow(poseStack, baseR, baseG, baseB, pulse);
      renderTexturedDisc(poseStack, level, baseR, baseG, baseB);
      renderCraters(poseStack, baseR, baseG, baseB);
      renderSurfaceShading(poseStack, level, baseR, baseG, baseB);
    }

    poseStack.popPose();
  }

  /**
   * Atmospheric glow halo around the moon.
   */
  private void renderAtmosphericGlow(PoseStack poseStack, float r, float g, float b, float pulse) {
    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    Matrix4f matrix = poseStack.last().pose();
    float glowRadius = 38.0f * size * pulse;
    int centerAlpha = 35;

    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
    builder.vertex(matrix, 0.0f, -100.0f, 0.0f)
        .color((int) (r * 255), (int) (g * 255), (int) (b * 255), centerAlpha).endVertex();
    for (int i = 0; i <= GLOW_SEGMENTS; i++) {
      float a = (float) i / (float) GLOW_SEGMENTS * (float) (Math.PI * 2.0);
      float gx = (float) Math.cos(a) * glowRadius;
      float gz = (float) Math.sin(a) * glowRadius;
      builder.vertex(matrix, gx, -100.0f, gz)
          .color((int) (r * 180), (int) (g * 180), (int) (b * 200), 0).endVertex();
    }
    BufferUploader.drawWithShader(builder.end());

    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  /**
   * Standard moon texture disc with phase support.
   */
  private void renderTexturedDisc(PoseStack poseStack, ClientLevel level,
                                  float r, float g, float b) {
    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, MOON_PHASES_LOCATION);
    RenderSystem.setShaderColor(r, g, b, 1.0f);

    float moonSize = 20.0f * size;

    float u0, v0, u1, v1;
    if (showPhases) {
      int moonPhase = level.getMoonPhase();
      int px = moonPhase % 4;
      int py = moonPhase / 4 % 2;
      u0 = (float) px / 4.0f;
      v0 = (float) py / 2.0f;
      u1 = (float) (px + 1) / 4.0f;
      v1 = (float) (py + 1) / 2.0f;
    } else {
      u0 = 0.0f;
      v0 = 0.0f;
      u1 = 0.25f;
      v1 = 0.5f;
    }

    Matrix4f matrix = poseStack.last().pose();
    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    builder.vertex(matrix, -moonSize, -100.0f, moonSize).uv(u1, v1).endVertex();
    builder.vertex(matrix, moonSize, -100.0f, moonSize).uv(u0, v1).endVertex();
    builder.vertex(matrix, moonSize, -100.0f, -moonSize).uv(u0, v0).endVertex();
    builder.vertex(matrix, -moonSize, -100.0f, -moonSize).uv(u1, v0).endVertex();
    BufferUploader.drawWithShader(builder.end());

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  /**
   * Procedural craters rendered as darkened circular patches on the moon face.
   */
  private void renderCraters(PoseStack poseStack, float r, float g, float b) {
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    Matrix4f matrix = poseStack.last().pose();
    float moonSize = 20.0f * size;

    // Deterministic crater positions based on a fixed seed
    long craterSeed = 7823491L;
    int craterCount = 12;

    BufferBuilder builder = Tesselator.getInstance().getBuilder();

    for (int c = 0; c < craterCount; c++) {
      // Deterministic pseudo-random per crater
      craterSeed = craterSeed * 6364136223846793005L + 1442695040888963407L;
      float cx = ((craterSeed >> 16 & 0xFFL) / 255.0f - 0.5f) * moonSize * 1.4f;
      craterSeed = craterSeed * 6364136223846793005L + 1442695040888963407L;
      float cz = ((craterSeed >> 16 & 0xFFL) / 255.0f - 0.5f) * moonSize * 1.4f;
      craterSeed = craterSeed * 6364136223846793005L + 1442695040888963407L;
      float craterRadius = 1.5f + ((craterSeed >> 16 & 0xFFL) / 255.0f) * 3.5f;
      craterRadius *= size;

      // Only render if within the moon disc
      float distFromCenter = (float) Math.sqrt(cx * cx + cz * cz);
      if (distFromCenter + craterRadius > moonSize * 0.9f) continue;

      // Darker shade for crater interior
      int darkR = (int) (r * 120);
      int darkG = (int) (g * 120);
      int darkB = (int) (b * 130);
      int craterAlpha = 80;

      builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      builder.vertex(matrix, cx, -100.0f, cz)
          .color(darkR, darkG, darkB, craterAlpha).endVertex();
      for (int i = 0; i <= 12; i++) {
        float a = (float) i / 12.0f * (float) (Math.PI * 2.0);
        float px = cx + (float) Math.cos(a) * craterRadius;
        float pz = cz + (float) Math.sin(a) * craterRadius;
        builder.vertex(matrix, px, -100.0f, pz)
            .color(darkR, darkG, darkB, 0).endVertex();
      }
      BufferUploader.drawWithShader(builder.end());
    }

    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  /**
   * Subtle directional shading to give the moon a 3D spherical appearance.
   */
  private void renderSurfaceShading(PoseStack poseStack, ClientLevel level,
                                    float r, float g, float b) {
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    Matrix4f matrix = poseStack.last().pose();
    float moonRadius = 20.0f * size;

    // Shadow gradient from one side (simulating sunlight direction)
    // The shadow sweeps across based on time to hint at the phase
    float dayTime = level.getTimeOfDay(0.0f);
    float shadowAngle = dayTime * (float) (Math.PI * 2.0);
    float shadowDirX = (float) Math.cos(shadowAngle);
    float shadowDirZ = (float) Math.sin(shadowAngle);

    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

    // Off-center shadow disc
    float offsetX = shadowDirX * moonRadius * 0.4f;
    float offsetZ = shadowDirZ * moonRadius * 0.4f;

    builder.vertex(matrix, offsetX, -100.0f, offsetZ)
        .color(0, 0, 10, 50).endVertex();
    for (int i = 0; i <= DISC_SEGMENTS; i++) {
      float a = (float) i / (float) DISC_SEGMENTS * (float) (Math.PI * 2.0);
      float px = offsetX + (float) Math.cos(a) * moonRadius * 0.9f;
      float pz = offsetZ + (float) Math.sin(a) * moonRadius * 0.9f;
      builder.vertex(matrix, px, -100.0f, pz)
          .color(0, 0, 10, 0).endVertex();
    }
    BufferUploader.drawWithShader(builder.end());

    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  /**
   * Lunar eclipse: dark disc with blood-red corona.
   */
  private void renderLunarEclipse(PoseStack poseStack, ClientLevel level, long timeMs, float pulse) {
    Matrix4f matrix = poseStack.last().pose();
    float discSize = 20.0f * size;
    float coronaInner = 21.0f * size;
    float coronaOuter = 36.0f * size * pulse;
    float timeOffset = timeMs * 0.0003f;

    // Dark disc
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
    builder.vertex(matrix, 0.0f, -100.0f, 0.0f).color(5, 0, 0, 255).endVertex();
    for (int i = 0; i <= 32; i++) {
      float a = (float) i / 32.0f * (float) (Math.PI * 2.0);
      float x = (float) Math.cos(a) * discSize;
      float z = (float) Math.sin(a) * discSize;
      builder.vertex(matrix, x, -100.0f, z).color(15, 2, 2, 255).endVertex();
    }
    BufferUploader.drawWithShader(builder.end());

    // Blood-red corona
    RenderSystem.blendFuncSeparate(
        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

    builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
    for (int i = 0; i <= GLOW_SEGMENTS; i++) {
      float a = (float) i / (float) GLOW_SEGMENTS * (float) (Math.PI * 2.0);
      float wobble = 1.0f + 0.15f * (float) Math.sin(a * 6.0 + timeOffset * 3.0)
          + 0.08f * (float) Math.sin(a * 10.0 - timeOffset * 5.0);

      float ix = (float) Math.cos(a) * coronaInner;
      float iz = (float) Math.sin(a) * coronaInner;
      float ox = (float) Math.cos(a) * coronaOuter * wobble;
      float oz = (float) Math.sin(a) * coronaOuter * wobble;

      builder.vertex(matrix, ix, -100.0f, iz).color(200, 50, 20, 160).endVertex();
      builder.vertex(matrix, ox, -100.0f, oz).color(120, 15, 5, 0).endVertex();
    }
    BufferUploader.drawWithShader(builder.end());

    // Outer glow
    float glowRadius = 50.0f * size * pulse;
    builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
    builder.vertex(matrix, 0.0f, -100.0f, 0.0f).color(150, 30, 10, 20).endVertex();
    for (int i = 0; i <= GLOW_SEGMENTS; i++) {
      float a = (float) i / (float) GLOW_SEGMENTS * (float) (Math.PI * 2.0);
      float x = (float) Math.cos(a) * glowRadius;
      float z = (float) Math.sin(a) * glowRadius;
      builder.vertex(matrix, x, -100.0f, z).color(100, 10, 5, 0).endVertex();
    }
    BufferUploader.drawWithShader(builder.end());

    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }
}
