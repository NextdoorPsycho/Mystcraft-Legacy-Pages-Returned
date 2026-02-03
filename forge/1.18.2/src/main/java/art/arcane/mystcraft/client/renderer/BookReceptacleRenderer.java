package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Book Receptacle block entity.
 * Renders a closed book on the receptacle plate, oriented based on facing direction.
 * <p>
 * 1.18.2 version - APIs are compatible with 1.19.2.
 */
public class BookReceptacleRenderer implements BlockEntityRenderer<BookReceptacleBlockEntity> {

  private static final ResourceLocation LINKBOOK_TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
  private static final ResourceLocation AGEBOOK_TEXTURE =
      new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/agebook.png");

  private final BookModel bookModel;

  public BookReceptacleRenderer(BlockEntityRendererProvider.Context context) {
    this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
  }

  @Override
  public void render(@NotNull BookReceptacleBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                     @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

    ItemStack book = blockEntity.getBook();
    if (book.isEmpty()) {
      return;
    }

    if (!(book.getItem() instanceof LinkbookItem) && !(book.getItem() instanceof AgebookItem)) {
      return;
    }

    Direction facing = blockEntity.getBlockState().getValue(BookReceptacleBlock.FACING);

    poseStack.pushPose();

    // Translate to center of block (matches old: x + 0.5, y + 0.5, z + 0.5)
    poseStack.translate(0.5, 0.5, 0.5);

    // Apply rotation based on facing - matches old RenderBookReceptacle switch
    switch (facing) {
      case DOWN -> {
        // No rotation needed (default orientation)
      }
      case UP -> {
        poseStack.mulPose(Vector3f.XN.rotationDegrees(90));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
      }
      case NORTH -> {
        poseStack.mulPose(Vector3f.YN.rotationDegrees(90));
      }
      case SOUTH -> {
        poseStack.mulPose(Vector3f.YP.rotationDegrees(90));
      }
      case WEST -> {
        // No Y rotation (matches old default)
      }
      case EAST -> {
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180));
      }
    }

    poseStack.scale(0.8f, 0.8f, 0.8f); // Book display scale

    bookModel.setupAnim(0, 0, 0, 0.0f); // Closed state

    // Choose texture based on book type
    ResourceLocation bookTexture = (book.getItem() instanceof AgebookItem)
        ? AGEBOOK_TEXTURE
        : LINKBOOK_TEXTURE;

    // Render the book model
    VertexConsumer bookConsumer = bufferSource.getBuffer(RenderType.entitySolid(bookTexture));
    bookModel.render(poseStack, bookConsumer, packedLight, OverlayTexture.NO_OVERLAY,
        1.0f, 1.0f, 1.0f, 1.0f);

    poseStack.popPose();
  }
}
