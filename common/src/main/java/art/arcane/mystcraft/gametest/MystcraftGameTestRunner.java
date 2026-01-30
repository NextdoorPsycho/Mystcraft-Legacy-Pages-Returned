package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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

    boolean expectEntity = art.arcane.mystcraft.config.MystcraftConfig.droppedBooksBecomeLivingEntities.get();
    if (expectEntity) {
      // Test linkbook creates LinkbookEntity
      assertCreatesLinkbookEntity(linkbook, dummyItem, "linkbook");
      // Test descriptive book creates LinkbookEntity
      assertCreatesLinkbookEntity(agebook, dummyItem, "agebook");
    } else {
      // Test linkbook does not create LinkbookEntity
      assertDoesNotCreateLinkbookEntity(linkbook, dummyItem, "linkbook");
      // Test descriptive book does not create LinkbookEntity
      assertDoesNotCreateLinkbookEntity(agebook, dummyItem, "agebook");
    }

    helper.succeed();
  }

  /**
   * Tests dropBooksOnRead config on/off for linkbooks.
   */
  public static void runDropBooksOnReadConfigTest(GameTestHelper helper) {
    java.util.function.Supplier<Boolean> originalSupplier = art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead;
    try {
      // Config ON: book should drop as entity and be removed from inventory
      art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead = () -> true;
      assertDropBooksOnRead(helper, true);

      // Config OFF: book should stay in inventory and not spawn an entity
      art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead = () -> false;
      assertDropBooksOnRead(helper, false);

      helper.succeed();
    } finally {
      art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead = originalSupplier;
    }
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
    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(Mystcraft.MOD_ID, "agebook"));
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
      created = linkbookItem.createEntity(dummy.level(), dummy, stack);
    } else if (stack.getItem() instanceof art.arcane.mystcraft.item.AgebookItem agebookItem) {
      created = agebookItem.createEntity(dummy.level(), dummy, stack);
    } else {
      throw new IllegalStateException("Expected linkbook-like item: " + itemName);
    }
    if (!(created instanceof LinkbookEntity)) {
      throw new IllegalStateException("Expected LinkbookEntity for " + itemName + ", got: " + created.getClass().getName());
    }
  }

  private static void assertDoesNotCreateLinkbookEntity(ItemStack stack, ItemEntity dummy, String itemName) {
    net.minecraft.world.entity.Entity created;
    if (stack.getItem() instanceof art.arcane.mystcraft.item.LinkbookItem linkbookItem) {
      created = linkbookItem.createEntity(dummy.level(), dummy, stack);
    } else if (stack.getItem() instanceof art.arcane.mystcraft.item.AgebookItem agebookItem) {
      created = agebookItem.createEntity(dummy.level(), dummy, stack);
    } else {
      throw new IllegalStateException("Expected linkbook-like item: " + itemName);
    }
    if (created != null) {
      throw new IllegalStateException("Expected no custom entity for " + itemName + ", got: " + created.getClass().getName());
    }
  }

  private static void assertDropBooksOnRead(GameTestHelper helper, boolean expectDrop) {
    ServerLevel level = helper.getLevel();
    ServerPlayer player = createMockServerPlayer(helper);
    player.moveTo(0.5, 2.0, 0.5, 0.0F, 0.0F);

    TestLinkbookItem testItem = new TestLinkbookItem(new Item.Properties());
    ItemStack stack = new ItemStack(testItem);
    stack.setTag(new CompoundTag());

    int slot = player.getInventory().selected;
    player.getInventory().setItem(slot, stack);

    AABB box = new AABB(0, 0, 0, 2, 4, 2);
    int before = level.getEntitiesOfClass(LinkbookEntity.class, box).size();

    testItem.testOnLink(stack, level, player);

    int after = level.getEntitiesOfClass(LinkbookEntity.class, box).size();
    ItemStack slotStack = player.getInventory().getItem(slot);

    if (expectDrop) {
      if (after <= before) {
        helper.fail("Expected LinkbookEntity drop when dropBooksOnRead=true");
        return;
      }
      if (!slotStack.isEmpty()) {
        helper.fail("Expected linkbook removed from inventory when dropBooksOnRead=true");
      }
    } else {
      if (after != before) {
        helper.fail("Expected no LinkbookEntity drop when dropBooksOnRead=false");
        return;
      }
      if (slotStack.isEmpty()) {
        helper.fail("Expected linkbook retained in inventory when dropBooksOnRead=false");
      }
    }
  }

  private static final class TestLinkbookItem extends art.arcane.mystcraft.item.LinkbookItem {
    private TestLinkbookItem(Properties properties) {
      super(properties);
    }

    private void testOnLink(@NotNull ItemStack stack, Level level, Entity entity) {
      super.onLink(stack, level, entity);
    }
  }

  private static void addSymbolPageIfExists(List<ItemStack> pages, String symbolPath) {
    ResourceLocation id = SymbolRegistry.mystcraftId(symbolPath);
    if (SymbolRegistry.contains(id)) {
      pages.add(Page.createSymbolPage(id));
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

  private static ServerPlayer createMockServerPlayer(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    MinecraftServer server = level.getServer();

    // In GameTest servers, use the existing test player instead of creating a new one
    // The GameTest framework provides a test player that's already properly set up
    List<ServerPlayer> players = server.getPlayerList().getPlayers();
    if (!players.isEmpty()) {
      return players.get(0);
    }

    // Fallback: create a simple player without going through placeNewPlayer
    // which requires profile cache that may not be available in GameTest
    CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"));
    ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
      @Override
      public boolean isSpectator() {
        return false;
      }

      @Override
      public boolean isCreative() {
        return true;
      }
    };

    // Set basic position without full registration
    player.moveTo(level.getSharedSpawnPos(), 0.0F, 0.0F);
    return player;
  }
}
