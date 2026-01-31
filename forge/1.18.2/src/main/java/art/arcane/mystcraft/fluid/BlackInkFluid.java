package art.arcane.mystcraft.fluid;

import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.function.Supplier;

/**
 * Black Ink fluid implementation for 1.18.2.
 * Used in the ink mixer and writing desk for creating pages.
 *
 * Note: 1.18.2 Forge uses FluidAttributes instead of FluidType.
 */
public abstract class BlackInkFluid extends ForgeFlowingFluid {

  protected BlackInkFluid(Properties properties) {
    super(properties);
  }

  /**
   * Creates the properties for black ink fluid using common registry accessors.
   * Note: This must be called after the source, block, and bucket have been registered.
   *
   * @param source    Supplier for the source fluid
   * @param flowing   Supplier for the flowing fluid
   */
  public static ForgeFlowingFluid.Properties createProperties(
      Supplier<? extends FlowingFluid> source,
      Supplier<? extends FlowingFluid> flowing) {
    // 1.18.2 uses FluidAttributes instead of FluidType
    return new ForgeFlowingFluid.Properties(
            source,
            flowing,
            FluidAttributes.builder(
                    new ResourceLocation("mystcraft", "block/fluid_ink_still"),
                    new ResourceLocation("mystcraft", "block/fluid_ink_flow"))
                .density(1200)
                .viscosity(2000)
                .color(0xFF1A1A1A)
                .translationKey("fluid.mystcraft.black_ink"))
        .slopeFindDistance(2)
        .levelDecreasePerBlock(2)
        .block(() -> (LiquidBlock) ModBlocks.FLUID_INK.get())
        .bucket(ModItems.INK_BUCKET);
  }

  /**
   * Source (still) variant of black ink.
   */
  public static class Source extends BlackInkFluid {

    public Source(Properties properties) {
      super(properties);
    }

    @Override
    public int getAmount(FluidState state) {
      return 8;
    }

    @Override
    public boolean isSource(FluidState state) {
      return true;
    }
  }

  /**
   * Flowing variant of black ink.
   */
  public static class Flowing extends BlackInkFluid {

    public Flowing(Properties properties) {
      super(properties);
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
      super.createFluidStateDefinition(builder);
      builder.add(LEVEL);
    }

    @Override
    public int getAmount(FluidState state) {
      return state.getValue(LEVEL);
    }

    @Override
    public boolean isSource(FluidState state) {
      return false;
    }
  }
}
