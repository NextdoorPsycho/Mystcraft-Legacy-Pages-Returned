package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/** Animated, layered End-portal-style rendering for the Star Fissure. */
public class StarFissureRenderer
    implements BlockEntityRenderer<StarFissureBlockEntity, StarFissureRenderer.State> {

  public StarFissureRenderer(BlockEntityRendererProvider.Context context) {
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(StarFissureBlockEntity blockEntity, State state,
                                 float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(
        blockEntity, state, partialTicks, cameraPosition, breakProgress);
    state.gameTime = (blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime())
        + partialTicks;
    state.progress = blockEntity.getFormProgress();
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    if (state.progress <= 0.0F) {
      return;
    }
    poseStack.pushPose();
    poseStack.translate(0.5F, 0.0F, 0.5F);
    poseStack.scale(state.progress, 1.0F, state.progress);
    poseStack.translate(-0.5F, 0.0F, -0.5F);

    for (int layer = 0; layer < 3; layer++) {
      poseStack.pushPose();
      float layerOffset = layer * 0.02F;
      float rotation = state.gameTime * (1.0F + layer * 0.5F) % 360.0F;
      poseStack.translate(0.5F, 0.05F + layerOffset, 0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
      float pulse = 1.0F + Mth.sin(state.gameTime * 0.1F + layer) * 0.02F;
      poseStack.scale(pulse, 1.0F, pulse);
      poseStack.translate(-0.5F, 0.0F, -0.5F);
      submitNodeCollector.submitCustomGeometry(
          poseStack,
          RenderTypes.endPortal(),
          (pose, buffer) -> {
            buffer.addVertex(pose, 0.0F, 0.0F, 0.0F);
            buffer.addVertex(pose, 1.0F, 0.0F, 0.0F);
            buffer.addVertex(pose, 1.0F, 0.0F, 1.0F);
            buffer.addVertex(pose, 0.0F, 0.0F, 1.0F);
          });
      poseStack.popPose();
    }

    float glowIntensity = (0.3F + Mth.sin(state.gameTime * 0.15F) * 0.1F) * state.progress;
    float size = 0.6F + Mth.sin(state.gameTime * 0.1F) * 0.05F;
    int alpha = Math.round(glowIntensity * 100.0F);
    poseStack.pushPose();
    poseStack.translate(0.5F, 0.1F, 0.5F);
    submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
      buffer.addVertex(pose, -size, 0.0F, -size).setColor(100, 50, 200, alpha);
      buffer.addVertex(pose, size, 0.0F, -size).setColor(100, 50, 200, alpha);
      buffer.addVertex(pose, size, 0.0F, size).setColor(100, 50, 200, alpha);
      buffer.addVertex(pose, -size, 0.0F, size).setColor(100, 50, 200, alpha);
    });
    poseStack.popPose();
    poseStack.popPose();
  }

  @Override
  public int getViewDistance() {
    return 256;
  }

  public static final class State extends BlockEntityRenderState {
    private float gameTime;
    private float progress;
  }
}
