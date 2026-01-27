package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.LecternBlockEntity;
import art.arcane.mystcraft.client.model.LecternModel;
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
 * Renderer for the Mystcraft Lectern block entity.
 * Renders the lectern model using the entity texture with proper wedge geometry,
 * then displays an open book on the sloped surface.
 */
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {

    private static final ResourceLocation LINKBOOK_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/linkbook.png");
    private static final ResourceLocation AGEBOOK_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/agebook.png");

    private final LecternModel model;
    private final BookModel bookModel;

    public LecternRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LecternModel();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(@NotNull LecternBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // Get facing direction from block state - block faces toward player when placed
        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // Calculate rotation - the sloped reading surface should face the stored direction
        // Add 180 to make the model face toward the player instead of away
        float rotation = facing.toYRot() + 180;

        // Render the lectern model
        poseStack.pushPose();
        // Position at center of block, at ground level (model bottom is at Y=0)
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        VertexConsumer vertexConsumer = bufferSource.getBuffer(model.renderType());
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();

        // Render the open book on the lectern surface
        ItemStack book = blockEntity.getBook();
        if (!book.isEmpty() && (book.getItem() instanceof LinkbookItem || book.getItem() instanceof AgebookItem)) {
            poseStack.pushPose();

            // Position the book on the sloped lectern surface
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            // Position book on the lectern's sloped surface
            poseStack.translate(0, 0.255, 0);

            // Tilt to match the lectern slope
            poseStack.mulPose(Axis.ZP.rotationDegrees(110));

            poseStack.scale(0.8f, 0.8f, 0.8f); // Book display scale

            bookModel.setupAnim(0, 0, 0, 1.22f); // Open state

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
