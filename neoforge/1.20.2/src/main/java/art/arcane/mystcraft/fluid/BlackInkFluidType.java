package art.arcane.mystcraft.fluid;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/**
 * FluidType for Black Ink.
 * Defines the properties and appearance of the ink fluid.
 */
public class BlackInkFluidType extends FluidType {

    private static final ResourceLocation STILL_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "blocks/fluid");
    private static final ResourceLocation FLOWING_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "blocks/fluid_flow");
    private static final ResourceLocation OVERLAY_TEXTURE =
            new ResourceLocation(Mystcraft.MOD_ID, "blocks/fluid");

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
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return 0xFF1A1A1A;
            }
        });
    }
}
