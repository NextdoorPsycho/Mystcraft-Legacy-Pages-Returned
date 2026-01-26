package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
        // Custom grass color handler
        BlockColor grassColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return GrassColor.getDefaultColor();
            }

            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getGrassColor(ageUID);
                if (customColor != -1) {
                    return customColor;
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

        Mystcraft.LOGGER.info("Registered Age block color handlers");
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Item colors for grass/foliage blocks in inventory
        ItemColor grassItemColor = (stack, tintIndex) -> {
            int ageUID = getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getGrassColor(ageUID);
                if (customColor != -1) {
                    return customColor;
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
