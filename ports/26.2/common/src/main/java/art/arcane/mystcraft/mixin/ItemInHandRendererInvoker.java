package art.arcane.mystcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla map submission for the genuine-map fallback path. */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {

  @Invoker("renderMap")
  void mystcraft$invokeRenderMap(PoseStack poseStack,
                                 SubmitNodeCollector submitNodeCollector,
                                 int lightCoords, ItemStack stack);
}
