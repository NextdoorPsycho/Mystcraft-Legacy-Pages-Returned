package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.WritingDeskBlock;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.client.model.WritingDeskModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Writing Desk block entity.
 * Uses the entity model and texture from the original implementation.
 * <p>
 * 1.18.2 version - APIs are compatible with 1.19.2.
 */
public class WritingDeskRenderer implements BlockEntityRenderer<WritingDeskBlockEntity> {

  private static final ResourceLocation TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/desk.png");

  private final WritingDeskModel model;

  public WritingDeskRenderer(BlockEntityRendererProvider.Context context) {
    this.model = new WritingDeskModel(context.bakeLayer(WritingDeskModel.LAYER_LOCATION));
  }

  @Override
  public void render(@NotNull WritingDeskBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                     @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

    BlockState state = blockEntity.getBlockState();

    // Only render from the main block (not top, not foot)
    if (state.getValue(WritingDeskBlock.IS_TOP) || state.getValue(WritingDeskBlock.IS_FOOT)) {
      return;
    }

    Direction facing = state.getValue(WritingDeskBlock.FACING);
    int horizontalIndex = facing.get2DDataValue();

    // Calculate maximum light level to avoid dark shading on rotated model
    // Entity rendering applies diffuse shading based on normals, but with complex
    // rotations the normals can point in unexpected directions causing shadow-like darkening.
    // Use full sky light to minimize this effect while still respecting block light.
    Level level = blockEntity.getLevel();
    BlockPos pos = blockEntity.getBlockPos();
    int combinedLight;
    if (level != null) {
      // Get light from both main position and above
      BlockPos topPos = pos.above();
      int blockLight = Math.max(
          level.getBrightness(LightLayer.BLOCK, pos),
          level.getBrightness(LightLayer.BLOCK, topPos)
      );
      int skyLight = Math.max(
          level.getBrightness(LightLayer.SKY, pos),
          level.getBrightness(LightLayer.SKY, topPos)
      );
      combinedLight = LightTexture.pack(blockLight, skyLight);
    } else {
      combinedLight = LightTexture.FULL_BRIGHT;
    }

    // Render the desk model
    poseStack.pushPose();

    // Position and transform like the original
    poseStack.translate(0.5, 1.5, 0.5);
    poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
    poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
    poseStack.mulPose(Vector3f.ZP.rotationDegrees(90));
    poseStack.mulPose(Vector3f.YP.rotationDegrees(90 * horizontalIndex));

    VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType(TEXTURE));
    model.renderToBuffer(poseStack, vertexConsumer, combinedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);

    poseStack.popPose();

    // Render item on desk
    ItemStack writingItem = blockEntity.getWritingItem();
    if (!writingItem.isEmpty()) {
      renderDisplayItem(poseStack, bufferSource, writingItem, facing, combinedLight, packedOverlay, blockEntity);
    }
  }

  private void renderDisplayItem(PoseStack poseStack, MultiBufferSource bufferSource, ItemStack item,
                                 Direction facing, int light, int overlay, WritingDeskBlockEntity blockEntity) {
    poseStack.pushPose();

    // Position on desk surface
    poseStack.translate(0.5, 1.1, 0.5);

    // Rotate to face based on desk facing
    float rotation = switch (facing) {
      case NORTH -> 0;
      case SOUTH -> 180;
      case WEST -> 90;
      case EAST -> -90;
      default -> 0;
    };
    poseStack.mulPose(Vector3f.YP.rotationDegrees(rotation));

    // Tilt slightly
    poseStack.mulPose(Vector3f.XP.rotationDegrees(-22.5f));

    poseStack.scale(0.5f, 0.5f, 0.5f);

    Minecraft.getInstance().getItemRenderer().renderStatic(
        item, ItemTransforms.TransformType.FIXED, light, overlay, poseStack, bufferSource, 0);

    poseStack.popPose();
  }

  @Override
  public int getViewDistance() {
    return 64;
  }

  @Override
  public boolean shouldRenderOffScreen(@NotNull WritingDeskBlockEntity blockEntity) {
    // Always render when in range - the desk is a multi-block structure
    // and we need to render even when the base block position is off-screen
    return true;
  }

  public net.minecraft.world.phys.AABB getRenderBoundingBox(@NotNull WritingDeskBlockEntity blockEntity) {
    // Expand the render bounding box to cover the full 2x2 multi-block structure
    // Must account for facing direction since the foot extends in different directions
    BlockPos pos = blockEntity.getBlockPos();
    BlockState state = blockEntity.getBlockState();
    Direction facing = state.getValue(WritingDeskBlock.FACING);

    // Calculate bounds based on facing direction
    // The foot extends to the "left" of the facing direction
    int minX = pos.getX();
    int maxX = pos.getX() + 1;
    int minZ = pos.getZ();
    int maxZ = pos.getZ() + 1;

    switch (facing) {
      case SOUTH -> maxX = pos.getX() + 2; // foot to EAST
      case WEST -> maxZ = pos.getZ() + 2;  // foot to SOUTH
      case NORTH -> minX = pos.getX() - 1; // foot to WEST
      case EAST -> minZ = pos.getZ() - 1;  // foot to NORTH
    }

    return new net.minecraft.world.phys.AABB(
        minX, pos.getY(), minZ,
        maxX, pos.getY() + 2, maxZ
    );
  }
}
