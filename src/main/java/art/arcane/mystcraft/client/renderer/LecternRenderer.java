package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.blockentity.LecternBlockEntity;
import art.arcane.mystcraft.client.model.LecternModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Mystcraft Lectern block entity.
 * Renders the lectern model using the entity texture with proper wedge geometry,
 * then displays the book item on the sloped surface.
 */
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {

    private final LecternModel model;
    private final ItemRenderer itemRenderer;

    public LecternRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LecternModel();
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
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

        // Render the book on the lectern surface
        ItemStack book = blockEntity.getBook();
        if (!book.isEmpty()) {
            poseStack.pushPose();

            // Position the book on the sloped lectern surface
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

            // The lectern slopes from height 1/16 (left) to 7/16 (right)
            // Center height is about 4/16 = 0.25, position book there
            poseStack.translate(0, 0.3, 0);

            // Tilt to match the lectern slope (approximately 20 degrees around Z axis)
            poseStack.mulPose(Axis.ZP.rotationDegrees(-20f));

            // Scale down the book
            poseStack.scale(0.5f, 0.5f, 0.5f);

            // Render the book item
            itemRenderer.renderStatic(book, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, bufferSource, blockEntity.getLevel(), 0);

            poseStack.popPose();
        }
    }
}
