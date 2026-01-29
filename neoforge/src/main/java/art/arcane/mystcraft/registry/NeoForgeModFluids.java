package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.fluid.BlackInkFluid;
import art.arcane.mystcraft.fluid.BlackInkFluidType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Fluid registrations for Mystcraft.
 */
public final class NeoForgeModFluids {

    public static final DeferredHolder<FluidType, FluidType> BLACK_INK_TYPE =
            MystcraftRegistries.FLUID_TYPES.register("black_ink",
                    BlackInkFluidType::new);

    public static final DeferredHolder<Fluid, FlowingFluid> BLACK_INK_SOURCE =
            MystcraftRegistries.FLUIDS.register("black_ink",
                    () -> new BlackInkFluid.Source(BlackInkFluid.createProperties()));

    public static final DeferredHolder<Fluid, FlowingFluid> BLACK_INK_FLOWING =
            MystcraftRegistries.FLUIDS.register("black_ink_flowing",
                    () -> new BlackInkFluid.Flowing(BlackInkFluid.createProperties()));

    private NeoForgeModFluids() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
