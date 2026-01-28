package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import art.arcane.mystcraft.client.model.BookstandModel;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.LinkbookItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Bookstand block entity.
 * Renders the stand model using the entity texture, then displays an open book on top.
 */
public class BookstandRenderer implements BlockEntityRenderer<BookstandBlockEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/bookstand.png");
    private static final ResourceLocation LINKBOOK_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
    private static final ResourceLocation AGEBOOK_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/agebook.png");

    private final BookstandModel model;
    private final BookModel bookModel;

    public BookstandRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BookstandModel(context.bakeLayer(BookstandModel.LAYER_LOCATION));
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(@NotNull BookstandBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Get facing direction - block faces toward player when placed
        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // Calculate rotation - add 180 to make the model face toward the player instead of away
        float rotation = facing.toYRot() + 180;

        // Render the stand model
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180)); // Flip like original (Z axis)
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Render the open book on top
        ItemStack book = blockEntity.getBook();
        if (!book.isEmpty() && (book.getItem() instanceof LinkbookItem || book.getItem() instanceof AgebookItem)) {
            poseStack.pushPose();

            // Position the book on top of the stand
            poseStack.translate(0.5, 0.55, 0.5);

            // Rotate based on block facing - book faces same direction as stand
            // Original: rotate(90 + 45 * rotationIndex, 0, -1, 0) then rotate(120, 0, 0, 1)
            poseStack.mulPose(Axis.YN.rotationDegrees(90 + rotation));
            poseStack.mulPose(Axis.ZP.rotationDegrees(120));

            poseStack.scale(0.8f, 0.8f, 0.8f); // Book display scale

            bookModel.setupAnim(0, 0, 0, 1.05f); // Open state

            // Choose texture based on book type
            ResourceLocation bookTexture = (book.getItem() instanceof AgebookItem)
                    ? AGEBOOK_TEXTURE
                    : LINKBOOK_TEXTURE;

            // Render the open book model
            VertexConsumer bookConsumer = bufferSource.getBuffer(RenderType.entitySolid(bookTexture));
            bookModel.render(poseStack, bookConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, 1.0f);

            poseStack.popPose();
        }
    }
}
