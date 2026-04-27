package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkFlags;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import art.arcane.mystcraft.event.AgeReturnHandler;
import art.arcane.mystcraft.event.PersonalPocketEscapeHandler;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.CommonListenerCookieCompat;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.ServerPlayerTeleport;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.AgeReturnData;
import art.arcane.mystcraft.world.PersonalPocketData;
import art.arcane.mystcraft.world.PersonalPocketDimension;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class MystcraftGameTestRunner {

  private MystcraftGameTestRunner() {
  }

  public static void runCoreGameplayContentLoaded(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.CORE)) {
      return;
    }
    MystcraftGameTestAssertions.assertCoreGameplayContentLoaded();
    MystcraftGameTestAssertions.assertRegisteredObjectsReachable();
    helper.succeed();
  }

  public static void runPersonalBookAlwaysStaysCarriedTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    Supplier<Boolean> originalDrop = MystcraftConfig.dropBooksOnRead;
    try {
      MystcraftConfig.dropBooksOnRead = () -> true;
      ServerLevel level = helper.getLevel();
      PersonalLinkBookItem personalBookItem = (PersonalLinkBookItem) ModItems.PERSONAL_LINK_BOOK.get();
      ItemStack personalBook = new ItemStack(personalBookItem);
      ItemEntity dummy = new ItemEntity(level, 0.5, 2.0, 0.5, personalBook.copy());

      if (personalBookItem.dropItemOnLink(personalBook)) {
        helper.fail("Personal book opted into drop-on-link while dropBooksOnRead=true");
        return;
      }
      if (personalBookItem.hasCustomEntity(personalBook)) {
        helper.fail("Personal book would become a dropped LinkbookEntity");
        return;
      }
      if (personalBookItem.createEntity(level, dummy, personalBook) != null) {
        helper.fail("Personal book created a custom dropped entity");
        return;
      }
      if (personalBook.isEmpty() || personalBook.getCount() != 1) {
        helper.fail("Personal book stack was consumed by carry checks");
        return;
      }

      CompoundTag staleLink = returnLink(0, new BlockPos(12, 65, 12), 90.0F);
      LinkOptions.setDisplayName(staleLink, "Stale Personal Pocket");
      staleLink.putFloat("MaxHealth", 10.0F);
      ItemStackNbt.setTag(personalBook, staleLink);
      personalBookItem.inventoryTick(personalBook, level, dummy, 0, false);
      if (hasStoredLinkData(personalBook)) {
        helper.fail("Personal book kept stored link NBT after inventory tick: " + ItemStackNbt.getTag(personalBook));
        return;
      }

      CompoundTag staleLinkAgain = returnLink(0, new BlockPos(12, 65, 12), 90.0F);
      LinkOptions.setDisplayName(staleLinkAgain, "Stale Personal Pocket");
      staleLinkAgain.putFloat("MaxHealth", 10.0F);
      ItemStackNbt.setTag(personalBook, staleLinkAgain);
      personalBookItem.validate(level, personalBook, dummy);
      if (hasStoredLinkData(personalBook)) {
        helper.fail("Personal book validate wrote or kept stored link NBT: " + ItemStackNbt.getTag(personalBook));
        return;
      }

      personalBookItem.setDisplayName(personalBook, "Should Not Stick");
      if (hasStoredLinkData(personalBook)) {
        helper.fail("Personal book accepted editable linkbook data: " + ItemStackNbt.getTag(personalBook));
        return;
      }

      helper.succeed();
    } finally {
      MystcraftConfig.dropBooksOnRead = originalDrop;
    }
  }

  public static void runNormalLinkbookDropOnReadFollowsFlagsTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    Supplier<Boolean> originalDrop = MystcraftConfig.dropBooksOnRead;
    try {
      MystcraftConfig.dropBooksOnRead = () -> true;
      assertDropBooksOnRead(helper, true, false);
      assertDropBooksOnRead(helper, false, true);

      MystcraftConfig.dropBooksOnRead = () -> false;
      assertDropBooksOnRead(helper, false, false);
      helper.succeed();
    } finally {
      MystcraftConfig.dropBooksOnRead = originalDrop;
    }
  }

  public static void runPersonalProxyStateLifecycleTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    ServerLevel level = helper.getLevel();
    UUID ownerId = UUID.randomUUID();
    CompoundTag returnLink = returnLink(0, new BlockPos(4, 65, 6), 45.0F);
    GameProfile ownerProfile = new GameProfile(ownerId, "proxy-test");
    ownerProfile.getProperties().put("textures", new Property("textures", "skin-value", "skin-signature"));

    PersonalPocketProxyEntity proxy = new PersonalPocketProxyEntity(level);
    proxy.setOwner(ownerProfile, returnLink);
    proxy.moveTo(4.5, 65.0, 6.5, 45.0F, 10.0F);

    if (!ownerId.equals(proxy.getOwnerId())) {
      helper.fail("Proxy did not store owner UUID");
      return;
    }
    if (!"proxy-test".equals(proxy.getOwnerName())) {
      helper.fail("Proxy did not store owner name");
      return;
    }
    GameProfile proxyProfile = proxy.getOwnerProfile();
    if (proxyProfile == null || proxyProfile.getProperties().get("textures").isEmpty()) {
      helper.fail("Proxy did not store owner skin profile data");
      return;
    }
    if (!new BlockPos(4, 65, 6).equals(LinkOptions.getSpawn(proxy.getReturnLink()))) {
      helper.fail("Proxy did not retain return link position");
      return;
    }

    CompoundTag savedEntity = new CompoundTag();
    proxy.saveWithoutId(savedEntity);
    PersonalPocketProxyEntity loadedProxy = new PersonalPocketProxyEntity(level);
    loadedProxy.load(savedEntity);
    if (!ownerId.equals(loadedProxy.getOwnerId())) {
      helper.fail("Loaded proxy did not restore owner UUID");
      return;
    }
    GameProfile loadedProfile = loadedProxy.getOwnerProfile();
    if (loadedProfile == null || loadedProfile.getProperties().get("textures").isEmpty()) {
      helper.fail("Loaded proxy did not restore owner skin profile data");
      return;
    }
    if (!new BlockPos(4, 65, 6).equals(LinkOptions.getSpawn(loadedProxy.getReturnLink()))) {
      helper.fail("Loaded proxy did not restore return link");
      return;
    }

    CompoundTag dataTag = new CompoundTag();
    art.arcane.mystcraft.world.PersonalPocketData data = new art.arcane.mystcraft.world.PersonalPocketData();
    data.setReturnLink(ownerId, returnLink);
    data.setActiveProxy(ownerId, new art.arcane.mystcraft.world.PersonalPocketData.ProxyState(
        "proxy-test", 0, new BlockPos(4, 65, 6), 45.0F, 10.0F, loadedProxy.getUUID(), returnLink));
    data.save(dataTag);
    art.arcane.mystcraft.world.PersonalPocketData loadedData =
        art.arcane.mystcraft.world.PersonalPocketData.load(dataTag);
    art.arcane.mystcraft.world.PersonalPocketData.ProxyState loadedState = loadedData.getActiveProxy(ownerId);
    if (loadedState == null || !loadedProxy.getUUID().equals(loadedState.proxyId())) {
      helper.fail("Personal pocket data did not save/load active proxy state");
      return;
    }

    if (!loadedProxy.hurt(level.damageSources().generic(), 1.0F)) {
      helper.fail("Proxy damage was not handled");
      return;
    }
    if (!loadedProxy.isRemoved()) {
      helper.fail("Proxy was not removed after damage");
      return;
    }
    helper.succeed();
  }

  public static void runPersonalPocketDamageAndDeathReturnTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    Supplier<Boolean> originalEnable = MystcraftConfig.enablePersonalLinkBooks;
    try {
      MystcraftConfig.enablePersonalLinkBooks = () -> true;
      ServerLevel originLevel = helper.getLevel();
      MinecraftServer server = originLevel.getServer();
      ServerPlayer player = createMockServerPlayer(helper);
      if (player == null) {
        return;
      }
      player.getInventory().clearContent();
      PersonalPocketEscapeHandler.removeActiveProxy(server, player.getUUID());

      BlockPos originPos = helper.absolutePos(new BlockPos(4, 2, 4));
      ServerPlayerTeleport.teleport(player, originLevel, originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5, 135.0F, 8.0F);
      player.setHealth(player.getMaxHealth());
      player.setRemainingFireTicks(0);

      CompoundTag returnLink = returnLink(LinkingManager.getDimensionUID(originLevel), originPos, 135.0F);
      ServerLevel pocketLevel = PersonalPocketDimension.getOrCreate(server, player.getUUID());
      if (pocketLevel == null) {
        helper.fail("Personal pocket was not created");
        return;
      }
      if (!PersonalPocketEscapeHandler.spawnOrReplaceProxy(player, returnLink)) {
        helper.fail("Personal pocket proxy was not spawned before entry");
        return;
      }

      BlockPos pocketSpawn = PersonalPocketDimension.getPocketSpawn();
      ServerPlayerTeleport.teleport(
          player,
          pocketLevel,
          pocketSpawn.getX() + 0.5,
          pocketSpawn.getY(),
          pocketSpawn.getZ() + 0.5,
          20.0F,
          4.0F
      );
      if (!(player.level() instanceof ServerLevel currentLevel) || !PersonalPocketDimension.isPersonalPocket(currentLevel)) {
        helper.fail("Player was not inside their personal pocket after entry setup");
        return;
      }
      if (PersonalPocketData.get(server).getActiveProxy(player.getUUID()) == null) {
        helper.fail("Personal pocket entry did not keep active proxy state");
        return;
      }

      PersonalPocketData.ProxyState firstProxyState = PersonalPocketData.get(server).getActiveProxy(player.getUUID());
      Entity firstProxyEntity = firstProxyState == null ? null : originLevel.getEntity(firstProxyState.proxyId());
      if (!(firstProxyEntity instanceof PersonalPocketProxyEntity firstProxy)) {
        helper.fail("Personal pocket entry did not leave a damageable body proxy");
        return;
      }
      float healthBeforeProxyHit = player.getHealth();
      if (!firstProxy.hurt(originLevel.damageSources().generic(), 1.0F)) {
        helper.fail("Body proxy damage was not handled");
        return;
      }
      if (player.level() != originLevel) {
        helper.fail("Body proxy damage did not rip the player back to their body dimension");
        return;
      }
      if (!player.blockPosition().equals(originPos)) {
        helper.fail("Body proxy damage returned to " + player.blockPosition() + " instead of body " + originPos);
        return;
      }
      if (player.getHealth() != healthBeforeProxyHit) {
        helper.fail("Body proxy damage leaked damage to the real player");
        return;
      }
      if (PersonalPocketData.get(server).getActiveProxy(player.getUUID()) != null) {
        helper.fail("Body proxy damage did not clear active proxy state");
        return;
      }

      ServerPlayerTeleport.teleport(player, originLevel, originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5, 135.0F, 8.0F);
      if (!PersonalPocketEscapeHandler.spawnOrReplaceProxy(player, returnLink)) {
        helper.fail("Personal pocket proxy was not respawned for the death path");
        return;
      }
      ServerPlayerTeleport.teleport(
          player,
          pocketLevel,
          pocketSpawn.getX() + 0.5,
          pocketSpawn.getY(),
          pocketSpawn.getZ() + 0.5,
          20.0F,
          4.0F
      );
      clearDamageImmunity(player);

      float maxHealth = player.getMaxHealth();
      if (!player.hurt(pocketLevel.damageSources().generic(), 2.0F)) {
        helper.fail("Non-lethal personal pocket damage was not applied; mode="
            + player.gameMode.getGameModeForPlayer()
            + ", invulnerable=" + player.isInvulnerable()
            + ", abilityInvulnerable=" + player.getAbilities().invulnerable
            + ", invulnerableTime=" + player.invulnerableTime
            + ", health=" + player.getHealth()
            + ", level=" + player.level().dimension().location());
        return;
      }
      if (player.level() != pocketLevel) {
        helper.fail("Non-lethal damage should not rip the player out of the pocket");
        return;
      }
      if (PersonalPocketData.get(server).getActiveProxy(player.getUUID()) == null) {
        helper.fail("Non-lethal damage removed active proxy state");
        return;
      }
      if (player.getHealth() >= maxHealth) {
        helper.fail("Non-lethal damage did not reduce player health");
        return;
      }

      player.setHealth(1.0F);
      if (!PersonalPocketEscapeHandler.handleDeath(player, pocketLevel.damageSources().generic())) {
        helper.fail("Personal pocket death was not intercepted");
        return;
      }
      if (player.level() != originLevel) {
        helper.fail("Personal pocket death did not return player to their body dimension");
        return;
      }
      if (!player.blockPosition().equals(originPos)) {
        helper.fail("Personal pocket death returned to " + player.blockPosition() + " instead of body " + originPos);
        return;
      }
      if (player.getHealth() != player.getMaxHealth()) {
        helper.fail("Personal pocket death did not restore health");
        return;
      }
      if (PersonalPocketData.get(server).getActiveProxy(player.getUUID()) != null) {
        helper.fail("Personal pocket death did not clear active proxy state");
        return;
      }
      helper.succeed();
    } finally {
      MystcraftConfig.enablePersonalLinkBooks = originalEnable;
    }
  }

  public static void runLinkbookEntityDamageAndDropsTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    ServerLevel level = helper.getLevel();
    BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
    LinkbookEntity entity = new LinkbookEntity(level, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
    entity.setBookItem(new ItemStack(ModItems.LINKBOOK.get()));
    level.addFreshEntity(entity);

    if (entity.getHealth() != 5.0F) {
      helper.fail("Expected dropped book entity to start at 5hp, got " + entity.getHealth());
      return;
    }

    entity.damageBook(10.0F);
    helper.runAtTickTime(5, () -> {
      AABB box = new AABB(origin).inflate(3.0D);
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
      if (!foundPage || !foundLeather) {
        helper.fail("Expected destroyed book to drop page and leather, page=" + foundPage + ", leather=" + foundLeather);
        return;
      }
      helper.succeed();
    });
  }

  public static void runFolderWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_CRAFTING)) {
      return;
    }
    ItemStack folder = new ItemStack(ModItems.FOLDER.get());
    ItemStack blank = Page.createPage();
    ItemStack plains = Page.createSymbolPage(symbolId("biome_plains"));

    if (!FolderItem.addPage(folder, blank) || !FolderItem.addPage(folder, plains)) {
      helper.fail("Folder rejected valid pages");
      return;
    }
    if (FolderItem.getPageCount(folder) != 2 || !Page.isBlank(FolderItem.getPageAt(folder, 0))) {
      helper.fail("Folder did not keep ordered pages");
      return;
    }
    if (!FolderItem.writeSymbol(folder, symbolId("terrain_flat"))) {
      helper.fail("Folder did not write the first blank page");
      return;
    }
    if (!symbolId("terrain_flat").equals(Page.getSymbol(FolderItem.getPageAt(folder, 0)))) {
      helper.fail("Folder wrote the wrong symbol");
      return;
    }
    for (int i = FolderItem.getPageCount(folder); i < FolderItem.MAX_PAGES; i++) {
      if (!FolderItem.addPage(folder, Page.createPage())) {
        helper.fail("Folder filled before capacity");
        return;
      }
    }
    if (FolderItem.addPage(folder, Page.createPage())) {
      helper.fail("Folder accepted a page past capacity");
      return;
    }
    List<ItemStack> extracted = FolderItem.extractAllPages(folder);
    if (extracted.size() != FolderItem.MAX_PAGES || !FolderItem.isEmpty(folder)) {
      helper.fail("Folder did not extract and clear its pages");
      return;
    }
    helper.succeed();
  }

  public static void runPortfolioWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_CRAFTING)) {
      return;
    }
    ItemStack portfolio = new ItemStack(ModItems.PORTFOLIO.get());
    ItemStack linkPanel = Page.createLinkPage();
    ItemStack flat = Page.createSymbolPage(symbolId("terrain_flat"));
    ItemStack plains = Page.createSymbolPage(symbolId("biome_plains"));
    List<ItemStack> overflow = PortfolioItem.importFrom(portfolio, List.of(flat, plains, linkPanel, flat.copy()));

    if (!overflow.isEmpty()) {
      helper.fail("Portfolio overflowed before capacity");
      return;
    }
    if (PortfolioItem.countSymbolPages(portfolio) != 3 || PortfolioItem.countLinkPanels(portfolio) != 1) {
      helper.fail("Portfolio type counts are wrong");
      return;
    }
    if (PortfolioItem.countMatchingPages(portfolio, flat) != 2) {
      helper.fail("Portfolio did not preserve duplicate collected pages");
      return;
    }
    PortfolioItem.sortPages(portfolio);
    if (!Page.isLinkPanel(PortfolioItem.getPages(portfolio).get(0))) {
      helper.fail("Portfolio sort should keep link panels at the front");
      return;
    }
    if (PortfolioItem.removeByContent(portfolio, plains).isEmpty()) {
      helper.fail("Portfolio could not remove a page by content");
      return;
    }
    if (PortfolioItem.getUniqueSymbols(portfolio).size() != 1) {
      helper.fail("Portfolio unique symbol list did not update after removal");
      return;
    }
    while (PortfolioItem.getPageCount(portfolio) < PortfolioItem.MAX_PAGES) {
      PortfolioItem.addPage(portfolio, Page.createPage());
    }
    if (PortfolioItem.addPage(portfolio, Page.createPage())) {
      helper.fail("Portfolio accepted a page past capacity");
      return;
    }
    helper.succeed();
  }

  public static void runBoosterPackPlayerUseTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_CRAFTING)) {
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    player.getInventory().clearContent();
    ItemStack pack = new ItemStack(ModItems.BOOSTER_PACK.get());
    player.getInventory().setItem(player.getInventory().selected, pack);

    ModItems.BOOSTER_PACK.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

    if (!player.getInventory().getSelected().isEmpty()) {
      helper.fail("Opening a booster pack should consume the pack");
      return;
    }
    int pageCount = countInventoryItems(player, ModItems.PAGE.get());
    if (pageCount < 3) {
      helper.fail("Opening a booster pack should give at least 3 pages, got " + pageCount);
      return;
    }
    helper.succeed();
  }

  public static void runBookBinderWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_CRAFTING)) {
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    BookBinderBlockEntity binder = new BookBinderBlockEntity(BlockPos.ZERO, ModBlocks.BOOK_BINDER.get().defaultBlockState());
    binder.setCoverStack(new ItemStack(Items.LEATHER));
    binder.setBookTitle("Flat Plains Test Age");
    binder.insertPage(Page.createLinkPage(LinkFlags.FOLLOWING), 0);
    binder.insertPage(Page.createSymbolPage(symbolId("terrain_flat")), 1);
    binder.insertPage(Page.createSymbolPage(symbolId("biome_plains")), 2);

    if (!binder.canBuildItem()) {
      helper.fail("Book Binder should build with cover, title, link panel, and symbols");
      return;
    }
    ItemStack output = binder.getCraftedItem();
    binder.buildItem(output, player);
    if (!(output.getItem() instanceof AgebookItem agebookItem)) {
      helper.fail("Book Binder did not produce an Agebook");
      return;
    }
    if (!AgebookItem.isNewAgebook(output)) {
      helper.fail("Bound Agebook should be ready to create a new age");
      return;
    }
    if (agebookItem.getPageList(output).size() != 3) {
      helper.fail("Bound Agebook did not retain all pages");
      return;
    }
    if (!LinkOptions.getFlag(ItemStackNbt.getTag(output), LinkFlags.FOLLOWING)) {
      helper.fail("Bound Agebook did not inherit link panel flags");
      return;
    }
    if (!binder.getPageList().isEmpty() || !binder.getCoverStack().isEmpty()) {
      helper.fail("Book Binder did not consume pages and cover after building");
      return;
    }
    helper.succeed();
  }

  public static void runInkMixerWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_CRAFTING)) {
      return;
    }
    InkMixerBlockEntity mixer = new InkMixerBlockEntity(BlockPos.ZERO, ModBlocks.INK_MIXER.get().defaultBlockState());
    SimpleContainer inventory = (SimpleContainer) mixer.getInventory();
    inventory.setItem(InkMixerBlockEntity.SLOT_PAPER, Page.createPage());
    mixer.setHasInk(true);

    if (!mixer.canBuildItem()) {
      helper.fail("Ink Mixer should build a link panel from ink and blank page");
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    ItemStack output = mixer.getCraftedItem();
    mixer.buildItem(output, player);
    if (!Page.isLinkPanel(output)) {
      helper.fail("Ink Mixer output was not a link panel");
      return;
    }
    if (mixer.hasInk()) {
      helper.fail("Ink Mixer did not consume its ink after building");
      return;
    }
    if (!inventory.getItem(InkMixerBlockEntity.SLOT_PAPER).isEmpty()) {
      helper.fail("Ink Mixer did not consume the blank page");
      return;
    }
    helper.succeed();
  }

  public static void runWritingDeskWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_CRAFTING)) {
      return;
    }
    ServerLevel level = helper.getLevel();
    BlockPos localPos = new BlockPos(1, 1, 1);
    helper.setBlock(localPos, ModBlocks.WRITING_DESK.get().defaultBlockState());

    helper.runAtTickTime(3, () -> {
      BlockEntity blockEntity = level.getBlockEntity(helper.absolutePos(localPos));
      if (!(blockEntity instanceof WritingDeskBlockEntity desk)) {
        helper.fail("Writing Desk block entity was not created");
        return;
      }
      desk.getInkTank().fill(WritingDeskBlockEntity.INK_COST);
      desk.setWritingItem(Page.createPage());
      if (!desk.writeSymbol(null, symbolId("terrain_flat"))) {
        helper.fail("Writing Desk did not write a symbol to a blank page");
        return;
      }
      if (!symbolId("terrain_flat").equals(Page.getSymbol(desk.getWritingItem()))) {
        helper.fail("Writing Desk wrote the wrong symbol");
        return;
      }
      if (desk.getInkAmount() != 0) {
        helper.fail("Writing Desk did not consume ink");
        return;
      }
      helper.succeed();
    });
  }

  public static void runAgebookCreationDataWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.AGE_CREATION)) {
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());
    List<ItemStack> pages = new ArrayList<>();
    pages.add(Page.createLinkPage(LinkFlags.FOLLOWING));
    pages.add(Page.createSymbolPage(symbolId("terrain_flat")));
    pages.add(Page.createSymbolPage(symbolId("biome_plains")));
    pages.add(Page.createSymbolPage(symbolId("weather_normal")));
    pages.add(Page.createSymbolPage(symbolId("lighting_normal")));

    AgebookItem.create(agebook, player, pages, "Player Built Age");
    if (!AgebookItem.isNewAgebook(agebook)) {
      helper.fail("Created Agebook should be a new agebook with a link panel");
      return;
    }
    AgebookItem item = (AgebookItem) agebook.getItem();
    if (!"Player Built Age".equals(item.getDisplayName(agebook))) {
      helper.fail("Agebook did not retain player-selected title");
      return;
    }
    if (!item.getAuthors(agebook).contains(player.getGameProfile().getName())) {
      helper.fail("Agebook did not retain its author");
      return;
    }
    if (item.getPageList(agebook).size() != pages.size()) {
      helper.fail("Agebook did not retain bound pages");
      return;
    }
    if (!LinkOptions.getFlag(ItemStackNbt.getTag(agebook), LinkFlags.FOLLOWING)) {
      helper.fail("Agebook did not inherit following flag from its link panel");
      return;
    }
    helper.succeed();
  }

  public static void runAgeBuilderStableRulesTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.AGE_CREATION)) {
      return;
    }
    AgeBuilder builder = new AgeBuilder(List.of(
        requiredSymbol("terrain_flat"),
        requiredSymbol("biome_plains"),
        requiredSymbol("weather_normal"),
        requiredSymbol("lighting_normal"),
        requiredSymbol("color_sky_natural")
    ), 12345L);
    AgeDirectorImpl director = builder.build();

    if (!"flat".equals(director.getTerrainType())) {
      helper.fail("AgeBuilder did not apply flat terrain");
      return;
    }
    if (!"normal".equals(director.getWeatherType())) {
      helper.fail("AgeBuilder did not apply normal weather");
      return;
    }
    if (!"normal".equals(director.getLightingType())) {
      helper.fail("AgeBuilder did not apply normal lighting");
      return;
    }
    if (!director.isSkyColorNatural()) {
      helper.fail("AgeBuilder did not apply natural sky color");
      return;
    }
    helper.succeed();
  }

  public static void runMultipleAgebooksCreateDistinctAgesTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.AGE_CREATION)) {
      return;
    }
    ServerLevel originLevel = helper.getLevel();
    MinecraftServer server = originLevel.getServer();
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    BlockPos originPos = helper.absolutePos(new BlockPos(6, 2, 6));
    ServerPlayerTeleport.teleport(player, originLevel, originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5, 0.0F, 0.0F);

    AgeManager ageManager = AgeManager.get(server);
    Set<Integer> beforeAges = ageIdSet(ageManager);

    ItemStack firstBook = createTestAgebook(player, "Multiple Age A",
        symbolId("terrain_flat"), symbolId("biome_plains"), symbolId("weather_normal"), symbolId("lighting_normal"));
    ItemStack secondBook = createTestAgebook(player, "Multiple Age B",
        symbolId("terrain_void"), symbolId("biome_desert"), symbolId("weather_off"), symbolId("lighting_bright"));

    AgebookItem agebookItem = (AgebookItem) ModItems.AGEBOOK.get();
    agebookItem.activate(firstBook, originLevel, player);
    Integer firstUid = linkedAgeUid(firstBook);
    if (firstUid == null) {
      helper.fail("First Agebook did not receive an age UID");
      return;
    }
    ServerLevel firstLevel = ageManager.getAgeLevel(server, firstUid);
    if (firstLevel == null) {
      helper.fail("First Agebook UID did not resolve to a loaded age");
      return;
    }
    AgeData firstData = AgeData.get(firstLevel);
    if (!"Multiple Age A".equals(firstData.getAgeName())) {
      helper.fail("First AgeData kept wrong name: " + firstData.getAgeName());
      return;
    }

    ServerPlayerTeleport.teleport(player, originLevel, originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5, 0.0F, 0.0F);
    agebookItem.activate(secondBook, originLevel, player);
    Integer secondUid = linkedAgeUid(secondBook);
    if (secondUid == null) {
      helper.fail("Second Agebook did not receive an age UID");
      return;
    }
    if (firstUid.equals(secondUid)) {
      helper.fail("Two separate Agebooks created the same age UID: " + firstUid);
      return;
    }

    ServerLevel secondLevel = ageManager.getAgeLevel(server, secondUid);
    if (secondLevel == null) {
      helper.fail("Second Agebook UID did not resolve to a loaded age");
      return;
    }
    if (firstLevel.dimension().equals(secondLevel.dimension())) {
      helper.fail("Two separate Agebooks resolved to the same dimension");
      return;
    }
    AgeData secondData = AgeData.get(secondLevel);
    if (!"Multiple Age B".equals(secondData.getAgeName())) {
      helper.fail("Second AgeData kept wrong name: " + secondData.getAgeName());
      return;
    }
    if (firstData.getAgeUUID().equals(secondData.getAgeUUID())) {
      helper.fail("Two separate Ages reused the same UUID");
      return;
    }

    Set<Integer> afterAges = ageIdSet(ageManager);
    afterAges.removeAll(beforeAges);
    if (!afterAges.contains(firstUid) || !afterAges.contains(secondUid) || afterAges.size() < 2) {
      helper.fail("AgeManager did not keep both newly created ages: " + afterAges);
      return;
    }
    if (AgebookItem.isNewAgebook(firstBook) || AgebookItem.isNewAgebook(secondBook)) {
      helper.fail("Agebooks stayed marked as new after creating their dimensions");
      return;
    }

    helper.succeed();
  }

  public static void runAgeDeathReturnUsesStoredEntryTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.AGE_CREATION)) {
      return;
    }
    Supplier<Boolean> originalSafeStories = MystcraftConfig.safeStories;
    try {
      MystcraftConfig.safeStories = () -> true;
      ServerLevel originLevel = helper.getLevel();
      MinecraftServer server = originLevel.getServer();
      ServerPlayer player = createMockServerPlayer(helper);
      if (player == null) {
        return;
      }
      BlockPos originPos = helper.absolutePos(new BlockPos(8, 2, 8));
      ServerPlayerTeleport.teleport(player, originLevel, originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5, 90.0F, 0.0F);
      player.setHealth(player.getMaxHealth());

      ItemStack agebook = createTestAgebook(player, "Safe Story Return Age",
          symbolId("terrain_flat"), symbolId("biome_plains"), symbolId("weather_normal"), symbolId("lighting_normal"));
      ((AgebookItem) ModItems.AGEBOOK.get()).activate(agebook, originLevel, player);

      Integer ageUid = linkedAgeUid(agebook);
      if (ageUid == null) {
        helper.fail("Agebook did not create an age before death-return test");
        return;
      }
      if (!(player.level() instanceof ServerLevel ageLevel) || !AgeDimensionFactory.isMystcraftAge(ageLevel.dimension())) {
        helper.fail("Agebook activation did not move player into a Mystcraft Age");
        return;
      }
      CompoundTag storedReturn = AgeReturnData.get(server).getReturnLink(player.getUUID(), ageUid);
      if (storedReturn == null || !originPos.equals(LinkOptions.getSpawn(storedReturn))) {
        helper.fail("Age entry did not store the original return position");
        return;
      }

      player.setHealth(1.0F);
      if (!AgeReturnHandler.handleDeath(player, ageLevel.damageSources().generic())) {
        helper.fail("Safe-story death was not intercepted in a Mystcraft Age");
        return;
      }
      if (player.level() != originLevel) {
        helper.fail("Safe-story death did not return to the origin dimension");
        return;
      }
      if (!player.blockPosition().equals(originPos)) {
        helper.fail("Safe-story death returned to " + player.blockPosition() + " instead of " + originPos);
        return;
      }
      if (player.getHealth() != player.getMaxHealth()) {
        helper.fail("Safe-story death did not restore player health");
        return;
      }
      helper.succeed();
    } finally {
      MystcraftConfig.safeStories = originalSafeStories;
    }
  }

  public static void runWorldRulesPersistToAgeDataTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.WORLD_RULES)) {
      return;
    }
    AgeDirectorImpl director = new AgeDirectorImpl(777L);
    director.setWeatherType("storm");
    director.setLightingType("bright");
    director.setBiomeController("single");
    director.setMeteorsEnabled(true);
    director.setLightningEnabled(true);
    director.setExplosionsEnabled(true);
    director.setTimescale(2.0F);
    director.setCloudHeight(144.0F);
    director.setHorizonHeight(72.0F);
    director.setInstability(42.5F);

    AgeData ageData = new AgeData();
    ageData.copyFromDirector(director);

    if (!"storm".equals(ageData.getWeatherType()) || !"bright".equals(ageData.getLightingType())) {
      helper.fail("AgeData did not persist weather/lighting rules");
      return;
    }
    if (!"single".equals(ageData.getBiomeController())) {
      helper.fail("AgeData did not persist biome controller");
      return;
    }
    if (!ageData.areMeteorsEnabled() || !ageData.isLightningEnabled() || !ageData.areExplosionsEnabled()) {
      helper.fail("AgeData did not persist instability backend toggles");
      return;
    }
    if (ageData.getTimescale() != 2.0F || ageData.getCloudHeight() != 144.0F || ageData.getHorizonHeight() != 72.0F) {
      helper.fail("AgeData did not persist world timing/height rules");
      return;
    }
    if (ageData.getInstability() != 42.5F) {
      helper.fail("AgeData did not persist instability value");
      return;
    }
    helper.succeed();
  }

  public static void runCommandWorkflowTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.COMMANDS)) {
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    player.getInventory().clearContent();
    MystcraftCommands.registerCommands(helper.getLevel().getServer().getCommands().getDispatcher());
    CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
    CommandSourceStack playerSource = player.createCommandSourceStack().withPermission(0).withSuppressedOutput();
    AgeManager ageManager = AgeManager.get(helper.getLevel().getServer());
    java.util.Set<Integer> beforeAges = ageIdSet(ageManager);

    assertCommandSucceeds(helper, source, "/mystcraft symbol info \"mystcraft:terrain_flat\"");
    assertCommandSucceeds(helper, source, "/mystcraft symbol list terrain");
    assertCommandFails(helper, playerSource, "/mystcraft give page blank");
    assertCommandSucceeds(helper, source, "/mystcraft age create symbols \"mystcraft:terrain_flat,mystcraft:biome_plains\" name Command Age");
    java.util.Set<Integer> afterAges = ageIdSet(ageManager);
    afterAges.removeAll(beforeAges);
    if (afterAges.isEmpty()) {
      helper.fail("Age create command did not register a new age");
      return;
    }
    Integer createdAgeId = afterAges.iterator().next();
    assertCommandSucceeds(helper, source, "/mystcraft age list");
    assertCommandSucceeds(helper, source, "/mystcraft age info " + createdAgeId);
    assertCommandSucceeds(helper, source, "/mystcraft give page blank");
    if (countInventoryItems(player, ModItems.PAGE.get()) < 1) {
      helper.fail("Give page command did not add a page to the player's inventory");
      return;
    }
    assertCommandSucceeds(helper, source, "/mystcraft give book custom \"mystcraft:terrain_flat,mystcraft:biome_plains\" Command Age");
    if (countInventoryItems(player, ModItems.AGEBOOK.get()) < 1) {
      helper.fail("Give custom book command did not add an Agebook to the player's inventory");
      return;
    }
    helper.succeed();
  }

  public static void runTableBlocksCreateExpectedStateTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.WORLD_RULES)) {
      return;
    }
    ServerLevel level = helper.getLevel();
    BlockPos inkMixerPos = new BlockPos(1, 1, 1);
    BlockPos bookBinderPos = new BlockPos(3, 1, 1);
    BlockPos bookstandPos = new BlockPos(5, 1, 1);

    helper.setBlock(inkMixerPos, ModBlocks.INK_MIXER.get().defaultBlockState());
    helper.setBlock(bookBinderPos, ModBlocks.BOOK_BINDER.get().defaultBlockState());
    helper.setBlock(bookstandPos, ModBlocks.BOOKSTAND.get().defaultBlockState());

    helper.runAtTickTime(5, () -> {
      if (!(level.getBlockEntity(helper.absolutePos(inkMixerPos)) instanceof InkMixerBlockEntity)) {
        helper.fail("Ink Mixer block entity was not created");
        return;
      }
      if (!(level.getBlockEntity(helper.absolutePos(bookBinderPos)) instanceof BookBinderBlockEntity)) {
        helper.fail("Book Binder block entity was not created");
        return;
      }
      if (!(level.getBlockEntity(helper.absolutePos(bookstandPos)) instanceof art.arcane.mystcraft.blockentity.BookstandBlockEntity)) {
        helper.fail("Bookstand block entity was not created");
        return;
      }
      helper.succeed();
    });
  }

  private static void assertDropBooksOnRead(GameTestHelper helper, boolean expectDrop, boolean following) {
    ServerLevel level = helper.getLevel();
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    player.getInventory().clearContent();
    player.moveTo(0.5, 2.0, 0.5, 0.0F, 0.0F);

    LinkbookItemAccessor linkbook = new LinkbookItemAccessor((art.arcane.mystcraft.item.LinkbookItem) ModItems.LINKBOOK.get());
    ItemStack stack = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag tag = returnLink(0, new BlockPos(0, 2, 0), 0.0F);
    LinkOptions.setFlag(tag, LinkFlags.FOLLOWING, following);
    ItemStackNbt.setTag(stack, tag);

    int slot = player.getInventory().selected;
    player.getInventory().setItem(slot, stack);

    AABB box = new AABB(-2, 0, -2, 2, 5, 2);
    int before = level.getEntitiesOfClass(LinkbookEntity.class, box).size();
    linkbook.onLink(stack, level, player);
    int after = level.getEntitiesOfClass(LinkbookEntity.class, box).size();
    ItemStack slotStack = player.getInventory().getItem(slot);

    if (expectDrop) {
      if (after <= before) {
        helper.fail("Expected normal linkbook to drop when linking");
        return;
      }
      if (!slotStack.isEmpty()) {
        helper.fail("Expected dropped normal linkbook to be removed from inventory");
      }
      return;
    }
    if (after != before) {
      helper.fail("Expected no dropped linkbook entity");
      return;
    }
    if (slotStack.isEmpty()) {
      helper.fail("Expected normal linkbook to remain in inventory");
    }
  }

  private static void assertCommandSucceeds(GameTestHelper helper, CommandSourceStack source, String command) {
    CommandAttempt attempt = performCommand(source, command);
    if (attempt.result <= 0) {
      helper.fail("Expected command to succeed: " + command + " (" + attempt.diagnostics + ")");
    }
  }

  private static void assertCommandFails(GameTestHelper helper, CommandSourceStack source, String command) {
    CommandAttempt attempt = performCommand(source, command);
    if (attempt.result > 0) {
      helper.fail("Expected command to fail: " + command);
    }
  }

  private static CommandAttempt performCommand(CommandSourceStack source, String command) {
    String normalized = normalizeCommand(command);
    var commands = source.getServer().getCommands();
    var parse = commands.getDispatcher().parse(normalized, source);
    int result = commands.performCommand(parse, normalized);
    return new CommandAttempt(result, commandDiagnostics(parse));
  }

  private static String normalizeCommand(String command) {
    return command.startsWith("/") ? command.substring(1) : command;
  }

  private static String commandDiagnostics(com.mojang.brigadier.ParseResults<CommandSourceStack> parse) {
    if (parse.getExceptions().isEmpty()) {
      return "result=0";
    }
    StringBuilder out = new StringBuilder("parse exceptions=");
    parse.getExceptions().forEach((node, exception) -> {
      if (out.length() > "parse exceptions=".length()) {
        out.append("; ");
      }
      out.append(node.getName()).append(": ").append(exception.getMessage());
    });
    return out.toString();
  }

  private static final class CommandAttempt {
    private final int result;
    private final String diagnostics;

    private CommandAttempt(int result, String diagnostics) {
      this.result = result;
      this.diagnostics = diagnostics;
    }
  }

  private static Set<Integer> ageIdSet(AgeManager ageManager) {
    Set<Integer> ids = new java.util.HashSet<>();
    for (Integer ageId : ageManager.getAllAgeUIDs()) {
      ids.add(ageId);
    }
    return ids;
  }

  private static ItemStack createTestAgebook(ServerPlayer player, String title, ResourceLocation... symbols) {
    ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());
    List<ItemStack> pages = new ArrayList<>();
    pages.add(Page.createLinkPage(LinkFlags.FOLLOWING));
    for (ResourceLocation symbol : symbols) {
      pages.add(Page.createSymbolPage(symbol));
    }
    AgebookItem.create(agebook, player, pages, title);
    return agebook;
  }

  private static Integer linkedAgeUid(ItemStack agebook) {
    CompoundTag tag = ItemStackNbt.getTag(agebook);
    return tag == null ? null : LinkOptions.getDimensionUID(tag);
  }

  private static int countInventoryItems(ServerPlayer player, Item item) {
    int count = 0;
    for (ItemStack stack : player.getInventory().items) {
      if (stack.getItem() == item) {
        count += stack.getCount();
      }
    }
    if (player.getOffhandItem().getItem() == item) {
      count += player.getOffhandItem().getCount();
    }
    return count;
  }

  private static CompoundTag returnLink(int dimensionUid, BlockPos position, float yaw) {
    CompoundTag tag = new CompoundTag();
    LinkOptions.setDimensionUID(tag, dimensionUid);
    LinkOptions.setSpawn(tag, position);
    LinkOptions.setSpawnYaw(tag, yaw);
    return tag;
  }

  private static boolean hasStoredLinkData(ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag == null) {
      return false;
    }
    return LinkOptions.getDimensionUID(tag) != null
        || LinkOptions.getSpawn(tag) != null
        || !"???".equals(LinkOptions.getDisplayName(tag))
        || tag.contains("DimensionKey")
        || tag.contains("TargetUUID")
        || tag.contains("LinkOptions")
        || tag.contains("Flags")
        || tag.contains("Props")
        || tag.contains("MaxHealth")
        || tag.contains("damage")
        || tag.contains("NoDecay");
  }

  private static ResourceLocation symbolId(String path) {
    return SymbolRegistry.mystcraftId(path);
  }

  private static art.arcane.mystcraft.api.symbol.IAgeSymbol requiredSymbol(String path) {
    ResourceLocation id = symbolId(path);
    art.arcane.mystcraft.api.symbol.IAgeSymbol symbol = SymbolRegistry.get(id);
    if (symbol == null) {
      throw new IllegalStateException("Missing required symbol: " + id);
    }
    return symbol;
  }

  private static ServerPlayer createMockServerPlayer(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    MinecraftServer server = level.getServer();
    try {
      ServerPlayer player = helper.makeMockServerPlayerInLevel();
      player.moveTo(level.getSharedSpawnPos(), 0.0F, 0.0F);
      prepareSurvivalTestPlayer(player);
      return player;
    } catch (RuntimeException ignored) {
      // Fall through to the reflective constructor used by older harnesses.
    }

    GameProfile profile = new GameProfile(UUID.randomUUID(), "test-player");
    ServerPlayer player = createServerPlayer(server, level, profile);
    if (player == null) {
      helper.fail("Failed to create test player");
      return null;
    }
    player.moveTo(level.getSharedSpawnPos(), 0.0F, 0.0F);
    prepareSurvivalTestPlayer(player);
    return player;
  }

  private static void prepareSurvivalTestPlayer(ServerPlayer player) {
    player.setGameMode(GameType.SURVIVAL);
    player.getAbilities().invulnerable = false;
    player.getAbilities().instabuild = false;
    player.getAbilities().mayfly = false;
    player.getAbilities().flying = false;
    player.setInvulnerable(false);
    clearDamageImmunity(player);
    player.setHealth(player.getMaxHealth());
    player.setRemainingFireTicks(0);
  }

  private static void clearDamageImmunity(ServerPlayer player) {
    player.invulnerableTime = 0;
    player.hurtTime = 0;
    try {
      java.lang.reflect.Field spawnInvulnerableTime = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
      spawnInvulnerableTime.setAccessible(true);
      spawnInvulnerableTime.setInt(player, 0);
    } catch (ReflectiveOperationException ignored) {
    }
  }

  private static ServerPlayer createServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile) {
    Object cookie = CommonListenerCookieCompat.createInitial(profile);
    if (cookie != null) {
      try {
        java.lang.reflect.Method gameProfile = cookie.getClass().getMethod("gameProfile");
        java.lang.reflect.Method clientInfo = cookie.getClass().getMethod("clientInformation");
        Object clientInformation = clientInfo.invoke(cookie);
        GameProfile cookieProfile = (GameProfile) gameProfile.invoke(cookie);
        java.lang.reflect.Constructor<ServerPlayer> ctor = ServerPlayer.class.getConstructor(
            MinecraftServer.class,
            ServerLevel.class,
            GameProfile.class,
            clientInformation.getClass()
        );
        return ctor.newInstance(server, level, cookieProfile, clientInformation);
      } catch (ReflectiveOperationException ignored) {
      }
    }

    try {
      java.lang.reflect.Constructor<ServerPlayer> ctor = ServerPlayer.class.getConstructor(
          MinecraftServer.class,
          ServerLevel.class,
          GameProfile.class
      );
      return ctor.newInstance(server, level, profile);
    } catch (ReflectiveOperationException ignored) {
    }

    return null;
  }

  private static final class LinkbookItemAccessor {
    private final art.arcane.mystcraft.item.LinkbookItem item;

    private LinkbookItemAccessor(art.arcane.mystcraft.item.LinkbookItem item) {
      this.item = item;
    }

    private void onLink(@NotNull ItemStack stack, Level level, Entity entity) {
      try {
        java.lang.reflect.Method method = art.arcane.mystcraft.item.LinkbookItem.class
            .getDeclaredMethod("onLink", ItemStack.class, Level.class, Entity.class);
        method.setAccessible(true);
        method.invoke(item, stack, level, entity);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Unable to invoke LinkbookItem.onLink", e);
      }
    }
  }
}
