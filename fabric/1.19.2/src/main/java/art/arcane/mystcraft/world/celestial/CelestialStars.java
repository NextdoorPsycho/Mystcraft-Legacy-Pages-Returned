package art.arcane.mystcraft.world.celestial;

import art.arcane.mystcraft.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Stars celestial with multi-layer parallax, varied color temperatures, nebula clouds,
 * per-star twinkle, and occasional shooting stars.
 * <p>
 * Three star layers rotate at different speeds for depth parallax.
 * Stars have varied sizes and color temperatures (blue-white through orange-red).
 * Nebula patches render as soft colored clouds across the sky dome.
 * Shooting stars streak across randomly during night.
 */
public class CelestialStars extends AbstractCelestial {

  private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");

  private final String identifier;
  private String starType = "normal";
  private int starCount = 1500;

  // Shooting star state (client-side only, ephemeral)
  private long nextShootingStarTime = 0;
  private float shootingStarStartAngle = 0;
  private float shootingStarStartElev = 0;
  private float shootingStarDirection = 0;
  private float shootingStarSpeed = 0;

  public CelestialStars(String identifier) {
    super(CelestialType.STARS);
    this.identifier = identifier;
  }

  /**
   * Maps a 0-1 temperature value to an RGB star color.
   */
  private static int[] starColorFromTemperature(float t) {
    // Blue-white -> White -> Yellow -> Orange -> Red
    if (t < 0.15f) {
      // Hot blue-white
      return new int[]{180, 200, 255};
    } else if (t < 0.4f) {
      // White
      return new int[]{255, 255, 255};
    } else if (t < 0.65f) {
      // Yellow-white
      return new int[]{255, 245, 200};
    } else if (t < 0.85f) {
      // Orange
      return new int[]{255, 200, 130};
    } else {
      // Red
      return new int[]{255, 150, 100};
    }
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  public String getStarType() {
    return starType;
  }

  public void setStarType(String starType) {
    this.starType = starType != null ? starType : "normal";
  }

  public void setStarCount(int count) {
    this.starCount = Math.max(100, Math.min(8000, count));
  }

  @Override
  public boolean providesLight() {
    return false;
  }

  @Override
  public void render(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime, float rainLevel) {
    if (!visible) return;
    if ("dark".equals(starType)) return;

    float brightness = calculateStarBrightness(dayTime, rainLevel);
    if (brightness <= 0) return;

    if ("end".equals(starType)) {
      renderEndStars(poseStack, brightness);
    } else {
      long timeMs = System.currentTimeMillis();
      boolean twinkle = "twinkle".equals(starType);

      renderNebulaeClouds(poseStack, level, partialTick, dayTime, brightness);
      renderStarLayers(poseStack, level, partialTick, dayTime, brightness, twinkle, timeMs);
      renderShootingStar(poseStack, dayTime, brightness, timeMs);
    }
  }

  // --- Multi-Layer Parallax Stars ---

  private float calculateStarBrightness(float dayTime, float rainLevel) {
    float brightness = 1.0f - (Mth.cos(dayTime * ((float) Math.PI * 2f)) * 2.0f + 0.25f);
    brightness = Mth.clamp(brightness, 0.0f, 1.0f);
    float rain = 1.0f - rainLevel;
    return brightness * brightness * rain;
  }

  private void renderStarLayers(PoseStack poseStack, ClientLevel level, float partialTick,
                                float dayTime, float brightness, boolean twinkle, long timeMs) {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.depthMask(false);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    // Layer 0: Distant dim stars (slow rotation)
    int layer0Count = starCount / 3;
    float layer0Speed = 0.7f;
    float layer0Brightness = brightness * 0.5f;
    renderSingleStarLayer(poseStack, dayTime, layer0Brightness, twinkle, timeMs,
        10842L, layer0Count, layer0Speed, 0.08f, 0.13f, 100.0f, 0.0f);

    // Layer 1: Mid-distance stars (normal rotation)
    int layer1Count = starCount / 2;
    float layer1Speed = 1.0f;
    float layer1Brightness = brightness * 0.8f;
    renderSingleStarLayer(poseStack, dayTime, layer1Brightness, twinkle, timeMs,
        29471L, layer1Count, layer1Speed, 0.12f, 0.20f, 100.0f, 15.0f);

    // Layer 2: Bright foreground stars (faster rotation, larger)
    int layer2Count = starCount / 6;
    float layer2Speed = 1.15f;
    float layer2Brightness = brightness;
    renderSingleStarLayer(poseStack, dayTime, layer2Brightness, twinkle, timeMs,
        58293L, layer2Count, layer2Speed, 0.18f, 0.35f, 100.0f, -8.0f);

    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
  }

  private void renderSingleStarLayer(PoseStack poseStack, float dayTime, float brightness,
                                     boolean twinkle, long timeMs, long layerSeed,
                                     int count, float speedMult, float minSize, float maxSize,
                                     float distance, float tiltDegrees) {
    BufferBuilder builder = Tesselator.getInstance().getBuilder();
    poseStack.pushPose();

    poseStack.mulPose(Vector3f.YP.rotationDegrees(-90.0f));
    poseStack.mulPose(Vector3f.ZP.rotationDegrees(angle + tiltDegrees));
    poseStack.mulPose(Vector3f.XP.rotationDegrees(dayTime * 360.0f * speedMult));

    Matrix4f matrix = poseStack.last().pose();
    builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

    Random starRandom = new Random(layerSeed);
    for (int i = 0; i < count; i++) {
      double x = starRandom.nextFloat() * 2.0f - 1.0f;
      double y = starRandom.nextFloat() * 2.0f - 1.0f;
      double z = starRandom.nextFloat() * 2.0f - 1.0f;
      double starSize = minSize + starRandom.nextFloat() * (maxSize - minSize);
      double dist = x * x + y * y + z * z;

      // Color temperature: each star gets a temperature value
      float temperature = starRandom.nextFloat();

      // Per-star twinkle phase offset
      float twinklePhase = starRandom.nextFloat() * (float) (Math.PI * 2.0);
      float twinkleFreq = 0.002f + starRandom.nextFloat() * 0.006f;

      if (dist < 1.0 && dist > 0.01) {
        dist = 1.0 / Math.sqrt(dist);
        x *= dist;
        y *= dist;
        z *= dist;

        double px = x * distance;
        double py = y * distance;
        double pz = z * distance;

        double starAngle = Math.atan2(x, z);
        double sinAngle = Math.sin(starAngle);
        double cosAngle = Math.cos(starAngle);
        double angle2 = Math.atan2(Math.sqrt(x * x + z * z), y);
        double sinAngle2 = Math.sin(angle2);
        double cosAngle2 = Math.cos(angle2);

        // Compute star alpha with optional twinkle
        float alpha = brightness;
        if (twinkle) {
          float tw = 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(timeMs * twinkleFreq + twinklePhase));
          alpha *= tw;
        }
        int starAlpha = (int) (alpha * 255);
        if (starAlpha <= 0) continue;

        // Color based on temperature
        int sr, sg, sb;
        if (color != -1) {
          sr = ColorUtils.getRed(color);
          sg = ColorUtils.getGreen(color);
          sb = ColorUtils.getBlue(color);
        } else {
          int[] rgb = starColorFromTemperature(temperature);
          sr = rgb[0];
          sg = rgb[1];
          sb = rgb[2];
        }

        for (int v = 0; v < 4; v++) {
          double vx = (double) ((v & 2) - 1) * starSize;
          double vy = (double) ((v + 1 & 2) - 1) * starSize;

          double ry = vx * sinAngle2 + vy * cosAngle2;
          double rz = vy * sinAngle2 - vx * cosAngle2;
          double rx = ry * sinAngle - rz * cosAngle;
          rz = rz * sinAngle + ry * cosAngle;

          builder.vertex(matrix, (float) (px + rx), (float) (py + rz), (float) pz)
              .color(sr, sg, sb, starAlpha).endVertex();
        }
      }
    }

    BufferUploader.drawWithShader(builder.end());
    poseStack.popPose();
  }

  // --- Nebula Clouds ---

  private void renderNebulaeClouds(PoseStack poseStack, ClientLevel level, float partialTick,
                                   float dayTime, float brightness) {
    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(
        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
    RenderSystem.depthMask(false);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    poseStack.pushPose();
    poseStack.mulPose(Vector3f.YP.rotationDegrees(-90.0f));
    poseStack.mulPose(Vector3f.XP.rotationDegrees(dayTime * 360.0f * 0.5f));

    Matrix4f matrix = poseStack.last().pose();

    // Fixed nebula patches
    Random nebRandom = new Random(439281L);
    int nebulaCount = 5;
    float nebulaAlpha = brightness * 0.12f;

    for (int n = 0; n < nebulaCount; n++) {
      float nx = nebRandom.nextFloat() * 2.0f - 1.0f;
      float ny = nebRandom.nextFloat() * 2.0f - 1.0f;
      float nz = nebRandom.nextFloat() * 2.0f - 1.0f;
      float dist = nx * nx + ny * ny + nz * nz;
      if (dist > 1.0f || dist < 0.01f) continue;

      float invDist = 1.0f / (float) Math.sqrt(dist);
      nx *= invDist;
      ny *= invDist;
      nz *= invDist;

      float nebulaSize = 12.0f + nebRandom.nextFloat() * 20.0f;

      // Nebula color
      int nr = 80 + nebRandom.nextInt(100);
      int ng = 40 + nebRandom.nextInt(80);
      int nb = 120 + nebRandom.nextInt(135);
      if (color != -1) {
        nr = ColorUtils.getRed(color);
        ng = ColorUtils.getGreen(color);
        nb = ColorUtils.getBlue(color);
      }
      int alpha = (int) (nebulaAlpha * 255);

      // Render as a soft patch (billboard quad cluster)
      float px = nx * 95.0f;
      float py = ny * 95.0f;
      float pz = nz * 95.0f;

      renderNebulaBlob(matrix, px, py, pz, nebulaSize, nr, ng, nb, alpha, nebRandom);
    }

    poseStack.popPose();
    RenderSystem.depthMask(true);
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  private void renderNebulaBlob(Matrix4f matrix, float cx, float cy, float cz,
                                float radius, int r, int g, int b, int alpha, Random rand) {
    // Render multiple overlapping soft circles to form an amorphous cloud
    int blobCount = 4 + rand.nextInt(4);
    BufferBuilder builder = Tesselator.getInstance().getBuilder();

    for (int bl = 0; bl < blobCount; bl++) {
      float ox = (rand.nextFloat() - 0.5f) * radius * 0.8f;
      float oy = (rand.nextFloat() - 0.5f) * radius * 0.8f;
      float oz = (rand.nextFloat() - 0.5f) * radius * 0.8f;
      float blobRadius = radius * (0.4f + rand.nextFloat() * 0.4f);

      float bx = cx + ox;
      float by = cy + oy;
      float bz = cz + oz;

      // Compute billboard axes
      float len = (float) Math.sqrt(bx * bx + by * by + bz * bz);
      if (len < 0.01f) continue;
      float nx = bx / len;
      float ny = by / len;
      float nz = bz / len;

      // Two tangent vectors perpendicular to the normal
      float tx, ty, tz;
      if (Math.abs(ny) < 0.9f) {
        tx = -nz;
        ty = 0;
        tz = nx;
      } else {
        tx = 1;
        ty = 0;
        tz = 0;
      }
      float tLen = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
      tx /= tLen;
      ty /= tLen;
      tz /= tLen;

      float bix = ny * tz - nz * ty;
      float biy = nz * tx - nx * tz;
      float biz = nx * ty - ny * tx;

      builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      builder.vertex(matrix, bx, by, bz).color(r, g, b, alpha).endVertex();
      for (int i = 0; i <= 12; i++) {
        float a = (float) i / 12.0f * (float) (Math.PI * 2.0);
        float dx = (float) Math.cos(a) * blobRadius;
        float dy = (float) Math.sin(a) * blobRadius;
        float vx = bx + tx * dx + bix * dy;
        float vy = by + ty * dx + biy * dy;
        float vz = bz + tz * dx + biz * dy;
        builder.vertex(matrix, vx, vy, vz).color(r, g, b, 0).endVertex();
      }
      BufferUploader.drawWithShader(builder.end());
    }
  }

  // --- Shooting Stars ---

  private void renderShootingStar(PoseStack poseStack, float dayTime, float brightness, long timeMs) {
    // Only at night, and only occasionally
    if (brightness < 0.3f) return;

    if (timeMs > nextShootingStarTime) {
      // Schedule next shooting star
      Random schedRand = new Random(timeMs);
      nextShootingStarTime = timeMs + 5000 + schedRand.nextInt(25000);
      shootingStarStartAngle = schedRand.nextFloat() * (float) (Math.PI * 2.0);
      shootingStarStartElev = 0.2f + schedRand.nextFloat() * 0.6f;
      shootingStarDirection = schedRand.nextFloat() * (float) (Math.PI * 2.0);
      shootingStarSpeed = 0.3f + schedRand.nextFloat() * 0.5f;
    }

    // Render if a shooting star is active (within 800ms window of scheduled time)
    long elapsed = timeMs - (nextShootingStarTime - 5000);
    if (elapsed < 0 || elapsed > 800) return;

    float t = elapsed / 800.0f;
    float fadeIn = Math.min(1.0f, t * 4.0f);
    float fadeOut = Math.max(0.0f, 1.0f - (t - 0.5f) * 2.0f);
    float alpha = brightness * fadeIn * fadeOut;
    if (alpha <= 0) return;

    float travelDist = t * shootingStarSpeed;

    // Head position on sky sphere
    float headAngle = shootingStarStartAngle + (float) Math.cos(shootingStarDirection) * travelDist;
    float headElev = shootingStarStartElev + (float) Math.sin(shootingStarDirection) * travelDist * 0.5f;

    float hx = (float) (Math.cos(headAngle) * Math.cos(headElev * Math.PI * 0.5)) * 98.0f;
    float hy = (float) Math.sin(headElev * Math.PI * 0.5) * 98.0f;
    float hz = (float) (Math.sin(headAngle) * Math.cos(headElev * Math.PI * 0.5)) * 98.0f;

    // Tail position (slightly behind)
    float tailT = Math.max(0, t - 0.15f);
    float tailDist = tailT * shootingStarSpeed;
    float tailAngle = shootingStarStartAngle + (float) Math.cos(shootingStarDirection) * tailDist;
    float tailElev = shootingStarStartElev + (float) Math.sin(shootingStarDirection) * tailDist * 0.5f;

    float tx = (float) (Math.cos(tailAngle) * Math.cos(tailElev * Math.PI * 0.5)) * 98.0f;
    float ty = (float) Math.sin(tailElev * Math.PI * 0.5) * 98.0f;
    float tz = (float) (Math.sin(tailAngle) * Math.cos(tailElev * Math.PI * 0.5)) * 98.0f;

    RenderSystem.enableBlend();
    RenderSystem.blendFuncSeparate(
        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
    RenderSystem.depthMask(false);
    RenderSystem.setShader(GameRenderer::getPositionColorShader);

    Matrix4f matrix = poseStack.last().pose();
    BufferBuilder builder = Tesselator.getInstance().getBuilder();

    // Direction vector for width calculation
    float dx = hx - tx;
    float dy = hy - ty;
    float dz = hz - tz;
    float dLen = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (dLen < 0.01f) {
      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      return;
    }
    dx /= dLen;
    dy /= dLen;
    dz /= dLen;

    // Perpendicular for width
    float wx, wy, wz;
    if (Math.abs(dy) < 0.9f) {
      wx = -dz;
      wy = 0;
      wz = dx;
    } else {
      wx = 1;
      wy = 0;
      wz = 0;
    }
    float wLen = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
    wx /= wLen;
    wy /= wLen;
    wz /= wLen;

    float headWidth = 0.3f;
    float tailWidth = 0.05f;
    int headAlpha = (int) (alpha * 255);

    builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    builder.vertex(matrix, hx + wx * headWidth, hy + wy * headWidth, hz + wz * headWidth)
        .color(255, 255, 240, headAlpha).endVertex();
    builder.vertex(matrix, hx - wx * headWidth, hy - wy * headWidth, hz - wz * headWidth)
        .color(255, 255, 240, headAlpha).endVertex();
    builder.vertex(matrix, tx - wx * tailWidth, ty - wy * tailWidth, tz - wz * tailWidth)
        .color(200, 200, 255, 0).endVertex();
    builder.vertex(matrix, tx + wx * tailWidth, ty + wy * tailWidth, tz + wz * tailWidth)
        .color(200, 200, 255, 0).endVertex();
    BufferUploader.drawWithShader(builder.end());

    RenderSystem.depthMask(true);
    RenderSystem.defaultBlendFunc();
    RenderSystem.disableBlend();
  }

  // --- End Stars (skybox) ---

  private void renderEndStars(PoseStack poseStack, float alpha) {
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.depthMask(false);
    RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    RenderSystem.setShaderTexture(0, END_SKY_LOCATION);

    BufferBuilder builder = Tesselator.getInstance().getBuilder();

    int r, g, b;
    if (color != -1) {
      r = ColorUtils.getRed(color);
      g = ColorUtils.getGreen(color);
      b = ColorUtils.getBlue(color);
    } else {
      r = g = b = 40;
    }
    int a = (int) (alpha * 255);

    for (int layer = 0; layer < 6; layer++) {
      poseStack.pushPose();

      switch (layer) {
        case 1 -> poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0f));
        case 2 -> poseStack.mulPose(Vector3f.XP.rotationDegrees(-90.0f));
        case 3 -> poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0f));
        case 4 -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0f));
        case 5 -> poseStack.mulPose(Vector3f.ZP.rotationDegrees(-90.0f));
      }

      Matrix4f matrix = poseStack.last().pose();
      builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

      builder.vertex(matrix, -100.0f, -100.0f, -100.0f).uv(0.0f, 0.0f).color(r, g, b, a).endVertex();
      builder.vertex(matrix, -100.0f, -100.0f, 100.0f).uv(0.0f, 1.0f).color(r, g, b, a).endVertex();
      builder.vertex(matrix, 100.0f, -100.0f, 100.0f).uv(1.0f, 1.0f).color(r, g, b, a).endVertex();
      builder.vertex(matrix, 100.0f, -100.0f, -100.0f).uv(1.0f, 0.0f).color(r, g, b, a).endVertex();

      BufferUploader.drawWithShader(builder.end());
      poseStack.popPose();
    }

    RenderSystem.depthMask(true);
    RenderSystem.disableBlend();
  }
}
