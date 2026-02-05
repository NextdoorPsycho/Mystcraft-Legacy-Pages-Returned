package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.common.extensions.IForgeServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.function.Consumer;

public class ForgePlatformHelper implements IPlatformHelper {

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
    IForgeServerPlayer forgePlayer = (IForgeServerPlayer) player;
    if (extraDataWriter == null) {
      forgePlayer.openMenu(provider, (Consumer<FriendlyByteBuf>) null);
      return;
    }
    forgePlayer.openMenu(provider, extraDataWriter);
  }

  /**
   * Sets the black ink source fluid. Called by ForgeRegistrationHelper after fluid registration.
   */
  public void setBlackInkSource(FlowingFluid fluid) {
    this.blackInkSource = fluid;
  }

  /**
   * Sets the black ink flowing fluid. Called by ForgeRegistrationHelper after fluid registration.
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

  @Override
  public Block getShortGrassBlock() {
    return resolveShortGrass();
  }

  private static Block resolveShortGrass() {
    try {
      return (Block) Blocks.class.getField("SHORT_GRASS").get(null);
    } catch (ReflectiveOperationException ignored) {
      // Older versions use GRASS.
    }
    try {
      return (Block) Blocks.class.getField("GRASS").get(null);
    } catch (ReflectiveOperationException ignored) {
      // Fallback below.
    }
    return Blocks.GRASS_BLOCK;
  }
}
