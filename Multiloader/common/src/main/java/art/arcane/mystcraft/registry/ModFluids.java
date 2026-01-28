package art.arcane.mystcraft.registry;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Common accessor for registered fluids.
 * Platform modules populate these suppliers during initialization.
 */
public final class ModFluids {

    public static Supplier<FlowingFluid> BLACK_INK_SOURCE;
    public static Supplier<FlowingFluid> BLACK_INK_FLOWING;
    public static Supplier<Item> BLACK_INK_BUCKET;

    private ModFluids() {
    }
}
