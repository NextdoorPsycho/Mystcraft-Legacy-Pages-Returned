package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Extracted-state renderer for Mystcraft's custom falling block entity. */
final class ForgeMystcraftFallingBlockRenderer
    extends EntityRenderer<MystcraftFallingBlockEntity, FallingBlockRenderState> {

  ForgeMystcraftFallingBlockRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.5F;
  }

  @Override
  public boolean shouldRender(MystcraftFallingBlockEntity entity, Frustum frustum,
                              double cameraX, double cameraY, double cameraZ) {
    return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)
        && entity.getBlockState() != entity.level().getBlockState(entity.blockPosition());
  }

  @Override
  public FallingBlockRenderState createRenderState() {
    return new FallingBlockRenderState();
  }

  @Override
  public void extractRenderState(MystcraftFallingBlockEntity entity,
                                 FallingBlockRenderState state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    BlockPos position = BlockPos.containing(
        entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
    state.movingBlockRenderState.randomSeedPos = entity.getStartPos();
    state.movingBlockRenderState.blockPos = position;
    state.movingBlockRenderState.blockState = entity.getBlockState();
    if (entity.level() instanceof ClientLevel level) {
      state.movingBlockRenderState.biome = level.getBiome(position);
      state.movingBlockRenderState.cardinalLighting = level.cardinalLighting();
      state.movingBlockRenderState.lightEngine = level.getLightEngine();
    }
  }

  @Override
  public void submit(FallingBlockRenderState state, PoseStack poseStack,
                     SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
    BlockState blockState = state.movingBlockRenderState.blockState;
    if (blockState.getRenderShape() != RenderShape.MODEL) {
      return;
    }
    poseStack.pushPose();
    poseStack.translate(-0.5D, 0.0D, -0.5D);
    submitNodeCollector.submitMovingBlock(
        poseStack, state.movingBlockRenderState, state.outlineColor);
    poseStack.popPose();
    super.submit(state, poseStack, submitNodeCollector, camera);
  }
}
