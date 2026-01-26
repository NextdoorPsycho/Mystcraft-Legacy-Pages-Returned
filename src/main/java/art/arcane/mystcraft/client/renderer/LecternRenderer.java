package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.blockentity.LecternBlockEntity;
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
 * Renderer for the Mystcraft Lectern block entity.
 * Displays the book item on the lectern surface.
 */
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {

    private final ItemRenderer itemRenderer;

    public LecternRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(@NotNull LecternBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack book = blockEntity.getBook();
        if (book.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Position the book on the lectern surface
        poseStack.translate(0.5, 1.0, 0.5);

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

        // Tilt to match lectern angle (roughly 45 degrees)
        poseStack.mulPose(Axis.XP.rotationDegrees(-67.5f));

        // Move forward slightly to sit on the angled surface
        poseStack.translate(0, 0, -0.1);

        // Scale down the book
        poseStack.scale(0.5f, 0.5f, 0.5f);

        // Render the book item
        itemRenderer.renderStatic(book, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, blockEntity.getLevel(), 0);

        poseStack.popPose();
    }
}
