package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Renders the receptacle's stored linking book against the mounting face. */
public class BookReceptacleRenderer
    implements BlockEntityRenderer<BookReceptacleBlockEntity, BookReceptacleRenderer.State> {

  private static final Identifier LINKBOOK_TEXTURE =
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
  private static final Identifier AGEBOOK_TEXTURE =
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/agebook.png");
  private static final BookModel.State CLOSED_BOOK =
      BookModel.State.forAnimation(0.0F, 0.0F, 0.0F, 0.0F);

  private final BookModel bookModel;

  public BookReceptacleRenderer(BlockEntityRendererProvider.Context context) {
    this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(BookReceptacleBlockEntity blockEntity, State state,
                                 float partialTicks, Vec3 cameraPosition,
                                 ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(
        blockEntity, state, partialTicks, cameraPosition, breakProgress);
    ItemStack book = blockEntity.getBook();
    state.hasBook = !book.isEmpty() &&
        (book.getItem() instanceof LinkbookItem || book.getItem() instanceof AgebookItem);
    state.agebook = book.getItem() instanceof AgebookItem;
    state.facing = blockEntity.getBlockState().getValue(BookReceptacleBlock.FACING);
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    if (!state.hasBook) {
      return;
    }
    poseStack.pushPose();
    poseStack.translate(0.5F, 0.5F, 0.5F);
    Vec3i normal = state.facing.getUnitVec3i();
    float bookOffset = 0.20F;
    poseStack.translate(
        normal.getX() * bookOffset,
        normal.getY() * bookOffset,
        normal.getZ() * bookOffset);
    switch (state.facing) {
      case UP -> {
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      }
      case NORTH -> poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
      case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      default -> {
      }
    }
    poseStack.scale(0.8F, 0.8F, 0.8F);
    submitNodeCollector.submitModel(
        this.bookModel,
        CLOSED_BOOK,
        poseStack,
        state.agebook ? AGEBOOK_TEXTURE : LINKBOOK_TEXTURE,
        state.lightCoords,
        OverlayTexture.NO_OVERLAY,
        0,
        state.breakProgress);
    poseStack.popPose();
  }

  public static final class State extends BlockEntityRenderState {
    private boolean hasBook;
    private boolean agebook;
    private Direction facing = Direction.NORTH;
  }
}
