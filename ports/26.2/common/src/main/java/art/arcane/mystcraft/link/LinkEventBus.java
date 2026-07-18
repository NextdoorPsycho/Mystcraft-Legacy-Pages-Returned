package art.arcane.mystcraft.link;

import art.arcane.mystcraft.Mystcraft;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loader-neutral dispatcher for Mystcraft link lifecycle events.
 *
 * <p>Listeners run synchronously on the server thread that performs the link.
 * A broken listener is logged and isolated so it cannot strand the linking
 * transaction halfway through dispatch.</p>
 */
public final class LinkEventBus {

  private static final CopyOnWriteArrayList<Listener> LISTENERS =
      new CopyOnWriteArrayList<>();

  private LinkEventBus() {
  }

  /**
   * Registers a listener and returns a handle that removes it when closed.
   */
  @NotNull
  public static Registration register(@NotNull Listener listener) {
    Listener checkedListener = Objects.requireNonNull(listener, "listener");
    LISTENERS.add(checkedListener);
    return () -> LISTENERS.remove(checkedListener);
  }

  /**
   * Removes a previously registered listener.
   */
  public static boolean unregister(@NotNull Listener listener) {
    return LISTENERS.remove(Objects.requireNonNull(listener, "listener"));
  }

  static void post(@NotNull LinkEvent event) {
    for (Listener listener : LISTENERS) {
      try {
        listener.onLinkEvent(event);
      } catch (RuntimeException e) {
        Mystcraft.LOGGER.error(
            "[LinkEventBus] Listener {} failed while handling {}",
            listener.getClass().getName(),
            event.getClass().getSimpleName(),
            e
        );
      }
    }
  }

  @FunctionalInterface
  public interface Listener {
    void onLinkEvent(@NotNull LinkEvent event);
  }

  @FunctionalInterface
  public interface Registration extends AutoCloseable {
    @Override
    void close();
  }
}
