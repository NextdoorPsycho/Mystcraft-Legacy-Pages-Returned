package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.NetworkHooks;

import java.util.function.Consumer;

public class NeoForgePlatformHelper implements IPlatformHelper {

  @Override
  public String getPlatformName() {
    return "NeoForge";
  }

  @Override
  public boolean isModLoaded(String modId) {
    return ModList.get().isLoaded(modId);
  }

  @Override
  public boolean isDevelopmentEnvironment() {
    return !FMLLoader.isProduction();
  }

  @Override
  public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
    if (extraDataWriter == null) {
      player.openMenu(provider);
      return;
    }
    NetworkHooks.openScreen(player, provider, extraDataWriter);
  }
}
