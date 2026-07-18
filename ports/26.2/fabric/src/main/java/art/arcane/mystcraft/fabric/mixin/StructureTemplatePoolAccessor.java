package art.arcane.mystcraft.fabric.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mapping-aware access used to append Archivist houses to village pools. */
@Mixin(StructureTemplatePool.class)
public interface StructureTemplatePoolAccessor {

  @Accessor("rawTemplates")
  List<Pair<StructurePoolElement, Integer>> mystcraft$getRawTemplates();

  @Mutable
  @Accessor("rawTemplates")
  void mystcraft$setRawTemplates(List<Pair<StructurePoolElement, Integer>> templates);

  @Accessor("templates")
  ObjectArrayList<StructurePoolElement> mystcraft$getTemplates();

  @Mutable
  @Accessor("templates")
  void mystcraft$setTemplates(ObjectArrayList<StructurePoolElement> templates);
}
