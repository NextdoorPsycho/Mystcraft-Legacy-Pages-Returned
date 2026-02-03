package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.LinkPortalBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Static renderer helper for Link Portal blocks.
 * Provides a glowing portal effect.
 */
public class LinkPortalRenderer {

  private static final ResourceLocation PORTAL_TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "textures/block/link_portal.png");

  /**
   * Renders additional portal effects for a link portal block.
   * Called from block rendering or custom render pipeline.
   */
  public static void renderPortalEffect(PoseStack poseStack, MultiBufferSource bufferSource,
                                        BlockPos pos, BlockState state, float partialTick) {
    if (!(state.getBlock() instanceof LinkPortalBlock)) {
      return;
    }

    boolean active = state.getValue(LinkPortalBlock.ACTIVE);
    if (!active) {
      return;
    }

    poseStack.pushPose();

    // Get portal color (default mystcraft blue)
    int color = 0x4488FF;
    float r = ((color >> 16) & 0xFF) / 255.0f;
    float g = ((color >> 8) & 0xFF) / 255.0f;
    float b = (color & 0xFF) / 255.0f;
    float a = 0.8f;

    // Animate the portal
    Level level = Minecraft.getInstance().level;
    float time = level != null ? (level.getGameTime() + partialTick) * 0.05f : 0;
    float pulse = (float) (Math.sin(time) * 0.1 + 0.9);

    Matrix4f matrix = poseStack.last().pose();
    Matrix3f normal = poseStack.last().normal();

    // Render using translucent render type
    VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());

    // Render portal faces based on direction
    Direction sourceDir = state.getValue(LinkPortalBlock.SOURCE_DIRECTION);
    renderPortalFaces(buffer, matrix, normal, sourceDir, r * pulse, g * pulse, b * pulse, a);

    poseStack.popPose();
  }

  /**
   * Renders the portal faces with glow effect.
   */
  private static void renderPortalFaces(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                                        Direction sourceDir, float r, float g, float b, float a) {
    float min = 0.25f;
    float max = 0.75f;

    // Render center cube with glow
    // Top face
    addQuad(buffer, matrix, normal,
        min, max, min,
        max, max, min,
        max, max, max,
        min, max, max,
        0, 1, 0, r, g, b, a);

    // Bottom face
    addQuad(buffer, matrix, normal,
        min, min, max,
        max, min, max,
        max, min, min,
        min, min, min,
        0, -1, 0, r, g, b, a);

    // North face
    addQuad(buffer, matrix, normal,
        max, min, min,
        max, max, min,
        min, max, min,
        min, min, min,
        0, 0, -1, r, g, b, a);

    // South face
    addQuad(buffer, matrix, normal,
        min, min, max,
        min, max, max,
        max, max, max,
        max, min, max,
        0, 0, 1, r, g, b, a);

    // West face
    addQuad(buffer, matrix, normal,
        min, min, min,
        min, max, min,
        min, max, max,
        min, min, max,
        -1, 0, 0, r, g, b, a);

    // East face
    addQuad(buffer, matrix, normal,
        max, min, max,
        max, max, max,
        max, max, min,
        max, min, min,
        1, 0, 0, r, g, b, a);
  }

  /**
   * Adds a quad to the vertex buffer.
   */
  private static void addQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              float nx, float ny, float nz,
                              float r, float g, float b, float a) {
    int light = 0xF000F0; // Full bright
    int ir = (int) (r * 255);
    int ig = (int) (g * 255);
    int ib = (int) (b * 255);
    int ia = (int) (a * 255);

    buffer.vertex(matrix, x1, y1, z1)
        .color(ir, ig, ib, ia)
        .uv(0, 0)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, nx, ny, nz)
        .endVertex();

    buffer.vertex(matrix, x2, y2, z2)
        .color(ir, ig, ib, ia)
        .uv(0, 1)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, nx, ny, nz)
        .endVertex();

    buffer.vertex(matrix, x3, y3, z3)
        .color(ir, ig, ib, ia)
        .uv(1, 1)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, nx, ny, nz)
        .endVertex();

    buffer.vertex(matrix, x4, y4, z4)
        .color(ir, ig, ib, ia)
        .uv(1, 0)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(normal, nx, ny, nz)
        .endVertex();
  }
}
