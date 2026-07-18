package art.arcane.mystcraft.fabric.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed access used to make village-pool injection idempotent across reloads. */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {

  @Accessor("template")
  Either<Identifier, StructureTemplate> mystcraft$getTemplate();
}
