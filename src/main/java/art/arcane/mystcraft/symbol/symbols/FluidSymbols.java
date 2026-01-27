package art.arcane.mystcraft.symbol.symbols;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.api.world.AgeDirector;
import art.arcane.mystcraft.symbol.SymbolBase;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamically registers sea block symbols for every fluid in the Forge registry.
 * Each registered fluid gets a symbol that sets it as the Age's ocean/sea fluid.
 */
public final class FluidSymbols {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluidSymbols.class);

    private FluidSymbols() {}

    public static void register() {
        int count = 0;

        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            // Skip empty/air fluid
            if (fluid == Fluids.EMPTY) {
                continue;
            }

            // Only register source fluids, not flowing variants
            FluidState defaultState = fluid.defaultFluidState();
            if (!defaultState.isSource()) {
                continue;
            }

            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
            if (fluidId == null) {
                continue;
            }

            boolean isWater = fluid == Fluids.WATER;
            boolean isLava = fluid == Fluids.LAVA;

            int cardRank = determineCardRank(fluidId, isWater, isLava);
            float instability = determineInstability(fluidId, isWater, isLava);
            String[] poem = determinePoem(fluidId, isWater, isLava);

            String symbolPath = "sea_" + fluidId.getNamespace() + "_" + fluidId.getPath();
            ResourceLocation symbolId = SymbolRegistry.mystcraftId(symbolPath);

            SymbolRegistry.register(new FluidSeaSymbol(symbolId, fluid, fluidId, cardRank, instability, poem));
            count++;
        }

        LOGGER.info("Registered {} fluid sea symbols", count);
    }

    private static int determineCardRank(ResourceLocation fluidId, boolean isWater, boolean isLava) {
        if (isWater) {
            return 1;
        }
        if (isLava) {
            return 4;
        }
        // Vanilla namespace fluids are slightly more common than modded
        if ("minecraft".equals(fluidId.getNamespace())) {
            return 3;
        }
        return 3;
    }

    private static float determineInstability(ResourceLocation fluidId, boolean isWater, boolean isLava) {
        if (isWater) {
            return 0.0f;
        }
        if (isLava) {
            return 25.0f;
        }
        return 15.0f;
    }

    private static String[] determinePoem(ResourceLocation fluidId, boolean isWater, boolean isLava) {
        if (isWater) {
            return new String[]{"Terrain", "Water", "Flow", "Sea"};
        }
        if (isLava) {
            return new String[]{"Terrain", "Fire", "Flow", "Chaos"};
        }
        return new String[]{"Terrain", "Liquid", "Flow", "Strange"};
    }

    /**
     * A sea symbol backed by a registered fluid. Sets the Age's sea block to this fluid.
     */
    public static class FluidSeaSymbol extends SymbolBase {

        private final Fluid fluid;
        private final ResourceLocation fluidId;

        public FluidSeaSymbol(ResourceLocation symbolId, Fluid fluid, ResourceLocation fluidId,
                              int cardRank, float instability, String[] poem) {
            super(symbolId, SymbolCategory.SEA);
            this.fluid = fluid;
            this.fluidId = fluidId;
            setCardRank(cardRank);
            setInstabilityCost(instability);
            setPoem(poem);
        }

        @Override
        public void registerLogic(AgeDirector director, long seed) {
            director.setSeaBlock(fluid.defaultFluidState().createLegacyBlock());
            director.addInstability(getInstabilityCost());
        }

        @Override
        public String getLocalizedName() {
            // Derive a display name from the fluid's registry name
            String path = fluidId.getPath();
            String name = formatFluidName(path);
            return name + " Sea";
        }

        /**
         * Converts a registry path like "flowing_lava" or "black_ink" to "Flowing Lava" or "Black Ink".
         */
        private static String formatFluidName(String path) {
            String[] parts = path.split("_");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    builder.append(' ');
                }
                String part = parts[i];
                if (!part.isEmpty()) {
                    builder.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        builder.append(part.substring(1));
                    }
                }
            }
            return builder.toString();
        }
    }
}
