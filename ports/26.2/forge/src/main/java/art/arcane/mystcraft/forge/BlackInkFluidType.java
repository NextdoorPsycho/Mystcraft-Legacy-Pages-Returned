package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

/** Physical and client rendering properties for Mystcraft black ink. */
public final class BlackInkFluidType extends FluidType {

  private static final Identifier STILL_TEXTURE = id("block/fluid");
  private static final Identifier FLOWING_TEXTURE = id("block/fluid_flow");

  public BlackInkFluidType() {
    super(Properties.create()
        .density(1200)
        .viscosity(2000)
        .canSwim(false)
        .canDrown(true)
        .supportsBoating(false)
        .descriptionId("fluid.mystcraft.black_ink"));
  }

  @Override
  public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
    consumer.accept(new IClientFluidTypeExtensions() {
      @Override
      public Identifier getStillTexture() {
        return STILL_TEXTURE;
      }

      @Override
      public Identifier getFlowingTexture() {
        return FLOWING_TEXTURE;
      }

      @Override
      public Identifier getOverlayTexture() {
        return STILL_TEXTURE;
      }

      @Override
      public int getTintColor() {
        return 0xFF1A1A1A;
      }
    });
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }
}
