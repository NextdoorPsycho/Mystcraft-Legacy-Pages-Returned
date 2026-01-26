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
 * Sun celestial implementation.
 * Supports custom colors, sizes, orbital angles, and phases.
 */
public class CelestialSun extends AbstractCelestial {

    private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation DARK_SUN_LOCATION = new ResourceLocation("textures/environment/sun.png"); // Could use custom texture

    private final String identifier;
    private int sunsetColor = -1;

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

        // Calculate celestial position
        float adjustedTime = (dayTime + phase) / period;
        adjustedTime = adjustedTime - (float) Math.floor(adjustedTime);
        float celestialAngle = adjustedTime * 360.0f;

        RenderSystem.enableBlend();
        if (dark) {
            // Dark sun uses different blending
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
        float sunSize = 30.0F * size;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, dark ? DARK_SUN_LOCATION : SUN_LOCATION);

        // Apply color tint if set
        if (color != -1 && !dark) {
            float r = ColorUtils.getRed(color) / 255.0f;
            float g = ColorUtils.getGreen(color) / 255.0f;
            float b = ColorUtils.getBlue(color) / 255.0f;
            RenderSystem.setShaderColor(r, g, b, 1.0f);
        } else if (dark) {
            RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, -sunSize, 100.0F, -sunSize).uv(0.0F, 0.0F).endVertex();
        builder.vertex(matrix, sunSize, 100.0F, -sunSize).uv(1.0F, 0.0F).endVertex();
        builder.vertex(matrix, sunSize, 100.0F, sunSize).uv(1.0F, 1.0F).endVertex();
        builder.vertex(matrix, -sunSize, 100.0F, sunSize).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
