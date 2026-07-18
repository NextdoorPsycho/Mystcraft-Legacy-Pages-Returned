package art.arcane.mystcraft.fabric.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed access to Minecraft's late-bound special-model codec registry. */
@Mixin(SpecialModelRenderers.class)
public interface SpecialModelRenderersAccessor {

  @Accessor("ID_MAPPER")
  static ExtraCodecs.LateBoundIdMapper<Identifier,
      MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> mystcraft$getIdMapper() {
    throw new AssertionError("Mixin accessor was not transformed");
  }
}
