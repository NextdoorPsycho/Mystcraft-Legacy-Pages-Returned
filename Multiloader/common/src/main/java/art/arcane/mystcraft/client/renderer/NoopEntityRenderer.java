package art.arcane.mystcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * A no-op entity renderer that doesn't render anything.
 * Used for entities like DummyEntity that should never be visible.
 */
public class NoopEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    public NoopEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // No-op - render nothing
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
