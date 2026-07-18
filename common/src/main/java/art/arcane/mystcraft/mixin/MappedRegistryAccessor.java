package art.arcane.mystcraft.mixin;

import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Mapping-aware access for the short registration window used to add a
 * dynamic Age's level stem.
 */
@Mixin(MappedRegistry.class)
public interface MappedRegistryAccessor {

  @Accessor("frozen")
  boolean mystcraft$isFrozen();

  @Accessor("frozen")
  void mystcraft$setFrozen(boolean frozen);
}
