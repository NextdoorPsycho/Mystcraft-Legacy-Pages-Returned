package art.arcane.mystcraft.network;

import art.arcane.mystcraft.util.ClientAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Packet sent from server to client to display link effects. Triggers particle
 * effects at the specified position.
 */
public record LinkEffectPacket(BlockPos pos, LinkEffectType effectType) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<LinkEffectPacket> TYPE =
      MystcraftNetwork.type("link_effect");
  public static final StreamCodec<RegistryFriendlyByteBuf, LinkEffectPacket> STREAM_CODEC =
      CustomPacketPayload.codec(LinkEffectPacket::encode, LinkEffectPacket::decode);

  public static void encode(LinkEffectPacket packet, FriendlyByteBuf buf) {
    buf.writeBlockPos(packet.pos);
    buf.writeEnum(packet.effectType);
  }

  public static LinkEffectPacket decode(FriendlyByteBuf buf) {
    return new LinkEffectPacket(buf.readBlockPos(), buf.readEnum(LinkEffectType.class));
  }

  @Override
  public CustomPacketPayload.Type<LinkEffectPacket> type() {
    return TYPE;
  }

  public static void handle(LinkEffectPacket packet, PacketContext ctx) {
    if (!ctx.isClientSide()) {
      return;
    }
    ctx.enqueueWork(() -> {
      Level level = ClientAccess.getClientLevel();
      if (level == null) return;

      spawnLinkParticles(level, packet.pos, packet.effectType);
    });
  }

  private static void spawnLinkParticles(Level level, BlockPos pos, LinkEffectType type) {
    RandomSource random = level.getRandom();
    double x = pos.getX() + 0.5;
    double y = pos.getY() + 0.5;
    double z = pos.getZ() + 0.5;

    switch (type) {
      case DEPARTURE -> {

        for (int i = 0; i < 20; i++) {
          double angle = (i / 20.0) * Math.PI * 4;
          double radius = 0.5 - (i / 40.0);
          double px = x + Math.cos(angle) * radius;
          double pz = z + Math.sin(angle) * radius;
          double py = y + (i / 20.0) * 2;
          level.addParticle(ParticleTypes.PORTAL,
              px, py, pz,
              0, 0.1, 0);
        }
      }
      case ARRIVAL -> {

        for (int i = 0; i < 30; i++) {
          double dx = (random.nextDouble() - 0.5) * 2;
          double dy = (random.nextDouble() - 0.5) * 2;
          double dz = (random.nextDouble() - 0.5) * 2;
          level.addParticle(ParticleTypes.REVERSE_PORTAL,
              x, y, z,
              dx * 0.1, dy * 0.1, dz * 0.1);
        }
      }
      case PORTAL_ACTIVE -> {

        for (int i = 0; i < 5; i++) {
          double px = x + (random.nextDouble() - 0.5) * 0.5;
          double py = y + (random.nextDouble() - 0.5) * 0.5;
          double pz = z + (random.nextDouble() - 0.5) * 0.5;
          level.addParticle(ParticleTypes.PORTAL,
              px, py, pz,
              (random.nextDouble() - 0.5) * 0.1,
              random.nextDouble() * 0.1,
              (random.nextDouble() - 0.5) * 0.1);
        }
      }
      case FOLLOWING -> {

        for (int i = 0; i < 16; i++) {
          double angle = (i / 16.0) * Math.PI * 2;
          double px = x + Math.cos(angle) * 1.5;
          double pz = z + Math.sin(angle) * 1.5;
          level.addParticle(ParticleTypes.ENCHANT,
              px, y, pz,
              0, 0.2, 0);
        }
      }
    }
  }

  public enum LinkEffectType {
    DEPARTURE,
    ARRIVAL,
    PORTAL_ACTIVE,
    FOLLOWING
  }
}
