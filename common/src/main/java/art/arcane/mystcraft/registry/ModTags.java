package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Tag keys consumed by common logic when loader resources are merged into the active 1.20.1 build.
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
      return TagKey.create(Registries.ITEM, new ResourceLocation(Mystcraft.MOD_ID, path));
    }
  }
}
