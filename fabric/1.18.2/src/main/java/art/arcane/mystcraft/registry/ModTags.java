package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Mystcraft item tags - 1.18.2 version.
 * Uses Registry.ITEM instead of Registries.ITEM.
 */
public final class ModTags {

  private ModTags() {
  }

  public static final class Items {
    public static final TagKey<Item> INK_BUCKETS = create("ink_buckets");
    public static final TagKey<Item> INK_SACS = create("ink_sacs");

    private static TagKey<Item> create(String path) {
      return TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Mystcraft.MOD_ID, path));
    }

    private Items() {
    }
  }
}
