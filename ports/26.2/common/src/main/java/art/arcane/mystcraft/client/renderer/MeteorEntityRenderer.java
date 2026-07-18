package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/** Renders the meteor as a full-bright tumbling magma cube. */
public class MeteorEntityRenderer
    extends EntityRenderer<MeteorEntity, MeteorEntityRenderer.State> {

  private static final Identifier FALLBACK_TEXTURE =
      Identifier.withDefaultNamespace("textures/block/magma.png");

  public MeteorEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.5F;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(MeteorEntity entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.size = entity.getSize();
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    poseStack.pushPose();
    poseStack.scale(state.size, state.size, state.size);
    float tumble = state.ageInTicks * 10.0F;
    poseStack.mulPose(Axis.YP.rotationDegrees(tumble));
    poseStack.mulPose(Axis.XP.rotationDegrees(tumble * 0.7F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(tumble * 0.3F));

    float pulse = (float) (Math.sin(state.ageInTicks * 0.5F) * 0.2F + 0.8F);
    int red = 255;
    int green = Math.round(127.5F * pulse);
    int blue = Math.round(51.0F * pulse);
    submitNodeCollector.submitCustomGeometry(
        poseStack,
        RenderTypes.entityTranslucent(FALLBACK_TEXTURE),
        (pose, buffer) -> renderCube(pose, buffer, red, green, blue));
    poseStack.popPose();
    super.submit(state, poseStack, submitNodeCollector, camera);
  }

  private static void renderCube(PoseStack.Pose pose, VertexConsumer buffer,
                                 int red, int green, int blue) {
    float min = -0.5F;
    float max = 0.5F;
    addFace(pose, buffer, min, max, min, max, max, max,
        0.0F, 1.0F, 0.0F, red, green, blue);
    addFace(pose, buffer, min, min, max, max, min, min,
        0.0F, -1.0F, 0.0F, red, green, blue);
    addFace(pose, buffer,
        max, min, min, max, max, min, min, max, min, min, min, min,
        0.0F, 0.0F, -1.0F, red, green, blue);
    addFace(pose, buffer,
        min, min, max, min, max, max, max, max, max, max, min, max,
        0.0F, 0.0F, 1.0F, red, green, blue);
    addFace(pose, buffer,
        min, min, min, min, max, min, min, max, max, min, min, max,
        -1.0F, 0.0F, 0.0F, red, green, blue);
    addFace(pose, buffer,
        max, min, max, max, max, max, max, max, min, max, min, min,
        1.0F, 0.0F, 0.0F, red, green, blue);
  }

  private static void addFace(PoseStack.Pose pose, VertexConsumer buffer,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float nx, float ny, float nz, int red, int green, int blue) {
    addFace(pose, buffer,
        x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1,
        nx, ny, nz, red, green, blue);
  }

  private static void addFace(PoseStack.Pose pose, VertexConsumer buffer,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float x3, float y3, float z3, float x4, float y4, float z4,
                              float nx, float ny, float nz, int red, int green, int blue) {
    addVertex(pose, buffer, x1, y1, z1, 0.0F, 0.0F, nx, ny, nz, red, green, blue);
    addVertex(pose, buffer, x2, y2, z2, 0.0F, 1.0F, nx, ny, nz, red, green, blue);
    addVertex(pose, buffer, x3, y3, z3, 1.0F, 1.0F, nx, ny, nz, red, green, blue);
    addVertex(pose, buffer, x4, y4, z4, 1.0F, 0.0F, nx, ny, nz, red, green, blue);
  }

  private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer,
                                float x, float y, float z, float u, float v,
                                float nx, float ny, float nz,
                                int red, int green, int blue) {
    buffer.addVertex(pose, x, y, z)
        .setColor(red, green, blue, 255)
        .setUv(u, v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(LightCoordsUtil.FULL_BRIGHT)
        .setNormal(pose, nx, ny, nz);
  }

  public static final class State extends EntityRenderState {
    private float size;
  }
}
