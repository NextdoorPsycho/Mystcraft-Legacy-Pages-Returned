package art.arcane.mystcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;

/**
 * Short-lived chunk leases used around linking so immediate returns do not
 * force the server to unload and reload the same origin/destination chunks.
 */
public final class MystcraftChunkLeases {

  public static final int RETURN_LEASE_TICKS = 20 * 60;

  private static final TicketType<ChunkPos> RETURN_LINK =
      TicketType.create("mystcraft_return_link", Comparator.comparingLong(ChunkPos::toLong), RETURN_LEASE_TICKS);

  private MystcraftChunkLeases() {
  }

  public static void leaseReturnWindow(ServerLevel level, BlockPos pos) {
    leaseReturnWindow(level, new ChunkPos(pos));
  }

  public static void leaseReturnWindow(ServerLevel level, ChunkPos pos) {
    level.getChunkSource().addRegionTicket(RETURN_LINK, pos, 1, pos);
  }
}
