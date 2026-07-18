package art.arcane.mystcraft.util;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Server-safe boundary for the small slice of client state used by common
 * packet handlers. Loader client initializers install a direct bridge;
 * dedicated servers keep the unavailable implementation and never load a
 * Minecraft client class.
 */
public final class ClientAccess {

  private static final ClientBridge UNAVAILABLE = () -> null;
  private static volatile ClientBridge bridge = UNAVAILABLE;

  private ClientAccess() {
  }

  public static void install(ClientBridge clientBridge) {
    bridge = Objects.requireNonNull(clientBridge, "clientBridge");
  }

  public static void clear() {
    bridge = UNAVAILABLE;
  }

  @Nullable
  public static Level getClientLevel() {
    return bridge.level();
  }

  @FunctionalInterface
  public interface ClientBridge {
    @Nullable
    Level level();
  }
}
