package art.arcane.mystcraft.platform.services;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Consumer;

// Provides basic platform information
public interface IPlatformHelper {

  /**
   * Returns the name of the current platform (e.g. "Forge", "Fabric", "NeoForge").
   */
  String getPlatformName();

  /**
   * Returns true if the named mod is currently loaded.
   */
  boolean isModLoaded(String modId);

  /**
   * Returns true if the current environment is a development (non-production) environment.
   */
  boolean isDevelopmentEnvironment();

  /**
   * Opens a menu with optional extra data for client-side menu construction.
   */
  void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter);

  /**
   * Creates the Black Ink Source fluid.
   */
  FlowingFluid createBlackInkSource();

  /**
   * Creates the Black Ink Flowing fluid.
   */
  FlowingFluid createBlackInkFlowing();

  /**
   * Returns the short grass block (Blocks.GRASS in 1.20.1-1.20.2, Blocks.SHORT_GRASS in 1.20.4+).
   */
  Block getShortGrassBlock();
}
