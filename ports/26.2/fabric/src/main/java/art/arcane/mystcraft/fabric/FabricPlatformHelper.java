package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;

/** Fabric implementation of the loader-neutral platform boundary. */
public final class FabricPlatformHelper implements IPlatformHelper {

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
  public boolean isClientEnvironment() {
    return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
  }

  @Override
  public void openMenu(
      ServerPlayer player,
      MenuProvider provider,
      Consumer<FriendlyByteBuf> extraDataWriter
  ) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(provider, "provider");
    if (extraDataWriter == null) {
      player.openMenu(provider);
      return;
    }

    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    byte[] openingData;
    try {
      extraDataWriter.accept(buffer);
      openingData = ByteBufUtil.getBytes(buffer, buffer.readerIndex(), buffer.readableBytes());
    } finally {
      buffer.release();
    }

    player.openMenu(new ExtendedMenuProvider<byte[]>() {
      @Override
      public byte[] getScreenOpeningData(ServerPlayer openingPlayer) {
        return openingData;
      }

      @Override
      public net.minecraft.network.chat.Component getDisplayName() {
        return provider.getDisplayName();
      }

      @Override
      public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
          int containerId,
          net.minecraft.world.entity.player.Inventory inventory,
          net.minecraft.world.entity.player.Player menuPlayer
      ) {
        return provider.createMenu(containerId, inventory, menuPlayer);
      }
    });
  }

  @Override
  public FlowingFluid createBlackInkSource() {
    return FabricRegistries.BLACK_INK_SOURCE.get();
  }

  @Override
  public FlowingFluid createBlackInkFlowing() {
    return FabricRegistries.BLACK_INK_FLOWING.get();
  }

  @Override
  public Block getShortGrassBlock() {
    return Blocks.SHORT_GRASS;
  }
}
