package art.arcane.mystcraft.platform.services;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Platform service for loader-specific lifecycle hooks and registry objects that common code uses.
 */
public interface IPlatformHelper {

  /**
   * Returns the name of the current platform (e.g. "Forge" or "Fabric").
   */
  String getPlatformName();

  /**
   * Returns true if the named mod is currently loaded.
   */
  boolean isModLoaded(String modId);

  /**
   * Returns true if the current environment is a development (non-production)
   * environment.
   */
  boolean isDevelopmentEnvironment();

  /**
   * Returns true when the current process is allowed to load client-only
   * Minecraft classes.
   */
  boolean isClientEnvironment();

  /**
   * Returns the live server-level map used when registering a dynamic Age.
   */
  Map<ResourceKey<Level>, ServerLevel> getLevelMap(MinecraftServer server);

  /**
   * Returns the current world's storage handle for constructing a dynamic
   * {@link ServerLevel}.
   */
  LevelStorageSource.LevelStorageAccess getLevelStorage(MinecraftServer server);

  /**
   * Notifies the loader that the set of server levels changed.
   */
  void markWorldsDirty(MinecraftServer server);

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
   * Returns the short grass block for the supported Minecraft version.
   */
  Block getShortGrassBlock();
}
