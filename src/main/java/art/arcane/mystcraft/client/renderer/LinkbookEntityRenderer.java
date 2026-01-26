package art.arcane.mystcraft.client.renderer;

import art.arcane.mystcraft.entity.LinkbookEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for the Linkbook entity (dropped book in world).
 * Renders the book item with a slight spin animation.
 */
public class LinkbookEntityRenderer extends EntityRenderer<LinkbookEntity> {

    private final ItemRenderer itemRenderer;

    public LinkbookEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(@NotNull LinkbookEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        ItemStack book = entity.getBookItem();
        if (book.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Bob up and down
        float bobOffset = (float) Math.sin((entity.tickCount + partialTick) * 0.1) * 0.05f;
        poseStack.translate(0, 0.25 + bobOffset, 0);

        // Slow spin
        float spin = (entity.tickCount + partialTick) * 2.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));

        // Tilt slightly
        poseStack.mulPose(Axis.XP.rotationDegrees(-15f));

        // Scale
        poseStack.scale(0.5f, 0.5f, 0.5f);

        // Render the book
        itemRenderer.renderStatic(book, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId());

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull LinkbookEntity entity) {
        // Not used since we render an item
        return new ResourceLocation("textures/misc/unknown_pack.png");
    }
}
