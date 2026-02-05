package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class FabricNetworkHelper implements INetworkHelper {
  @Override
  public void register() {
  }

  @Override
  public void sendToServer(Object packet) {
    throw new UnsupportedOperationException("Fabric networking not implemented");
  }

  @Override
  public void sendToPlayer(ServerPlayer player, Object packet) {
    throw new UnsupportedOperationException("Fabric networking not implemented");
  }

  @Override
  public void sendToAllTracking(Entity entity, Object packet) {
    throw new UnsupportedOperationException("Fabric networking not implemented");
  }

  @Override
  public void sendToAll(Object packet) {
    throw new UnsupportedOperationException("Fabric networking not implemented");
  }
}
