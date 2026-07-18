package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.CrystalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Centralized block property definitions for Mystcraft. All
 * BlockBehaviour.Properties are defined here and referenced by
 * platform-specific registration.
 */
public final class BlockDefinitions {

  public static final BlockBehaviour.Properties INK_MIXER = properties("blockinkmixer")
      .mapColor(MapColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOK_BINDER = properties("blockbookbinder")
      .mapColor(MapColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties BOOK_RECEPTACLE = properties("blockbookreceptacle")
      .mapColor(MapColor.STONE)
      .strength(3.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties LINK_MODIFIER = properties("blocklinkmodifier")
      .mapColor(MapColor.STONE)
      .strength(3.0F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties WRITING_DESK = properties("writingdesk")
      .mapColor(MapColor.WOOD)
      .strength(2.5F)
      .requiresCorrectToolForDrops();

  public static final BlockBehaviour.Properties CRYSTAL = properties("blockcrystal")
      .mapColor(MapColor.COLOR_LIGHT_BLUE)
      .strength(1.5F)
      .lightLevel(CrystalBlock::getLightLevel)
      .noOcclusion();

  public static final BlockBehaviour.Properties DECAY = properties("blockdecay")
      .mapColor(MapColor.COLOR_BLACK)
      .strength(-1.0F, 3600000.0F)
      .noLootTable()
      .randomTicks();

  public static final BlockBehaviour.Properties LINK_PORTAL = properties("linkportal")
      .mapColor(MapColor.COLOR_BLACK)
      .noCollision()
      .strength(-1.0F)
      .noLootTable()
      .lightLevel(state -> 11)
      .pushReaction(PushReaction.BLOCK);

  public static final BlockBehaviour.Properties STAR_FISSURE = properties("blockstarfissure")
      .mapColor(MapColor.COLOR_BLACK)
      .noCollision()
      .strength(-1.0F, 3600000.0F)
      .noLootTable()
      .lightLevel(state -> 15)
      .pushReaction(PushReaction.BLOCK);

  private BlockDefinitions() {
  }

  private static BlockBehaviour.Properties properties(String path) {
    Identifier id = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
    return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
  }
}
