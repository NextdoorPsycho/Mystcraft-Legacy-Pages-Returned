package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.WritingDeskBlock;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Renderer for the Writing Desk block entity.
 * Renders the desk structure and any items on it.
 * The writing desk is a 2x2x2 multi-block structure.
 */
public class WritingDeskRenderer implements BlockEntityRenderer<WritingDeskBlockEntity> {

    private static final ResourceLocation DESK_TEXTURE = new ResourceLocation(Mystcraft.MOD_ID, "textures/entity/desk.png");
    private static final ResourceLocation OAK_PLANKS = new ResourceLocation("minecraft", "block/oak_planks");

    public WritingDeskRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull WritingDeskBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState state = blockEntity.getBlockState();

        // Only render from the main block (not top, not foot)
        if (state.getValue(WritingDeskBlock.IS_TOP) || state.getValue(WritingDeskBlock.IS_FOOT)) {
            return;
        }

        Direction facing = state.getValue(WritingDeskBlock.FACING);

        poseStack.pushPose();

        // Center on the block
        poseStack.translate(0.5, 0, 0.5);

        // Rotate based on facing
        float rotation = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Get oak planks texture from atlas
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(OAK_PLANKS);

        // Render the desk structure
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());
        renderDeskStructure(poseStack, buffer, sprite, packedLight, packedOverlay);

        poseStack.popPose();

        // Render item on desk (the writing item - book or page collection)
        ItemStack writingItem = blockEntity.getWritingItem();
        if (!writingItem.isEmpty()) {
            renderDisplayItem(poseStack, bufferSource, writingItem, facing, packedLight, packedOverlay);
        }
    }

    private void renderDeskStructure(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, int light, int overlay) {
        Matrix4f matrix = poseStack.last().pose();

        // Get UV coordinates from sprite
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Desk top surface (2 blocks wide, 1 block deep)
        // Main desk top at y=1
        renderBox(poseStack, buffer, -0.5f, 0.875f, -0.5f, 1.5f, 1.0f, 0.5f, sprite, light, overlay);

        // Left leg
        renderBox(poseStack, buffer, -0.5f, 0, -0.375f, -0.25f, 0.875f, 0.375f, sprite, light, overlay);

        // Right leg (in the foot block position)
        renderBox(poseStack, buffer, 1.25f, 0, -0.375f, 1.5f, 0.875f, 0.375f, sprite, light, overlay);

        // Back panel
        renderBox(poseStack, buffer, -0.5f, 0.5f, 0.25f, 1.5f, 0.875f, 0.5f, sprite, light, overlay);

        // Top hutch back
        renderBox(poseStack, buffer, -0.5f, 1.0f, 0.25f, 1.5f, 1.75f, 0.5f, sprite, light, overlay);

        // Top shelf
        renderBox(poseStack, buffer, -0.5f, 1.375f, -0.25f, 1.5f, 1.5f, 0.25f, sprite, light, overlay);

        // Side panels of hutch
        renderBox(poseStack, buffer, -0.5f, 1.0f, -0.25f, -0.375f, 1.75f, 0.25f, sprite, light, overlay);
        renderBox(poseStack, buffer, 1.375f, 1.0f, -0.25f, 1.5f, 1.75f, 0.25f, sprite, light, overlay);
    }

    private void renderBox(PoseStack poseStack, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                           TextureAtlasSprite sprite, int light, int overlay) {
        Matrix4f matrix = poseStack.last().pose();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Top face
        buffer.vertex(matrix, x1, y2, z1).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x1, y2, z2).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z1).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();

        // Bottom face
        buffer.vertex(matrix, x1, y1, z2).color(200, 200, 200, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
        buffer.vertex(matrix, x1, y1, z1).color(200, 200, 200, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
        buffer.vertex(matrix, x2, y1, z1).color(200, 200, 200, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();
        buffer.vertex(matrix, x2, y1, z2).color(200, 200, 200, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, -1, 0).endVertex();

        // North face (negative Z)
        buffer.vertex(matrix, x2, y2, z1).color(220, 220, 220, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        buffer.vertex(matrix, x2, y1, z1).color(220, 220, 220, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        buffer.vertex(matrix, x1, y1, z1).color(220, 220, 220, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        buffer.vertex(matrix, x1, y2, z1).color(220, 220, 220, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();

        // South face (positive Z)
        buffer.vertex(matrix, x1, y2, z2).color(220, 220, 220, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
        buffer.vertex(matrix, x1, y1, z2).color(220, 220, 220, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
        buffer.vertex(matrix, x2, y1, z2).color(220, 220, 220, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(220, 220, 220, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();

        // West face (negative X)
        buffer.vertex(matrix, x1, y2, z1).color(180, 180, 180, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
        buffer.vertex(matrix, x1, y1, z1).color(180, 180, 180, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
        buffer.vertex(matrix, x1, y1, z2).color(180, 180, 180, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();
        buffer.vertex(matrix, x1, y2, z2).color(180, 180, 180, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(-1, 0, 0).endVertex();

        // East face (positive X)
        buffer.vertex(matrix, x2, y2, z2).color(180, 180, 180, 255).uv(u0, v0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        buffer.vertex(matrix, x2, y1, z2).color(180, 180, 180, 255).uv(u0, v1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        buffer.vertex(matrix, x2, y1, z1).color(180, 180, 180, 255).uv(u1, v1).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
        buffer.vertex(matrix, x2, y2, z1).color(180, 180, 180, 255).uv(u1, v0).overlayCoords(overlay).uv2(light).normal(1, 0, 0).endVertex();
    }

    private void renderDisplayItem(PoseStack poseStack, MultiBufferSource bufferSource, ItemStack item,
                                   Direction facing, int light, int overlay) {
        poseStack.pushPose();

        // Position on desk surface
        poseStack.translate(0.5, 1.1, 0.5);

        // Rotate to face player based on desk facing
        float rotation = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Tilt slightly
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5f));

        poseStack.scale(0.5f, 0.5f, 0.5f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                item, ItemDisplayContext.FIXED, light, overlay, poseStack, bufferSource, null, 0);

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
