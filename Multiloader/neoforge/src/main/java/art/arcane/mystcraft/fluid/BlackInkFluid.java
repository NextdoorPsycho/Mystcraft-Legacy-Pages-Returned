package art.arcane.mystcraft.fluid;

import art.arcane.mystcraft.registry.NeoForgeModBlocks;
import art.arcane.mystcraft.registry.NeoForgeModFluids;
import art.arcane.mystcraft.registry.NeoForgeModItems;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Black Ink fluid implementation.
 * Used in the ink mixer and writing desk for creating pages.
 */
public abstract class BlackInkFluid extends BaseFlowingFluid {

    protected BlackInkFluid(Properties properties) {
        super(properties);
    }

    @Override
    public FluidType getFluidType() {
        return NeoForgeModFluids.BLACK_INK_TYPE.get();
    }

    /** Source (still) variant of black ink. */
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

    /** Flowing variant of black ink. */
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

    /** Creates the properties for black ink fluid. */
    public static BaseFlowingFluid.Properties createProperties() {
        return new BaseFlowingFluid.Properties(
                NeoForgeModFluids.BLACK_INK_TYPE,
                NeoForgeModFluids.BLACK_INK_SOURCE,
                NeoForgeModFluids.BLACK_INK_FLOWING
        )
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .block(NeoForgeModBlocks.FLUID_INK)
                .bucket(NeoForgeModItems.INK_BUCKET);
    }
}
