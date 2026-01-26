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
 * Moon celestial implementation.
 * Supports phases, custom colors, sizes, orbital angles, and phases.
 */
public class CelestialMoon extends AbstractCelestial {

    private static final ResourceLocation MOON_PHASES_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");

    private final String identifier;
    private boolean showPhases = true;

    public CelestialMoon(String identifier) {
        super(CelestialType.MOON);
        this.identifier = identifier;
        // Moons are typically opposite to suns (180 degree phase shift)
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

        // Calculate celestial position
        float adjustedTime = (dayTime + phase) / period;
        adjustedTime = adjustedTime - (float) Math.floor(adjustedTime);
        float celestialAngle = adjustedTime * 360.0f;

        RenderSystem.enableBlend();
        if (dark) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        } else {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }

        poseStack.pushPose();

        // Apply orbital tilt
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(celestialAngle));

        Matrix4f matrix = poseStack.last().pose();
        float moonSize = 20.0F * size;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MOON_PHASES_LOCATION);

        // Apply color tint if set
        if (color != -1 && !dark) {
            float r = ColorUtils.getRed(color) / 255.0f;
            float g = ColorUtils.getGreen(color) / 255.0f;
            float b = ColorUtils.getBlue(color) / 255.0f;
            RenderSystem.setShaderColor(r, g, b, 1.0f);
        } else if (dark) {
            RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        // Calculate moon phase UV coordinates
        float u0, v0, u1, v1;
        if (showPhases) {
            int moonPhase = level.getMoonPhase();
            int px = moonPhase % 4;
            int py = moonPhase / 4 % 2;
            u0 = (float) px / 4.0F;
            v0 = (float) py / 2.0F;
            u1 = (float) (px + 1) / 4.0F;
            v1 = (float) (py + 1) / 2.0F;
        } else {
            // Full moon (no phases)
            u0 = 0.0F;
            v0 = 0.0F;
            u1 = 0.25F;
            v1 = 0.5F;
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // Moon is opposite to sun (negative Y)
        builder.vertex(matrix, -moonSize, -100.0F, moonSize).uv(u1, v1).endVertex();
        builder.vertex(matrix, moonSize, -100.0F, moonSize).uv(u0, v1).endVertex();
        builder.vertex(matrix, moonSize, -100.0F, -moonSize).uv(u0, v0).endVertex();
        builder.vertex(matrix, -moonSize, -100.0F, -moonSize).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
