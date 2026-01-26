package art.arcane.mystcraft.world.celestial;

import art.arcane.mystcraft.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Stars celestial implementation.
 * Supports different star types: normal, twinkle, end, dark.
 */
public class CelestialStars extends AbstractCelestial {

    private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");

    private final String identifier;
    private String starType = "normal";
    private int starCount = 1500;

    public CelestialStars(String identifier) {
        super(CelestialType.STARS);
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setStarType(String starType) {
        this.starType = starType != null ? starType : "normal";
    }

    public String getStarType() {
        return starType;
    }

    public void setStarCount(int count) {
        this.starCount = Math.max(100, Math.min(5000, count));
    }

    @Override
    public boolean providesLight() {
        return false; // Stars don't provide light
    }

    @Override
    public void render(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime, float rainLevel) {
        if (!visible) return;
        if ("dark".equals(starType)) return; // No stars for dark type

        // Calculate star brightness based on time and rain
        float brightness = calculateStarBrightness(dayTime, rainLevel);
        if (brightness <= 0) return;

        // Apply twinkle effect
        float twinkle = 1.0f;
        if ("twinkle".equals(starType)) {
            long time = System.currentTimeMillis();
            twinkle = 0.7f + 0.3f * (float) Math.sin(time * 0.005);
        }

        float alpha = brightness * twinkle * size;

        if ("end".equals(starType)) {
            renderEndStars(poseStack, alpha);
        } else {
            renderNormalStars(poseStack, level, partialTick, dayTime, alpha);
        }
    }

    private float calculateStarBrightness(float dayTime, float rainLevel) {
        float brightness = 1.0F - (Mth.cos(dayTime * ((float) Math.PI * 2F)) * 2.0F + 0.25F);
        brightness = Mth.clamp(brightness, 0.0F, 1.0F);
        float rain = 1.0F - rainLevel;
        return brightness * brightness * rain;
    }

    private void renderNormalStars(PoseStack poseStack, ClientLevel level, float partialTick, float dayTime, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        poseStack.pushPose();

        // Rotate stars with time
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(dayTime * 360.0F));

        Matrix4f matrix = poseStack.last().pose();

        // Generate pseudo-random star positions
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Random starRandom = new Random(10842L);
        for (int i = 0; i < starCount; i++) {
            double x = (starRandom.nextFloat() * 2.0F - 1.0F);
            double y = (starRandom.nextFloat() * 2.0F - 1.0F);
            double z = (starRandom.nextFloat() * 2.0F - 1.0F);
            double starSize = 0.15F + starRandom.nextFloat() * 0.1F;
            double dist = x * x + y * y + z * z;

            if (dist < 1.0D && dist > 0.01D) {
                dist = 1.0D / Math.sqrt(dist);
                x *= dist;
                y *= dist;
                z *= dist;

                double px = x * 100.0D;
                double py = y * 100.0D;
                double pz = z * 100.0D;

                double starAngle = Math.atan2(x, z);
                double sinAngle = Math.sin(starAngle);
                double cosAngle = Math.cos(starAngle);

                double angle2 = Math.atan2(Math.sqrt(x * x + z * z), y);
                double sinAngle2 = Math.sin(angle2);
                double cosAngle2 = Math.cos(angle2);

                int starAlpha = (int) (alpha * 255);
                int sr, sg, sb;
                if (color != -1) {
                    sr = ColorUtils.getRed(color);
                    sg = ColorUtils.getGreen(color);
                    sb = ColorUtils.getBlue(color);
                } else {
                    sr = sg = sb = 255;
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

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

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
                case 1 -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                case 2 -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                case 3 -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
                case 4 -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                case 5 -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
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
