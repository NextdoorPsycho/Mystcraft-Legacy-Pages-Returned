package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Consumer;

/**
 * Fabric service implementation for common platform lifecycle and registry hooks.
 */
public class FabricPlatformHelper implements IPlatformHelper {

  @Override
  public String getPlatformName() {
    return "Fabric";
  }

  @Override
  public boolean isModLoaded(String modId) {
    return FabricLoader.getInstance().isModLoaded(modId);
  }

  @Override
  public boolean isDevelopmentEnvironment() {
    return FabricLoader.getInstance().isDevelopmentEnvironment();
  }

  @Override
  public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
    if (extraDataWriter == null) {
      player.openMenu(provider);
      return;
    }

    if (provider instanceof ExtendedScreenHandlerFactory extended) {
      player.openMenu(extended);
      return;
    }

    player.openMenu(new ExtendedScreenHandlerFactory() {
      @Override
      public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
        extraDataWriter.accept(buf);
      }

      @Override
      public net.minecraft.network.chat.Component getDisplayName() {
        return provider.getDisplayName();
      }

      @Override
      public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
          int syncId,
          net.minecraft.world.entity.player.Inventory inventory,
          net.minecraft.world.entity.player.Player player) {
        return provider.createMenu(syncId, inventory, player);
      }
    });
  }

  @Override
  public FlowingFluid createBlackInkSource() {
    throw new UnsupportedOperationException("Fabric fluid implementation not yet available");
  }

  @Override
  public FlowingFluid createBlackInkFlowing() {
    throw new UnsupportedOperationException("Fabric fluid implementation not yet available");
  }

  @Override
  public Block getShortGrassBlock() {
    return Blocks.GRASS;
  }
}
