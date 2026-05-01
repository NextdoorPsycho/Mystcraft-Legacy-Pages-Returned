package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renderer for MystcraftFallingBlockEntity. Similar to vanilla
 * FallingBlockRenderer.
 */
public class MystcraftFallingBlockRenderer extends EntityRenderer<MystcraftFallingBlockEntity> {

  private final BlockRenderDispatcher blockRenderer;

  public MystcraftFallingBlockRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.5F;
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(MystcraftFallingBlockEntity entity, float entityYaw, float partialTicks,
                     PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
    BlockState blockState = entity.getBlockState();
    if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
      return;
    }

    Level level = entity.level();
    if (blockState == level.getBlockState(entity.blockPosition()) && blockState.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
      return;
    }

    poseStack.pushPose();
    BlockPos blockPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
    poseStack.translate(-0.5D, 0.0D, -0.5D);

    var model = blockRenderer.getBlockModel(blockState);
    for (var renderType : model.getRenderTypes(blockState, RandomSource.create(blockState.getSeed(entity.getStartPos())), net.minecraftforge.client.model.data.ModelData.EMPTY)) {
      blockRenderer.getModelRenderer().tesselateBlock(
          level,
          model,
          blockState,
          blockPos,
          poseStack,
          buffer.getBuffer(net.minecraft.client.renderer.ItemBlockRenderTypes.getMovingBlockRenderType(blockState)),
          false,
          RandomSource.create(),
          blockState.getSeed(entity.getStartPos()),
          OverlayTexture.NO_OVERLAY,
          net.minecraftforge.client.model.data.ModelData.EMPTY,
          renderType
      );
    }

    poseStack.popPose();
    super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
  }

  @Override
  public ResourceLocation getTextureLocation(MystcraftFallingBlockEntity entity) {
    return InventoryMenu.BLOCK_ATLAS;
  }
}
