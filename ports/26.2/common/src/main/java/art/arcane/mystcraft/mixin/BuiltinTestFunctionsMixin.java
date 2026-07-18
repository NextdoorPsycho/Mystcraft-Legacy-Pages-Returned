package art.arcane.mystcraft.mixin;

import art.arcane.mystcraft.gametest.MystcraftGameTests;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Registers Mystcraft's function-backed GameTests before vanilla runs loaders. */
@Mixin(BuiltinTestFunctions.class)
public abstract class BuiltinTestFunctionsMixin {

  @Inject(method = "bootstrap", at = @At("HEAD"))
  private static void mystcraft$registerGameTests(
      Registry<Consumer<GameTestHelper>> registry,
      CallbackInfoReturnable<Consumer<GameTestHelper>> callback
  ) {
    MystcraftGameTests.register();
  }
}
