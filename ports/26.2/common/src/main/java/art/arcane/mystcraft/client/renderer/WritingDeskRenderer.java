package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.WritingDeskBlock;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.client.model.WritingDeskModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Extracted-state renderer for the multi-block Writing Desk. */
public final class WritingDeskRenderer
    implements BlockEntityRenderer<WritingDeskBlockEntity, WritingDeskRenderer.State> {

  private static final Identifier TEXTURE =
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/desk.png");

  private final WritingDeskModel model;
  private final ItemModelResolver itemModelResolver;

  public WritingDeskRenderer(BlockEntityRendererProvider.Context context) {
    this.model = new WritingDeskModel(context.bakeLayer(WritingDeskModel.LAYER_LOCATION));
    this.itemModelResolver = context.itemModelResolver();
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(WritingDeskBlockEntity blockEntity, State state,
                                 float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(
        blockEntity, state, partialTicks, cameraPosition, breakProgress);

    BlockState blockState = blockEntity.getBlockState();
    state.hiddenPart = blockState.getValue(WritingDeskBlock.IS_TOP)
        || blockState.getValue(WritingDeskBlock.IS_FOOT);
    state.facing = blockState.getValue(WritingDeskBlock.FACING);
    state.deskLight = lightAcrossBothDeskLevels(blockEntity);

    this.itemModelResolver.updateForTopItem(
        state.writingItem,
        blockEntity.getWritingItem(),
        ItemDisplayContext.FIXED,
        blockEntity.getLevel(),
        null,
        (int) blockEntity.getBlockPos().asLong());
  }

  private static int lightAcrossBothDeskLevels(WritingDeskBlockEntity blockEntity) {
    Level level = blockEntity.getLevel();
    if (level == null) {
      return LightCoordsUtil.FULL_BRIGHT;
    }

    BlockPos pos = blockEntity.getBlockPos();
    BlockPos topPos = pos.above();
    int blockLight = Math.max(
        level.getBrightness(LightLayer.BLOCK, pos),
        level.getBrightness(LightLayer.BLOCK, topPos));
    int skyLight = Math.max(
        level.getBrightness(LightLayer.SKY, pos),
        level.getBrightness(LightLayer.SKY, topPos));
    return LightCoordsUtil.pack(blockLight, skyLight);
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    if (state.hiddenPart) {
      return;
    }

    poseStack.pushPose();
    poseStack.translate(0.5, 1.5, 0.5);
    poseStack.mulPose(Axis.XP.rotationDegrees(90));
    poseStack.mulPose(Axis.YP.rotationDegrees(90));
    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
    poseStack.mulPose(Axis.YP.rotationDegrees(90 * state.facing.get2DDataValue()));
    submitNodeCollector.submitModel(
        this.model,
        Unit.INSTANCE,
        poseStack,
        TEXTURE,
        state.deskLight,
        OverlayTexture.NO_OVERLAY,
        0,
        state.breakProgress);
    poseStack.popPose();

    if (!state.writingItem.isEmpty()) {
      submitWritingItem(state, poseStack, submitNodeCollector);
    }
  }

  private static void submitWritingItem(State state, PoseStack poseStack,
                                        SubmitNodeCollector submitNodeCollector) {
    poseStack.pushPose();
    poseStack.translate(0.5, 1.1, 0.5);
    float rotation = switch (state.facing) {
      case SOUTH -> 180;
      case WEST -> 90;
      case EAST -> -90;
      default -> 0;
    };
    poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
    poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
    poseStack.scale(0.5F, 0.5F, 0.5F);
    state.writingItem.submit(
        poseStack, submitNodeCollector, state.deskLight, OverlayTexture.NO_OVERLAY, 0);
    poseStack.popPose();
  }

  @Override
  public int getViewDistance() {
    return 64;
  }

  @Override
  public boolean shouldRenderOffScreen() {
    // The model spans neighboring desk blocks, so the anchor block alone is
    // not a reliable frustum-culling volume.
    return true;
  }

  public static final class State extends BlockEntityRenderState {
    private boolean hiddenPart;
    private Direction facing = Direction.NORTH;
    private int deskLight = LightCoordsUtil.FULL_BRIGHT;
    private final ItemStackRenderState writingItem = new ItemStackRenderState();
  }
}
