package art.arcane.mystcraft.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Server-safe boundary for item actions that must open client-only screens.
 * Client loader entrypoints install the actions without making common item
 * classes link directly against Minecraft client classes.
 */
public final class ItemClientHooks {

  private static Consumer<ItemStack> bookOpener = stack -> {
  };
  private static Runnable guidebookOpener = () -> {
  };

  private ItemClientHooks() {
  }

  public static void setBookOpener(@NotNull Consumer<ItemStack> opener) {
    bookOpener = Objects.requireNonNull(opener, "opener");
  }

  public static void setGuidebookOpener(@NotNull Runnable opener) {
    guidebookOpener = Objects.requireNonNull(opener, "opener");
  }

  static void openBook(@NotNull ItemStack stack) {
    bookOpener.accept(stack);
  }

  static void openGuidebook() {
    guidebookOpener.run();
  }
}
