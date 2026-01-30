package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.block.CrystalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Centralized block property definitions for Mystcraft.
 * All BlockBehaviour.Properties are defined here and referenced by platform-specific registration.
 */
public final class BlockDefinitions {

  // Workstation blocks
  public static final BlockBehaviour.Properties INK_MIXER = BlockBehaviour.Properties.of()
      .mapColor(MapColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOK_BINDER = BlockBehaviour.Properties.of()
      .mapColor(MapColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOK_RECEPTACLE = BlockBehaviour.Properties.of()
      .mapColor(MapColor.STONE)
      .strength(3.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOKSTAND = BlockBehaviour.Properties.of()
      .mapColor(MapColor.WOOD)
      .strength(2.0F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties LINK_MODIFIER = BlockBehaviour.Properties.of()
      .mapColor(MapColor.STONE)
      .strength(3.0F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties WRITING_DESK = BlockBehaviour.Properties.of()
      .mapColor(MapColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  // Special blocks
  public static final BlockBehaviour.Properties CRYSTAL = BlockBehaviour.Properties.of()
      .mapColor(MapColor.COLOR_LIGHT_BLUE)
      .strength(1.5F)
      .lightLevel(CrystalBlock::getLightLevel)
      .noOcclusion();

  public static final BlockBehaviour.Properties DECAY = BlockBehaviour.Properties.of()
      .mapColor(MapColor.COLOR_BLACK)
      .strength(-1.0F, 3600000.0F)
      .noLootTable()
      .randomTicks();

  public static final BlockBehaviour.Properties LINK_PORTAL = BlockBehaviour.Properties.of()
      .mapColor(MapColor.COLOR_BLACK)
      .noCollission()
      .strength(-1.0F)
      .noLootTable()
      .lightLevel(state -> 11)
      .pushReaction(PushReaction.BLOCK);

  public static final BlockBehaviour.Properties STAR_FISSURE = BlockBehaviour.Properties.of()
      .mapColor(MapColor.COLOR_BLACK)
      .noCollission()
      .strength(-1.0F, 3600000.0F)
      .noLootTable()
      .lightLevel(state -> 15)
      .pushReaction(PushReaction.BLOCK);

  private BlockDefinitions() {
  }
}
