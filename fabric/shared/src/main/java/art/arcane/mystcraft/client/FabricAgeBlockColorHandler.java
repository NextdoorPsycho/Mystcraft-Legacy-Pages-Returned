package art.arcane.mystcraft.client;

import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.fabric.FabricRegistries;
import art.arcane.mystcraft.portal.PortalUtils;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Registers block/item color handlers for Mystcraft blocks in Fabric.
 */
public final class FabricAgeBlockColorHandler {

  private FabricAgeBlockColorHandler() {
  }

  public static void register() {
    BlockColor grassColor = (state, level, pos, tintIndex) -> {
      if (level == null || pos == null) {
        return GrassColor.getDefaultColor();
      }

      int ageUID = AgeColorUtils.getCurrentAgeUID();
      if (ageUID >= 0) {
        java.util.List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
        if (colors.size() == 1) {
          return colors.get(0);
        } else if (colors.size() >= 2) {
          return AgeColorUtils.selectColorFromPalette(colors, pos, ageUID);
        }
      }

      return BiomeColors.getAverageGrassColor(level, pos);
    };

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

    BlockColor waterColor = (state, level, pos, tintIndex) -> {
      if (level == null || pos == null) {
        return 0x3F76E4;
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

    BlockColor crystalColor = (state, level, pos, tintIndex) -> {
      if (level == null || pos == null) return 0xFFFFFF;
      int ageUID = AgeColorUtils.getCurrentAgeUID();
      if (ageUID < 0) return 0xFFFFFF;

      int baseColor = ClientAgeDataCache.getSkyColor(ageUID);
      if (baseColor == -1) return 0xFFFFFF;
      return baseColor;
    };

    BlockColor portalColor = (state, level, pos, tintIndex) -> {
      // BE-direct lookup. Every cell of a lit portal carries a stamped
      // colour from PortalUtils.firePortal (see §6.6 in the v2 plan).
      // No level lookup, no BFS, no client-cache races. If the BE is
      // missing for any reason (chunk loading, pre-v2 portal on disk),
      // fall back to the BookReceptacle BFS so legacy portals still
      // tint sensibly until the next ignition.
      if (pos == null || level == null) return 0x4488FF;
      BlockEntity portalBe = level.getBlockEntity(pos);
      if (portalBe instanceof art.arcane.mystcraft.blockentity.LinkPortalBlockEntity portalBE) {
        return portalBE.getPortalColor();
      }

      // Legacy fallback (pre-v2 portals stored without a BE).
      Level clientLevel = Minecraft.getInstance().level;
      if (clientLevel == null) return 0x4488FF;
      BlockEntity be = PortalUtils.findReceptacle(clientLevel, pos);
      if (be instanceof BookReceptacleBlockEntity receptacle) {
        return receptacle.getPortalColor();
      }
      return 0x4488FF;
    };

    ColorProviderRegistry.BLOCK.register(grassColor,
        Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN,
        Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.TALL_GRASS);

    ColorProviderRegistry.BLOCK.register(foliageColor,
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES, Blocks.VINE);

    ColorProviderRegistry.BLOCK.register(waterColor, Blocks.WATER, Blocks.WATER_CAULDRON);

    ColorProviderRegistry.BLOCK.register(crystalColor, FabricRegistries.CRYSTAL.get());

    ColorProviderRegistry.BLOCK.register(portalColor, FabricRegistries.LINK_PORTAL.get());

    ItemColor grassItemColor = (stack, tintIndex) -> {
      int ageUID = AgeColorUtils.getCurrentAgeUID();
      if (ageUID >= 0) {
        java.util.List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUID);
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

    ColorProviderRegistry.ITEM.register(grassItemColor,
        Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN,
        Blocks.LARGE_FERN, Blocks.TALL_GRASS);

    ColorProviderRegistry.ITEM.register(foliageItemColor,
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES, Blocks.VINE);
  }
}
