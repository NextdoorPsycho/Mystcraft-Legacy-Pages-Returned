package art.arcane.mystcraft.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.portal.PortalUtils;
import art.arcane.mystcraft.registry.NeoForgeModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.List;

/** Handles custom block colors for grass, foliage, and water in Age dimensions. */
@Mod.EventBusSubscriber(modid = Mystcraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AgeBlockColorHandler {

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        // Custom grass color handler with multi-color noise support
        BlockColor grassColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return GrassColor.getDefaultColor();
            }

            int ageUID = AgeColorUtils.getCurrentAgeUID();
            if (ageUID >= 0) {
                List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
                if (colors.size() == 1) {
                    return colors.get(0);
                } else if (colors.size() >= 2) {
                    return AgeColorUtils.selectColorFromPalette(colors, pos, ageUID);
                }
            }

            return BiomeColors.getAverageGrassColor(level, pos);
        };

        // Custom foliage color handler
        BlockColor foliageColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return FoliageColor.getDefaultColor();
            }

            int ageUID = AgeColorUtils.getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getFoliageColor(ageUID);
                if (customColor != -1) {
                    return customColor;
                }
            }

            return BiomeColors.getAverageFoliageColor(level, pos);
        };

        // Custom water color handler
        BlockColor waterColor = (state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return 0x3F76E4; // Default water color
            }

            int ageUID = AgeColorUtils.getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getWaterColor(ageUID);
                if (customColor != -1) {
                    return customColor;
                }
            }

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
        BlockColor portalColor = (state, blockAndTintGetter, pos, tintIndex) -> {
            if (pos == null) {
                return 0x4488FF; // Default mystcraft blue
            }

            Level clientLevel = Minecraft.getInstance().level;
            if (clientLevel != null) {
                BlockEntity be = PortalUtils.findReceptacle(clientLevel, pos);
                if (be instanceof BookReceptacleBlockEntity receptacle) {
                    return receptacle.getPortalColor();
                }
            }

            return 0x4488FF; // Default mystcraft blue
        };

        event.register(portalColor, NeoForgeModBlocks.LINK_PORTAL.get());

        Mystcraft.LOGGER.info("Registered Age block color handlers");
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor grassItemColor = (stack, tintIndex) -> {
            int ageUID = AgeColorUtils.getCurrentAgeUID();
            if (ageUID >= 0) {
                List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
                if (!colors.isEmpty()) {
                    return colors.get(0);
                }
            }
            return GrassColor.getDefaultColor();
        };

        ItemColor foliageItemColor = (stack, tintIndex) -> {
            int ageUID = AgeColorUtils.getCurrentAgeUID();
            if (ageUID >= 0) {
                int customColor = ClientAgeDataCache.getFoliageColor(ageUID);
                if (customColor != -1) {
                    return customColor;
                }
            }
            return FoliageColor.getDefaultColor();
        };

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
