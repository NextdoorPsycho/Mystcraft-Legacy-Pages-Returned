package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Client-side rendering handler for Age visual effects.
 * Handles custom sky color, fog color, and celestial rendering based on Age symbols.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public class AgeRenderingHandler {

    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON_PHASES_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
    private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");

    // Track the current age to detect dimension changes
    private static int lastKnownAgeUID = -1;

    /**
     * Gets the current Age UID the player is in, or -1 if not in an Age.
     */
    private static int getCurrentAgeUID() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;

        if (!AgeDimensionFactory.isMystcraftAge(mc.level.dimension())) {
            return -1;
        }

        // Extract age UID from dimension key
        String path = mc.level.dimension().location().getPath();
        if (path.startsWith("mystcraft_age_")) {
            try {
                return Integer.parseInt(path.substring("mystcraft_age_".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Handles level load to register dimension effects for ages.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            // Check if we're entering a Mystcraft age
            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0 && ageUID != lastKnownAgeUID) {
                // Register dimension effects for this age
                AgeDimensionEffectsManager.registerAgeEffects(ageUID);
                lastKnownAgeUID = ageUID;
                Mystcraft.LOGGER.debug("Entered Age {}, registered dimension effects", ageUID);
            }
        }
    }

    /**
     * Handles level unload to cleanup dimension effects.
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            // Cleanup when leaving
            if (lastKnownAgeUID >= 0) {
                AgeDimensionEffectsManager.unregisterAgeEffects(lastKnownAgeUID);
                lastKnownAgeUID = -1;
            }
        }
    }

    /**
     * Tracks dimension changes during gameplay.
     */
    private static void checkDimensionChange() {
        int currentAgeUID = getCurrentAgeUID();
        if (currentAgeUID != lastKnownAgeUID) {
            // Unregister old effects
            if (lastKnownAgeUID >= 0) {
                AgeDimensionEffectsManager.unregisterAgeEffects(lastKnownAgeUID);
            }

            // Register new effects
            if (currentAgeUID >= 0) {
                AgeDimensionEffectsManager.registerAgeEffects(currentAgeUID);
            }

            lastKnownAgeUID = currentAgeUID;
        }
    }

    /**
     * Modifies fog color based on Age configuration.
     */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        // Check for dimension changes
        checkDimensionChange();
        int ageUID = getCurrentAgeUID();
        if (ageUID < 0) return;

        int fogColor = ClientAgeDataCache.getFogColor(ageUID);
        if (fogColor != -1) {
            float r = ((fogColor >> 16) & 0xFF) / 255.0f;
            float g = ((fogColor >> 8) & 0xFF) / 255.0f;
            float b = (fogColor & 0xFF) / 255.0f;

            event.setRed(r);
            event.setGreen(g);
            event.setBlue(b);
        }

        // Apply lighting type modifications
        String lightingType = ClientAgeDataCache.getLightingType(ageUID);
        switch (lightingType) {
            case "bright" -> {
                // Brighten the fog slightly
                event.setRed(Math.min(1.0f, event.getRed() * 1.2f));
                event.setGreen(Math.min(1.0f, event.getGreen() * 1.2f));
                event.setBlue(Math.min(1.0f, event.getBlue() * 1.2f));
            }
            case "dark" -> {
                // Darken the fog
                event.setRed(event.getRed() * 0.5f);
                event.setGreen(event.getGreen() * 0.5f);
                event.setBlue(event.getBlue() * 0.5f);
            }
        }
    }

    /**
     * Modifies sky color based on Age configuration.
     * Note: This uses fog color event as a proxy since there's no direct sky color event.
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        int ageUID = getCurrentAgeUID();
        if (ageUID < 0) return;

        // Apply lighting type to fog density
        String lightingType = ClientAgeDataCache.getLightingType(ageUID);
        if ("dark".equals(lightingType)) {
            // Increase fog density in dark Ages
            if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
                event.setNearPlaneDistance(event.getNearPlaneDistance() * 0.5f);
                event.setFarPlaneDistance(event.getFarPlaneDistance() * 0.7f);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Custom sky rendering for Ages.
     * Handles hiding horizon, custom celestials, and sky colors.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        int ageUID = getCurrentAgeUID();
        if (ageUID < 0) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        // Check if we need custom sky rendering
        int skyColor = ClientAgeDataCache.getSkyColor(ageUID);
        boolean sunVisible = ClientAgeDataCache.isSunVisible(ageUID);
        boolean moonVisible = ClientAgeDataCache.isMoonVisible(ageUID);
        boolean starsVisible = ClientAgeDataCache.areStarsVisible(ageUID);
        String starType = ClientAgeDataCache.getStarType(ageUID);
        boolean horizonHidden = ClientAgeDataCache.isHorizonHidden(ageUID);

        // Only do custom rendering if we have modifications
        if (skyColor == -1 && sunVisible && moonVisible && starsVisible &&
            "normal".equals(starType) && !horizonHidden) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();

        // Apply custom sky color overlay
        if (skyColor != -1) {
            renderSkyColorOverlay(poseStack, skyColor, level.getTimeOfDay(event.getPartialTick()));
        }

        // Hide horizon with black band if requested
        if (horizonHidden) {
            renderHiddenHorizon(poseStack);
        }

        // Render custom celestials
        renderCustomCelestials(poseStack, level, event.getPartialTick(),
                sunVisible, moonVisible, starsVisible, starType, ageUID);
    }

    /**
     * Renders a sky color overlay.
     */
    private static void renderSkyColorOverlay(PoseStack poseStack, int color, float timeOfDay) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // Adjust for time of day
        float dayFactor = 1.0f - Math.abs(timeOfDay - 0.5f) * 2.0f; // 1 at noon, 0 at midnight
        dayFactor = Mth.clamp(dayFactor, 0.2f, 1.0f);

        r *= dayFactor;
        g *= dayFactor;
        b *= dayFactor;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Render sky dome with custom color
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, 0.0f, 16.0f, 0.0f).color(r, g, b, 0.3f).endVertex();

        for (int i = 0; i <= 16; i++) {
            float angle = (float) i * ((float) Math.PI * 2.0f) / 16.0f;
            float x = Mth.sin(angle) * 120.0f;
            float z = Mth.cos(angle) * 120.0f;
            builder.vertex(matrix, x, 16.0f, z).color(r, g, b, 0.3f).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * Renders a black band at the horizon to hide it.
     */
    private static void renderHiddenHorizon(PoseStack poseStack) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Render black band around horizon
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= 64; i++) {
            float angle = (float) i * ((float) Math.PI * 2.0f) / 64.0f;
            float x = Mth.sin(angle);
            float z = Mth.cos(angle);

            builder.vertex(matrix, x * 400.0f, -10.0f, z * 400.0f).color(0.0f, 0.0f, 0.0f, 1.0f).endVertex();
            builder.vertex(matrix, x * 400.0f, 10.0f, z * 400.0f).color(0.0f, 0.0f, 0.0f, 1.0f).endVertex();
        }

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * Renders custom celestials (sun, moon, stars) based on Age configuration.
     */
    private static void renderCustomCelestials(PoseStack poseStack, ClientLevel level, float partialTick,
                                                boolean sunVisible, boolean moonVisible, boolean starsVisible,
                                                String starType, int ageUID) {
        // Get night sky color if set
        int nightSkyColor = ClientAgeDataCache.getNightSkyColor(ageUID);

        float timeOfDay = level.getTimeOfDay(partialTick);
        float celestialAngle = level.getSunAngle(partialTick);

        // Calculate star visibility based on time
        float starBrightness = level.getStarBrightness(partialTick);

        // Render stars if visible
        if (starsVisible && starBrightness > 0.0f) {
            renderStars(poseStack, starBrightness, starType, nightSkyColor);
        }

        // Note: Sun and Moon are rendered by vanilla after this event.
        // To hide them, we would need mixins or access transformers.
        // For now, we render overlay effects but can't easily hide vanilla celestials.

        // Apply night sky color as an overlay during night
        if (nightSkyColor != -1 && starBrightness > 0.0f) {
            float r = ((nightSkyColor >> 16) & 0xFF) / 255.0f;
            float g = ((nightSkyColor >> 8) & 0xFF) / 255.0f;
            float b = (nightSkyColor & 0xFF) / 255.0f;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            Matrix4f matrix = poseStack.last().pose();

            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(matrix, 0.0f, 100.0f, 0.0f).color(r, g, b, starBrightness * 0.5f).endVertex();

            for (int i = 0; i <= 16; i++) {
                float angle = (float) i * ((float) Math.PI * 2.0f) / 16.0f;
                float x = Mth.sin(angle) * 120.0f;
                float z = Mth.cos(angle) * 120.0f;
                builder.vertex(matrix, x, 100.0f, z).color(r, g, b, starBrightness * 0.5f).endVertex();
            }

            BufferUploader.drawWithShader(builder.end());
            RenderSystem.disableBlend();
        }
    }

    /**
     * Renders stars with custom effects based on star type.
     */
    private static void renderStars(PoseStack poseStack, float brightness, String starType, int nightSkyColor) {
        if ("dark".equals(starType)) {
            // No stars for dark type
            return;
        }

        // Twinkle effect for "twinkle" star type
        float twinkle = 1.0f;
        if ("twinkle".equals(starType)) {
            long time = System.currentTimeMillis();
            twinkle = 0.7f + 0.3f * (float) Math.sin(time * 0.005);
        }

        // End-style stars for "end" star type
        if ("end".equals(starType)) {
            // Render end sky-like effect
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, END_SKY_LOCATION);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();

            for (int layer = 0; layer < 6; layer++) {
                poseStack.pushPose();

                switch (layer) {
                    case 1 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0f));
                    case 2 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0f));
                    case 3 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0f));
                    case 4 -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0f));
                    case 5 -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0f));
                }

                Matrix4f matrix = poseStack.last().pose();
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                int r = 40;
                int g = 40;
                int b = 40;
                int a = (int) (brightness * 255 * twinkle);

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
}
