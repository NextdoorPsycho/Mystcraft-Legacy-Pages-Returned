package art.arcane.mystcraft.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * Centralized item property definitions for Mystcraft.
 * Factory methods return fresh Item.Properties instances since they are mutable.
 */
public final class ItemDefinitions {

  private ItemDefinitions() {
  }

  // Symbol pages
  public static Item.Properties page() {
    return new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON);
  }

  // Books
  public static Item.Properties agebook() {
    return new Item.Properties().stacksTo(1);
  }

  public static Item.Properties linkbook() {
    return new Item.Properties().stacksTo(1);
  }

  public static Item.Properties linkbookUnlinked() {
    return new Item.Properties().stacksTo(16);
  }

  public static Item.Properties personalLinkBook() {
    return new Item.Properties().stacksTo(1);
  }

  // Booster packs
  public static Item.Properties boosterPack() {
    return new Item.Properties().stacksTo(16);
  }

  // Storage items (Folder stacks controlled dynamically by FolderItem.getMaxStackSize())
  public static Item.Properties folder() {
    return new Item.Properties();
  }

  public static Item.Properties portfolio() {
    return new Item.Properties().stacksTo(1);
  }

  // Ink vial
  public static Item.Properties inkVial() {
    return new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON);
  }

  // Guidebook
  public static Item.Properties guidebook() {
    return new Item.Properties().stacksTo(1);
  }

  // Ink bucket
  public static Item.Properties inkBucket() {
    return new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET).rarity(Rarity.UNCOMMON);
  }

  // Block items (standard properties)
  public static Item.Properties blockItem() {
    return new Item.Properties();
  }
}
