package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

/**
 * Forge 1.19.2-specific platform helper.
 * Uses NetworkHooks.openScreen() instead of IForgeServerPlayer.openMenu().
 */
public class ForgePlatformHelper_1_19_2 implements IPlatformHelper {

  private FlowingFluid blackInkSource;
  private FlowingFluid blackInkFlowing;

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
    if (extraDataWriter == null) {
      NetworkHooks.openScreen(player, provider);
    } else {
      NetworkHooks.openScreen(player, provider, extraDataWriter);
    }
  }

  public void setBlackInkSource(FlowingFluid fluid) {
    this.blackInkSource = fluid;
  }

  public void setBlackInkFlowing(FlowingFluid fluid) {
    this.blackInkFlowing = fluid;
  }

  @Override
  public FlowingFluid createBlackInkSource() {
    if (blackInkSource == null) {
      throw new IllegalStateException("Black ink source fluid not initialized. Call setBlackInkSource first.");
    }
    return blackInkSource;
  }

  @Override
  public FlowingFluid createBlackInkFlowing() {
    if (blackInkFlowing == null) {
      throw new IllegalStateException("Black ink flowing fluid not initialized. Call setBlackInkFlowing first.");
    }
    return blackInkFlowing;
  }
}
