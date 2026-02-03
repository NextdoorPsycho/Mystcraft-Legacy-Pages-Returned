package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IComponentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * NeoForge 1.20.2 implementation of IComponentHelper.
 * Uses static factory methods on Component class.
 */
public class NeoForgeComponentHelper implements IComponentHelper {

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
