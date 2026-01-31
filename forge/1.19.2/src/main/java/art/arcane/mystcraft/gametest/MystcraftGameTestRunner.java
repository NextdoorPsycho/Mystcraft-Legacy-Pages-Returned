package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class MystcraftGameTestRunner {

  private MystcraftGameTestRunner() {
  }

  /**
   * Tests that linkbooks and descriptive books drop as LinkbookEntity when dropped.
   */
  public static void runBookDropsAsEntityTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
    ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());

    ItemEntity dummyItem = new ItemEntity(level, 0.5, 2.0, 0.5, linkbook.copy());

    // Test linkbook creates LinkbookEntity
    assertCreatesLinkbookEntity(linkbook, dummyItem, "linkbook");
    // Test descriptive book creates LinkbookEntity
    assertCreatesLinkbookEntity(agebook, dummyItem, "agebook");

    helper.succeed();
  }

  /**
   * Tests that personal link book item is registered and can be instantiated.
   * Note: Full dimension creation requires a fully-initialized player which isn't
   * available in GameTest environment, so we only test item registration.
   */
  public static void runPersonalBookCreatesAndTeleportsTest(GameTestHelper helper) {
    // Verify the personal link book item exists and is properly registered
    Item personalBookItem = ModItems.PERSONAL_LINK_BOOK.get();
    if (!(personalBookItem instanceof PersonalLinkBookItem)) {
      helper.fail("Personal link book is not a PersonalLinkBookItem");
      return;
    }

    // Verify we can create an ItemStack
    ItemStack personalBook = new ItemStack(personalBookItem);
    if (personalBook.isEmpty()) {
      helper.fail("Failed to create personal link book ItemStack");
      return;
    }

    // Verify the item has the expected class
    if (!(personalBook.getItem() instanceof PersonalLinkBookItem)) {
      helper.fail("ItemStack item is not PersonalLinkBookItem");
      return;
    }

    helper.succeed();
  }

  /**
   * Tests that agebook item is registered, pages can be created, and symbols exist.
   * Note: Full dimension creation requires a fully-initialized player which isn't
   * available in GameTest environment, so we only test item and symbol registration.
   */
  public static void runRandomBookWith5SymbolsTest(GameTestHelper helper) {
    // Verify agebook item exists
    Item item = Registry.ITEM.get(new ResourceLocation(Mystcraft.MOD_ID, "agebook"));
    if (!(item instanceof AgebookItem)) {
      helper.fail("Agebook item not found or wrong type");
      return;
    }

    // Verify we can create an ItemStack
    ItemStack agebook = new ItemStack(item);
    if (agebook.isEmpty()) {
      helper.fail("Failed to create agebook ItemStack");
      return;
    }

    // Verify we can create pages
    ItemStack linkPage = Page.createLinkPage();
    if (linkPage.isEmpty()) {
      helper.fail("Failed to create link page");
      return;
    }

    // Verify expected symbols exist
    List<String> expectedSymbols = List.of("terrain_flat", "biome_plains", "sun", "weather_normal", "lighting_normal");
    for (String symbolPath : expectedSymbols) {
      ResourceLocation id = SymbolRegistry.mystcraftId(symbolPath);
      if (!SymbolRegistry.contains(id)) {
        helper.fail("Expected symbol not found: " + id);
        return;
      }
    }

    // Verify we can create symbol pages
    ItemStack symbolPage = Page.createSymbolPage(SymbolRegistry.mystcraftId("terrain_flat"));
    if (symbolPage.isEmpty()) {
      helper.fail("Failed to create symbol page");
      return;
    }

    helper.succeed();
  }

  /**
   * Tests that linkbook decays over time but personal book does not.
   */
  public static void runLinkbookDecaysButPersonalDoesNotTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(BlockPos.ZERO);
    double x = origin.getX() + 0.5;
    double y = origin.getY() + 2.0;
    double z = origin.getZ() + 0.5;

    // Speed up decay for testing
    LinkbookEntity.setDecayMultiplierForTests(10.0f);

    // Create linkbook entity (should decay)
    LinkbookEntity linkbookEntity = new LinkbookEntity(level, x, y, z);
    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
    linkbookEntity.setBookItem(linkbook);
    level.addFreshEntity(linkbookEntity);

    // Create personal book entity (should NOT decay)
    LinkbookEntity personalEntity = new LinkbookEntity(level, x + 2, y, z);
    ItemStack personalBook = new ItemStack(ModItems.PERSONAL_LINK_BOOK.get());
    // Manually set NoDecay tag since we don't have a player to initialize
    net.minecraft.nbt.CompoundTag personalTag = personalBook.getOrCreateTag();
    personalTag.putBoolean("NoDecay", true);
    personalEntity.setBookItem(personalBook);
    level.addFreshEntity(personalEntity);

    float initialLinkbookHealth = linkbookEntity.getHealth();
    float initialPersonalHealth = personalEntity.getHealth();

    helper.runAtTickTime(100, () -> {
      LinkbookEntity.setDecayMultiplierForTests(1.0f);

      float linkbookHealth = linkbookEntity.getHealth();
      float personalHealth = personalEntity.getHealth();

      // Linkbook should have decayed (lower health or removed)
      boolean linkbookDecayed = linkbookEntity.isRemoved() || linkbookHealth < initialLinkbookHealth;
      // Personal book should NOT have decayed
      boolean personalNotDecayed = !personalEntity.isRemoved() && personalHealth == initialPersonalHealth;

      if (linkbookDecayed && personalNotDecayed) {
        helper.succeed();
      } else {
        helper.fail("Linkbook decayed=" + linkbookDecayed + ", Personal not decayed=" + personalNotDecayed);
      }
    });
  }

  /**
   * Tests that book entity dies instantly when touching fluid.
   */
  public static void runBookDiesInFluidTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(BlockPos.ZERO);

    // Place water at origin
    helper.setBlock(BlockPos.ZERO, Blocks.WATER.defaultBlockState());

    double x = origin.getX() + 0.5;
    double y = origin.getY() + 0.5;
    double z = origin.getZ() + 0.5;

    helper.runAtTickTime(5, () -> {
      LinkbookEntity entity = new LinkbookEntity(level, x, y, z);
      entity.setBookItem(new ItemStack(ModItems.LINKBOOK.get()));
      level.addFreshEntity(entity);

      // Force a tick to process the fluid damage
      helper.runAtTickTime(7, () -> {
        if (entity.isRemoved() || entity.getHealth() <= 0) {
          helper.succeed();
        } else {
          helper.fail("Book entity did not die in fluid, health=" + entity.getHealth());
        }
      });
    });
  }

  /**
   * Tests that book entity dies when damaged 5hp (book has 5hp).
   */
  public static void runBookDiesAt5DamageTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(BlockPos.ZERO);
    double x = origin.getX() + 0.5;
    double y = origin.getY() + 2.0;
    double z = origin.getZ() + 0.5;

    LinkbookEntity entity = new LinkbookEntity(level, x, y, z);
    entity.setBookItem(new ItemStack(ModItems.LINKBOOK.get()));
    level.addFreshEntity(entity);

    float initialHealth = entity.getHealth();
    if (initialHealth != 5.0f) {
      helper.fail("Expected book to have 5hp, but has " + initialHealth);
      return;
    }

    // Damage the book by 5hp
    entity.damageBook(5.0f);

    helper.runAtTickTime(3, () -> {
      if (entity.getHealth() <= 0 || entity.isRemoved()) {
        helper.succeed();
      } else {
        helper.fail("Book did not die after 5 damage, health=" + entity.getHealth());
      }
    });
  }

  /**
   * Tests that when books die they drop an empty page and leather.
   */
  public static void runBookDropsPageAndLeatherOnDeathTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(BlockPos.ZERO);
    double x = origin.getX() + 0.5;
    double y = origin.getY() + 2.0;
    double z = origin.getZ() + 0.5;

    LinkbookEntity entity = new LinkbookEntity(level, x, y, z);
    entity.setBookItem(new ItemStack(ModItems.LINKBOOK.get()));
    level.addFreshEntity(entity);

    // Kill the book
    entity.damageBook(10.0f);

    helper.runAtTickTime(5, () -> {
      AABB box = new AABB(x - 2.0, origin.getY(), z - 2.0, x + 2.0, origin.getY() + 4.0, z + 2.0);
      boolean foundPage = false;
      boolean foundLeather = false;

      for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, box)) {
        ItemStack stack = drop.getItem();
        if (stack.getItem() == ModItems.PAGE.get()) {
          foundPage = true;
        } else if (stack.getItem() == Items.LEATHER) {
          foundLeather = true;
        }
      }

      if (foundPage && foundLeather) {
        helper.succeed();
      } else {
        helper.fail("Expected page=" + foundPage + " and leather=" + foundLeather + " drops");
      }
    });
  }

  private static void assertCreatesLinkbookEntity(ItemStack stack, ItemEntity dummy, String itemName) {
    net.minecraft.world.entity.Entity created;
    if (stack.getItem() instanceof art.arcane.mystcraft.item.LinkbookItem linkbookItem) {
      created = linkbookItem.createEntity(dummy.getLevel(), dummy, stack);
    } else if (stack.getItem() instanceof art.arcane.mystcraft.item.AgebookItem agebookItem) {
      created = agebookItem.createEntity(dummy.getLevel(), dummy, stack);
    } else {
      throw new IllegalStateException("Expected linkbook-like item: " + itemName);
    }
    if (!(created instanceof LinkbookEntity)) {
      throw new IllegalStateException("Expected LinkbookEntity for " + itemName + ", got: " + created.getClass().getName());
    }
  }

  /**
   * Tests that books can be placed in a vanilla lectern and the book is stored.
   */
  public static void runLecternBookPlacementTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(BlockPos.ZERO);

    // Place a vanilla lectern
    helper.setBlock(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.LECTERN.defaultBlockState());

    helper.runAtTickTime(3, () -> {
      net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(origin);
      if (!(be instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern)) {
        helper.fail("Lectern block entity not found");
        return;
      }

      // Create a linkbook and place it on the lectern
      ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());

      // Simulate placing book: set book and page count directly
      try {
        // Use reflection to set the book since it's normally done through player interaction
        java.lang.reflect.Field bookField = net.minecraft.world.level.block.entity.LecternBlockEntity.class.getDeclaredField("book");
        bookField.setAccessible(true);
        bookField.set(lectern, linkbook.copy());

        java.lang.reflect.Field pageCountField = net.minecraft.world.level.block.entity.LecternBlockEntity.class.getDeclaredField("pageCount");
        pageCountField.setAccessible(true);
        pageCountField.set(lectern, 1);

        lectern.setChanged();
      } catch (Exception e) {
        helper.fail("Failed to set book on lectern: " + e.getMessage());
        return;
      }

      // Verify the book is on the lectern
      ItemStack bookOnLectern = lectern.getBook();
      if (bookOnLectern.isEmpty()) {
        helper.fail("Book was not stored on lectern");
        return;
      }

      if (!art.arcane.mystcraft.util.MystcraftLecternHelper.isMystcraftBook(bookOnLectern)) {
        helper.fail("Book on lectern is not a Mystcraft book");
        return;
      }

      helper.succeed();
    });
  }

  /**
   * Tests that table blocks can be placed and have their block entities created.
   * Note: Writing desk is a multi-block structure that requires special placement,
   * so we only test single-block tables (ink mixer and book binder).
   */
  public static void runTableBlockEntitiesTest(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();

    // Place blocks at different positions
    BlockPos inkMixerPos = BlockPos.ZERO;
    BlockPos bookBinderPos = new BlockPos(2, 0, 0);

    helper.setBlock(inkMixerPos, art.arcane.mystcraft.registry.ModBlocks.INK_MIXER.get().defaultBlockState());
    helper.setBlock(bookBinderPos, art.arcane.mystcraft.registry.ModBlocks.BOOK_BINDER.get().defaultBlockState());

    helper.runAtTickTime(5, () -> {
      BlockPos absInkMixer = helper.absolutePos(inkMixerPos);
      BlockPos absBookBinder = helper.absolutePos(bookBinderPos);

      // Check ink mixer block entity
      net.minecraft.world.level.block.entity.BlockEntity inkMixerBE = level.getBlockEntity(absInkMixer);
      if (!(inkMixerBE instanceof art.arcane.mystcraft.blockentity.InkMixerBlockEntity)) {
        helper.fail("Ink mixer block entity not created");
        return;
      }

      // Check book binder block entity
      net.minecraft.world.level.block.entity.BlockEntity bookBinderBE = level.getBlockEntity(absBookBinder);
      if (!(bookBinderBE instanceof art.arcane.mystcraft.blockentity.BookBinderBlockEntity)) {
        helper.fail("Book binder block entity not created");
        return;
      }

      helper.succeed();
    });
  }
}
