package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IComponentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Forge 1.19.2 implementation of IComponentHelper.
 * Uses Component.literal() and Component.translatable() static factory methods.
 * In 1.19+, TextComponent/TranslatableComponent classes were removed.
 */
public class ForgeComponentHelper_1_19_2 implements IComponentHelper {

  @Override
  public MutableComponent literal(String text) {
    return Component.literal(text);
  }

  @Override
  public MutableComponent empty() {
    return Component.empty();
  }

  @Override
  public MutableComponent translatable(String key) {
    return Component.translatable(key);
  }

  @Override
  public MutableComponent translatable(String key, Object... args) {
    return Component.translatable(key, args);
  }
}
