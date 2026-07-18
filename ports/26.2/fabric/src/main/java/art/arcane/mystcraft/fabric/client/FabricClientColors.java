package art.arcane.mystcraft.fabric.client;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.LinkPortalBlockEntity;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.fabric.FabricRegistries;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.portal.PortalUtils;
import java.util.List;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Client tint and fluid-model registration for synced Age colors. */
public final class FabricClientColors {

  private static final int OPAQUE_BLACK_INK = 0xFF1A1A1A;
  private static final int OPAQUE_PORTAL_BLUE = 0xFF4488FF;

  private FabricClientColors() {
  }

  public static void register() {
    BlockTintSource grass = new GrassTintSource();
    BlockTintSource foliage = new FoliageTintSource();
    BlockTintSource water = new WaterTintSource();
    BlockTintSource crystal = new CrystalTintSource();
    BlockTintSource portal = new PortalTintSource();

    BlockColorRegistry.register(List.of(grass),
        Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN,
        Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.TALL_GRASS);
    BlockColorRegistry.register(List.of(foliage),
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.MANGROVE_LEAVES, Blocks.VINE);
    BlockColorRegistry.register(List.of(water), Blocks.WATER, Blocks.WATER_CAULDRON);
    BlockColorRegistry.register(List.of(crystal), FabricRegistries.CRYSTAL.get());
    BlockColorRegistry.register(List.of(portal), FabricRegistries.LINK_PORTAL.get());

    FluidModel.Unbaked waterModel = new FluidModel.Unbaked(
        vanillaMaterial("block/water_still"),
        vanillaMaterial("block/water_flow"),
        vanillaMaterial("block/water_overlay"),
        water);
    FluidRenderingRegistry.register(Fluids.WATER, Fluids.FLOWING_WATER, waterModel);

    FluidModel.Unbaked inkModel = new FluidModel.Unbaked(
        mystcraftMaterial("blocks/fluid"),
        mystcraftMaterial("blocks/fluid_flow"),
        null,
        constant(OPAQUE_BLACK_INK));
    FluidRenderingRegistry.register(
        FabricRegistries.BLACK_INK_SOURCE.get(),
        FabricRegistries.BLACK_INK_FLOWING.get(),
        inkModel);
  }

  private static Material vanillaMaterial(String path) {
    return new Material(Identifier.withDefaultNamespace(path), true);
  }

  private static Material mystcraftMaterial(String path) {
    return new Material(Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path), true);
  }

  private static BlockTintSource constant(int color) {
    return state -> color;
  }

  private static int opaque(int color) {
    return 0xFF000000 | color;
  }

  private static final class GrassTintSource implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return GrassColor.getDefaultColor();
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        List<Integer> colors = ClientAgeDataCache.getGrassColors(ageUid);
        if (colors.size() == 1) {
          return opaque(colors.getFirst());
        }
        if (colors.size() > 1) {
          return opaque(AgeColorUtils.selectColorFromPalette(colors, position, ageUid));
        }
      }
      return BiomeColors.getAverageGrassColor(level, position);
    }
  }

  private static final class FoliageTintSource implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return FoliageColor.FOLIAGE_DEFAULT;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        int custom = ClientAgeDataCache.getFoliageColor(ageUid);
        if (custom != -1) {
          return opaque(custom);
        }
      }
      return BiomeColors.getAverageFoliageColor(level, position);
    }
  }

  private static final class WaterTintSource implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid >= 0) {
        int custom = ClientAgeDataCache.getWaterColor(ageUid);
        if (custom != -1) {
          return opaque(custom);
        }
      }
      return BiomeColors.getAverageWaterColor(level, position);
    }
  }

  private static final class CrystalTintSource implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
      int ageUid = AgeColorUtils.getCurrentAgeUID();
      if (ageUid < 0) {
        return -1;
      }
      int custom = ClientAgeDataCache.getSkyColor(ageUid);
      return custom == -1 ? -1 : opaque(custom);
    }
  }

  private static final class PortalTintSource implements BlockTintSource {
    @Override
    public int color(BlockState state) {
      return OPAQUE_PORTAL_BLUE;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos position) {
      BlockEntity portalBlockEntity = level.getBlockEntity(position);
      if (portalBlockEntity instanceof LinkPortalBlockEntity portal) {
        return opaque(portal.getPortalColor());
      }

      if (level instanceof Level actualLevel) {
        BlockEntity receptacleBlockEntity = PortalUtils.findReceptacle(actualLevel, position);
        if (receptacleBlockEntity instanceof BookReceptacleBlockEntity receptacle) {
          return opaque(receptacle.getPortalColor());
        }
      }
      return OPAQUE_PORTAL_BLUE;
    }
  }
}
