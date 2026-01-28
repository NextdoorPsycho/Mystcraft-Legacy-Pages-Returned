package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.fluid.BlackInkFluid;
import art.arcane.mystcraft.fluid.BlackInkFluidType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Fluid registrations for Mystcraft.
 */
public final class ForgeModFluids {

    public static final RegistryObject<FluidType> BLACK_INK_TYPE =
            MystcraftRegistries.FLUID_TYPES.register("black_ink",
                    BlackInkFluidType::new);

    public static final RegistryObject<FlowingFluid> BLACK_INK_SOURCE =
            MystcraftRegistries.FLUIDS.register("black_ink",
                    () -> new BlackInkFluid.Source(BlackInkFluid.createProperties()));

    public static final RegistryObject<FlowingFluid> BLACK_INK_FLOWING =
            MystcraftRegistries.FLUIDS.register("black_ink_flowing",
                    () -> new BlackInkFluid.Flowing(BlackInkFluid.createProperties()));

    private ForgeModFluids() {
    }

    /**
     * Call to ensure static initialization runs.
     */
    public static void register() {
    }
}
