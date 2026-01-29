package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.fluid.FabricBlackInkFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Fluid registrations for Mystcraft (Fabric 1.20.1).
 * Fabric does not have FluidType; only source and flowing fluids are registered.
 */
public final class FabricModFluids {

    /**
     * Direct fluid references used by FabricBlackInkFluid and LiquidBlock constructors.
     * These are created eagerly so that FabricModBlocks.FLUID_INK can reference the source fluid.
     */
    static final FabricBlackInkFluid.Source BLACK_INK_SOURCE_FLUID = new FabricBlackInkFluid.Source();
    static final FabricBlackInkFluid.Flowing BLACK_INK_FLOWING_FLUID = new FabricBlackInkFluid.Flowing();

    public static final Supplier<FlowingFluid> BLACK_INK_SOURCE = () -> BLACK_INK_SOURCE_FLUID;
    public static final Supplier<FlowingFluid> BLACK_INK_FLOWING = () -> BLACK_INK_FLOWING_FLUID;

    private FabricModFluids() {
    }

    /**
     * Performs Registry.register calls for all fluids.
     */
    public static void register() {
        Registry.register(BuiltInRegistries.FLUID, new ResourceLocation(Mystcraft.MOD_ID, "black_ink"), BLACK_INK_SOURCE_FLUID);
        Registry.register(BuiltInRegistries.FLUID, new ResourceLocation(Mystcraft.MOD_ID, "black_ink_flowing"), BLACK_INK_FLOWING_FLUID);
    }
}
