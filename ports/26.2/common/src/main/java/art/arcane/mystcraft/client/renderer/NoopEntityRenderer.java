package art.arcane.mystcraft.client.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * A no-op entity renderer that doesn't render anything. Used for entities like
 * DummyEntity that should never be visible.
 */
public class NoopEntityRenderer<T extends Entity> extends EntityRenderer<T, EntityRenderState> {

  public NoopEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.shadowRadius = 0.0F;
  }

  @Override
  public EntityRenderState createRenderState() {
    return new EntityRenderState();
  }
}
