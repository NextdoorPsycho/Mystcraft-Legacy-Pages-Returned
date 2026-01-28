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
 * Sun celestial with layered corona, pulsing rays, and atmospheric glow.
 * Renders a bright core disc, an animated corona ring with radial rays,
 * and a soft outer glow halo. Dark suns render as eclipses with a visible corona.
 */
public class CelestialSun extends AbstractCelestial {

    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");

    private final String identifier;
    private int sunsetColor = -1;

    // Ray geometry constants
    private static final int RAY_COUNT = 24;
    private static final int CORONA_SEGMENTS = 48;

    public CelestialSun(String identifier) {
        super(CelestialType.SUN);
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setSunsetColor(int color) {
        this.sunsetColor = color;
    }

    @Override
    public int getSunsetColor(float dayTime) {
        return sunsetColor;
    }

    @Override
    public void render(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime, float rainLevel) {
        if (!visible) return;

        float adjustedTime = (dayTime + phase) / period;
        adjustedTime = adjustedTime - (float) Math.floor(adjustedTime);
        float celestialAngle = adjustedTime * 360.0f;

        long timeMs = System.currentTimeMillis();

        // Pulsing animation
        float pulse = 0.92f + 0.08f * (float) Math.sin(timeMs * 0.002);
        float rayPulse = 0.85f + 0.15f * (float) Math.sin(timeMs * 0.0013 + 1.0);

        float baseR, baseG, baseB;
        if (color != -1) {
            baseR = ColorUtils.getRed(color) / 255.0f;
            baseG = ColorUtils.getGreen(color) / 255.0f;
            baseB = ColorUtils.getBlue(color) / 255.0f;
        } else {
            baseR = 1.0f;
            baseG = 0.95f;
            baseB = 0.8f;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(celestialAngle));

        if (dark) {
            renderEclipseSun(poseStack, timeMs, pulse);
        } else {
            renderOuterGlow(poseStack, baseR, baseG, baseB, pulse);
            renderCorona(poseStack, baseR, baseG, baseB, timeMs, rayPulse);
            renderRays(poseStack, baseR, baseG, baseB, timeMs, rayPulse);
            renderCoreDisc(poseStack, baseR, baseG, baseB, pulse);
        }

        poseStack.popPose();
    }

    /** Bright inner disc rendered with the vanilla sun texture, overlaid with a procedural glow disc. */
    private void renderCoreDisc(PoseStack poseStack, float r, float g, float b, float pulse) {
        float sunSize = 30.0f * size * pulse;

        // Textured sun disc
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN_LOCATION);
        RenderSystem.setShaderColor(r, g, b, 1.0f);

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, -sunSize, 100.0f, -sunSize).uv(0.0f, 0.0f).endVertex();
        builder.vertex(matrix, sunSize, 100.0f, -sunSize).uv(1.0f, 0.0f).endVertex();
        builder.vertex(matrix, sunSize, 100.0f, sunSize).uv(1.0f, 1.0f).endVertex();
        builder.vertex(matrix, -sunSize, 100.0f, sunSize).uv(0.0f, 1.0f).endVertex();
        BufferUploader.drawWithShader(builder.end());

        // Additive glow overlay disc (brighter center)
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        float glowSize = sunSize * 1.1f;
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int coreAlpha = 180;
        builder.vertex(matrix, 0.0f, 100.0f, 0.0f)
                .color((int) (r * 255), (int) (g * 255), (int) (b * 255), coreAlpha).endVertex();
        for (int i = 0; i <= 32; i++) {
            float a = (float) i / 32.0f * (float) (Math.PI * 2.0);
            float gx = (float) Math.cos(a) * glowSize;
            float gz = (float) Math.sin(a) * glowSize;
            builder.vertex(matrix, gx, 100.0f, gz)
                    .color((int) (r * 255), (int) (g * 255), (int) (b * 255), 0).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Soft outer glow halo that extends well beyond the sun disc. */
    private void renderOuterGlow(PoseStack poseStack, float r, float g, float b, float pulse) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        float glowRadius = 65.0f * size * pulse;
        int outerAlpha = 60;

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        builder.vertex(matrix, 0.0f, 100.0f, 0.0f)
                .color((int) (r * 255), (int) (g * 255), (int) (b * 255), outerAlpha).endVertex();
        for (int i = 0; i <= CORONA_SEGMENTS; i++) {
            float a = (float) i / (float) CORONA_SEGMENTS * (float) (Math.PI * 2.0);
            float gx = (float) Math.cos(a) * glowRadius;
            float gz = (float) Math.sin(a) * glowRadius;
            builder.vertex(matrix, gx, 100.0f, gz)
                    .color((int) (r * 200), (int) (g * 200), (int) (b * 200), 0).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Animated corona ring around the sun, with noise-varied radius. */
    private void renderCorona(PoseStack poseStack, float r, float g, float b, long timeMs, float pulse) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        float innerRadius = 32.0f * size;
        float outerRadius = 46.0f * size * pulse;
        float timeOffset = timeMs * 0.0003f;

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= CORONA_SEGMENTS; i++) {
            float a = (float) i / (float) CORONA_SEGMENTS * (float) (Math.PI * 2.0);

            // Animated wobble on the outer edge
            float wobble = 1.0f + 0.15f * (float) Math.sin(a * 7.0 + timeOffset * 3.0)
                    + 0.1f * (float) Math.sin(a * 13.0 - timeOffset * 5.0);

            float ix = (float) Math.cos(a) * innerRadius;
            float iz = (float) Math.sin(a) * innerRadius;
            float ox = (float) Math.cos(a) * outerRadius * wobble;
            float oz = (float) Math.sin(a) * outerRadius * wobble;

            int innerAlpha = 100;
            int outerAlpha = 0;

            builder.vertex(matrix, ix, 100.0f, iz)
                    .color((int) (r * 255), (int) (g * 255), (int) (b * 255), innerAlpha).endVertex();
            builder.vertex(matrix, ox, 100.0f, oz)
                    .color((int) (r * 230), (int) (g * 180), (int) (b * 100), outerAlpha).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Radial rays emanating from the sun, rotating slowly over time. */
    private void renderRays(PoseStack poseStack, float r, float g, float b, long timeMs, float pulse) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = poseStack.last().pose();
        float rayInner = 28.0f * size;
        float rayOuter = 55.0f * size * pulse;
        float rotation = timeMs * 0.00005f;
        float halfWidth = 0.06f; // Half-angular width per ray in radians

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < RAY_COUNT; i++) {
            float baseAngle = (float) i / (float) RAY_COUNT * (float) (Math.PI * 2.0) + rotation;

            // Each ray has a slightly different length driven by a slow sine
            float rayLength = rayOuter * (0.7f + 0.3f * (float) Math.sin(baseAngle * 3.0 + timeMs * 0.001));

            float tipX = (float) Math.cos(baseAngle) * rayLength;
            float tipZ = (float) Math.sin(baseAngle) * rayLength;

            float leftAngle = baseAngle - halfWidth;
            float rightAngle = baseAngle + halfWidth;
            float lx = (float) Math.cos(leftAngle) * rayInner;
            float lz = (float) Math.sin(leftAngle) * rayInner;
            float rx = (float) Math.cos(rightAngle) * rayInner;
            float rz = (float) Math.sin(rightAngle) * rayInner;

            int baseAlpha = 50;

            builder.vertex(matrix, lx, 100.0f, lz)
                    .color((int) (r * 255), (int) (g * 255), (int) (b * 200), baseAlpha).endVertex();
            builder.vertex(matrix, rx, 100.0f, rz)
                    .color((int) (r * 255), (int) (g * 255), (int) (b * 200), baseAlpha).endVertex();
            builder.vertex(matrix, tipX, 100.0f, tipZ)
                    .color((int) (r * 255), (int) (g * 200), (int) (b * 100), 0).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Eclipse sun: black disc with bright corona ring and faint outer glow. */
    private void renderEclipseSun(PoseStack poseStack, long timeMs, float pulse) {
        Matrix4f matrix = poseStack.last().pose();
        float discSize = 28.0f * size;
        float coronaInner = 29.0f * size;
        float coronaOuter = 50.0f * size * pulse;
        float timeOffset = timeMs * 0.0004f;

        // Black disc
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, 0.0f, 100.0f, 0.0f).color(0, 0, 0, 255).endVertex();
        for (int i = 0; i <= 32; i++) {
            float a = (float) i / 32.0f * (float) (Math.PI * 2.0);
            float x = (float) Math.cos(a) * discSize;
            float z = (float) Math.sin(a) * discSize;
            builder.vertex(matrix, x, 100.0f, z).color(0, 0, 0, 255).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        // Bright corona ring
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= CORONA_SEGMENTS; i++) {
            float a = (float) i / (float) CORONA_SEGMENTS * (float) (Math.PI * 2.0);
            float wobble = 1.0f + 0.2f * (float) Math.sin(a * 5.0 + timeOffset * 4.0)
                    + 0.12f * (float) Math.sin(a * 11.0 - timeOffset * 7.0);

            float ix = (float) Math.cos(a) * coronaInner;
            float iz = (float) Math.sin(a) * coronaInner;
            float ox = (float) Math.cos(a) * coronaOuter * wobble;
            float oz = (float) Math.sin(a) * coronaOuter * wobble;

            builder.vertex(matrix, ix, 100.0f, iz).color(255, 200, 120, 200).endVertex();
            builder.vertex(matrix, ox, 100.0f, oz).color(255, 140, 40, 0).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        // Outer glow
        float glowRadius = 70.0f * size * pulse;
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, 0.0f, 100.0f, 0.0f).color(255, 180, 80, 30).endVertex();
        for (int i = 0; i <= CORONA_SEGMENTS; i++) {
            float a = (float) i / (float) CORONA_SEGMENTS * (float) (Math.PI * 2.0);
            float x = (float) Math.cos(a) * glowRadius;
            float z = (float) Math.sin(a) * glowRadius;
            builder.vertex(matrix, x, 100.0f, z).color(255, 120, 20, 0).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
