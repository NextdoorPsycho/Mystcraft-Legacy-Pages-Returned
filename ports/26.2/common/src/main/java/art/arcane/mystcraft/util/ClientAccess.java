package art.arcane.mystcraft.util;

import java.util.Objects;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Server-safe boundary for the tiny slice of client state used by common
 * packet handlers.
 *
 * <p>Loader client initializers install a direct method reference. Dedicated
 * servers never load a Minecraft client class and simply observe a
 * {@code null} level.</p>
 */
public final class ClientAccess {

  private static final ClientBridge UNAVAILABLE = () -> null;
  private static volatile ClientBridge bridge = UNAVAILABLE;

  private ClientAccess() {
  }

  /** Installs the loader's direct client implementation during client setup. */
  public static void install(ClientBridge clientBridge) {
    bridge = Objects.requireNonNull(clientBridge, "clientBridge");
  }

  /** Restores the server-safe unavailable implementation, primarily for tests. */
  public static void clear() {
    bridge = UNAVAILABLE;
  }

  @Nullable
  public static Level getClientLevel() {
    return bridge.level();
  }

  @FunctionalInterface
  public interface ClientBridge {
    @Nullable Level level();
  }
}
