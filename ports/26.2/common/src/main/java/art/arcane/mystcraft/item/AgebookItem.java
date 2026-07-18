package art.arcane.mystcraft.item;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.LinkOptions;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.event.AgeDataSyncHandler;
import art.arcane.mystcraft.grammar.AgeBuilder;
import art.arcane.mystcraft.link.LinkingManager;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.util.ItemStackNbt;
import art.arcane.mystcraft.util.TooltipCompat;
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDefinition;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.AgeSeed;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Descriptive Book (Agebook) item. Contains the complete description of an
 * Age and allows travel to it. Ages are created when the book is first used
 * with a link panel.
 */
public class AgebookItem extends Item implements TooltipCompat {

  private static final String TAG_PAGES = "Pages";
  private static final String TAG_AUTHORS = "Authors";

  public AgebookItem(Properties properties) {
    super(properties);
  }

  /**
   * Creates an Agebook from pages.
   */
  public static void create(ItemStack agebook, Player player, List<ItemStack> pages, String title) {
    ItemStackNbt.setTag(agebook, new CompoundTag());

    AgebookItem item = (AgebookItem) agebook.getItem();
    item.addPages(agebook, pages);
    item.addAuthor(agebook, player);
    item.setDisplayName(agebook, title);

    if (!pages.isEmpty()) {
      ItemStack linkpanel = pages.get(0);
      if (Page.isLinkPanel(linkpanel)) {
        Page.applyLinkPanel(linkpanel, agebook);
      }
    }
  }

  /**
   * Checks if this is a new (unlinked) Agebook.
   */
  public static boolean isNewAgebook(ItemStack stack) {
    if (!(stack.getItem() instanceof AgebookItem)) {
      return false;
    }
    if (ItemStackNbt.getTag(stack) == null) {
      return false;
    }
    Integer dimId = LinkOptions.getDimensionUID(ItemStackNbt.getTag(stack));
    if (dimId != null) {
      return false;
    }
    List<ItemStack> pages = ((AgebookItem) stack.getItem()).getPageList(stack);
    return !pages.isEmpty() && Page.isLinkPanel(pages.get(0));
  }

  @NotNull
  public Rarity getRarity(@NotNull ItemStack stack) {
    return stack.isEnchanted() ? Rarity.RARE : Rarity.EPIC;
  }

  @Override
  @NotNull
  public Component getName(@NotNull ItemStack stack) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null) {
      String displayName = LinkOptions.getDisplayName(tag);
      if (!"???".equals(displayName)) {
        return Component.literal(displayName);
      }
    }
    return super.getName(stack);
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                              @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltip,
                              @NotNull TooltipFlag flag) {
    CompoundTag tag = ItemStackNbt.getTag(stack);
    if (tag != null) {
      Integer dimId = LinkOptions.getDimensionUID(tag);
      if (dimId != null) {
        tooltip.accept(Component.translatable("item.mystcraft.agebook.age", dimId));
      } else {
        tooltip.accept(Component.translatable("item.mystcraft.agebook.unwritten"));
      }

      Collection<String> authors = getAuthors(stack);
      if (!authors.isEmpty()) {
        tooltip.accept(Component.translatable("item.mystcraft.agebook.authors",
            String.join(", ", authors)));
      }

      List<ItemStack> pages = getPageList(stack);
      if (!pages.isEmpty()) {
        tooltip.accept(Component.translatable("item.mystcraft.agebook.pages", pages.size()));
      }
    }
  }

  /**
   * Right-click always opens the book GUI, matching LinkbookItem behavior.
   * Actual linking happens via packet from the GUI's Link button.
   */
  @Override
  @NotNull
  public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (level.isClientSide()) {
      ItemClientHooks.openBook(stack);
    }

    return InteractionResult.CONSUME;
  }

  /**
   * Activates the book, potentially creating a new Age. Called from packet
   * handler when player clicks Link button in GUI.
   */
  public void activate(ItemStack stack, Level level, Entity entity) {
    if (!(entity instanceof Player player)) {
      return;
    }
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return;
    }

    if (ItemStackNbt.getTag(stack) == null) {
      ItemStackNbt.setTag(stack, new CompoundTag());
    }

    Integer dimId = LinkOptions.getDimensionUID(ItemStackNbt.getTag(stack));

    if (dimId == null) {

      List<ItemStack> pages = getPageList(stack);
      if (!pages.isEmpty() && Page.isLinkPanel(pages.get(0))) {

        createAge(stack, serverLevel, serverPlayer);
      }

    } else {

      linkToAge(stack, serverLevel, serverPlayer);
    }
  }

  private void createAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
    Mystcraft.LOGGER.info("Creating age for player {}", player.getGameProfile().name());

    List<ItemStack> pages = getPageList(stack);
    List<IAgeSymbol> symbols = extractSymbols(pages);

    long seed = AgeSeed.resolve(stack, pages);

    AgeBuilder builder = new AgeBuilder(symbols, seed);
    AgeDirectorImpl director = builder.build();
    if (MystcraftConfig.microDimensionsEnabled.get() && !director.isPersonalPocket()) {
      director.setMicroDimensions(
          true,
          MystcraftConfig.microDimensionRadiusChunks.get(),
          MystcraftConfig.microDimensionExtraChunks.get()
      );
    }

    AgeManager ageManager = AgeManager.get(level);
    int ageUID = ageManager.allocateUID();

    java.util.UUID ageUUID = java.util.UUID.randomUUID();
    Identifier dimLoc = Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, "mystcraft_age_" + ageUID);
    AgeDefinition definition = AgeDefinition.fromDirector(
        symbols.stream().map(IAgeSymbol::getRegistryName).toList(),
        director
    );
    ageManager.registerAge(ageUID, dimLoc, ageUUID, definition);

    ServerLevel ageLevel = AgeDimensionFactory.createAgeDimension(
        level.getServer(), ageUID, ageUUID, director);

    if (ageLevel == null) {
      ageManager.unregisterAge(ageUID);
      Mystcraft.LOGGER.warn("Age creation failed for player {}", player.getGameProfile().name());
      return;
    }

    AgeData ageData = AgeData.get(ageLevel);
    ageData.setAgeUID(ageUID);
    ageData.setAgeUUID(ageUUID);
    ageData.setAgeName(getDisplayName(stack));
    for (String author : getAuthors(stack)) {
      ageData.addAuthor(author);
    }
    ageData.setPages(pages);

    ageData.copyFromDirector(director);

    AgeDataSyncHandler.syncAgeDataToPlayer(player, ageLevel);

    BlockPos spawn = findAgeSpawnPosition(ageLevel);
    ageData.setSpawn(spawn.getX(), spawn.getY(), spawn.getZ());
    AgeDimensionFactory.applyMicroDimensionBorder(ageLevel, ageData);

    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    LinkOptions.setDimensionUID(tag, ageUID);
    LinkOptions.setSpawn(tag, spawn);
    LinkOptions.setUUID(tag, ageUUID);
    ItemStackNbt.setTag(stack, tag);

    if (!builder.isComplete()) {
      Mystcraft.LOGGER.info("Created incomplete age uid={} instability={} player={}",
          ageUID, String.format("%.1f", builder.getInstability()), player.getGameProfile().name());
    } else {
      Mystcraft.LOGGER.info("Created age uid={} player={}", ageUID, player.getGameProfile().name());
    }

    linkToAge(stack, level, player);
  }

  private BlockPos findAgeSpawnPosition(ServerLevel ageLevel) {

    int spawnX = 8;
    int spawnZ = 8;

    if (ageLevel.getChunkSource().getGenerator() instanceof AgeChunkGenerator ageGen) {
      AgeDirectorImpl director = ageGen.getDirector();
      if (director != null) {
        String terrainType = director.getTerrainType();
        int spawnY;

        if ("void".equals(terrainType)) {

          spawnY = 65;
        } else if ("flat".equals(terrainType)) {

          int gl = director.getAverageGroundLevel();
          spawnY = (gl > 0) ? gl + 1 : 65;
        } else if ("nether".equals(terrainType)) {
          spawnY = 33;
        } else if ("end".equals(terrainType)) {
          spawnY = 65;
        } else {

          spawnY = 65;
        }

        art.arcane.mystcraft.Mystcraft.LOGGER.info(
            "[AgebookItem] findAgeSpawnPosition: terrain={}, estimated spawn Y={} (no chunk gen forced)",
            terrainType, spawnY);
        return new BlockPos(spawnX, spawnY, spawnZ);
      }
    }

    art.arcane.mystcraft.Mystcraft.LOGGER.info("[AgebookItem] findAgeSpawnPosition: no director, using fallback Y=65");
    return new BlockPos(spawnX, 65, spawnZ);
  }

  private List<IAgeSymbol> extractSymbols(List<ItemStack> pages) {
    List<IAgeSymbol> symbols = new ArrayList<>();
    for (ItemStack page : pages) {
      if (Page.isLinkPanel(page)) {
        continue;
      }
      Identifier symbolId = Page.getSymbol(page);
      if (symbolId != null) {
        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
        if (symbol != null) {
          symbols.add(symbol);
        }
      }
    }
    return symbols;
  }

  private void linkToAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
    CompoundTag linkData = ItemStackNbt.getTag(stack);
    if (linkData == null) {
      return;
    }

    Integer ageUID = LinkOptions.getDimensionUID(linkData);
    if (ageUID == null) {
      Mystcraft.LOGGER.warn("Agebook link failed: no age UID for player {}", player.getGameProfile().name());
      return;
    }

    LinkingManager.LinkResult result = LinkingManager.performLink(player, linkData);

    if (result != LinkingManager.LinkResult.SUCCESS) {
      Mystcraft.LOGGER.warn("Agebook link failed: result={} player={}", result.name(),
          player.getGameProfile().name());
    }
  }

  /**
   * Gets the list of pages in this book.
   */
  public List<ItemStack> getPageList(ItemStack stack) {
    if (ItemStackNbt.getTag(stack) == null) {
      return Collections.emptyList();
    }
    CompoundTag tag = ItemStackNbt.getTag(stack);
    ListTag listTag = tag.getListOrEmpty(TAG_PAGES);
    List<ItemStack> pages = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      pages.add(ItemStackNbt.load(listTag.getCompoundOrEmpty(i)));
    }
    return pages;
  }

  /**
   * Adds pages to this book.
   */
  public void addPages(ItemStack stack, Collection<ItemStack> pages) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    ListTag listTag = tag.getListOrEmpty(TAG_PAGES);
    for (ItemStack page : pages) {
      listTag.add(ItemStackNbt.save(page));
    }
    tag.put(TAG_PAGES, listTag);
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Sets the page list.
   */
  public void setPageList(ItemStack stack, List<ItemStack> pages) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    ListTag listTag = new ListTag();
    for (ItemStack page : pages) {
      listTag.add(ItemStackNbt.save(page));
    }
    tag.put(TAG_PAGES, listTag);
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Adds an author to this book.
   */
  public void addAuthor(ItemStack stack, Player player) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    ListTag listTag = tag.getListOrEmpty(TAG_AUTHORS);
    String playerName = player.getGameProfile().name();
    boolean found = false;
    for (int i = 0; i < listTag.size(); i++) {
      if (listTag.getStringOr(i, "").equals(playerName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      listTag.add(net.minecraft.nbt.StringTag.valueOf(playerName));
      tag.put(TAG_AUTHORS, listTag);
    }
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Gets the authors of this book.
   */
  public Collection<String> getAuthors(ItemStack stack) {
    if (ItemStackNbt.getTag(stack) == null) {
      return Collections.emptyList();
    }
    CompoundTag tag = ItemStackNbt.getTag(stack);
    ListTag listTag = tag.getListOrEmpty(TAG_AUTHORS);
    List<String> authors = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      authors.add(listTag.getStringOr(i, ""));
    }
    return authors;
  }

  /**
   * Sets the display name of the book.
   */
  public void setDisplayName(ItemStack stack, String name) {
    CompoundTag tag = ItemStackNbt.getOrCreateTag(stack);
    LinkOptions.setDisplayName(tag, name);
    ItemStackNbt.setTag(stack, tag);
  }

  /**
   * Gets the display name of the book.
   */
  public String getDisplayName(ItemStack stack) {
    return LinkOptions.getDisplayName(ItemStackNbt.getTag(stack));
  }

  /**
   * Agebooks with a linked Age have a foil effect to show they're active.
   */
  @Override
  public boolean isFoil(@NotNull ItemStack stack) {

    return ItemStackNbt.getTag(stack) != null && LinkOptions.getDimensionUID(ItemStackNbt.getTag(stack)) != null;
  }

  /**
   * Forge: Q-dropped agebooks should spawn as LinkbookEntity, not ItemEntity.
   * On Forge, subclasses override hasCustomEntity/createEntity.
   */
  public boolean hasCustomEntity(@NotNull ItemStack stack) {
    return MystcraftConfig.droppedBooksBecomeLivingEntities.get();
  }

  /**
   * Creates a LinkbookEntity when Q-dropped so the book renders open on the
   * ground.
   */
  @Nullable
  public Entity createEntity(Level level, Entity location, @NotNull ItemStack stack) {
    if (!MystcraftConfig.droppedBooksBecomeLivingEntities.get()) {
      return null;
    }
    LinkbookEntity entity = new LinkbookEntity(level, location.getX(), location.getY(), location.getZ());
    entity.setBookItem(stack.copy());
    entity.setDeltaMovement(location.getDeltaMovement());
    return entity;
  }
}
