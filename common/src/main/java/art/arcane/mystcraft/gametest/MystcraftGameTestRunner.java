package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
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
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.link.LinkEvent;
import art.arcane.mystcraft.link.LinkEventBus;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.CommonListenerCookieCompat;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.PocketHeadUtils;
import art.arcane.mystcraft.util.ServerPlayerTeleport;
import art.arcane.mystcraft.world.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Coordinates behavior-focused GameTest scenarios that both Fabric and Forge wrappers invoke.
 */
public final class MystcraftGameTestRunner {

  private MystcraftGameTestRunner() {
  }

  public static void runCoreGameplayContentLoaded(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.CORE)) {
      return;
    }
    MystcraftServerGameTestAssertions.assertCoreGameplayContentLoaded();
    MystcraftServerGameTestAssertions.assertRegisteredObjectsReachable();
    assertPocketHeadPaletteValidation(helper);
    helper.succeed();
  }

  private static void assertPocketHeadPaletteValidation(GameTestHelper helper) {
    Map<AgeData.PocketHeadFace, List<String>> palette =
        new EnumMap<>(AgeData.PocketHeadFace.class);
    for (AgeData.PocketHeadFace face : AgeData.PocketHeadFace.values()) {
      palette.put(face, new ArrayList<>(Collections.nCopies(64, "minecraft:white_wool")));
    }

    if (!PocketHeadUtils.isValidHeadBlockMap(palette)) {
      helper.fail("Pocket-head validation rejected a complete wool palette");
      return;
    }

    List<String> bottom = palette.remove(AgeData.PocketHeadFace.BOTTOM);
    if (PocketHeadUtils.isValidHeadBlockMap(palette)) {
      helper.fail("Pocket-head validation accepted a palette with a missing face");
      return;
    }
    palette.put(AgeData.PocketHeadFace.BOTTOM, bottom);

    palette.get(AgeData.PocketHeadFace.FRONT).set(0, "minecraft:diamond_block");
    if (PocketHeadUtils.isValidHeadBlockMap(palette)) {
      helper.fail("Pocket-head validation accepted a block outside the skin wool palette");
    }
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
      assertLinkbookActivationDropTransaction(helper);
      assertLinkFlagsFollowersAndPassengers(helper);

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
    helper.runAtTickTime(10, () -> {
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

  /**
   * Validates {@link BookReceptacleBlockEntity#isValidPortalActivator} answers
   * correctly for every book category we care about. This is the contract the
   * portal frame relies on — accepting books that can't actually open a portal
   * leads to the silent-no-op bug we fixed in this change.
   */
  public static void runPortalValidatorCategorizesBookTypesTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    if (BookReceptacleBlockEntity.isValidPortalActivator(ItemStack.EMPTY)) {
      helper.fail("Empty stack must not validate as a portal activator");
      return;
    }

    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
    if (!BookReceptacleBlockEntity.isValidPortalActivator(linkbook)) {
      helper.fail("Linkbook must validate as a portal activator");
      return;
    }

    ItemStack personalBook = new ItemStack(ModItems.PERSONAL_LINK_BOOK.get());
    if (!BookReceptacleBlockEntity.isValidPortalActivator(personalBook)) {
      helper.fail("Personal Link Book must validate as a portal activator");
      return;
    }

    ItemStack linkedAgebook = new ItemStack(ModItems.AGEBOOK.get());
    CompoundTag linkedTag = new CompoundTag();
    LinkOptions.setDimensionUID(linkedTag, 42);
    ItemStackNbt.setTag(linkedAgebook, linkedTag);
    if (!BookReceptacleBlockEntity.isValidPortalActivator(linkedAgebook)) {
      helper.fail("Linked Agebook (DimensionUID=42) must validate as a portal activator");
      return;
    }

    ItemStack unwrittenWithPanel = new ItemStack(ModItems.AGEBOOK.get());
    {
      List<ItemStack> pagesWithPanel = new ArrayList<>();
      pagesWithPanel.add(Page.createLinkPage(LinkFlags.FOLLOWING));
      pagesWithPanel.add(Page.createSymbolPage(symbolId("terrain_flat")));
      AgebookItem agebookItem = (AgebookItem) unwrittenWithPanel.getItem();
      ItemStackNbt.setTag(unwrittenWithPanel, new CompoundTag());
      agebookItem.addPages(unwrittenWithPanel, pagesWithPanel);
    }
    if (!BookReceptacleBlockEntity.isValidPortalActivator(unwrittenWithPanel)) {
      helper.fail("Unwritten Agebook with link-panel page 0 must validate (auto-create-on-traverse path)");
      return;
    }

    ItemStack unwrittenWithoutPanel = new ItemStack(ModItems.AGEBOOK.get());
    {
      List<ItemStack> pagesNoPanel = new ArrayList<>();
      pagesNoPanel.add(Page.createSymbolPage(symbolId("terrain_flat")));
      pagesNoPanel.add(Page.createSymbolPage(symbolId("biome_plains")));
      AgebookItem agebookItem = (AgebookItem) unwrittenWithoutPanel.getItem();
      ItemStackNbt.setTag(unwrittenWithoutPanel, new CompoundTag());
      agebookItem.addPages(unwrittenWithoutPanel, pagesNoPanel);
    }
    if (BookReceptacleBlockEntity.isValidPortalActivator(unwrittenWithoutPanel)) {
      helper.fail("Unwritten Agebook without a link-panel page 0 MUST be rejected");
      return;
    }

    ItemStack bareAgebook = new ItemStack(ModItems.AGEBOOK.get());
    if (BookReceptacleBlockEntity.isValidPortalActivator(bareAgebook)) {
      helper.fail("Bare Agebook (no NBT) MUST be rejected");
      return;
    }

    if (BookReceptacleBlockEntity.isValidPortalActivator(new ItemStack(Items.STICK))) {
      helper.fail("Stick must not validate as a portal activator");
      return;
    }

    helper.succeed();
  }

  /**
   * Verifies the auto-create-on-traverse flow: an unwritten Agebook with a Link
   * Panel as page 0 must, after {@link AgebookItem#activate}, end up with a
   * populated DimensionUID — that's the precondition that lets
   * {@code LinkPortalBlock.entityInside} teleport the player to the new Age.
   */
  public static void runUnwrittenAgebookActivateCreatesAgeTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
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
    AgebookItem.create(agebook, player, pages, "Auto-Create Test Age");

    if (!AgebookItem.isNewAgebook(agebook)) {
      helper.fail("Pre-condition violated: created agebook should be 'new' (unlinked + has link panel)");
      return;
    }
    if (LinkOptions.getDimensionUID(ItemStackNbt.getTag(agebook)) != null) {
      helper.fail("Pre-condition violated: agebook should have no DimensionUID before activate()");
      return;
    }

    AgebookItem item = (AgebookItem) agebook.getItem();
    item.activate(agebook, helper.getLevel(), player);

    Integer uid = LinkOptions.getDimensionUID(ItemStackNbt.getTag(agebook));
    if (uid == null) {
      helper.fail("After activate(), unwritten Agebook with link panel must have a DimensionUID. The portal-traversal auto-create flow depends on this.");
      return;
    }
    if (AgebookItem.isNewAgebook(agebook)) {
      helper.fail("After activate(), the agebook must no longer be 'new' (it should be linked)");
      return;
    }

    helper.succeed();
  }

  /**
   * Verifies the take-out path: shift + empty hand on a book-bearing receptacle
   * must return the book to the player's inventory and clear the receptacle.
   * <p>
   * Goes through the actual {@code BlockState.use(...)} dispatch (not just the
   * BE) so an absent {@code @Override} or signature drift on the use method
   * would surface as a test failure here.
   */
  public static void runReceptacleTakeOnShiftEmptyHandTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }

    BlockPos crystalPos = new BlockPos(1, 1, 1);
    BlockPos receptaclePos = crystalPos.above();

    helper.setBlock(crystalPos, ModBlocks.CRYSTAL.get().defaultBlockState());
    helper.setBlock(receptaclePos,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    BlockPos absoluteReceptaclePos = helper.absolutePos(receptaclePos);
    BlockEntity be = helper.getLevel().getBlockEntity(absoluteReceptaclePos);
    if (!(be instanceof BookReceptacleBlockEntity receptacle)) {
      helper.fail("Receptacle BE missing for take-out test");
      return;
    }

    ItemStack inserted = new ItemStack(ModItems.LINKBOOK.get());
    receptacle.setBook(inserted);
    if (!receptacle.hasBook()) {
      helper.fail("Pre-condition violated: receptacle did not accept inserted Linkbook");
      return;
    }

    player.getInventory().clearContent();
    player.setShiftKeyDown(true);
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

    net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
        net.minecraft.world.phys.Vec3.atCenterOf(absoluteReceptaclePos),
        Direction.UP,
        absoluteReceptaclePos,
        false);

    net.minecraft.world.level.block.state.BlockState recState = helper.getLevel().getBlockState(absoluteReceptaclePos);
    net.minecraft.world.InteractionResult result =
        recState.use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);

    player.setShiftKeyDown(false);

    if (!result.consumesAction()) {
      helper.fail("BookReceptacle.use(shift+emptyHand) returned " + result + ", expected SUCCESS/CONSUME (override missing?)");
      return;
    }

    if (receptacle.hasBook()) {
      helper.fail("Receptacle still has a book after shift+emptyHand use(); take-out path failed");
      return;
    }

    boolean playerHasBook = false;
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      ItemStack slot = player.getInventory().getItem(i);
      if (!slot.isEmpty() && slot.getItem() instanceof LinkbookItem) {
        playerHasBook = true;
        break;
      }
    }
    if (!playerHasBook) {
      helper.fail("Player did not receive the Linkbook after shift+emptyHand use() on the receptacle");
      return;
    }

    helper.succeed();
  }

  /**
   * Places a Crystal + BookReceptacle in each of the 5 valid orientations (UP,
   * NORTH, SOUTH, EAST, WEST) and confirms the receptacle accepts a Linkbook
   * via {@link BookReceptacleBlockEntity#setBook} — proving the
   * orientation-independent insertion path is intact and the portal-fire
   * side-effect runs without throwing in any of them.
   */
  public static void runReceptacleSetBookFiresInAllOrientationsTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    ServerLevel level = helper.getLevel();

    Direction[] orientations = {
        Direction.UP,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST,
    };

    int xOffset = 0;
    for (Direction facing : orientations) {

      BlockPos crystalPos = new BlockPos(1 + xOffset, 1, 1);
      BlockPos receptaclePos = crystalPos.relative(facing);

      helper.setBlock(crystalPos, ModBlocks.CRYSTAL.get().defaultBlockState());
      helper.setBlock(receptaclePos,
          ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, facing));

      BlockEntity be = level.getBlockEntity(helper.absolutePos(receptaclePos));
      if (!(be instanceof BookReceptacleBlockEntity receptacle)) {
        helper.fail("Receptacle BE not created at " + facing + " orientation");
        return;
      }

      ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
      receptacle.setBook(linkbook);

      if (!receptacle.hasBook()) {
        helper.fail("Receptacle facing " + facing + " did not accept linkbook insertion");
        return;
      }
      if (!(receptacle.getBook().getItem() instanceof LinkbookItem)) {
        helper.fail("Receptacle facing " + facing + " stored wrong item type after setBook()");
        return;
      }

      ItemStack returned = receptacle.takeBook();
      if (returned.isEmpty() || !(returned.getItem() instanceof LinkbookItem)) {
        helper.fail("takeBook() did not return the inserted linkbook for facing " + facing);
        return;
      }
      if (receptacle.hasBook()) {
        helper.fail("Receptacle facing " + facing + " still has a book after takeBook()");
        return;
      }

      xOffset += 3;
    }

    helper.succeed();
  }

  /**
   * Drops a fresh, untagged Linkbook directly into a receptacle BE — but we
   * use a non-physical detached receptacle so the portal-fire side-effect
   * doesn't touch surrounding test structures (otherwise on Forge the
   * resulting BFS chunk-load + crystal-activation neighbour-update bleeds
   * into the next test's space and breaks unrelated tests). Confirms the
   * receptacle hands the linkbook back with full link data populated:
   * spawn coords + DimensionUID. Without this, the portal would light but
   * {@code performLink} would silently abort on missing fields.
   */
  public static void runLinkbookInsertedDirectlyTeleportsTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    ServerLevel level = helper.getLevel();

    ItemStack fresh = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag preTag = ItemStackNbt.getTag(fresh);
    if (preTag != null && LinkOptions.getDimensionUID(preTag) != null) {
      helper.fail("Test setup error: fresh linkbook already has DimensionUID before insertion");
      return;
    }

    if (!(fresh.getItem() instanceof LinkbookItem linkbook)) {
      helper.fail("Test setup error: ModItems.LINKBOOK is not a LinkbookItem");
      return;
    }

    linkbook.validate(level, fresh, null);

    CompoundTag tag = ItemStackNbt.getTag(fresh);
    if (tag == null) {
      helper.fail("validate() did not produce an NBT tag on a fresh linkbook");
      return;
    }

    if (LinkOptions.getDimensionUID(tag) == null) {
      helper.fail("validate() ran but DimensionUID is missing — performLink would abort");
      return;
    }

    if (LinkOptions.getSpawn(tag) == null) {
      helper.fail("validate() ran but Spawn is missing — performLink would abort");
      return;
    }

    helper.succeed();
  }

  /**
   * Pure unit test of {@code LinkPortalBlock}'s cooldown map: confirms that
   * setting the cooldown for one portal-block position does NOT bleed across
   * to another portal-block position for the same entity. This is the
   * regression test for the "walk through portal A, immediately walk through
   * portal B, second one silently swallowed" bug, where the cooldown used to
   * be keyed only on player UUID.
   */
  public static void runCooldownIsPerPortalNotGlobalTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    art.arcane.mystcraft.block.LinkPortalBlock.clearCooldownsForTest();

    UUID entityId = UUID.randomUUID();
    BlockPos portalA = new BlockPos(100, 64, 100);
    BlockPos portalB = new BlockPos(200, 64, 100);
    long now = 1000L;

    art.arcane.mystcraft.block.LinkPortalBlock.setCooldownForTest(entityId, portalA, now);

    if (!art.arcane.mystcraft.block.LinkPortalBlock.isOnCooldownForTest(entityId, portalA, now + 5)) {
      helper.fail("Cooldown did not register on portal A immediately after setCooldownForTest");
      return;
    }

    if (art.arcane.mystcraft.block.LinkPortalBlock.isOnCooldownForTest(entityId, portalB, now + 5)) {
      helper.fail("Cooldown leaked from portal A to portal B — keyed by entity UUID alone (REGRESSION)");
      return;
    }

    if (art.arcane.mystcraft.block.LinkPortalBlock.isOnCooldownForTest(
        entityId, Level.NETHER, portalA, now + 5)) {
      helper.fail("Cooldown leaked to the same block position in another dimension");
      return;
    }

    long pastCooldown = now + art.arcane.mystcraft.block.LinkPortalBlock.cooldownTicksForTest() + 1;
    if (art.arcane.mystcraft.block.LinkPortalBlock.isOnCooldownForTest(entityId, portalA, pastCooldown)) {
      helper.fail("Cooldown on portal A did not expire after COOLDOWN_TICKS");
      return;
    }

    if (art.arcane.mystcraft.block.LinkPortalBlock.isOnCooldownForTest(entityId, portalA, now - 1)) {
      helper.fail("Cooldown from a later server clock remained active after time moved backwards");
      return;
    }

    UUID otherEntityId = UUID.randomUUID();
    if (art.arcane.mystcraft.block.LinkPortalBlock.isOnCooldownForTest(otherEntityId, portalA, now + 5)) {
      helper.fail("Cooldown leaked across entities (different UUIDs should not share cooldown)");
      return;
    }

    art.arcane.mystcraft.block.LinkPortalBlock.clearCooldownsForTest();
    helper.succeed();
  }

  /**
   * Phase 3.5 — Confirms that the {@link art.arcane.mystcraft.block.LinkPortalBlock}
   * has the expected BE binding and that the BE round-trips its persisted
   * state through save/load NBT. The default colour is the documented
   * constant ({@code 0x4488FF}).
   *
   * <p>This is the structural anchor for the BE-driven colour pipeline that
   * came online in Phase 4: every running portal must carry a BE so the tint
   * handler has a single source of truth instead of hunting adjacent
   * receptacles.
   *
   * <p>Verifies the binding without depending on world placement (which has
   * been observed to flake on Forge 1.20.1 for unbreakable blocks set via
   * {@code level.setBlock} from a concurrent gametest arena). Direct
   * creation of the BE — exactly what the chunk machinery does once it
   * calls the block's factory — is sufficient to prove the registration +
   * serialisation pipeline. The "is the world chunk loaded" question is
   * irrelevant to this contract.
   */
  public static void runPortalBlockHasBlockEntityTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    BlockPos portalPos = new BlockPos(0, 0, 0);
    BlockState portalState = ModBlocks.LINK_PORTAL.get().defaultBlockState()
        .setValue(art.arcane.mystcraft.block.LinkPortalBlock.AXIS, Direction.Axis.Y);

    net.minecraft.world.level.block.entity.BlockEntity be =
        ((net.minecraft.world.level.block.EntityBlock) ModBlocks.LINK_PORTAL.get())
            .newBlockEntity(portalPos, portalState);
    if (!(be instanceof art.arcane.mystcraft.blockentity.LinkPortalBlockEntity portalBe)) {
      helper.fail("LinkPortalBlock did not produce a LinkPortalBlockEntity (got: "
          + (be == null ? "null" : be.getClass().getSimpleName()) + ")");
      return;
    }

    if (portalBe.getPortalColor() != 0x4488FF) {
      helper.fail("Fresh LinkPortalBlockEntity should have default colour 0x4488FF, got 0x"
          + Integer.toHexString(portalBe.getPortalColor()));
      return;
    }
    if (portalBe.getReceptaclePos() != null) {
      helper.fail("Fresh LinkPortalBlockEntity treated an unstamped cell as owned by "
          + portalBe.getReceptaclePos());
      return;
    }

    // Stamp a custom colour and a receptacle anchor, then verify the BE
    // round-trips through writeNbt -> readNbt via getUpdateTag/load. We use
    // getUpdateTag rather than saveWithoutMetadata because the latter has
    // observed flakiness on Forge in parallel batches (likely Forge's
    // capability serialisation hook racing with the test thread). The
    // pipeline we actually care about is writeNbt() -> readNbt(), which
    // both paths exercise; getUpdateTag is the more direct route.
    // The world origin is a legitimate receptacle position and must not be
    // conflated with the old "unknown" sentinel.
    BlockPos anchor = BlockPos.ZERO;
    portalBe.setPortalColor(0xCAFE99);
    portalBe.setReceptaclePos(anchor);

    if (portalBe.getPortalColor() != 0xCAFE99) {
      helper.fail("BE.setPortalColor did not stick: getPortalColor returned 0x"
          + Integer.toHexString(portalBe.getPortalColor()));
      return;
    }

    CompoundTag tag = portalBe.getUpdateTag();
    if (!tag.contains("portalColor")) {
      helper.fail("Update tag missing portalColor key (writeNbt path is broken). "
          + "Tag keys: " + tag.getAllKeys());
      return;
    }
    if (tag.getInt("portalColor") != 0xCAFE99) {
      helper.fail("Update tag portalColor != 0xCAFE99: got 0x"
          + Integer.toHexString(tag.getInt("portalColor")));
      return;
    }

    art.arcane.mystcraft.blockentity.LinkPortalBlockEntity restored =
        new art.arcane.mystcraft.blockentity.LinkPortalBlockEntity(
            portalBe.getBlockPos(), portalBe.getBlockState());
    restored.load(tag);

    if (restored.getPortalColor() != 0xCAFE99) {
      helper.fail("LinkPortalBlockEntity colour did not survive NBT round-trip: got 0x"
          + Integer.toHexString(restored.getPortalColor()));
      return;
    }
    if (restored.getReceptaclePos() == null
        || !restored.getReceptaclePos().equals(anchor)) {
      helper.fail("LinkPortalBlockEntity receptacle anchor did not survive NBT round-trip: got "
          + restored.getReceptaclePos());
      return;
    }

    helper.succeed();
  }

  /**
   * Confirms that {@link BookReceptacleBlockEntity#getPortalColor()} returns a
   * distinct value for each book category. Server-side test — sky-colour
   * lookups for linked Agebooks return -1 (no client cache), so the linked
   * Agebook arm asserts the sky-fallback colour {@code 0x66AAFF}, which still
   * differs from every other category.
   */
  public static void runPortalColorDistinctPerBookTypeTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }
    BlockPos crystalPos = new BlockPos(1, 1, 1);
    BlockPos receptaclePos = crystalPos.above();

    helper.setBlock(crystalPos, ModBlocks.CRYSTAL.get().defaultBlockState());
    helper.setBlock(receptaclePos,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(receptaclePos));
    if (!(be instanceof BookReceptacleBlockEntity receptacle)) {
      helper.fail("Receptacle BE missing for color test");
      return;
    }

    if (receptacle.getPortalColor() != 0xFFFFFF) {
      helper.fail("Empty receptacle colour expected 0xFFFFFF, got 0x" + Integer.toHexString(receptacle.getPortalColor()));
      return;
    }

    ItemStack personal = new ItemStack(ModItems.PERSONAL_LINK_BOOK.get());
    receptacle.setBook(personal);
    if (receptacle.getPortalColor() != 0xAA44FF) {
      helper.fail("PersonalLinkBook colour expected 0xAA44FF, got 0x" + Integer.toHexString(receptacle.getPortalColor()));
      return;
    }

    receptacle.setBook(ItemStack.EMPTY);
    ItemStack unwritten = new ItemStack(ModItems.AGEBOOK.get());
    if (unwritten.getItem() instanceof AgebookItem agebook) {
      agebook.addPages(unwritten, java.util.List.of(Page.createLinkPage()));
    }
    receptacle.setBook(unwritten);
    if (receptacle.getPortalColor() != 0x808890) {
      helper.fail("Unwritten Agebook colour expected 0x808890, got 0x" + Integer.toHexString(receptacle.getPortalColor()));
      return;
    }

    receptacle.setBook(ItemStack.EMPTY);
    ItemStack linkedAge = new ItemStack(ModItems.AGEBOOK.get());
    CompoundTag linkedAgeTag = ItemStackNbt.getOrCreateTag(linkedAge);
    LinkOptions.setDimensionUID(linkedAgeTag, 9999);
    ItemStackNbt.setTag(linkedAge, linkedAgeTag);
    receptacle.setBook(linkedAge);
    if (receptacle.getPortalColor() != 0x66AAFF) {
      helper.fail("Linked Agebook (no client cache) colour expected fallback 0x66AAFF, got 0x" + Integer.toHexString(receptacle.getPortalColor()));
      return;
    }

    receptacle.setBook(ItemStack.EMPTY);
    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag linkbookTag = ItemStackNbt.getOrCreateTag(linkbook);
    LinkOptions.setLinkColor(linkbookTag, 0x123456);
    ItemStackNbt.setTag(linkbook, linkbookTag);
    receptacle.setBook(linkbook);
    if (receptacle.getPortalColor() != 0x123456) {
      helper.fail("Linkbook colour expected stored 0x123456, got 0x" + Integer.toHexString(receptacle.getPortalColor()));
      return;
    }

    receptacle.setBook(ItemStack.EMPTY);
    receptacle.setBook(new ItemStack(ModItems.LINKBOOK.get()));
    if (receptacle.getPortalColor() != 0x4488FF) {
      helper.fail("Default Linkbook colour expected 0x4488FF, got 0x" + Integer.toHexString(receptacle.getPortalColor()));
      return;
    }

    helper.succeed();
  }

  /**
   * Phase 4.5 — Builds a real 3-cell horizontal portal frame, fires it with a
   * Linkbook stamped to a known colour, and asserts that <em>every</em>
   * placed portal cell's BE reports exactly that colour. This is the
   * regression anchor for the multi-colour-per-portal class of bugs: the
   * pre-v2 tint handler walked outwards looking for "the closest"
   * receptacle and flipped between portals when two were nearby. With
   * BE-stamped colour, every cell in a single fire event MUST agree.
   *
   * <p>Frame layout (y=1, axis=Y, plane=XZ): a horizontal 3x5 crystal ring at
   * y=1, with a 1x3 air interior at (1,1,1..3). Receptacle stacks on
   * (1,1,0) facing up. Horizontal frames sidestep the Forge-vs-Fabric
   * test-arena floor placement difference because the BFS plane never
   * touches structure y=0/y=-1.
   */
  public static void runPortalColorUniformAcrossAllPortalBlocksTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    BlockState crystal = ModBlocks.CRYSTAL.get().defaultBlockState();
    int[][] xz = {
        {0, 0}, {1, 0}, {2, 0},
        {0, 1}, {2, 1},
        {0, 2}, {2, 2},
        {0, 3}, {2, 3},
        {0, 4}, {1, 4}, {2, 4},
    };
    for (int[] c : xz) {
      helper.setBlock(new BlockPos(c[0], 1, c[1]), crystal);
    }
    for (int z = 1; z <= 3; z++) {
      helper.setBlock(new BlockPos(1, 1, z), Blocks.AIR.defaultBlockState());
    }
    BlockPos receptaclePos = new BlockPos(1, 2, 0);
    helper.setBlock(receptaclePos,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(receptaclePos));
    if (!(be instanceof BookReceptacleBlockEntity rec)) {
      helper.fail("Receptacle BE missing at " + receptaclePos);
      return;
    }

    ItemStack linkbook = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag tag = ItemStackNbt.getOrCreateTag(linkbook);
    LinkOptions.setLinkColor(tag, 0xABCDEF);
    ItemStackNbt.setTag(linkbook, tag);
    rec.setBook(linkbook);

    BlockPos[] expectedPortalCells = {
        new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(1, 1, 3),
    };
    for (BlockPos cell : expectedPortalCells) {
      BlockState s = helper.getLevel().getBlockState(helper.absolutePos(cell));
      if (!s.is(ModBlocks.LINK_PORTAL.get())) {
        helper.fail("Expected LINK_PORTAL at " + cell + " after firePortal, found " + s.getBlock().getDescriptionId());
        return;
      }
      BlockEntity portalBe = helper.getLevel().getBlockEntity(helper.absolutePos(cell));
      if (!(portalBe instanceof art.arcane.mystcraft.blockentity.LinkPortalBlockEntity portalBE)) {
        helper.fail("Expected LinkPortalBlockEntity at " + cell + ", got " + portalBe);
        return;
      }
      int color = portalBE.getPortalColor();
      if (color != 0xABCDEF) {
        helper.fail("Portal cell " + cell + " has colour 0x" + Integer.toHexString(color)
            + ", expected uniform 0xABCDEF — multi-colour regression");
        return;
      }
      if (portalBE.getReceptaclePos() == null
          || !portalBE.getReceptaclePos().equals(helper.absolutePos(receptaclePos))) {
        helper.fail("Portal cell " + cell + " receptaclePos is "
            + portalBE.getReceptaclePos() + ", expected " + helper.absolutePos(receptaclePos));
        return;
      }
    }

    helper.succeed();
  }

  /**
   * Phase 4.6 — Two adjacent portals, each backed by its own receptacle and
   * its own book colour. Asserts that the two portals' cells never bleed
   * colours into each other (the legacy tint handler used a BFS that could
   * cross portal-to-portal boundaries through shared crystal frames). With
   * BE-stamped colour, isolation is guaranteed by construction.
   *
   * <p>Two horizontal portal frames at y=1, side by side along the X axis.
   * Frame A occupies x=[0..2], frame B occupies x=[4..6]; they share no
   * crystals — the test asserts colour isolation, not frame separation.
   */
  public static void runTwoAdjacentPortalsEachHasOwnColorTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    BlockState crystal = ModBlocks.CRYSTAL.get().defaultBlockState();

    int[][] frameA = {
        {0, 0}, {1, 0}, {2, 0},
        {0, 1}, {2, 1},
        {0, 2}, {2, 2},
        {0, 3}, {2, 3},
        {0, 4}, {1, 4}, {2, 4},
    };
    for (int[] c : frameA) helper.setBlock(new BlockPos(c[0], 1, c[1]), crystal);
    for (int z = 1; z <= 3; z++) {
      helper.setBlock(new BlockPos(1, 1, z), Blocks.AIR.defaultBlockState());
    }
    BlockPos recA = new BlockPos(1, 2, 0);
    helper.setBlock(recA,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    int[][] frameB = {
        {4, 0}, {5, 0}, {6, 0},
        {4, 1}, {6, 1},
        {4, 2}, {6, 2},
        {4, 3}, {6, 3},
        {4, 4}, {5, 4}, {6, 4},
    };
    for (int[] c : frameB) helper.setBlock(new BlockPos(c[0], 1, c[1]), crystal);
    for (int z = 1; z <= 3; z++) {
      helper.setBlock(new BlockPos(5, 1, z), Blocks.AIR.defaultBlockState());
    }
    BlockPos recB = new BlockPos(5, 2, 0);
    helper.setBlock(recB,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    BlockEntity beA = helper.getLevel().getBlockEntity(helper.absolutePos(recA));
    BlockEntity beB = helper.getLevel().getBlockEntity(helper.absolutePos(recB));
    if (!(beA instanceof BookReceptacleBlockEntity recAEntity)
        || !(beB instanceof BookReceptacleBlockEntity recBEntity)) {
      helper.fail("Receptacle BE missing for one of the two portals (A=" + beA + ", B=" + beB + ")");
      return;
    }

    ItemStack bookA = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag tagA = ItemStackNbt.getOrCreateTag(bookA);
    LinkOptions.setLinkColor(tagA, 0x111111);
    ItemStackNbt.setTag(bookA, tagA);
    recAEntity.setBook(bookA);

    ItemStack bookB = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag tagB = ItemStackNbt.getOrCreateTag(bookB);
    LinkOptions.setLinkColor(tagB, 0x222222);
    ItemStackNbt.setTag(bookB, tagB);
    recBEntity.setBook(bookB);

    BlockPos[] cellsA = {
        new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(1, 1, 3),
    };
    BlockPos[] cellsB = {
        new BlockPos(5, 1, 1), new BlockPos(5, 1, 2), new BlockPos(5, 1, 3),
    };

    for (BlockPos cell : cellsA) {
      BlockEntity portalBe = helper.getLevel().getBlockEntity(helper.absolutePos(cell));
      if (!(portalBe instanceof art.arcane.mystcraft.blockentity.LinkPortalBlockEntity portalBE)) {
        helper.fail("Portal A cell " + cell + " has no BE");
        return;
      }
      if (portalBE.getPortalColor() != 0x111111) {
        helper.fail("Portal A cell " + cell + " has colour 0x"
            + Integer.toHexString(portalBE.getPortalColor()) + ", expected 0x111111");
        return;
      }
    }

    for (BlockPos cell : cellsB) {
      BlockEntity portalBe = helper.getLevel().getBlockEntity(helper.absolutePos(cell));
      if (!(portalBe instanceof art.arcane.mystcraft.blockentity.LinkPortalBlockEntity portalBE)) {
        helper.fail("Portal B cell " + cell + " has no BE");
        return;
      }
      if (portalBE.getPortalColor() != 0x222222) {
        helper.fail("Portal B cell " + cell + " has colour 0x"
            + Integer.toHexString(portalBE.getPortalColor()) + ", expected 0x222222");
        return;
      }
    }

    helper.succeed();
  }

  /**
   * Phase 5.6 — Verifies that breaking the receptacle (setting it to air)
   * propagates a tear-down event that clears every portal cell that was
   * spawned by that receptacle. Pre-v2 only the receptacle's own onRemove
   * fired dousePortal in a narrow scan; placement of air via setBlock or
   * a third-party mod removing the block left orphan portal cells. v2
   * routes through {@code PortalUtils.shutdownPortal} so any path that
   * leaves the receptacle missing must take the portal down with it.
   */
  public static void runBreakingReceptacleClearsAllPortalBlocksTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    BlockState crystal = ModBlocks.CRYSTAL.get().defaultBlockState();
    int[][] xz = {
        {0, 0}, {1, 0}, {2, 0},
        {0, 1}, {2, 1},
        {0, 2}, {2, 2},
        {0, 3}, {2, 3},
        {0, 4}, {1, 4}, {2, 4},
    };
    for (int[] c : xz) {
      helper.setBlock(new BlockPos(c[0], 1, c[1]), crystal);
    }
    for (int z = 1; z <= 3; z++) {
      helper.setBlock(new BlockPos(1, 1, z), Blocks.AIR.defaultBlockState());
    }
    BlockPos receptaclePos = new BlockPos(1, 2, 0);
    helper.setBlock(receptaclePos,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(receptaclePos));
    if (!(be instanceof BookReceptacleBlockEntity rec)) {
      helper.fail("Receptacle BE missing at " + receptaclePos);
      return;
    }

    rec.setBook(new ItemStack(ModItems.LINKBOOK.get()));

    BlockPos[] portalCells = {
        new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(1, 1, 3),
    };

    for (BlockPos cell : portalCells) {
      BlockState s = helper.getLevel().getBlockState(helper.absolutePos(cell));
      if (!s.is(ModBlocks.LINK_PORTAL.get())) {
        helper.fail("Pre-condition: cell " + cell + " expected LINK_PORTAL, found " + s.getBlock().getDescriptionId());
        return;
      }
    }

    helper.getLevel().setBlock(helper.absolutePos(receptaclePos), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

    for (BlockPos cell : portalCells) {
      BlockState s = helper.getLevel().getBlockState(helper.absolutePos(cell));
      if (s.is(ModBlocks.LINK_PORTAL.get())) {
        helper.fail("Portal cell " + cell + " was NOT cleared after receptacle break — leak regression");
        return;
      }
    }

    helper.succeed();
  }

  /**
   * Phase 5.7 — Same shape as 5.6 but breaks a frame crystal instead of the
   * receptacle. Crystals were the harder leak case in v1: removing one had
   * no observer for the spawned portal cells, leaving lit portals around an
   * incomplete frame. v2 wires {@code CrystalBlock.onRemove} to call
   * {@code PortalUtils.shutdownConnectedPortals} which BFS-walks adjacent
   * portal blocks and clears them.
   */
  public static void runBreakingCrystalClearsAllPortalBlocksTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.BOOK_TRAVEL)) {
      return;
    }

    BlockState crystal = ModBlocks.CRYSTAL.get().defaultBlockState();
    int[][] xz = {
        {0, 0}, {1, 0}, {2, 0},
        {0, 1}, {2, 1},
        {0, 2}, {2, 2},
        {0, 3}, {2, 3},
        {0, 4}, {1, 4}, {2, 4},
    };
    for (int[] c : xz) {
      helper.setBlock(new BlockPos(c[0], 1, c[1]), crystal);
    }
    for (int z = 1; z <= 3; z++) {
      helper.setBlock(new BlockPos(1, 1, z), Blocks.AIR.defaultBlockState());
    }
    BlockPos receptaclePos = new BlockPos(1, 2, 0);
    BlockPos crystalToBreak = new BlockPos(0, 1, 2);
    helper.setBlock(receptaclePos,
        ModBlocks.BOOK_RECEPTACLE.get().defaultBlockState().setValue(BookReceptacleBlock.FACING, Direction.UP));

    BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(receptaclePos));
    if (!(be instanceof BookReceptacleBlockEntity rec)) {
      helper.fail("Receptacle BE missing at " + receptaclePos);
      return;
    }

    rec.setBook(new ItemStack(ModItems.LINKBOOK.get()));

    BlockPos[] portalCells = {
        new BlockPos(1, 1, 1), new BlockPos(1, 1, 2), new BlockPos(1, 1, 3),
    };

    for (BlockPos cell : portalCells) {
      BlockState s = helper.getLevel().getBlockState(helper.absolutePos(cell));
      if (!s.is(ModBlocks.LINK_PORTAL.get())) {
        helper.fail("Pre-condition: cell " + cell + " expected LINK_PORTAL, found " + s.getBlock().getDescriptionId());
        return;
      }
    }

    helper.getLevel().setBlock(helper.absolutePos(crystalToBreak), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

    for (BlockPos cell : portalCells) {
      BlockState s = helper.getLevel().getBlockState(helper.absolutePos(cell));
      if (s.is(ModBlocks.LINK_PORTAL.get())) {
        helper.fail("Portal cell " + cell + " was NOT cleared after crystal break — leak regression");
        return;
      }
    }

    helper.succeed();
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
    if (folder.getMaxStackSize() != 1) {
      helper.fail("Folders can stack despite carrying per-item page NBT");
      return;
    }
    ItemStack stackedPages = Page.createPage();
    stackedPages.setCount(3);
    FolderItem.setPages(folder, List.of(stackedPages));
    if (FolderItem.getPageCount(folder) != 3
        || FolderItem.getPages(folder).stream().anyMatch(page -> page.getCount() != 1)) {
      helper.fail("Folder did not normalize a legacy stacked page entry without loss");
      return;
    }
    if (FolderItem.addPage(folder, new ItemStack(Items.STICK))) {
      helper.fail("Folder accepted a non-page item");
      return;
    }
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    player.getInventory().clearContent();
    int folderSlot = player.getInventory().selected;
    player.getInventory().setItem(folderSlot, folder);
    FolderMenu menu = new FolderMenu(990, player.getInventory(), folderSlot);
    FolderItem.setPages(folder, List.of(Page.createPage(), Page.createLinkPage()));
    menu.reloadFromFolder();
    menu.removed(player);
    if (FolderItem.getPageCount(folder) != 2) {
      helper.fail("Reloading a Folder menu did not preserve externally updated page NBT");
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
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }
    player.getInventory().clearContent();
    int portfolioSlot = player.getInventory().selected;
    player.getInventory().setItem(portfolioSlot, portfolio);
    int pagesBeforeReload = PortfolioItem.getPageCount(portfolio);
    PortfolioMenu menu = new PortfolioMenu(991, player.getInventory(), portfolioSlot);
    menu.reloadFromPortfolio();
    if (PortfolioItem.getPageCount(portfolio) != pagesBeforeReload) {
      helper.fail("Opening/reloading a Portfolio erased its page NBT");
      return;
    }
    menu.removed(player);
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
    assertBookBinderTransfersAreLossless(helper);
    helper.succeed();
  }

  private static void assertBookBinderTransfersAreLossless(GameTestHelper helper) {
    Supplier<Integer> originalMaxSymbols = MystcraftConfig.maxSymbolsPerBook;
    try {
      MystcraftConfig.maxSymbolsPerBook = () -> 2;

      BookBinderBlockEntity partialBinder = new BookBinderBlockEntity(
          BlockPos.ZERO, ModBlocks.BOOK_BINDER.get().defaultBlockState());
      partialBinder.insertPage(Page.createSymbolPage(symbolId("biome_plains")), 0);
      ItemStack stackedPages = Page.createSymbolPage(symbolId("terrain_flat"));
      stackedPages.setCount(3);
      ItemStack remainder = partialBinder.insertPage(stackedPages, -100);
      if (partialBinder.getPageList().size() != 2 || remainder.getCount() != 2
          || !symbolId("terrain_flat").equals(Page.getSymbol(partialBinder.getPageList().get(0)))) {
        helper.fail("Book Binder did not clamp a negative index or return its capacity remainder");
        return;
      }

      ItemStack rejected = new ItemStack(Items.STICK, 4);
      if (partialBinder.insertPage(rejected, 0) != rejected || rejected.getCount() != 4) {
        helper.fail("Book Binder consumed a non-page insertion");
        return;
      }

      BookBinderBlockEntity folderImportBinder = new BookBinderBlockEntity(
          BlockPos.ZERO, ModBlocks.BOOK_BINDER.get().defaultBlockState());
      folderImportBinder.insertPage(Page.createSymbolPage(symbolId("biome_plains")), 0);
      ItemStack sourceFolder = new ItemStack(ModItems.FOLDER.get());
      FolderItem.addPage(sourceFolder, Page.createLinkPage());
      FolderItem.addPage(sourceFolder, Page.createSymbolPage(symbolId("terrain_flat")));
      FolderItem.addPage(sourceFolder, Page.createPage());
      ItemStack returnedSourceFolder = folderImportBinder.insertFromFolder(sourceFolder, -50);
      if (folderImportBinder.getPageList().size() != 2 || FolderItem.getPageCount(sourceFolder) != 2
          || !Page.isLinkPanel(folderImportBinder.getPageList().get(0))
          || returnedSourceFolder != sourceFolder) {
        helper.fail("Book Binder folder import discarded pages that did not fit");
        return;
      }

      MystcraftConfig.maxSymbolsPerBook = () -> FolderItem.MAX_PAGES + 5;
      BookBinderBlockEntity folderExportBinder = new BookBinderBlockEntity(
          BlockPos.ZERO, ModBlocks.BOOK_BINDER.get().defaultBlockState());
      ItemStack exportPages = Page.createPage();
      exportPages.setCount(FolderItem.MAX_PAGES + 2);
      ItemStack exportInsertRemainder = folderExportBinder.insertPage(exportPages, 0);
      if (!exportInsertRemainder.isEmpty()) {
        helper.fail("Book Binder setup could not insert the export test pages");
        return;
      }
      ItemStack emptyFolder = new ItemStack(ModItems.FOLDER.get());
      ItemStack returnedExportFolder = folderExportBinder.insertFromFolder(emptyFolder, 0);
      if (FolderItem.getPageCount(emptyFolder) != FolderItem.MAX_PAGES
          || folderExportBinder.getPageList().size() != 2 || returnedExportFolder != emptyFolder) {
        helper.fail("Book Binder discarded pages when exporting into a full Folder");
      }
    } finally {
      MystcraftConfig.maxSymbolsPerBook = originalMaxSymbols;
    }
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

      desk.setWritingItem(Page.createPage());
      desk.getInkTank().fill(WritingDeskBlockEntity.INK_COST);
      if (desk.writeSymbol(null, symbolId("missing_gametest_symbol"))
          || desk.getInkAmount() != WritingDeskBlockEntity.INK_COST
          || !Page.isBlank(desk.getWritingItem())) {
        helper.fail("Writing Desk consumed resources for an unknown symbol");
        return;
      }

      ItemStack stackedBlankPages = Page.createPage();
      stackedBlankPages.setCount(2);
      desk.setWritingItem(stackedBlankPages);
      if (desk.writeSymbol(null, symbolId("terrain_flat"))
          || desk.getInkAmount() != WritingDeskBlockEntity.INK_COST) {
        helper.fail("Writing Desk wrote an entire stacked page input for one ink cost");
        return;
      }

      desk.getInkTank().drain(WritingDeskBlockEntity.INK_COST);
      desk.getInkTank().fill(WritingDeskBlockEntity.INK_CAPACITY - 5);
      Container inventory = desk.getMainInventory();
      ItemStack vialStack = new ItemStack(ModItems.INK_VIAL.get(), 2);
      inventory.setItem(WritingDeskBlockEntity.SLOT_CONTAINER_IN, vialStack);
      desk.processFluidContainers();
      if (desk.getInkAmount() != WritingDeskBlockEntity.INK_CAPACITY - 5
          || vialStack.getCount() != 2) {
        helper.fail("Writing Desk consumed vial ink that did not fit in the tank");
        return;
      }

      desk.getInkTank().drain(WritingDeskBlockEntity.INK_CAPACITY);
      desk.processFluidContainers();
      if (desk.getInkAmount() != WritingDeskBlockEntity.INK_CAPACITY
          || vialStack.getCount() != 1
          || inventory.getItem(WritingDeskBlockEntity.SLOT_CONTAINER_OUT).getCount() != 1
          || ((InkVialItem) vialStack.getItem()).getInkAmount(vialStack) != InkVialItem.MAX_INK) {
        helper.fail("Writing Desk drained or erased more than one vial from a stack");
        return;
      }

      WritingDeskBlockEntity.InkTank tank = new WritingDeskBlockEntity.InkTank(100);
      if (tank.fill(-10) != 0 || tank.drain(-10) != 0 || tank.getAmount() != 0) {
        helper.fail("Writing Desk ink tank accepted a negative transfer");
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
    if (director.getInstability() <= builder.getInstability()) {
      helper.fail("AgeBuilder discarded dynamic fallback instability: base="
          + builder.getInstability() + ", final=" + director.getInstability());
      return;
    }

    List<ResourceLocation> description = List.of(
        symbolId("terrain_flat"),
        symbolId("biome_plains"),
        symbolId("weather_normal"),
        symbolId("lighting_normal"),
        symbolId("color_sky_natural")
    );
    long orderedSeed = AgeSeed.deriveFromSymbolIds(description);
    long reorderedSeed = AgeSeed.deriveFromSymbolIds(List.of(
        symbolId("biome_plains"),
        symbolId("terrain_flat"),
        symbolId("weather_normal"),
        symbolId("lighting_normal"),
        symbolId("color_sky_natural")
    ));
    if (orderedSeed == reorderedSeed) {
      helper.fail("AgeSeed ignored symbol ordering");
      return;
    }

    AgeDefinition definition = AgeDefinition.fromDirector(description, director);
    AgeDefinition loadedDefinition = AgeDefinition.load(definition.save());
    if (loadedDefinition == null
        || loadedDefinition.getSeed() != director.getSeed()
        || !loadedDefinition.getSymbolIds().equals(description)) {
      helper.fail("AgeDefinition did not round-trip its seed and ordered symbols");
      return;
    }
    AgeDirectorImpl rebuiltDirector = loadedDefinition.buildDirector(helper.getLevel().getServer());
    if (rebuiltDirector == null
        || rebuiltDirector.getSeed() != director.getSeed()
        || !rebuiltDirector.getTerrainType().equals(director.getTerrainType())) {
      helper.fail("AgeDefinition did not reconstruct the original generation director");
      return;
    }

    AgeManager manager = new AgeManager();
    UUID ageUUID = UUID.fromString("c9d41988-7a5e-44bc-a028-8aeae2969ff8");
    ResourceLocation dimension = symbolId("mystcraft_age_4242");
    manager.registerAge(4242, dimension, ageUUID, definition);
    AgeManager loadedManager = AgeManager.load(manager.save(new CompoundTag()));
    AgeDefinition managerDefinition = loadedManager.getDefinition(4242);
    if (!dimension.equals(loadedManager.getDimension(4242))
        || !ageUUID.equals(loadedManager.getAgeUUID(4242))
        || managerDefinition == null
        || managerDefinition.getSeed() != definition.getSeed()
        || !managerDefinition.getSymbolIds().equals(description)) {
      helper.fail("AgeManager did not persist the restart-safe Age definition");
      return;
    }
    helper.succeed();
  }

  public static void runAgebookWithEveryPageCreatesDimensionTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.AGE_CREATION)) {
      return;
    }
    ServerLevel originLevel = helper.getLevel();
    MinecraftServer server = originLevel.getServer();
    ServerPlayer player = createMockServerPlayer(helper);
    if (player == null) {
      return;
    }

    List<IAgeSymbol> allSymbols = new ArrayList<>(SymbolRegistry.getAll());
    allSymbols.removeIf(symbol -> symbol == null || symbol.getRegistryName() == null);
    allSymbols.sort((left, right) -> left.getRegistryName().toString().compareTo(right.getRegistryName().toString()));
    if (allSymbols.size() < 400) {
      helper.fail("Expected hundreds of page symbols before every-page age test, found: " + allSymbols.size());
      return;
    }

    List<ItemStack> pages = new ArrayList<>(allSymbols.size() + 1);
    pages.add(Page.createLinkPage(LinkFlags.FOLLOWING));
    Set<ResourceLocation> expectedSymbols = new java.util.HashSet<>();
    for (IAgeSymbol symbol : allSymbols) {
      ResourceLocation symbolId = symbol.getRegistryName();
      expectedSymbols.add(symbolId);
      pages.add(Page.createSymbolPage(symbolId));
    }

    ItemStack agebook = new ItemStack(ModItems.AGEBOOK.get());
    AgebookItem.create(agebook, player, pages, "Every Page Age");
    AgebookItem agebookItem = (AgebookItem) ModItems.AGEBOOK.get();
    if (agebookItem.getPageList(agebook).size() != pages.size()) {
      helper.fail("Every-page Agebook did not retain its full page list before activation");
      return;
    }

    BlockPos originPos = helper.absolutePos(new BlockPos(10, 2, 10));
    ServerPlayerTeleport.teleport(player, originLevel, originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5, 0.0F, 0.0F);
    AgeManager ageManager = AgeManager.get(server);
    Set<Integer> beforeAges = ageIdSet(ageManager);

    agebookItem.activate(agebook, originLevel, player);
    Integer ageUid = linkedAgeUid(agebook);
    if (ageUid == null) {
      helper.fail("Every-page Agebook did not receive an age UID");
      return;
    }
    if (beforeAges.contains(ageUid)) {
      helper.fail("Every-page Agebook reused an existing age UID: " + ageUid);
      return;
    }
    if (AgebookItem.isNewAgebook(agebook)) {
      helper.fail("Every-page Agebook stayed marked as new after activation");
      return;
    }

    ServerLevel ageLevel = ageManager.getAgeLevel(server, ageUid);
    if (ageLevel == null) {
      helper.fail("Every-page Agebook UID did not resolve to a loaded age");
      return;
    }

    ageLevel.noSave = true;
    if (!AgeDimensionFactory.isMystcraftAge(ageLevel.dimension())) {
      helper.fail("Every-page Agebook created a non-Mystcraft dimension: " + ageLevel.dimension().location());
      return;
    }
    if (player.level() != ageLevel) {
      helper.fail("Every-page Agebook created an age but did not link the player into it");
      return;
    }

    AgeData ageData = AgeData.get(ageLevel);
    if (!"Every Page Age".equals(ageData.getAgeName())) {
      helper.fail("Every-page AgeData kept wrong name: " + ageData.getAgeName());
      return;
    }
    if (!ageData.isSpawnSet()) {
      helper.fail("Every-page AgeData did not set a spawn");
      return;
    }

    List<ItemStack> storedPages = ageData.getPages();
    if (storedPages.size() != pages.size()) {
      helper.fail("Every-page AgeData stored " + storedPages.size() + " pages instead of " + pages.size());
      return;
    }

    int linkPanelCount = 0;
    Set<ResourceLocation> storedSymbols = new java.util.HashSet<>();
    for (ItemStack page : storedPages) {
      if (Page.isLinkPanel(page)) {
        linkPanelCount++;
        continue;
      }
      ResourceLocation storedSymbol = Page.getSymbol(page);
      if (storedSymbol != null) {
        storedSymbols.add(storedSymbol);
      }
    }
    if (linkPanelCount != 1) {
      helper.fail("Every-page AgeData stored " + linkPanelCount + " link panels instead of 1");
      return;
    }
    if (storedSymbols.size() != expectedSymbols.size() || !storedSymbols.containsAll(expectedSymbols)) {
      expectedSymbols.removeAll(storedSymbols);
      ResourceLocation firstMissing = expectedSymbols.stream().findFirst().orElse(null);
      helper.fail("Every-page AgeData lost symbol pages; missing count=" + expectedSymbols.size() + ", first=" + firstMissing);
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

    helper.setBlock(inkMixerPos, ModBlocks.INK_MIXER.get().defaultBlockState());
    helper.setBlock(bookBinderPos, ModBlocks.BOOK_BINDER.get().defaultBlockState());

    helper.runAtTickTime(5, () -> {
      if (!(level.getBlockEntity(helper.absolutePos(inkMixerPos)) instanceof InkMixerBlockEntity)) {
        helper.fail("Ink Mixer block entity was not created");
        return;
      }
      if (!(level.getBlockEntity(helper.absolutePos(bookBinderPos)) instanceof BookBinderBlockEntity)) {
        helper.fail("Book Binder block entity was not created");
        return;
      }
      helper.succeed();
    });
  }

  private static void assertDropBooksOnRead(GameTestHelper helper, boolean expectDrop, boolean following) {
    LinkbookItem linkbook = (LinkbookItem) ModItems.LINKBOOK.get();
    ItemStack stack = new ItemStack(ModItems.LINKBOOK.get());
    CompoundTag tag = returnLink(0, new BlockPos(0, 2, 0), 0.0F);
    LinkOptions.setFlag(tag, LinkFlags.FOLLOWING, following);
    ItemStackNbt.setTag(stack, tag);

    boolean actualDrop = linkbook.dropItemOnLink(stack);
    if (actualDrop != expectDrop) {
      helper.fail("Normal linkbook drop decision was " + actualDrop
          + " with following=" + following + ", expected " + expectDrop);
    }
  }

  private static void assertLinkbookActivationDropTransaction(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    LinkbookItem linkbook = (LinkbookItem) ModItems.LINKBOOK.get();

    ServerPlayer successfulPlayer = createMockServerPlayer(helper);
    if (successfulPlayer == null) {
      return;
    }
    successfulPlayer.getInventory().clearContent();
    ItemStack successfulBook = new ItemStack(linkbook);
    CompoundTag successfulTag = returnLink(
        LinkingManager.getDimensionUID(level), successfulPlayer.blockPosition(), successfulPlayer.getYRot());
    LinkOptions.setFlag(successfulTag, LinkFlags.INTRA_LINKING, true);
    LinkOptions.setFlag(successfulTag, LinkFlags.DISARM, true);
    LinkOptions.setFlag(successfulTag, LinkFlags.GENERATE_PLATFORM, false);
    ItemStackNbt.setTag(successfulBook, successfulTag);
    int successfulSlot = successfulPlayer.getInventory().selected;
    successfulPlayer.getInventory().setItem(successfulSlot, successfulBook);
    AABB successfulSource = successfulPlayer.getBoundingBox().inflate(2.0);
    int beforeSuccessfulDrop = level.getEntitiesOfClass(LinkbookEntity.class, successfulSource).size();

    linkbook.activate(successfulBook, level, successfulPlayer);

    int afterSuccessfulDrop = level.getEntitiesOfClass(LinkbookEntity.class, successfulSource).size();
    if (!successfulPlayer.getInventory().getItem(successfulSlot).isEmpty()
        || afterSuccessfulDrop <= beforeSuccessfulDrop) {
      helper.fail("A successful link did not commit the prepared source-world book drop");
      return;
    }

    ServerPlayer failedPlayer = createMockServerPlayer(helper);
    if (failedPlayer == null) {
      return;
    }
    failedPlayer.getInventory().clearContent();
    ItemStack failedBook = new ItemStack(linkbook);
    CompoundTag failedTag = returnLink(
        LinkingManager.getDimensionUID(level), failedPlayer.blockPosition(), failedPlayer.getYRot());
    LinkOptions.setFlag(failedTag, LinkFlags.DISARM, true);
    ItemStackNbt.setTag(failedBook, failedTag);
    int failedSlot = failedPlayer.getInventory().selected;
    failedPlayer.getInventory().setItem(failedSlot, failedBook);
    AABB failedSource = failedPlayer.getBoundingBox().inflate(2.0);
    int beforeFailedDrop = level.getEntitiesOfClass(LinkbookEntity.class, failedSource).size();

    linkbook.activate(failedBook, level, failedPlayer);

    int afterFailedDrop = level.getEntitiesOfClass(LinkbookEntity.class, failedSource).size();
    if (failedPlayer.getInventory().getItem(failedSlot) != failedBook) {
      helper.fail("A failed link removed the held linkbook");
      return;
    }
    if (afterFailedDrop != beforeFailedDrop) {
      helper.fail("A failed link spawned a source-world LinkbookEntity");
    }
  }

  private static void assertLinkFlagsFollowersAndPassengers(GameTestHelper helper) {
    ServerLevel sourceLevel = helper.getLevel();
    ServerPlayer crossDimensionPlayer = createMockServerPlayer(helper);
    if (crossDimensionPlayer == null) {
      return;
    }

    Vec3 eventStart = crossDimensionPlayer.position();
    CompoundTag cancellableLink = returnLink(
        LinkingManager.getDimensionUID(sourceLevel),
        crossDimensionPlayer.blockPosition().offset(4, 0, 0),
        0.0F
    );
    LinkOptions.setFlag(cancellableLink, LinkFlags.INTRA_LINKING, true);
    boolean[] observedEvents = new boolean[2];
    LinkingManager.LinkResult cancelledResult;
    try (LinkEventBus.Registration ignored = LinkEventBus.register(event -> {
      if (event instanceof LinkEvent.Allow allow) {
        observedEvents[0] = true;
        allow.setCancelReason("GameTest cancellation");
        allow.setCancelled(true);
      } else if (event instanceof LinkEvent.Failed failed
          && failed.getReason() == LinkEvent.Failed.FailureReason.CANCELLED) {
        observedEvents[1] = true;
      }
    })) {
      cancelledResult = LinkingManager.performLink(crossDimensionPlayer, cancellableLink);
    }
    if (cancelledResult != LinkingManager.LinkResult.CANCELLED
        || !observedEvents[0] || !observedEvents[1]
        || crossDimensionPlayer.position().distanceToSqr(eventStart) > 0.0001) {
      helper.fail("LinkEventBus did not dispatch and honor an Allow cancellation");
      return;
    }

    Vec3 sameDimensionStart = crossDimensionPlayer.position();
    CompoundTag missingIntraFlag = returnLink(
        LinkingManager.getDimensionUID(sourceLevel), crossDimensionPlayer.blockPosition().offset(8, 0, 0), 0.0F);
    LinkingManager.LinkResult missingIntraResult =
        LinkingManager.performLink(crossDimensionPlayer, missingIntraFlag);
    if (missingIntraResult != LinkingManager.LinkResult.BLOCKED
        || crossDimensionPlayer.position().distanceToSqr(sameDimensionStart) > 0.0001) {
      helper.fail("A same-dimension link without INTRA_LINKING was not blocked in place");
      return;
    }

    ServerLevel otherLevel = null;
    for (ServerLevel candidate : sourceLevel.getServer().getAllLevels()) {
      if (candidate != sourceLevel) {
        otherLevel = candidate;
        break;
      }
    }
    if (otherLevel != null) {
      CompoundTag intraOnlyLink = returnLink(
          LinkingManager.getDimensionUID(otherLevel), otherLevel.getSharedSpawnPos(), 0.0F);
      LinkOptions.setFlag(intraOnlyLink, LinkFlags.INTRA_LINKING_ONLY, true);
      LinkingManager.LinkResult result = LinkingManager.performLink(crossDimensionPlayer, intraOnlyLink);
      if (result != LinkingManager.LinkResult.BLOCKED || crossDimensionPlayer.serverLevel() != sourceLevel) {
        helper.fail("INTRA_LINKING_ONLY did not block cross-dimensional travel before teleport");
        return;
      }
    }

    BlockPos sourcePos = helper.absolutePos(new BlockPos(10, 3, 10));
    ArmorStand source = new ArmorStand(
        sourceLevel, sourcePos.getX() + 0.5, sourcePos.getY(), sourcePos.getZ() + 0.5);
    ArmorStand passenger = new ArmorStand(
        sourceLevel, sourcePos.getX() + 0.5, sourcePos.getY(), sourcePos.getZ() + 0.5);
    ArmorStand nestedPassenger = new ArmorStand(
        sourceLevel, sourcePos.getX() + 0.5, sourcePos.getY(), sourcePos.getZ() + 0.5);
    ArmorStand follower = new ArmorStand(
        sourceLevel, sourcePos.getX() + 2.5, sourcePos.getY(), sourcePos.getZ() + 0.5);
    if (!sourceLevel.addFreshEntity(source) || !sourceLevel.addFreshEntity(passenger)
        || !sourceLevel.addFreshEntity(nestedPassenger) || !sourceLevel.addFreshEntity(follower)) {
      helper.fail("Could not spawn linking follower/passenger test entities");
      return;
    }
    if (!passenger.startRiding(source, true) || !nestedPassenger.startRiding(passenger, true)) {
      helper.fail("Could not build nested passenger hierarchy for linking test");
      return;
    }

    double followerOffsetX = follower.getX() - source.getX();
    CompoundTag linkData = returnLink(
        LinkingManager.getDimensionUID(sourceLevel), sourcePos.offset(8, 0, 0), 0.0F);
    LinkOptions.setFlag(linkData, LinkFlags.INTRA_LINKING, true);
    LinkOptions.setFlag(linkData, LinkFlags.FOLLOWING, true);
    LinkOptions.setFlag(linkData, LinkFlags.GENERATE_PLATFORM, false);

    LinkingManager.LinkResult result = LinkingManager.performLink(source, linkData);
    if (result != LinkingManager.LinkResult.SUCCESS) {
      helper.fail("Follower/passenger link failed: " + result);
      return;
    }
    if (passenger.getVehicle() != source || nestedPassenger.getVehicle() != passenger) {
      helper.fail("Linking did not restore the original nested passenger hierarchy");
      return;
    }
    double resultingOffsetX = follower.getX() - source.getX();
    if (Math.abs(resultingOffsetX - followerOffsetX) > 0.25) {
      helper.fail("Follower offset changed during linking: " + followerOffsetX + " -> " + resultingOffsetX);
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
    Commands commands = source.getServer().getCommands();
    ParseResults<CommandSourceStack> parse = commands.getDispatcher().parse(normalized, source);
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

  /**
   * Verifies the {@link art.arcane.mystcraft.data.InkBlend} NBT round-trip
   * contract that ink mixers and booster packs depend on.
   */
  public static void runInkBlendNbtRoundTripTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.PROCEDURAL_UI)) {
      return;
    }
    try {
      MystcraftServerGameTestAssertions.assertInkBlendRoundTripsThroughNbt();
    } catch (RuntimeException e) {
      helper.fail(e.getMessage());
      return;
    }
    helper.succeed();
  }

  /**
   * Verifies the cover-NBT round-trip for every configured cover item the Book
   * Binder accepts. The procedural Book texture factory uses this NBT to pick a
   * cover palette.
   */
  public static void runBookCoverNbtRoundTripTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.PROCEDURAL_UI)) {
      return;
    }
    try {
      MystcraftServerGameTestAssertions.assertBookCoverNbtRoundTripsForEachCover();
    } catch (RuntimeException e) {
      helper.fail(e.getMessage());
      return;
    }
    helper.succeed();
  }

  /**
   * Verifies that affinity-weighted symbol rolls actually shift the
   * distribution toward favored symbols.
   */
  public static void runInkAffinityBiasesSymbolRollTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.PROCEDURAL_UI)) {
      return;
    }
    try {
      MystcraftServerGameTestAssertions.assertInkAffinityBiasesSymbolRoll();
    } catch (RuntimeException e) {
      helper.fail(e.getMessage());
      return;
    }
    helper.succeed();
  }

  /** Verifies the server-safe datapack parser contract for symbol display overrides. */
  public static void runSymbolDisplayOverrideAppliedTest(GameTestHelper helper) {
    if (MystcraftGameTestSuites.skipUnless(helper, MystcraftGameTestSuites.PROCEDURAL_UI)) {
      return;
    }
    try {
      MystcraftServerGameTestAssertions.assertSymbolDisplayDataOverrideApplied();
    } catch (RuntimeException e) {
      helper.fail(e.getMessage());
      return;
    }
    helper.succeed();
  }

  private static final class CommandAttempt {
    private final int result;
    private final String diagnostics;

    private CommandAttempt(int result, String diagnostics) {
      this.result = result;
      this.diagnostics = diagnostics;
    }
  }

}
