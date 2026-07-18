package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.item.AgebookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Renders a dropped linkbook as an open book resting on the ground. */
public class LinkbookEntityRenderer
    extends EntityRenderer<LinkbookEntity, LinkbookEntityRenderer.State> {

  private static final Identifier LINKBOOK_TEXTURE =
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
  private static final Identifier AGEBOOK_TEXTURE =
      Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "textures/entity/agebook.png");

  private final BookModel bookModel;

  public LinkbookEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    this.shadowRadius = 0.3F;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(LinkbookEntity entity, State state, float partialTicks) {
    super.extractRenderState(entity, state, partialTicks);
    state.hasBook = !entity.getBookItem().isEmpty();
    state.agebook = entity.getBookItem().getItem() instanceof AgebookItem;
    state.hurt = entity.hurtTime > 0;
    state.yRot = entity.getYRot(partialTicks);
  }

  @Override
  public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                     CameraRenderState camera) {
    if (!state.hasBook) {
      return;
    }
    poseStack.pushPose();
    poseStack.translate(0.0F, 0.0625F, 0.0F);
    poseStack.mulPose(Axis.YN.rotationDegrees(state.yRot + 90.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
    poseStack.scale(0.8F, 0.8F, 0.8F);

    Identifier texture = state.agebook ? AGEBOOK_TEXTURE : LINKBOOK_TEXTURE;
    BookModel.State bookState = BookModel.State.forAnimation(
        state.ageInTicks, 0.1F, 0.9F, 1.2F);
    submitNodeCollector.submitModel(
        this.bookModel,
        bookState,
        poseStack,
        texture,
        state.lightCoords,
        OverlayTexture.NO_OVERLAY,
        state.outlineColor,
        null);
    if (state.hurt) {
      submitNodeCollector.submitModel(
          this.bookModel,
          bookState,
          poseStack,
          RenderTypes.entityTranslucent(texture),
          state.lightCoords,
          OverlayTexture.pack(0, true),
          0x66B30000,
          null,
          state.outlineColor,
          null);
    }
    poseStack.popPose();
    super.submit(state, poseStack, submitNodeCollector, camera);
  }

  public static final class State extends EntityRenderState {
    private boolean hasBook;
    private boolean agebook;
    private boolean hurt;
    private float yRot;
  }
}
