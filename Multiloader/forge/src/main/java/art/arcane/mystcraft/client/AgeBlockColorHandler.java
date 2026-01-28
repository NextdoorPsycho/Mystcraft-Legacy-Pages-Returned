package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.ForgeModBlocks;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Handles custom block colors for grass, foliage, and water in Age dimensions.
 */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AgeBlockColorHandler {

    /**
     * Gets the current Age UID the player is in, or -1 if not in an Age.
     */
    private static int getCurrentAgeUID() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;

        if (!AgeDimensionFactory.isMystcraftAge(mc.level.dimension())) {
            return -1;
        }

        String path = mc.level.dimension().location().getPath();
        if (path.startsWith("mystcraft_age_")) {
            try {
                return Integer.parseInt(path.substring("mystcraft_age_".length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // Custom grass color handler with multi-color noise support
        BlockColor grassColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return GrassColor.getDefaultColor();
            }

            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
                if (colors.size() == 1) {
                    return colors.get(0);
                } else if (colors.size() >= 2) {
                    return selectColorFromPalette(colors, pos, ageUID);
                }
            }

            // Fall back to vanilla behavior
            return BiomeColors.getAverageGrassColor(level, pos);
        };

        // Custom foliage color handler
        BlockColor foliageColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return FoliageColor.getDefaultColor();
            }

            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getFoliageColor(ageUID);
                if (customColor != -1) {
                    return customColor;
                }
            }

            // Fall back to vanilla behavior
            return BiomeColors.getAverageFoliageColor(level, pos);
        };

        // Custom water color handler
        BlockColor waterColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return 0x3F76E4; // Default water color
            }

            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getWaterColor(ageUID);
                if (customColor != -1) {
                    return customColor;
                }
            }

            // Fall back to vanilla behavior
            return BiomeColors.getAverageWaterColor(level, pos);
        };

        // Register for grass blocks
        event.register(grassColor,
                Blocks.GRASS_BLOCK,
                Blocks.GRASS,
                Blocks.FERN,
                Blocks.LARGE_FERN,
                Blocks.POTTED_FERN,
                Blocks.TALL_GRASS
        );

        // Register for foliage blocks
        event.register(foliageColor,
                Blocks.OAK_LEAVES,
                Blocks.SPRUCE_LEAVES,
                Blocks.BIRCH_LEAVES,
                Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES,
                Blocks.MANGROVE_LEAVES,
                Blocks.VINE
        );

        // Register for water
        event.register(waterColor,
                Blocks.WATER,
                Blocks.WATER_CAULDRON
        );

        // Portal color handler - gets color from the book receptacle
        // Note: In rendering context, level may be a ChunkRenderCache, not a Level
        // We use Minecraft.getInstance().level for the trace which works for client
        BlockColor portalColor = (state, blockAndTintGetter, pos, tintIndex) -> {
            if (pos == null) {
                return 0x4488FF; // Default mystcraft blue
            }

            // Use the client level for tracing to receptacle
            Level clientLevel = Minecraft.getInstance().level;
            if (clientLevel != null) {
                BlockEntity be = PortalUtils.findReceptacle(clientLevel, pos);
                if (be instanceof BookReceptacleBlockEntity receptacle) {
                    return receptacle.getPortalColor();
                }
            }

            return 0x4488FF; // Default mystcraft blue
        };

        event.register(portalColor, ForgeModBlocks.LINK_PORTAL.get());

        Mystcraft.LOGGER.info("Registered Age block color handlers");
    }

    /**
     * Selects a color from the palette using 2D Perlin noise at the block position.
     * Produces organic blob patches of different colors across the landscape.
     */
    private static int selectColorFromPalette(List<Integer> colors, BlockPos pos, int ageUID) {
        double scale = 0.021; // ~48-block wavelength blobs
        double nx = pos.getX() * scale;
        double nz = pos.getZ() * scale;
        double noise = perlinNoise2D(nx, nz, ageUID);
        // Map noise from [-1, 1] to [0, colors.size())
        double normalized = (noise + 1.0) * 0.5; // [0, 1]
        int index = (int) (normalized * colors.size());
        index = Math.max(0, Math.min(index, colors.size() - 1));
        return colors.get(index);
    }

    // --- Inline 2D Perlin noise ---

    private static double perlinNoise2D(double x, double y, int seed) {
        // Offset by seed for per-Age uniqueness
        x += seed * 31.7;
        y += seed * 17.3;

        int xi = floorInt(x);
        int yi = floorInt(y);
        double xf = x - xi;
        double yf = y - yi;

        double u = fade(xf);
        double v = fade(yf);

        int aa = hash2D(xi, yi, seed);
        int ab = hash2D(xi, yi + 1, seed);
        int ba = hash2D(xi + 1, yi, seed);
        int bb = hash2D(xi + 1, yi + 1, seed);

        double x1 = lerp(grad2D(aa, xf, yf), grad2D(ba, xf - 1, yf), u);
        double x2 = lerp(grad2D(ab, xf, yf - 1), grad2D(bb, xf - 1, yf - 1), u);

        return lerp(x1, x2, v);
    }

    private static int floorInt(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static int hash2D(int x, int y, int seed) {
        int h = seed;
        h ^= x * 374761393;
        h ^= y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return h;
    }

    private static double grad2D(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Item colors for grass/foliage blocks in inventory (uses first color from palette)
        ItemColor grassItemColor = (stack, tintIndex) -> {
            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
                if (!colors.isEmpty()) {
                    return colors.get(0);
                }
            }
            return GrassColor.getDefaultColor();
        };

        ItemColor foliageItemColor = (stack, tintIndex) -> {
            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getFoliageColor(ageUID);
                if (customColor != -1) {
                    return customColor;
                }
            }
            return FoliageColor.getDefaultColor();
        };

        // Register item colors
        event.register(grassItemColor,
                Blocks.GRASS_BLOCK,
                Blocks.GRASS,
                Blocks.FERN,
                Blocks.LARGE_FERN,
                Blocks.TALL_GRASS
        );

        event.register(foliageItemColor,
                Blocks.OAK_LEAVES,
                Blocks.SPRUCE_LEAVES,
                Blocks.BIRCH_LEAVES,
                Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES,
                Blocks.MANGROVE_LEAVES,
                Blocks.VINE
        );
    }
}
