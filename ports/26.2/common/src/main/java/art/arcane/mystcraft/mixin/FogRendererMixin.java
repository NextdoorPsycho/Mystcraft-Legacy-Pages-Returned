package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.client.AgeDimensionSpecialEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Age fog to the extracted 26.2 FogData object. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

  @Inject(method = "setupFog", at = @At("RETURN"))
  private void mystcraft$applyAgeFog(Camera camera, int renderDistanceInChunks,
                                     DeltaTracker deltaTracker, float darkenWorldAmount,
                                     ClientLevel level,
                                     CallbackInfoReturnable<FogData> callbackInfo) {
    AgeDimensionSpecialEffects.applyFog(callbackInfo.getReturnValue());
  }
}
