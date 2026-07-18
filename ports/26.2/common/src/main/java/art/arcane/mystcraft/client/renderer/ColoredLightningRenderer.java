package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.ColoredLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4fc;

/** Renders a vanilla-shaped lightning bolt with the entity's synchronized color. */
public class ColoredLightningRenderer
    extends EntityRenderer<ColoredLightningEntity, ColoredLightningRenderer.State> {

  public ColoredLightningRenderer(EntityRendererProvider.Context context) {
    super(context);
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(ColoredLightningEntity entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.seed = entity.seed;
    state.red = entity.getRed();
    state.green = entity.getGreen();
    state.blue = entity.getBlue();
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    float[] xOffsets = new float[8];
    float[] zOffsets = new float[8];
    float xOffset = 0.0F;
    float zOffset = 0.0F;
    RandomSource random = RandomSource.createThreadLocalInstance(state.seed);
    for (int height = 7; height >= 0; height--) {
      xOffsets[height] = xOffset;
      zOffsets[height] = zOffset;
      xOffset += random.nextInt(11) - 5;
      zOffset += random.nextInt(11) - 5;
    }

    float finalXOffset = xOffset;
    float finalZOffset = zOffset;
    submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
      Matrix4fc poseMatrix = pose.pose();
      for (int branch = 0; branch < 4; branch++) {
        RandomSource branchRandom = RandomSource.createThreadLocalInstance(state.seed);
        for (int layer = 0; layer < 3; layer++) {
          int startHeight = layer == 0 ? 7 : 7 - layer;
          int endHeight = layer == 0 ? 0 : startHeight - 2;
          float currentX = xOffsets[startHeight] - finalXOffset;
          float currentZ = zOffsets[startHeight] - finalZOffset;
          for (int height = startHeight; height >= endHeight; height--) {
            float nextX = currentX;
            float nextZ = currentZ;
            if (layer == 0) {
              currentX += branchRandom.nextInt(11) - 5;
              currentZ += branchRandom.nextInt(11) - 5;
            } else {
              currentX += branchRandom.nextInt(31) - 15;
              currentZ += branchRandom.nextInt(31) - 15;
            }

            float lowerRadius = 0.1F + branch * 0.2F;
            if (layer == 0) {
              lowerRadius *= height * 0.1F + 1.0F;
            }
            float upperRadius = 0.1F + branch * 0.2F;
            if (layer == 0) {
              upperRadius *= (height - 1.0F) * 0.1F + 1.0F;
            }

            quad(poseMatrix, buffer, currentX, currentZ, height, nextX, nextZ,
                state, lowerRadius, upperRadius, false, false, true, false);
            quad(poseMatrix, buffer, currentX, currentZ, height, nextX, nextZ,
                state, lowerRadius, upperRadius, true, false, true, true);
            quad(poseMatrix, buffer, currentX, currentZ, height, nextX, nextZ,
                state, lowerRadius, upperRadius, true, true, false, true);
            quad(poseMatrix, buffer, currentX, currentZ, height, nextX, nextZ,
                state, lowerRadius, upperRadius, false, true, false, false);
          }
        }
      }
    });
    super.submit(state, poseStack, submitNodeCollector, camera);
  }

  private static void quad(Matrix4fc pose, VertexConsumer buffer, float lowerX, float lowerZ,
                           int height, float upperX, float upperZ, State state,
                           float upperRadius, float lowerRadius, boolean lowerPositiveX,
                           boolean lowerPositiveZ, boolean upperPositiveX,
                           boolean upperPositiveZ) {
    buffer.addVertex(pose,
            lowerX + (lowerPositiveX ? lowerRadius : -lowerRadius), height * 16,
            lowerZ + (lowerPositiveZ ? lowerRadius : -lowerRadius))
        .setColor(state.red, state.green, state.blue, 0.3F);
    buffer.addVertex(pose,
            upperX + (lowerPositiveX ? upperRadius : -upperRadius), (height + 1) * 16,
            upperZ + (lowerPositiveZ ? upperRadius : -upperRadius))
        .setColor(state.red, state.green, state.blue, 0.3F);
    buffer.addVertex(pose,
            upperX + (upperPositiveX ? upperRadius : -upperRadius), (height + 1) * 16,
            upperZ + (upperPositiveZ ? upperRadius : -upperRadius))
        .setColor(state.red, state.green, state.blue, 0.3F);
    buffer.addVertex(pose,
            lowerX + (upperPositiveX ? lowerRadius : -lowerRadius), height * 16,
            lowerZ + (upperPositiveZ ? lowerRadius : -lowerRadius))
        .setColor(state.red, state.green, state.blue, 0.3F);
  }

  public static final class State extends EntityRenderState {
    private long seed;
    private float red;
    private float green;
    private float blue;
  }
}
