package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.mixin.MinecraftServerAccessor;
import art.arcane.mystcraft.fabric.FabricRegistries;
import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.util.Map;
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
  public boolean isClientEnvironment() {
    return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
  }

  @Override
  public Map<ResourceKey<Level>, ServerLevel> getLevelMap(MinecraftServer server) {
    return ((MinecraftServerAccessor) server).mystcraft$getLevels();
  }

  @Override
  public LevelStorageSource.LevelStorageAccess getLevelStorage(MinecraftServer server) {
    return ((MinecraftServerAccessor) server).mystcraft$getStorageSource();
  }

  @Override
  public void markWorldsDirty(MinecraftServer server) {
    // Fabric reads the mutable level map directly. Forge maintains an
    // additional cached view that needs an explicit dirty notification.
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
    return FabricRegistries.BLACK_INK_SOURCE.get();
  }

  @Override
  public FlowingFluid createBlackInkFlowing() {
    return FabricRegistries.BLACK_INK_FLOWING.get();
  }

  @Override
  public Block getShortGrassBlock() {
    return Blocks.GRASS;
  }
}
