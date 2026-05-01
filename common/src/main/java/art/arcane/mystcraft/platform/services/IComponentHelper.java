package art.arcane.mystcraft.platform.services;

import net.minecraft.network.chat.MutableComponent;

/**
 * Abstracts Component creation across Minecraft versions.
 * <p>
 * 1.20.x: Uses {@code Component.literal("text")},
 * {@code Component.translatable("key")} 1.19.x: Uses
 * {@code new TextComponent("text")}, {@code new TranslatableComponent("key")}
 */
public interface IComponentHelper {

  /**
   * Creates a literal text component.
   * <p>
   * 1.20.x: {@code Component.literal(text)} 1.19.x:
   * {@code new TextComponent(text)}
   *
   * @param text The text content
   * @return A mutable text component
   */
  MutableComponent literal(String text);

  /**
   * Creates an empty text component.
   * <p>
   * 1.20.x: {@code Component.empty()} 1.19.x: {@code TextComponent.EMPTY}
   *
   * @return An empty mutable component
   */
  MutableComponent empty();

  /**
   * Creates a translatable component with the given key.
   * <p>
   * 1.20.x: {@code Component.translatable(key)} 1.19.x:
   * {@code new TranslatableComponent(key)}
   *
   * @param key The translation key
   * @return A mutable translatable component
   */
  MutableComponent translatable(String key);

  /**
   * Creates a translatable component with the given key and arguments.
   * <p>
   * 1.20.x: {@code Component.translatable(key, args)} 1.19.x:
   * {@code new TranslatableComponent(key, args)}
   *
   * @param key  The translation key
   * @param args The arguments to substitute
   * @return A mutable translatable component
   */
  MutableComponent translatable(String key, Object... args);
}
