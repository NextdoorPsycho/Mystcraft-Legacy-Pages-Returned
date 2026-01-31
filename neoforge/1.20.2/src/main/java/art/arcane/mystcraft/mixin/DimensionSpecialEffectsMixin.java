package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.AgeDimensionSpecialEffects;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects AgeDimensionSpecialEffects into the dimension effects map (NeoForge).
 * All Mystcraft Ages use "effects": "mystcraft:age" in their dimension type JSON.
 */
@Mixin(DimensionSpecialEffects.class)
public class DimensionSpecialEffectsMixin {

  @Shadow
  @Final
  private static Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> EFFECTS;

  @Inject(method = "<clinit>", at = @At("TAIL"))
  private static void mystcraft$registerAgeEffects(CallbackInfo ci) {
    ResourceLocation ageKey = new ResourceLocation(Mystcraft.MOD_ID, "age");
    EFFECTS.put(ageKey, new AgeDimensionSpecialEffects());
    Mystcraft.LOGGER.info("[Mystcraft] Registered DimensionSpecialEffects under key '{}'", ageKey);
  }
}
