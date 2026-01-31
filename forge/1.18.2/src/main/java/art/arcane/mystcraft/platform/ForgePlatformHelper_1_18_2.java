package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

/**
 * Forge 1.18.2-specific platform helper.
 * Uses NetworkHooks.openScreen() instead of IForgeServerPlayer.openMenu().
 */
public class ForgePlatformHelper_1_18_2 implements IPlatformHelper {

  @Override
  public String getPlatformName() {
    return "Forge";
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
    // 1.18.2 uses openGui instead of openScreen
    if (extraDataWriter == null) {
      NetworkHooks.openGui(player, provider);
    } else {
      NetworkHooks.openGui(player, provider, extraDataWriter);
    }
  }
}
