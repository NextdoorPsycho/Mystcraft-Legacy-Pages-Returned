package art.arcane.mystcraft.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.lang.reflect.Constructor;

public final class MerchantOfferCompat {

  private static final Constructor<MerchantOffer> CTOR_STACK =
      findCtor(MerchantOffer.class, ItemStack.class, ItemStack.class, int.class, int.class, float.class);
  private static final Constructor<MerchantOffer> CTOR_ITEM_COST =
      findCtor(MerchantOffer.class,
          getClassIfPresent("net.minecraft.world.item.trading.ItemCost"),
          ItemStack.class,
          int.class,
          int.class,
          float.class
      );
  private static final Constructor<?> ITEM_COST_CTOR =
      findCtorRaw(getClassIfPresent("net.minecraft.world.item.trading.ItemCost"), ItemStack.class);

  private MerchantOfferCompat() {
  }

  public static MerchantOffer create(ItemStack cost, ItemStack result, int maxUses, int xp, float priceMultiplier) {
    try {
      if (CTOR_STACK != null) {
        return CTOR_STACK.newInstance(cost, result, maxUses, xp, priceMultiplier);
      }
      if (CTOR_ITEM_COST != null && ITEM_COST_CTOR != null) {
        Object itemCost = ITEM_COST_CTOR.newInstance(cost);
        return CTOR_ITEM_COST.newInstance(itemCost, result, maxUses, xp, priceMultiplier);
      }
    } catch (ReflectiveOperationException ignored) {
    }
    return null;
  }

  private static <T> Constructor<T> findCtor(Class<T> owner, Class<?>... params) {
    if (owner == null) {
      return null;
    }
    try {
      return owner.getConstructor(params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static Constructor<?> findCtorRaw(Class<?> owner, Class<?>... params) {
    if (owner == null) {
      return null;
    }
    try {
      return owner.getConstructor(params);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private static Class<?> getClassIfPresent(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
