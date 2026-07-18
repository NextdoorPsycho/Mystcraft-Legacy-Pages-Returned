package art.arcane.mystcraft.fabric.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mapping-safe access for Mystcraft books, which are not vanilla book components. */
@Mixin(LecternBlockEntity.class)
public interface LecternBlockEntityAccessor {

  @Accessor("book")
  void mystcraft$setBook(ItemStack book);

  @Accessor("pageCount")
  void mystcraft$setPageCount(int pageCount);
}
