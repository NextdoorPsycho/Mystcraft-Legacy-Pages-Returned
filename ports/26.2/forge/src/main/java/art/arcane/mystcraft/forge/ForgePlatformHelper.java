package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.platform.services.IPlatformHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.common.extensions.IForgeServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Forge 65 implementation of Mystcraft's loader platform contract. */
public final class ForgePlatformHelper implements IPlatformHelper {

  private static Supplier<? extends FlowingFluid> blackInkSource;
  private static Supplier<? extends FlowingFluid> blackInkFlowing;

  static void installBlackInkFluids(Supplier<? extends FlowingFluid> source,
                                    Supplier<? extends FlowingFluid> flowing) {
    blackInkSource = Objects.requireNonNull(source, "source");
    blackInkFlowing = Objects.requireNonNull(flowing, "flowing");
  }

  @Override
  public String getPlatformName() {
    return "Forge";
  }

  @Override
  public boolean isModLoaded(String modId) {
    return ModList.isLoaded(modId);
  }

  @Override
  public boolean isDevelopmentEnvironment() {
    return !FMLLoader.isProduction();
  }

  @Override
  public boolean isClientEnvironment() {
    return FMLEnvironment.dist.isClient();
  }

  @Override
  public void openMenu(ServerPlayer player, MenuProvider provider,
                       Consumer<FriendlyByteBuf> extraDataWriter) {
    if (extraDataWriter == null) {
      player.openMenu(provider);
      return;
    }
    ((IForgeServerPlayer) player).openMenu(provider, extraDataWriter);
  }

  @Override
  public FlowingFluid createBlackInkSource() {
    if (blackInkSource == null) {
      throw new IllegalStateException("Black ink source fluid has not been registered");
    }
    return blackInkSource.get();
  }

  @Override
  public FlowingFluid createBlackInkFlowing() {
    if (blackInkFlowing == null) {
      throw new IllegalStateException("Black ink flowing fluid has not been registered");
    }
    return blackInkFlowing.get();
  }

  @Override
  public Block getShortGrassBlock() {
    return Blocks.SHORT_GRASS;
  }
}
