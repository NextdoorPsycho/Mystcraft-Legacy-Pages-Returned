package art.arcane.mystcraft.client;

import art.arcane.mystcraft.api.world.logic.ICelestial;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.util.ColorUtils;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Custom dimension special effects for Mystcraft Ages.
 * Provides control over sky rendering, celestials, and fog.
 *
 * This class is responsible for rendering the custom sky, including:
 * - Multiple celestials (suns, moons, stars)
 * - Custom sky colors
 * - Void rendering
 * - Horizon hiding
 */
public class AgeDimensionSpecialEffects extends DimensionSpecialEffects {

    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON_PHASES_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");

    private final int ageUID;

    public AgeDimensionSpecialEffects(int ageUID) {
        // Use overworld-like settings as base
        // float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight
        super(192.0F, true, SkyType.NORMAL, false, false);
        this.ageUID = ageUID;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float dayTime) {
        // Get custom fog color from age data
        int customFogColor = ClientAgeDataCache.getFogColor(ageUID);
        if (customFogColor != -1) {
            float r = ColorUtils.getRed(customFogColor) / 255.0f;
            float g = ColorUtils.getGreen(customFogColor) / 255.0f;
            float b = ColorUtils.getBlue(customFogColor) / 255.0f;

            // Apply time-of-day darkening
            float dayFactor = ColorUtils.calculateDayFactor(dayTime);
            return new Vec3(r * dayFactor, g * dayFactor, b * dayFactor);
        }

        // Default: darken fog at night like overworld
        return fogColor.multiply(dayTime * 0.94F + 0.06F, dayTime * 0.94F + 0.06F, dayTime * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    /**
     * Called to render the sky for this dimension.
     * Returns true if we fully handle sky rendering (hide vanilla sky).
     */
    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                             Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        // Check if we should use custom sky rendering
        boolean sunVisible = ClientAgeDataCache.isSunVisible(ageUID);
        boolean moonVisible = ClientAgeDataCache.isMoonVisible(ageUID);
        boolean starsVisible = ClientAgeDataCache.areStarsVisible(ageUID);
        int skyColor = ClientAgeDataCache.getSkyColor(ageUID);
        boolean horizonHidden = ClientAgeDataCache.isHorizonHidden(ageUID);
        List<ICelestial> celestials = ClientAgeDataCache.getCelestials(ageUID);

        // If all defaults and no custom celestials, let vanilla handle it
        if (sunVisible && moonVisible && starsVisible && skyColor == -1 &&
            !horizonHidden && (celestials == null || celestials.isEmpty())) {
            return false;
        }

        // We're handling sky rendering
        setupFog.run();

        float dayTime = level.getTimeOfDay(partialTick);
        float rainLevel = level.getRainLevel(partialTick);

        // Render sky dome
        renderSkyDome(poseStack, level, partialTick, skyColor, dayTime);

        // Render stars (if visible and it's dark enough)
        float starBrightness = getStarBrightness(level, partialTick);
        if (starsVisible && starBrightness > 0) {
            renderStars(poseStack, level, partialTick, starBrightness);
        }

        // Render custom celestials
        if (celestials != null && !celestials.isEmpty()) {
            renderCustomCelestials(poseStack, level, partialTick, dayTime, rainLevel, celestials);
        } else {
            // Render vanilla-style sun/moon if visible
            if (sunVisible) {
                renderDefaultSun(poseStack, level, partialTick, dayTime);
            }
            if (moonVisible) {
                renderDefaultMoon(poseStack, level, partialTick, dayTime);
            }
        }

        // Render void if player is below horizon
        renderVoid(poseStack, camera, horizonHidden);

        return true; // We handled sky rendering
    }

    /**
     * Renders the sky dome with the appropriate color.
     */
    private void renderSkyDome(PoseStack poseStack, ClientLevel level, float partialTick, int customColor, float dayTime) {
        float r, g, b;

        if (customColor != -1) {
            r = ColorUtils.getRed(customColor) / 255.0f;
            g = ColorUtils.getGreen(customColor) / 255.0f;
            b = ColorUtils.getBlue(customColor) / 255.0f;

            // Apply time-of-day dimming
            float dayFactor = ColorUtils.calculateDayFactor(dayTime);
            r *= dayFactor;
            g *= dayFactor;
            b *= dayFactor;
        } else {
            // Use vanilla sky color calculation
            Vec3 skyColor = level.getSkyColor(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), partialTick);
            r = (float) skyColor.x;
            g = (float) skyColor.y;
            b = (float) skyColor.z;
        }

        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Render sky dome
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        builder.vertex(matrix, 0.0F, 16.0F, 0.0F).endVertex();

        for (int i = 0; i <= 16; i++) {
            float angle = (float) i * ((float) Math.PI * 2.0F) / 16.0F;
            float x = Mth.sin(angle) * 120.0F;
            float z = Mth.cos(angle) * 120.0F;
            builder.vertex(matrix, x, 16.0F, z).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    /**
     * Renders stars.
     */
    private void renderStars(PoseStack poseStack, ClientLevel level, float partialTick, float starBrightness) {
        String starType = ClientAgeDataCache.getStarType(ageUID);

        if ("dark".equals(starType)) {
            return; // No stars for dark type
        }

        // Apply twinkle effect
        float twinkle = 1.0f;
        if ("twinkle".equals(starType)) {
            long time = System.currentTimeMillis();
            twinkle = 0.7f + 0.3f * (float) Math.sin(time * 0.005);
        }

        int nightSkyColor = ClientAgeDataCache.getNightSkyColor(ageUID);
        float alpha = starBrightness * twinkle;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Render a simple star field
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        poseStack.pushPose();

        // Rotate stars with time
        float dayTime = level.getTimeOfDay(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(dayTime * 360.0F));

        Matrix4f matrix = poseStack.last().pose();

        // Generate pseudo-random star positions
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        java.util.Random starRandom = new java.util.Random(10842L);
        for (int i = 0; i < 1500; i++) {
            double x = (starRandom.nextFloat() * 2.0F - 1.0F);
            double y = (starRandom.nextFloat() * 2.0F - 1.0F);
            double z = (starRandom.nextFloat() * 2.0F - 1.0F);
            double size = 0.15F + starRandom.nextFloat() * 0.1F;
            double dist = x * x + y * y + z * z;

            if (dist < 1.0D && dist > 0.01D) {
                dist = 1.0D / Math.sqrt(dist);
                x *= dist;
                y *= dist;
                z *= dist;

                double px = x * 100.0D;
                double py = y * 100.0D;
                double pz = z * 100.0D;

                double angle = Math.atan2(x, z);
                double sinAngle = Math.sin(angle);
                double cosAngle = Math.cos(angle);

                double angle2 = Math.atan2(Math.sqrt(x * x + z * z), y);
                double sinAngle2 = Math.sin(angle2);
                double cosAngle2 = Math.cos(angle2);

                int starAlpha = (int) (alpha * 255);
                int starColor = nightSkyColor != -1 ? nightSkyColor : 0xFFFFFF;
                int sr = ColorUtils.getRed(starColor);
                int sg = ColorUtils.getGreen(starColor);
                int sb = ColorUtils.getBlue(starColor);

                for (int v = 0; v < 4; v++) {
                    double vx = (double) ((v & 2) - 1) * size;
                    double vy = (double) ((v + 1 & 2) - 1) * size;

                    double ry = vx * sinAngle2 + vy * cosAngle2;
                    double rz = vy * sinAngle2 - vx * cosAngle2;
                    double rx = ry * sinAngle - rz * cosAngle;
                    rz = rz * sinAngle + ry * cosAngle;

                    builder.vertex(matrix, (float) (px + rx), (float) (py + rz), (float) (pz))
                            .color(sr, sg, sb, starAlpha).endVertex();
                }
            }
        }

        BufferUploader.drawWithShader(builder.end());
        poseStack.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * Renders custom celestials from the Age's celestial list.
     */
    private void renderCustomCelestials(PoseStack poseStack, ClientLevel level, float partialTick,
                                         float dayTime, float rainLevel, List<ICelestial> celestials) {
        for (ICelestial celestial : celestials) {
            if (celestial.isVisible()) {
                celestial.render(poseStack, level, partialTick, dayTime, rainLevel);
            }
        }
    }

    /**
     * Renders the default sun.
     */
    private void renderDefaultSun(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        poseStack.pushPose();

        float celestialAngle = level.getSunAngle(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F));

        Matrix4f matrix = poseStack.last().pose();
        float size = 30.0F;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SUN_LOCATION);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, -size, 100.0F, -size).uv(0.0F, 0.0F).endVertex();
        builder.vertex(matrix, size, 100.0F, -size).uv(1.0F, 0.0F).endVertex();
        builder.vertex(matrix, size, 100.0F, size).uv(1.0F, 1.0F).endVertex();
        builder.vertex(matrix, -size, 100.0F, size).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        poseStack.popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Renders the default moon.
     */
    private void renderDefaultMoon(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        poseStack.pushPose();

        float celestialAngle = level.getSunAngle(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(celestialAngle * 360.0F + 180.0F));

        Matrix4f matrix = poseStack.last().pose();
        float size = 20.0F;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MOON_PHASES_LOCATION);

        int moonPhase = level.getMoonPhase();
        int px = moonPhase % 4;
        int py = moonPhase / 4 % 2;
        float u0 = (float) px / 4.0F;
        float v0 = (float) py / 2.0F;
        float u1 = (float) (px + 1) / 4.0F;
        float v1 = (float) (py + 1) / 2.0F;

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, -size, -100.0F, size).uv(u1, v1).endVertex();
        builder.vertex(matrix, size, -100.0F, size).uv(u0, v1).endVertex();
        builder.vertex(matrix, size, -100.0F, -size).uv(u0, v0).endVertex();
        builder.vertex(matrix, -size, -100.0F, -size).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(builder.end());

        poseStack.popPose();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Renders the void below the horizon.
     */
    private void renderVoid(PoseStack poseStack, Camera camera, boolean horizonHidden) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double horizonDst = camera.getPosition().y - mc.level.getMinBuildHeight();

        if (horizonHidden || horizonDst < 64.0D) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            Matrix4f matrix = poseStack.last().pose();

            // Render black band at horizon
            builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i <= 64; i++) {
                float angle = (float) i * ((float) Math.PI * 2.0F) / 64.0F;
                float x = Mth.sin(angle);
                float z = Mth.cos(angle);

                builder.vertex(matrix, x * 400.0F, -16.0F, z * 400.0F).color(0, 0, 0, 255).endVertex();
                builder.vertex(matrix, x * 400.0F, 16.0F, z * 400.0F).color(0, 0, 0, 255).endVertex();
            }

            BufferUploader.drawWithShader(builder.end());

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    /**
     * Gets star brightness based on time and rain.
     */
    private float getStarBrightness(ClientLevel level, float partialTick) {
        float dayTime = level.getTimeOfDay(partialTick);
        float brightness = 1.0F - (Mth.cos(dayTime * ((float) Math.PI * 2F)) * 2.0F + 0.25F);
        brightness = Mth.clamp(brightness, 0.0F, 1.0F);

        float rain = 1.0F - level.getRainLevel(partialTick);
        return brightness * brightness * rain;
    }

    @Nullable
    @Override
    public float[] getSunriseColor(float dayTime, float partialTick) {
        // Custom sunset colors based on age configuration
        int sunsetColor = ClientAgeDataCache.getSunsetColor(ageUID);
        if (sunsetColor != -1) {
            float f1 = Mth.cos(dayTime * ((float) Math.PI * 2F)) - 0.0F;
            if (f1 >= -0.4F && f1 <= 0.4F) {
                float f3 = (f1 + 0.0F) / 0.4F * 0.5F + 0.5F;
                float alpha = 1.0F - (1.0F - Mth.sin(f3 * (float) Math.PI)) * 0.99F;
                alpha *= alpha;

                float r = ColorUtils.getRed(sunsetColor) / 255.0f;
                float g = ColorUtils.getGreen(sunsetColor) / 255.0f;
                float b = ColorUtils.getBlue(sunsetColor) / 255.0f;

                return new float[]{r, g, b, alpha};
            }
        }

        // Use default vanilla sunset colors
        return super.getSunriseColor(dayTime, partialTick);
    }

    /**
     * Renders custom clouds for the Age.
     * Returns true if we handled cloud rendering, false to let vanilla handle it.
     */
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                 double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        int cloudColor = ClientAgeDataCache.getCloudColor(ageUID);

        // If no custom cloud color, let vanilla handle it
        if (cloudColor == -1) {
            return false;
        }

        // Custom cloud rendering with Age-defined color
        renderCustomClouds(level, ticks, partialTick, poseStack, camX, camY, camZ, cloudColor);
        return true;
    }

    /**
     * Renders clouds with a custom color.
     */
    private void renderCustomClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                     double camX, double camY, double camZ, int cloudColor) {
        Minecraft mc = Minecraft.getInstance();

        float cloudHeight = ClientAgeDataCache.getCloudHeight(ageUID);
        float cloudSpeed = 0.03F;   // Cloud movement speed

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
        RenderSystem.setShaderTexture(0, new net.minecraft.resources.ResourceLocation("textures/environment/clouds.png"));

        float f = (float) (cloudHeight - camY + 0.33F);
        float cloudOffset = (float) (ticks + partialTick) * cloudSpeed;

        // Calculate cloud position
        double d0 = (camX + cloudOffset) / 12.0D;
        double d1 = camZ / 12.0D + 0.33D;

        d0 -= Mth.floor(d0 / 2048.0D) * 2048;
        d1 -= Mth.floor(d1 / 2048.0D) * 2048;

        float texU = (float) (d0 - (double) Mth.floor(d0));
        float texV = (float) (d1 - (double) Mth.floor(d1));

        // Extract custom cloud color
        float r = ColorUtils.getRed(cloudColor) / 255.0f;
        float g = ColorUtils.getGreen(cloudColor) / 255.0f;
        float b = ColorUtils.getBlue(cloudColor) / 255.0f;

        // Apply weather darkening
        Vec3 weatherColor = level.getCloudColor(partialTick);
        float weatherFactor = (float) ((weatherColor.x + weatherColor.y + weatherColor.z) / 3.0D);
        r *= weatherFactor;
        g *= weatherFactor;
        b *= weatherFactor;

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        poseStack.pushPose();
        poseStack.scale(12.0F, 1.0F, 12.0F);

        Matrix4f matrix = poseStack.last().pose();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);

        // Render cloud layer
        for (int i = -3; i <= 4; i++) {
            for (int j = -3; j <= 4; j++) {
                float x0 = (float) i * 8;
                float z0 = (float) j * 8;
                float x1 = x0 - texU;
                float z1 = z0 - texV;

                // Top face
                if (f > -5.0F) {
                    builder.vertex(matrix, x1 + 0, f + 4, z1 + 8)
                            .uv((x0 + 0) * 0.00390625F + texU * 0.00390625F, (z0 + 8) * 0.00390625F + texV * 0.00390625F)
                            .color(r, g, b, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                    builder.vertex(matrix, x1 + 8, f + 4, z1 + 8)
                            .uv((x0 + 8) * 0.00390625F + texU * 0.00390625F, (z0 + 8) * 0.00390625F + texV * 0.00390625F)
                            .color(r, g, b, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                    builder.vertex(matrix, x1 + 8, f + 4, z1 + 0)
                            .uv((x0 + 8) * 0.00390625F + texU * 0.00390625F, (z0 + 0) * 0.00390625F + texV * 0.00390625F)
                            .color(r, g, b, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                    builder.vertex(matrix, x1 + 0, f + 4, z1 + 0)
                            .uv((x0 + 0) * 0.00390625F + texU * 0.00390625F, (z0 + 0) * 0.00390625F + texV * 0.00390625F)
                            .color(r, g, b, 0.8F)
                            .normal(0.0F, 1.0F, 0.0F)
                            .endVertex();
                }

                // Bottom face
                if (f < 5.0F) {
                    float darkR = r * 0.7F;
                    float darkG = g * 0.7F;
                    float darkB = b * 0.7F;

                    builder.vertex(matrix, x1 + 0, f + 0, z1 + 8)
                            .uv((x0 + 0) * 0.00390625F + texU * 0.00390625F, (z0 + 8) * 0.00390625F + texV * 0.00390625F)
                            .color(darkR, darkG, darkB, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                    builder.vertex(matrix, x1 + 8, f + 0, z1 + 8)
                            .uv((x0 + 8) * 0.00390625F + texU * 0.00390625F, (z0 + 8) * 0.00390625F + texV * 0.00390625F)
                            .color(darkR, darkG, darkB, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                    builder.vertex(matrix, x1 + 8, f + 0, z1 + 0)
                            .uv((x0 + 8) * 0.00390625F + texU * 0.00390625F, (z0 + 0) * 0.00390625F + texV * 0.00390625F)
                            .color(darkR, darkG, darkB, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                    builder.vertex(matrix, x1 + 0, f + 0, z1 + 0)
                            .uv((x0 + 0) * 0.00390625F + texU * 0.00390625F, (z0 + 0) * 0.00390625F + texV * 0.00390625F)
                            .color(darkR, darkG, darkB, 0.8F)
                            .normal(0.0F, -1.0F, 0.0F)
                            .endVertex();
                }
            }
        }

        BufferUploader.drawWithShader(builder.end());
        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
