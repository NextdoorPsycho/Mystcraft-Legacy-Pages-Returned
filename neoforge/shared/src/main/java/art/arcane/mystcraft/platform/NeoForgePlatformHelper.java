package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.util.function.Consumer;

public class NeoForgePlatformHelper implements IPlatformHelper {

  private FlowingFluid blackInkSource;
  private FlowingFluid blackInkFlowing;

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
    // In NeoForge 1.20.4+, NetworkHooks was removed - use player.openMenu directly
    player.openMenu(provider, extraDataWriter::accept);
  }

  @Override
  public Block getShortGrassBlock() {
    // In 1.20.4+, Blocks.GRASS was renamed to Blocks.SHORT_GRASS
    return Blocks.SHORT_GRASS;
  }

  /**
   * Sets the black ink source fluid. Called by NeoForgeRegistrationHelper after fluid registration.
   */
  public void setBlackInkSource(FlowingFluid fluid) {
    this.blackInkSource = fluid;
  }

  /**
   * Sets the black ink flowing fluid. Called by NeoForgeRegistrationHelper after fluid registration.
   */
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
