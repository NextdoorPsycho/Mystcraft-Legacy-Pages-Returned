package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.block.CrystalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

/**
 * Centralized block property definitions for Mystcraft.
 * All BlockBehaviour.Properties are defined here and referenced by platform-specific registration.
 *
 * 1.18.2 port: Uses Material and MaterialColor (same as 1.19.2, not MapColor).
 */
public final class BlockDefinitions {

  // Workstation blocks
  public static final BlockBehaviour.Properties INK_MIXER = BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOK_BINDER = BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOK_RECEPTACLE = BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE)
      .strength(3.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOKSTAND = BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD)
      .strength(2.0F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties LINK_MODIFIER = BlockBehaviour.Properties.of(Material.STONE, MaterialColor.STONE)
      .strength(3.0F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties WRITING_DESK = BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  // Special blocks
  public static final BlockBehaviour.Properties CRYSTAL = BlockBehaviour.Properties.of(Material.GLASS, MaterialColor.COLOR_LIGHT_BLUE)
      .strength(1.5F)
      .lightLevel(CrystalBlock::getLightLevel)
      .noOcclusion();

  // 1.18.2: noLootTable() doesn't exist, use noDrops() instead
  public static final BlockBehaviour.Properties DECAY = BlockBehaviour.Properties.of(Material.STONE, MaterialColor.COLOR_BLACK)
      .strength(-1.0F, 3600000.0F)
      .noDrops()
      .randomTicks();

  public static final BlockBehaviour.Properties LINK_PORTAL = BlockBehaviour.Properties.of(Material.PORTAL, MaterialColor.COLOR_BLACK)
      .noCollission()
      .strength(-1.0F)
      .noDrops()
      .lightLevel(state -> 11);

  public static final BlockBehaviour.Properties STAR_FISSURE = BlockBehaviour.Properties.of(Material.PORTAL, MaterialColor.COLOR_BLACK)
      .noCollission()
      .strength(-1.0F, 3600000.0F)
      .noDrops()
      .lightLevel(state -> 15);

  private BlockDefinitions() {
  }
}
