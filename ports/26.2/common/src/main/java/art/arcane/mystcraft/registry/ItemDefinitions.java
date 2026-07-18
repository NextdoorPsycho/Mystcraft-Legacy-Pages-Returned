package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * Centralized item property definitions for Mystcraft. Factory methods return
 * fresh Item.Properties instances since they are mutable.
 */
public final class ItemDefinitions {

  private ItemDefinitions() {
  }

  public static Item.Properties page() {
    return properties("page").stacksTo(64).rarity(Rarity.UNCOMMON);
  }

  public static Item.Properties agebook() {
    return properties("agebook").stacksTo(1).rarity(Rarity.EPIC);
  }

  public static Item.Properties linkbook() {
    return properties("linkbook").stacksTo(1).rarity(Rarity.RARE);
  }

  public static Item.Properties linkbookUnlinked() {
    return properties("linkbook_unlinked").stacksTo(16);
  }

  public static Item.Properties personalLinkBook() {
    return properties("personal_link_book").stacksTo(1).rarity(Rarity.RARE);
  }

  public static Item.Properties boosterPack() {
    return properties("booster").stacksTo(16);
  }

  public static Item.Properties folder() {
    return properties("folder");
  }

  public static Item.Properties portfolio() {
    return properties("portfolio").stacksTo(1);
  }

  public static Item.Properties inkVial() {
    return properties("inkvial").stacksTo(16).rarity(Rarity.UNCOMMON);
  }

  public static Item.Properties guidebook() {
    return properties("guidebook").stacksTo(1);
  }

  public static Item.Properties inkBucket() {
    return properties("ink_bucket").stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.UNCOMMON);
  }

  public static Item.Properties blockItem(String path) {
    return properties(path).useBlockDescriptionPrefix();
  }

  private static Item.Properties properties(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
  }
}
