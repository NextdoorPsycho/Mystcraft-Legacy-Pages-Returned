package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 1.18.2 port: Uses Registry.ITEM_REGISTRY instead of Registries.ITEM for tag creation.
 */
public final class ModTags {

  private ModTags() {
  }

  public static final class Items {
    public static final TagKey<Item> INK_BUCKETS = create("ink_buckets");
    public static final TagKey<Item> INK_SACS = create("ink_sacs");

    private static TagKey<Item> create(String path) {
      // 1.18.2: Use Registry.ITEM_REGISTRY instead of Registries.ITEM
      return TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, path));
    }

    private Items() {
    }
  }
}
