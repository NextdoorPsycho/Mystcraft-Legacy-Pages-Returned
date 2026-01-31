package art.arcane.mystcraft.network;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.util.PocketHeadUtils;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.PersonalPocketData;
import art.arcane.mystcraft.world.PersonalPocketDimension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Client -> Server packet that syncs a player's head-based wall palette.
 * Used in dev/offline environments where the server cannot resolve skins.
 */
public class PocketHeadSyncPacket {

  private final EnumMap<AgeData.PocketHeadFace, List<String>> headBlocks;

  public PocketHeadSyncPacket(Map<AgeData.PocketHeadFace, List<String>> headBlocks) {
    this.headBlocks = new EnumMap<>(AgeData.PocketHeadFace.class);
    if (headBlocks != null) {
      for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
        List<String> blocks = headBlocks.get(face);
        if (blocks != null) {
          this.headBlocks.put(face, new ArrayList<>(blocks));
        }
      }
    }
  }

  public static void encode(PocketHeadSyncPacket packet, FriendlyByteBuf buf) {
    for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
      List<String> blocks = packet.headBlocks.get(face);
      if (blocks == null || blocks.size() != 64) {
        buf.writeBoolean(false);
        continue;
      }
      buf.writeBoolean(true);
      for (String id : blocks) {
        buf.writeUtf(id);
      }
    }
  }

  public static PocketHeadSyncPacket decode(FriendlyByteBuf buf) {
    EnumMap<AgeData.PocketHeadFace, List<String>> map = new EnumMap<>(AgeData.PocketHeadFace.class);
    for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
      boolean has = buf.readBoolean();
      if (!has) {
        continue;
      }
      List<String> blocks = new ArrayList<>(64);
      for (int i = 0; i < 64; i++) {
        blocks.add(buf.readUtf());
      }
      map.put(face, blocks);
    }
    return new PocketHeadSyncPacket(map);
  }

  public static void handle(PocketHeadSyncPacket packet, PacketContext ctx) {
    ctx.enqueueWork(() -> {
      ServerPlayer player = ctx.getServerPlayer();
      if (player == null) {
        return;
      }
      if (!PocketHeadUtils.isValidHeadBlockMap(packet.headBlocks)) {
        Mystcraft.LOGGER.warn("[PocketHead] Received invalid head palette from {}", player.getGameProfile().getName());
        return;
      }

      MinecraftServer server = player.getServer();
      if (server == null) {
        return;
      }

      // Cache for future pocket creation.
      PersonalPocketData.get(server).setHeadBlocks(player.getUUID(), packet.headBlocks);

      // If the pocket is already loaded, apply immediately.
      int uid = PersonalPocketDimension.getPersonalAgeUid(player.getUUID());
      ServerLevel level = AgeManager.get(server).getAgeLevel(server, uid);
      if (level == null) {
        return;
      }
      AgeData ageData = AgeData.get(level);
      for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
        List<String> blocks = packet.headBlocks.get(face);
        if (blocks != null) {
          ageData.setPocketHeadBlocks(face, blocks);
        }
      }
      net.minecraft.world.level.chunk.ChunkGenerator generator = level.getChunkSource().getGenerator();
      if (generator instanceof art.arcane.mystcraft.world.gen.AgeChunkGenerator ageGen) {
        ageGen.refreshPocketHeadBlocks(level);
      }
      Mystcraft.LOGGER.info("[PocketHead] Applied client head palette for {}", player.getGameProfile().getName());
    });
  }
}
