package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class MystcraftGameTestRunner {

  private MystcraftGameTestRunner() {
  }

  public static void runRandomBookDimensionTest(net.minecraft.gametest.framework.GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    MinecraftServer server = level.getServer();
    ServerPlayer player = createMockServerPlayer(helper);

    AtomicInteger createdCount = new AtomicInteger(0);
    int iterations = 5;
    long stepTicks = 40;
    long startTick = 5;
    long cooldownTicks = 120;

    for (int i = 0; i < iterations; i++) {
      long tick = startTick + (i * stepTicks);
      helper.runAtTickTime(tick, () -> {
        try {
          runRandomBookOnce(helper, server, level, player);
          createdCount.incrementAndGet();
        } catch (Exception e) {
          helper.fail("Random book dimension test failed: " + e.getMessage());
        }
      });
    }

    helper.runAtTickTime(startTick + (iterations * stepTicks) + cooldownTicks, () -> {
      if (createdCount.get() == iterations) {
        helper.succeed();
      } else {
        helper.fail("Expected " + iterations + " ages, created " + createdCount.get());
      }
    });
  }

  public static void runPresetBookDimensionTest(net.minecraft.gametest.framework.GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    MinecraftServer server = level.getServer();
    ServerPlayer player = createMockServerPlayer(helper);

    Mystcraft.LOGGER.info("GameTest: starting preset cave book test");
    AtomicBoolean created = new AtomicBoolean(false);
    long startTick = 5;
    long cooldownTicks = 140;

    helper.runAtTickTime(startTick, () -> {
      try {
        runPresetBookOnce(helper, server, level, player);
        created.set(true);
      } catch (Exception e) {
        Mystcraft.LOGGER.error("GameTest preset cave book failed", e);
        helper.fail("Preset book dimension test failed: " + e.getMessage());
      }
    });

    helper.runAtTickTime(startTick + cooldownTicks, () -> {
      if (created.get()) {
        helper.succeed();
      } else {
        helper.fail("Preset book did not create an age dimension");
      }
    });
  }

  public static void runBookDropEntityTest(net.minecraft.gametest.framework.GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
    ItemStack unlinked = new ItemStack(ModItems.LINKBOOK_UNLINKED.get());
    ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());

    ItemEntity dummy = new ItemEntity(level, 0.5, 2.0, 0.5, linkbook.copy());

    assertCreatesLinkbookEntity(linkbook, dummy);
    assertCreatesLinkbookEntity(unlinked, dummy);
    assertCreatesLinkbookEntity(agebook, dummy);

    helper.succeed();
  }

  public static void runLinkbookEntityDecayTest(net.minecraft.gametest.framework.GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(BlockPos.ZERO);
    double x = origin.getX() + 0.5;
    double y = origin.getY() + 2.0;
    double z = origin.getZ() + 0.5;

    LinkbookEntity.setDecayMultiplierForTests(6.0f);
    LinkbookEntity entity = new LinkbookEntity(level, x, y, z);
    entity.setBookItem(new ItemStack(ModItems.LINKBOOK.get()));
    level.addFreshEntity(entity);

    int checkTick = 140;
    helper.runAtTickTime(checkTick, () -> {
      LinkbookEntity.setDecayMultiplierForTests(1.0f);
      if (!entity.isRemoved() && entity.getHealth() > 0) {
        helper.fail("Linkbook entity did not decay within 30 seconds");
        return;
      }

      AABB box = new AABB(x - 2.0, origin.getY(), z - 2.0, x + 2.0, origin.getY() + 3.0, z + 2.0);
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
        helper.fail("Expected page and leather drops from decayed linkbook entity");
      }
    });
  }

  private static void assertCreatesLinkbookEntity(ItemStack stack, ItemEntity dummy) {
    net.minecraft.world.entity.Entity created;
    if (stack.getItem() instanceof art.arcane.mystcraft.item.LinkbookItem linkbookItem) {
      created = linkbookItem.createEntity(dummy.level(), dummy, stack);
    } else if (stack.getItem() instanceof art.arcane.mystcraft.item.LinkbookUnlinkedItem unlinkedItem) {
      created = unlinkedItem.createEntity(dummy.level(), dummy, stack);
    } else if (stack.getItem() instanceof art.arcane.mystcraft.item.AgebookItem agebookItem) {
      created = agebookItem.createEntity(dummy.level(), dummy, stack);
    } else {
      throw new IllegalStateException("Expected linkbook-like item for custom entity: " + stack.getItem());
    }
    if (!(created instanceof LinkbookEntity)) {
      throw new IllegalStateException("Expected LinkbookEntity for item: " + stack.getItem());
    }
  }

  private static void runRandomBookOnce(net.minecraft.gametest.framework.GameTestHelper helper,
                                        MinecraftServer server,
                                        ServerLevel level,
                                        ServerPlayer player) {
    StringBuilder steps = new StringBuilder();
    steps.append("start");
    CommandSourceStack source = player.createCommandSourceStack().withPermission(2);
    int result = server.getCommands().performPrefixedCommand(source, "mystcraft give randombook 20");
    if (result <= 0) {
      throw new IllegalStateException("Command failed: mystcraft give randombook 20 (steps: " + steps + ")");
    }
    steps.append(" -> command");

    ItemStack agebook = findUnlinkedAgebook(player);
    if (agebook.isEmpty()) {
      throw new IllegalStateException("No unlinked agebook found in player inventory (steps: " + steps + ")");
    }
    steps.append(" -> book");

    AgebookItem bookItem = (AgebookItem) agebook.getItem();
    bookItem.activate(agebook, level, player);
    steps.append(" -> activated");

    Integer uid = LinkOptions.getDimensionUID(agebook.getTag());
    if (uid == null) {
      throw new IllegalStateException("Agebook did not receive a dimension UID (steps: " + steps + ")");
    }
    steps.append(" -> uid=").append(uid);

    AgeManager ageManager = AgeManager.get(level);
    ResourceLocation dimId = ageManager.getDimension(uid);
    if (dimId == null) {
      throw new IllegalStateException("AgeManager does not contain dimension for UID " + uid + " (steps: " + steps + ")");
    }
    steps.append(" -> registered=").append(dimId);

    ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(server, uid);
    if (ageLevel == null) {
      throw new IllegalStateException("Failed to load age dimension for UID " + uid + " (steps: " + steps + ")");
    }
    steps.append(" -> loaded");

    BlockPos spawn = AgeDimensionFactory.getAgeSpawn(ageLevel);
    ageLevel.getChunk(spawn);
    steps.append(" -> chunk");

    Mystcraft.LOGGER.info("GameTest: opened age dimension uid={} id={} steps={}", uid, dimId, steps);
  }

  private static void runPresetBookOnce(net.minecraft.gametest.framework.GameTestHelper helper,
                                        MinecraftServer server,
                                        ServerLevel level,
                                        ServerPlayer player) {
    StringBuilder steps = new StringBuilder();
    steps.append("start");

    Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(Mystcraft.MOD_ID, "agebook"));
    if (item == Items.AIR) {
      throw new IllegalStateException("Agebook item not registered");
    }
    if (!(item instanceof AgebookItem)) {
      throw new IllegalStateException("Registered agebook item is not an AgebookItem: " + item.getClass().getName());
    }
    ItemStack agebook = new ItemStack(item);
    List<ItemStack> pages = createPresetCavePages(helper);
    AgebookItem.create(agebook, player, pages, "Preset Cave Test");
    steps.append(" -> book");

    AgebookItem bookItem = (AgebookItem) agebook.getItem();
    bookItem.activate(agebook, level, player);
    steps.append(" -> activated");

    Integer uid = LinkOptions.getDimensionUID(agebook.getTag());
    if (uid == null) {
      throw new IllegalStateException("Preset agebook did not receive a dimension UID (steps: " + steps + ")");
    }
    steps.append(" -> uid=").append(uid);

    AgeManager ageManager = AgeManager.get(level);
    ResourceLocation dimId = ageManager.getDimension(uid);
    if (dimId == null) {
      throw new IllegalStateException("AgeManager does not contain dimension for UID " + uid + " (steps: " + steps + ")");
    }
    steps.append(" -> registered=").append(dimId);

    ServerLevel ageLevel = AgeDimensionFactory.getOrCreateAgeDimension(server, uid);
    if (ageLevel == null) {
      throw new IllegalStateException("Failed to load age dimension for UID " + uid + " (steps: " + steps + ")");
    }
    steps.append(" -> loaded");

    BlockPos spawn = AgeDimensionFactory.getAgeSpawn(ageLevel);
    int chunkX = spawn.getX() >> 4;
    int chunkZ = spawn.getZ() >> 4;
    int radius = 2;
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dz = -radius; dz <= radius; dz++) {
        ageLevel.getChunk(chunkX + dx, chunkZ + dz);
      }
    }
    steps.append(" -> chunks");

    Mystcraft.LOGGER.info("GameTest: opened preset age dimension uid={} id={} steps={}", uid, dimId, steps);
  }

  private static List<ItemStack> createPresetCavePages(net.minecraft.gametest.framework.GameTestHelper helper) {
    List<ItemStack> pages = new ArrayList<>();
    pages.add(Page.createLinkPage());

    addSymbolPage(helper, pages, "terrain_cave");
    addSymbolPage(helper, pages, "biome_dripstone_caves");
    addSymbolPage(helper, pages, "biome_lush_caves");
    addSymbolPage(helper, pages, "dripstone_caves");
    addSymbolPage(helper, pages, "lush_caves");

    Mystcraft.LOGGER.info("GameTest: preset cave book pages={}", pages.size());
    return pages;
  }

  private static void addSymbolPage(net.minecraft.gametest.framework.GameTestHelper helper,
                                    List<ItemStack> pages,
                                    String symbolPath) {
    ResourceLocation id = SymbolRegistry.mystcraftId(symbolPath);
    if (!SymbolRegistry.contains(id)) {
      Mystcraft.LOGGER.error("GameTest: preset symbol not registered: {}", id);
      helper.fail("Preset symbol not registered: " + id);
    }
    pages.add(Page.createSymbolPage(id));
  }

  private static ServerPlayer createMockServerPlayer(net.minecraft.gametest.framework.GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"));
    ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
      @Override
      public boolean isSpectator() {
        return false;
      }

      @Override
      public boolean isCreative() {
        return true;
      }
    };

    Connection connection = new Connection(PacketFlow.SERVERBOUND);
    EmbeddedChannel channel = new EmbeddedChannel(connection);
    channel.attr(Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL).set(ConnectionProtocol.PLAY.codec(PacketFlow.SERVERBOUND));
    level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
    return player;
  }

  private static ItemStack findUnlinkedAgebook(ServerPlayer player) {
    for (ItemStack stack : player.getInventory().items) {
      if (stack.getItem() instanceof AgebookItem) {
        Integer uid = LinkOptions.getDimensionUID(stack.getTag());
        if (uid == null) {
          return stack;
        }
      }
    }
    for (ItemStack stack : player.getInventory().offhand) {
      if (stack.getItem() instanceof AgebookItem) {
        Integer uid = LinkOptions.getDimensionUID(stack.getTag());
        if (uid == null) {
          return stack;
        }
      }
    }
    return ItemStack.EMPTY;
  }
}
