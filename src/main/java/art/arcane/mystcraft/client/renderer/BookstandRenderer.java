package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.blockentity.BookstandBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * Renderer for the Bookstand block entity.
 * Displays the book item on top of the stand.
 */
public class BookstandRenderer implements BlockEntityRenderer<BookstandBlockEntity> {

    private final ItemRenderer itemRenderer;

    public BookstandRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(@NotNull BookstandBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack book = blockEntity.getBook();
        if (book.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Position the book on top of the stand
        poseStack.translate(0.5, 0.9, 0.5);

        // Rotate based on block facing
        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        float rotation = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Tilt the book slightly
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5f));

        // Scale down the book
        poseStack.scale(0.6f, 0.6f, 0.6f);

        // Render the book item
        itemRenderer.renderStatic(book, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, blockEntity.getLevel(), 0);

        poseStack.popPose();
    }
}
