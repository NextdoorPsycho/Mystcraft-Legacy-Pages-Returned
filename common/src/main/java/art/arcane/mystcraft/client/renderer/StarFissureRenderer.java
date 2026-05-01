package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Renderer for the Star Fissure block entity. Creates a starfield effect
 * similar to the End portal with animated layers.
 */
public class StarFissureRenderer implements BlockEntityRenderer<StarFissureBlockEntity> {

  private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
  private static final ResourceLocation END_PORTAL_LOCATION = new ResourceLocation("textures/entity/end_portal.png");

  public StarFissureRenderer(BlockEntityRendererProvider.Context context) {
  }

  @Override
  public void render(@NotNull StarFissureBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                     @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

    float gameTime = (float) Minecraft.getInstance().level.getGameTime() + partialTick;

    poseStack.pushPose();

    for (int layer = 0; layer < 3; layer++) {
      poseStack.pushPose();

      float layerOffset = layer * 0.02f;
      float rotation = (gameTime * (1 + layer * 0.5f)) % 360;

      poseStack.translate(0.5, 0.05 + layerOffset, 0.5);
      poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
      poseStack.translate(-0.5, 0, -0.5);

      float pulse = 1.0f + Mth.sin(gameTime * 0.1f + layer) * 0.02f;
      poseStack.translate(0.5, 0, 0.5);
      poseStack.scale(pulse, 1, pulse);
      poseStack.translate(-0.5, 0, -0.5);

      Matrix4f matrix = poseStack.last().pose();

      renderStarfield(matrix, bufferSource.getBuffer(RenderType.endPortal()));

      poseStack.popPose();
    }

    renderGlowEffect(poseStack, bufferSource, gameTime);

    poseStack.popPose();
  }

  private void renderStarfield(Matrix4f matrix, VertexConsumer buffer) {
    float y = 0.0f;
    float minX = 0.0f;
    float maxX = 1.0f;
    float minZ = 0.0f;
    float maxZ = 1.0f;

    buffer.vertex(matrix, minX, y, minZ).endVertex();
    buffer.vertex(matrix, maxX, y, minZ).endVertex();
    buffer.vertex(matrix, maxX, y, maxZ).endVertex();
    buffer.vertex(matrix, minX, y, maxZ).endVertex();
  }

  private void renderGlowEffect(PoseStack poseStack, MultiBufferSource bufferSource, float gameTime) {

    float glowIntensity = 0.3f + Mth.sin(gameTime * 0.15f) * 0.1f;

    poseStack.pushPose();
    poseStack.translate(0.5, 0.1, 0.5);

    VertexConsumer glowBuffer = bufferSource.getBuffer(RenderType.translucent());
    Matrix4f matrix = poseStack.last().pose();

    int r = 100;
    int g = 50;
    int b = 200;
    int a = (int) (glowIntensity * 100);

    float size = 0.6f + Mth.sin(gameTime * 0.1f) * 0.05f;

    glowBuffer.vertex(matrix, -size, 0, -size).color(r, g, b, a).uv(0, 0).overlayCoords(0).uv2(240).normal(0, 1, 0).endVertex();
    glowBuffer.vertex(matrix, size, 0, -size).color(r, g, b, a).uv(1, 0).overlayCoords(0).uv2(240).normal(0, 1, 0).endVertex();
    glowBuffer.vertex(matrix, size, 0, size).color(r, g, b, a).uv(1, 1).overlayCoords(0).uv2(240).normal(0, 1, 0).endVertex();
    glowBuffer.vertex(matrix, -size, 0, size).color(r, g, b, a).uv(0, 1).overlayCoords(0).uv2(240).normal(0, 1, 0).endVertex();

    poseStack.popPose();
  }

  @Override
  public int getViewDistance() {
    return 256;
  }
}
