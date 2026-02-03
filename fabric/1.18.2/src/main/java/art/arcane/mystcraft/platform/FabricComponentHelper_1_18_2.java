package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IComponentHelper;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Fabric 1.18.2 implementation of IComponentHelper.
 * Uses TextComponent and TranslatableComponent constructors.
 * In 1.18.2, Component.literal/translatable don't exist yet.
 */
public class FabricComponentHelper_1_18_2 implements IComponentHelper {

  @Override
  public MutableComponent literal(String text) {
    return new TextComponent(text);
  }

  @Override
  public MutableComponent empty() {
    return new TextComponent("");
  }

  @Override
  public MutableComponent translatable(String key) {
    return new TranslatableComponent(key);
  }

  @Override
  public MutableComponent translatable(String key, Object... args) {
    return new TranslatableComponent(key, args);
  }
}
