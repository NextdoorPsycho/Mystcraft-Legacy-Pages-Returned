package art.arcane.mystcraft.fabric.client;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** 26.2 extracted-state renderer for Mystcraft's loader-neutral falling block entity. */
public final class FabricMystcraftFallingBlockRenderer
    extends EntityRenderer<MystcraftFallingBlockEntity, FallingBlockRenderState> {

  public FabricMystcraftFallingBlockRenderer(EntityRendererProvider.Context context) {
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
                                 FallingBlockRenderState state, float partialTick) {
    super.extractRenderState(entity, state, partialTick);
    BlockPos renderPosition = BlockPos.containing(
        entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
    state.movingBlockRenderState.randomSeedPos = entity.getStartPos();
    state.movingBlockRenderState.blockPos = renderPosition;
    state.movingBlockRenderState.blockState = entity.getBlockState();

    Level level = entity.level();
    if (level instanceof ClientLevel clientLevel) {
      state.movingBlockRenderState.biome = clientLevel.getBiome(renderPosition);
      state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
      state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
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
