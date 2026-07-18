package art.arcane.mystcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Short-lived chunk leases used around linking so immediate returns do not
 * force the server to unload and reload the same origin/destination chunks.
 */
public final class MystcraftChunkLeases {

  public static final int RETURN_LEASE_TICKS = 20 * 60;

  private static final TicketType RETURN_LINK =
      new TicketType(RETURN_LEASE_TICKS, TicketType.FLAG_LOADING);

  private MystcraftChunkLeases() {
  }

  public static void leaseReturnWindow(ServerLevel level, BlockPos pos) {
    leaseReturnWindow(level, ChunkPos.containing(pos));
  }

  public static void leaseReturnWindow(ServerLevel level, ChunkPos pos) {
    level.getChunkSource().addTicketWithRadius(RETURN_LINK, pos, 1);
  }
}
