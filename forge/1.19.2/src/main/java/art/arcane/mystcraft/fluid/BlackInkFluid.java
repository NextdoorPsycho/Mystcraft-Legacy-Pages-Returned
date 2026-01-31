package art.arcane.mystcraft.fluid;

import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import java.util.function.Supplier;

/**
 * Black Ink fluid implementation.
 * Used in the ink mixer and writing desk for creating pages.
 */
public abstract class BlackInkFluid extends ForgeFlowingFluid {

  private static Supplier<FluidType> fluidTypeSupplier;

  protected BlackInkFluid(Properties properties) {
    super(properties);
  }

  /**
   * Sets the fluid type supplier. Must be called during registration setup.
   */
  public static void setFluidTypeSupplier(Supplier<FluidType> supplier) {
    fluidTypeSupplier = supplier;
  }

  /**
   * Creates the properties for black ink fluid.
   *
   * @param fluidType Supplier for the fluid type
   * @param source    Supplier for the source fluid
   * @param flowing   Supplier for the flowing fluid
   * @param block     Supplier for the fluid block
   * @param bucket    Supplier for the bucket item
   */
  public static ForgeFlowingFluid.Properties createProperties(
      Supplier<FluidType> fluidType,
      Supplier<? extends FlowingFluid> source,
      Supplier<? extends FlowingFluid> flowing,
      Supplier<? extends LiquidBlock> block,
      Supplier<? extends net.minecraft.world.item.Item> bucket) {
    return new ForgeFlowingFluid.Properties(fluidType, source, flowing)
        .slopeFindDistance(2)
        .levelDecreasePerBlock(2)
        .block(block)
        .bucket(bucket);
  }

  /**
   * Creates the properties for black ink fluid using common registry accessors.
   * Note: This must be called after the fluid type, block, and bucket have been registered.
   *
   * @param fluidType Supplier for the fluid type
   * @param source    Supplier for the source fluid
   * @param flowing   Supplier for the flowing fluid
   */
  public static ForgeFlowingFluid.Properties createProperties(
      Supplier<FluidType> fluidType,
      Supplier<? extends FlowingFluid> source,
      Supplier<? extends FlowingFluid> flowing) {
    return new ForgeFlowingFluid.Properties(fluidType, source, flowing)
        .slopeFindDistance(2)
        .levelDecreasePerBlock(2)
        .block(() -> (LiquidBlock) ModBlocks.FLUID_INK.get())
        .bucket(ModItems.INK_BUCKET);
  }

  @Override
  public FluidType getFluidType() {
    return fluidTypeSupplier.get();
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
