package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Renderer for the Star Fissure block entity.
 * Creates a starfield effect similar to the End portal but with a different texture.
 */
public class StarFissureRenderer implements BlockEntityRenderer<StarFissureBlockEntity> {

    // Use end portal sky texture for the starfield effect
    private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL_LOCATION = new ResourceLocation("textures/entity/end_portal.png");

    public StarFissureRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull StarFissureBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        poseStack.pushPose();

        // The star fissure is a flat plane
        Matrix4f matrix = poseStack.last().pose();

        // Render using the end portal render type for the cool effect
        renderStarfield(matrix, bufferSource.getBuffer(RenderType.endPortal()));

        poseStack.popPose();
    }

    /**
     * Renders the starfield effect as a flat plane.
     */
    private void renderStarfield(Matrix4f matrix, VertexConsumer buffer) {
        float y = 0.05f; // Slightly above ground
        float minX = 0.0f;
        float maxX = 1.0f;
        float minZ = 0.0f;
        float maxZ = 1.0f;

        // Render a quad with the portal effect
        // End portal render type only needs position
        buffer.vertex(matrix, minX, y, minZ).endVertex();
        buffer.vertex(matrix, maxX, y, minZ).endVertex();
        buffer.vertex(matrix, maxX, y, maxZ).endVertex();
        buffer.vertex(matrix, minX, y, maxZ).endVertex();
    }

    @Override
    public int getViewDistance() {
        return 256; // Visible from far away
    }
}
