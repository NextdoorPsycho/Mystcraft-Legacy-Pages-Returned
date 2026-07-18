package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Tag keys consumed by common logic in the 26.2 build.
 */
public final class ModTags {

  private ModTags() {
  }

  public static final class Items {
    public static final TagKey<Item> INK_BUCKETS = create("ink_buckets");
    public static final TagKey<Item> INK_SACS = create("ink_sacs");

    private Items() {
    }

    private static TagKey<Item> create(String path) {
      return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path));
    }
  }
}
