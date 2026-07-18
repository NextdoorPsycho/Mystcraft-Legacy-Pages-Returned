package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.client.AgeDimensionSpecialEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Injects Mystcraft Age values after vanilla extracts sky and cloud state. */
@Mixin(LevelExtractor.class)
public abstract class DimensionSpecialEffectsMixin {

  @Shadow
  @Final
  private LevelRenderState levelRenderState;

  @Shadow
  private @Nullable ClientLevel level;

  @Inject(method = "extract", at = @At("TAIL"))
  private void mystcraft$applyAgeVisualState(DeltaTracker deltaTracker, Camera camera,
                                             float partialTick, CallbackInfo callbackInfo) {
    if (this.level != null) {
      AgeDimensionSpecialEffects.applyLevelState(
          this.level, partialTick, this.levelRenderState);
    }
  }
}
