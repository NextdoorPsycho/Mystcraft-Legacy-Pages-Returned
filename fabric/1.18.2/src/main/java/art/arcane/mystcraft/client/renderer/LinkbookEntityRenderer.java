package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.item.AgebookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Linkbook entity (dropped book in world).
 * Uses the vanilla BookModel to render an open book on the ground.
 * <p>
 * 1.18.2 version - APIs are compatible with 1.19.2.
 */
public class LinkbookEntityRenderer extends EntityRenderer<LinkbookEntity> {

  private static final ResourceLocation LINKBOOK_TEXTURE =
      new ResourceLocation("mystcraft", "textures/entity/linkbook.png");
  private static final ResourceLocation AGEBOOK_TEXTURE =
      new ResourceLocation("mystcraft", "textures/entity/agebook.png");

  private final BookModel bookModel;

  public LinkbookEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    this.shadowRadius = 0.3f;
  }

  @Override
  public void render(@NotNull LinkbookEntity entity, float entityYaw, float partialTick,
                     @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
    ItemStack book = entity.getBookItem();
    if (book.isEmpty()) {
      return;
    }

    poseStack.pushPose();

    // Slight vertical offset so the book sits on top of the ground plane
    poseStack.translate(0, 0.0625, 0);

    // Lay the book flat on the ground, facing the entity direction
    poseStack.mulPose(Vector3f.YN.rotationDegrees(entityYaw + 90));
    poseStack.mulPose(Vector3f.ZP.rotationDegrees(90));

    poseStack.scale(0.8f, 0.8f, 0.8f); // Book display scale

    // Fully open state
    bookModel.setupAnim(0, 0, 0, 1.2f);

    // Choose texture based on book type
    ResourceLocation texture = (book.getItem() instanceof AgebookItem)
        ? AGEBOOK_TEXTURE
        : LINKBOOK_TEXTURE;

    // Render the book model
    VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(texture));
    bookModel.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
        1.0f, 1.0f, 1.0f, 1.0f);

    // Render hurt overlay if entity is damaged (red tint)
    if (entity.hurtTime > 0) {
      VertexConsumer hurtConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
      bookModel.render(poseStack, hurtConsumer, packedLight, OverlayTexture.pack(0, true),
          0.7f, 0.0f, 0.0f, 0.4f);
    }

    poseStack.popPose();

    super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
  }

  @Override
  @NotNull
  public ResourceLocation getTextureLocation(@NotNull LinkbookEntity entity) {
    ItemStack book = entity.getBookItem();
    if (book.getItem() instanceof AgebookItem) {
      return AGEBOOK_TEXTURE;
    }
    return LINKBOOK_TEXTURE;
  }
}
