package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Packet sent from server to client to sync profiling/debug state.
 * Used to enable or disable client-side debugging features.
 */
public record ProfilingStatePacket(boolean profilingEnabled, boolean debugOverlayEnabled, boolean verboseLogging) {

  public static void encode(ProfilingStatePacket packet, FriendlyByteBuf buf) {
    buf.writeBoolean(packet.profilingEnabled);
    buf.writeBoolean(packet.debugOverlayEnabled);
    buf.writeBoolean(packet.verboseLogging);
  }

  public static ProfilingStatePacket decode(FriendlyByteBuf buf) {
    return new ProfilingStatePacket(
        buf.readBoolean(),
        buf.readBoolean(),
        buf.readBoolean()
    );
  }

  public static void handle(ProfilingStatePacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      if (ClientAccess.getClientLevel() == null) return;

      ProfilingState.setProfilingEnabled(packet.profilingEnabled);
      ProfilingState.setDebugOverlayEnabled(packet.debugOverlayEnabled);
      ProfilingState.setVerboseLogging(packet.verboseLogging);

      Mystcraft.LOGGER.info("Profiling state updated: profiling={}, debugOverlay={}, verbose={}",
          packet.profilingEnabled, packet.debugOverlayEnabled, packet.verboseLogging);
    });
  }

  /**
   * Client-side profiling state.
   */
  public static class ProfilingState {
    private static boolean profilingEnabled = false;
    private static boolean debugOverlayEnabled = false;
    private static boolean verboseLogging = false;

    // Profiling metrics
    private static long lastTickTime = 0;
    private static long averageTickTime = 0;
    private static int tickCount = 0;

    public static boolean isProfilingEnabled() {
      return profilingEnabled;
    }

    public static void setProfilingEnabled(boolean enabled) {
      profilingEnabled = enabled;
      if (!enabled) {
        resetMetrics();
      }
    }

    public static boolean isDebugOverlayEnabled() {
      return debugOverlayEnabled;
    }

    public static void setDebugOverlayEnabled(boolean enabled) {
      debugOverlayEnabled = enabled;
    }

    public static boolean isVerboseLogging() {
      return verboseLogging;
    }

    public static void setVerboseLogging(boolean enabled) {
      verboseLogging = enabled;
    }

    public static void recordTickTime(long nanos) {
      if (!profilingEnabled) return;

      lastTickTime = nanos;
      tickCount++;

      // Rolling average
      if (tickCount == 1) {
        averageTickTime = nanos;
      } else {
        averageTickTime = (averageTickTime * (tickCount - 1) + nanos) / tickCount;
      }

      // Reset periodically to avoid overflow
      if (tickCount > 1000) {
        tickCount = 100;
      }
    }

    public static long getLastTickTime() {
      return lastTickTime;
    }

    public static long getAverageTickTime() {
      return averageTickTime;
    }

    public static double getLastTickMs() {
      return lastTickTime / 1_000_000.0;
    }

    public static double getAverageTickMs() {
      return averageTickTime / 1_000_000.0;
    }

    public static void resetMetrics() {
      lastTickTime = 0;
      averageTickTime = 0;
      tickCount = 0;
    }

    public static void reset() {
      profilingEnabled = false;
      debugOverlayEnabled = false;
      verboseLogging = false;
      resetMetrics();
    }
  }
}
