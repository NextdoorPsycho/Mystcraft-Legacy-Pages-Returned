package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.client.AgeColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Renders a rainbow arc in the sky for Ages with the Rainbow symbol enabled.
 * The rainbow appears as a semi-circular arc across the sky.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT)
public class RainbowRenderer {

    // Rainbow colors (ROYGBIV) with alpha
    private static final int[] RAINBOW_COLORS = {
            0xFFFF0000, // Red
            0xFFFF7F00, // Orange
            0xFFFFFF00, // Yellow
            0xFF00FF00, // Green
            0xFF0000FF, // Blue
            0xFF4B0082, // Indigo
            0xFF9400D3  // Violet
    };

    private static final float RAINBOW_RADIUS = 0.4f; // Radius as fraction of sky dome
    private static final float RAINBOW_WIDTH = 0.08f; // Width of each color band
    private static final int ARC_SEGMENTS = 64; // Smoothness of the arc

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Check if we're in a Mystcraft age with rainbow enabled
        int ageUID = getCurrentAgeUID();
        if (ageUID < 0) return;

        if (!ClientAgeDataCache.isRainbowEnabled(ageUID)) {
            return;
        }

        // Get time-based animation
        float gameTime = mc.level.getGameTime() + event.getPartialTick();
        float rainbowAlpha = calculateRainbowAlpha(mc, gameTime);

        if (rainbowAlpha <= 0.01f) {
            return; // Don't render if nearly invisible
        }

        renderRainbow(event.getPoseStack(), event.getCamera(), gameTime, rainbowAlpha);
    }

    private static float calculateRainbowAlpha(Minecraft mc, float gameTime) {
        // Rainbow is most visible during day, fades at night
        float dayTime = mc.level.getTimeOfDay(1.0f);
        float sunAngle = dayTime * (float) Math.PI * 2.0f;

        // Sun is highest at noon (dayTime = 0.25), calculate visibility
        float sunHeight = Mth.cos(sunAngle);

        // Rainbow visible when sun is up (sunHeight > 0)
        if (sunHeight < 0) {
            return 0.0f;
        }

        // Fade rainbow based on sun height, max visibility at dawn/dusk
        // Rainbows appear opposite the sun, so they're most visible when sun is low
        float optimalHeight = 0.3f; // Sun at 30% height is optimal
        float heightFactor = 1.0f - Math.abs(sunHeight - optimalHeight) / 0.7f;
        heightFactor = Mth.clamp(heightFactor, 0.0f, 1.0f);

        // Add slight pulsing animation
        float pulse = 0.9f + 0.1f * Mth.sin(gameTime * 0.02f);

        return heightFactor * pulse * 0.7f; // Max 70% opacity
    }

    private static void renderRainbow(PoseStack poseStack, Camera camera, float gameTime, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();

        poseStack.pushPose();

        // Position rainbow opposite the sun
        Vec3 cameraPos = camera.getPosition();
        Minecraft mc = Minecraft.getInstance();
        float dayTime = mc.level.getTimeOfDay(1.0f);

        // Rainbow appears opposite the sun (180 degrees)
        float sunAngle = dayTime * 360.0f;
        float rainbowAngle = sunAngle + 180.0f;

        // Apply rotation to face away from sun
        poseStack.mulPose(Axis.YP.rotationDegrees(-rainbowAngle));

        // Slight tilt based on sun height
        float tilt = 20.0f + Mth.sin(dayTime * (float) Math.PI * 2.0f) * 10.0f;
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt));

        Matrix4f matrix = poseStack.last().pose();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        // Render each color band of the rainbow
        float currentRadius = RAINBOW_RADIUS;
        for (int colorIndex = 0; colorIndex < RAINBOW_COLORS.length; colorIndex++) {
            int color = RAINBOW_COLORS[colorIndex];

            // Extract color components
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            // Outer and inner radius for this band
            float outerRadius = currentRadius;
            float innerRadius = currentRadius - RAINBOW_WIDTH;

            buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            // Draw arc from left to right (180 degrees)
            for (int i = 0; i <= ARC_SEGMENTS; i++) {
                float angle = (float) Math.PI * i / ARC_SEGMENTS;
                float cosAngle = Mth.cos(angle);
                float sinAngle = Mth.sin(angle);

                // Fade alpha at edges of arc
                float edgeFade = Mth.sin(angle); // 0 at edges, 1 at center
                float segmentAlpha = alpha * edgeFade;

                // Outer vertex
                float outerX = cosAngle * outerRadius * 100.0f;
                float outerY = sinAngle * outerRadius * 100.0f;
                buffer.vertex(matrix, outerX, outerY, 0)
                        .color(r, g, b, segmentAlpha * 0.8f)
                        .endVertex();

                // Inner vertex
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
