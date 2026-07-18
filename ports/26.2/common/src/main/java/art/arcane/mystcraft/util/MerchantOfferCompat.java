package art.arcane.mystcraft.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Creates merchant offers using the 26.2 item-cost API.
 */
public final class MerchantOfferCompat {

  private MerchantOfferCompat() {
  }

  public static MerchantOffer create(ItemStack cost, ItemStack result, int maxUses, int xp, float priceMultiplier) {
    ItemCost itemCost = new ItemCost(cost.getItem(), cost.getCount());
    return new MerchantOffer(itemCost, result, maxUses, xp, priceMultiplier);
  }
}
