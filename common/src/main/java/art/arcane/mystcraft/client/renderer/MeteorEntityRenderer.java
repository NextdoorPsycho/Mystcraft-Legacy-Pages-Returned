package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import art.arcane.mystcraft.util.RenderCompat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renderer for the Meteor entity.
 * Renders a glowing fireball with a trail effect.
 */
public class MeteorEntityRenderer extends EntityRenderer<MeteorEntity> {

  private static final ResourceLocation TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/meteor.png");

  // Fallback to magma block texture if custom texture doesn't exist
  private static final ResourceLocation FALLBACK_TEXTURE =
      new ResourceLocation("textures/block/magma.png");

  public MeteorEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.5f;
  }

  @Override
  public void render(@NotNull MeteorEntity entity, float entityYaw, float partialTick,
                     @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {

    poseStack.pushPose();

    // Scale based on meteor size
    float size = entity.getSize();
    poseStack.scale(size, size, size);

    // Tumble rotation
    float tumble = (entity.tickCount + partialTick) * 10.0f;
    poseStack.mulPose(Axis.YP.rotationDegrees(tumble));
    poseStack.mulPose(Axis.XP.rotationDegrees(tumble * 0.7f));
    poseStack.mulPose(Axis.ZP.rotationDegrees(tumble * 0.3f));

    // Render the meteor as a textured cube
    renderMeteorCube(poseStack, bufferSource, packedLight, entity.tickCount + partialTick);

    poseStack.popPose();

    // Render trail particles
    renderTrail(entity, partialTick, poseStack, bufferSource);

    super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
  }

  /**
   * Renders the meteor as a glowing cube.
   */
  private void renderMeteorCube(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float time) {
    Matrix4f matrix = poseStack.last().pose();
    Matrix3f normal = poseStack.last().normal();

    // Pulsing glow effect
    float pulse = (float) (Math.sin(time * 0.5) * 0.2 + 0.8);
    float r = 1.0f;
    float g = 0.5f * pulse;
    float b = 0.2f * pulse;

    VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(FALLBACK_TEXTURE));

    float size = 0.5f;

    // Render all 6 faces
    // Top
    PoseStack.Pose pose = poseStack.last();
    addFace(buffer, pose, matrix, normal, -size, size, -size, size, size, size, 0, 1, 0, r, g, b, 1.0f);
    // Bottom
    addFace(buffer, pose, matrix, normal, -size, -size, size, size, -size, -size, 0, -1, 0, r, g, b, 1.0f);
    // North
    addFace(buffer, pose, matrix, normal, size, -size, -size, size, size, -size, -size, size, -size, -size, -size, -size, 0, 0, -1, r, g, b, 1.0f);
    // South
    addFace(buffer, pose, matrix, normal, -size, -size, size, -size, size, size, size, size, size, size, -size, size, 0, 0, 1, r, g, b, 1.0f);
    // West
    addFace(buffer, pose, matrix, normal, -size, -size, -size, -size, size, -size, -size, size, size, -size, -size, size, -1, 0, 0, r, g, b, 1.0f);
    // East
    addFace(buffer, pose, matrix, normal, size, -size, size, size, size, size, size, size, -size, size, -size, -size, 1, 0, 0, r, g, b, 1.0f);
  }

  /**
   * Adds a face (quad) to the vertex buffer.
   */
  private void addFace(VertexConsumer buffer, PoseStack.Pose pose, Matrix4f matrix, Matrix3f normal,
                       float x1, float y1, float z1,
                       float x2, float y2, float z2,
                       float nx, float ny, float nz,
                       float r, float g, float b, float a) {
    // Simple quad for top/bottom faces
    int light = 0xF000F0; // Full bright
    int ir = (int) (r * 255);
    int ig = (int) (g * 255);
    int ib = (int) (b * 255);
    int ia = (int) (a * 255);

    VertexConsumer vertex = buffer.vertex(matrix, x1, y1, z1)
        .color(ir, ig, ib, ia)
        .uv(0, 0)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();

    vertex = buffer.vertex(matrix, x1, y1, z2)
        .color(ir, ig, ib, ia)
        .uv(0, 1)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();

    vertex = buffer.vertex(matrix, x2, y1, z2)
        .color(ir, ig, ib, ia)
        .uv(1, 1)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();

    vertex = buffer.vertex(matrix, x2, y1, z1)
        .color(ir, ig, ib, ia)
        .uv(1, 0)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();
  }

  /**
   * Adds a face with explicit 4 vertices.
   */
  private void addFace(VertexConsumer buffer, PoseStack.Pose pose, Matrix4f matrix, Matrix3f normal,
                       float x1, float y1, float z1,
                       float x2, float y2, float z2,
                       float x3, float y3, float z3,
                       float x4, float y4, float z4,
                       float nx, float ny, float nz,
                       float r, float g, float b, float a) {
    int light = 0xF000F0;
    int ir = (int) (r * 255);
    int ig = (int) (g * 255);
    int ib = (int) (b * 255);
    int ia = (int) (a * 255);

    VertexConsumer vertex = buffer.vertex(matrix, x1, y1, z1)
        .color(ir, ig, ib, ia)
        .uv(0, 0)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();

    vertex = buffer.vertex(matrix, x2, y2, z2)
        .color(ir, ig, ib, ia)
        .uv(0, 1)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();

    vertex = buffer.vertex(matrix, x3, y3, z3)
        .color(ir, ig, ib, ia)
        .uv(1, 1)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();

    vertex = buffer.vertex(matrix, x4, y4, z4)
        .color(ir, ig, ib, ia)
        .uv(1, 0)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light);
    RenderCompat.vertexNormal(vertex, pose, normal, nx, ny, nz).endVertex();
  }

  /**
   * Renders the fire trail behind the meteor.
   */
  private void renderTrail(MeteorEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
    // Trail is handled by particle system in the entity tick
    // This is a placeholder for additional visual effects
  }

  @Override
  @NotNull
  public ResourceLocation getTextureLocation(@NotNull MeteorEntity entity) {
    return FALLBACK_TEXTURE;
  }
}
