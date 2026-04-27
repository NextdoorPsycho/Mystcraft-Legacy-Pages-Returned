package art.arcane.mystcraft.mixin;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.util.datafix.schemas.V3328;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(V3328.class)
public abstract class DataFixerSchemaMixin {

  @Inject(method = "registerEntities", at = @At("RETURN"), cancellable = true)
  private void mystcraft$registerEntities(
      Schema schema,
      CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir
  ) {
    Map<String, Supplier<TypeTemplate>> entities = cir.getReturnValue();
    schema.registerSimple(entities, "mystcraft:linkbook");
    schema.registerSimple(entities, "mystcraft:falling_block");
    schema.registerSimple(entities, "mystcraft:meteor");
    schema.registerSimple(entities, "mystcraft:colored_lightning");
    schema.registerSimple(entities, "mystcraft:personal_pocket_proxy");
    cir.setReturnValue(entities);
  }
}
