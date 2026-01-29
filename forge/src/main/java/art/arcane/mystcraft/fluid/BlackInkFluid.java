package art.arcane.mystcraft.fluid;

import art.arcane.mystcraft.registry.ForgeModBlocks;
import art.arcane.mystcraft.registry.ForgeModFluids;
import art.arcane.mystcraft.registry.ForgeModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/**
 * Black Ink fluid implementation.
 * Used in the ink mixer and writing desk for creating pages.
 */
public abstract class BlackInkFluid extends ForgeFlowingFluid {

    protected BlackInkFluid(Properties properties) {
        super(properties);
    }

    @Override
    public FluidType getFluidType() {
        return ForgeModFluids.BLACK_INK_TYPE.get();
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

    /**
     * Creates the properties for black ink fluid.
     */
    public static ForgeFlowingFluid.Properties createProperties() {
        return new ForgeFlowingFluid.Properties(
                ForgeModFluids.BLACK_INK_TYPE,
                ForgeModFluids.BLACK_INK_SOURCE,
                ForgeModFluids.BLACK_INK_FLOWING
        )
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .block(ForgeModBlocks.FLUID_INK)
                .bucket(ForgeModItems.INK_BUCKET);
    }
}
