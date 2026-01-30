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
import art.arcane.mystcraft.world.AgeData;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.AgeDirectorImpl;
import art.arcane.mystcraft.world.AgeManager;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Descriptive Book (Agebook) item.
 * Contains the complete description of an Age and allows travel to it.
 * Ages are created when the book is first used with a link panel.
 */
public class AgebookItem extends Item {

  private static final String TAG_PAGES = "Pages";
  private static final String TAG_AUTHORS = "Authors";

  public AgebookItem(Properties properties) {
    super(properties);
  }

  /**
   * Creates an Agebook from pages.
   */
  public static void create(ItemStack agebook, Player player, List<ItemStack> pages, String title) {
    agebook.setTag(new CompoundTag());

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
    if (stack.getTag() == null) {
      return false;
    }
    Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
    if (dimId != null) {
      return false;
    }
    List<ItemStack> pages = ((AgebookItem) stack.getItem()).getPageList(stack);
    return !pages.isEmpty() && Page.isLinkPanel(pages.get(0));
  }

  @Override
  @NotNull
  public Rarity getRarity(@NotNull ItemStack stack) {
    return stack.isEnchanted() ? Rarity.RARE : Rarity.EPIC;
  }

  @Override
  @NotNull
  public Component getName(@NotNull ItemStack stack) {
    if (stack.getTag() != null) {
      String displayName = LinkOptions.getDisplayName(stack.getTag());
      if (!"???".equals(displayName)) {
        return Component.literal(displayName);
      }
    }
    return super.getName(stack);
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    if (stack.getTag() != null) {
      Integer dimId = LinkOptions.getDimensionUID(stack.getTag());
      if (dimId != null) {
        tooltip.add(Component.translatable("item.mystcraft.agebook.age", dimId));
      } else {
        tooltip.add(Component.translatable("item.mystcraft.agebook.unwritten"));
      }

      Collection<String> authors = getAuthors(stack);
      if (!authors.isEmpty()) {
        tooltip.add(Component.translatable("item.mystcraft.agebook.authors",
            String.join(", ", authors)));
      }

      List<ItemStack> pages = getPageList(stack);
      if (!pages.isEmpty()) {
        tooltip.add(Component.translatable("item.mystcraft.agebook.pages", pages.size()));
      }
    }
  }

  /**
   * Right-click always opens the book GUI, matching LinkbookItem behavior.
   * Actual linking happens via packet from the GUI's Link button.
   */
  @Override
  @NotNull
  public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    // Always open GUI on client (linking happens via packet from Link button)
    if (level.isClientSide) {
      art.arcane.mystcraft.client.screen.BookScreen.open(stack);
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  /**
   * Activates the book, potentially creating a new Age.
   * Called from packet handler when player clicks Link button in GUI.
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

    if (stack.getTag() == null) {
      stack.setTag(new CompoundTag());
    }

    Integer dimId = LinkOptions.getDimensionUID(stack.getTag());

    if (dimId == null) {
      // This is a new book - check if it has a link panel
      List<ItemStack> pages = getPageList(stack);
      if (!pages.isEmpty() && Page.isLinkPanel(pages.get(0))) {
        // Create a new Age dimension
        createAge(stack, serverLevel, serverPlayer);
      }
      // Silent in chat; logged to console in createAge/linkToAge.
    } else {
      // Existing Age - perform linking
      linkToAge(stack, serverLevel, serverPlayer);
    }
  }

  /**
   * Creates a new Age dimension for this Agebook.
   */
  private void createAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
    Mystcraft.LOGGER.info("Creating age for player {}", player.getGameProfile().getName());

    // Extract symbols from pages
    List<ItemStack> pages = getPageList(stack);
    List<IAgeSymbol> symbols = extractSymbols(pages);

    // Generate a seed from the player's position and time
    long seed = System.currentTimeMillis() ^ player.blockPosition().asLong();

    // Build the Age using the grammar system
    AgeBuilder builder = new AgeBuilder(symbols, seed);
    AgeDirectorImpl director = builder.build();
    if (MystcraftConfig.microDimensionsEnabled.get() && !director.isPersonalPocket()) {
      director.setMicroDimensions(
          true,
          MystcraftConfig.microDimensionRadiusChunks.get(),
          MystcraftConfig.microDimensionExtraChunks.get()
      );
    }

    // Allocate a new age UID
    AgeManager ageManager = AgeManager.get(level);
    int ageUID = ageManager.allocateUID();

    // Generate a UUID for this age
    java.util.UUID ageUUID = java.util.UUID.randomUUID();

    // Create the dimension with director configuration
    ServerLevel ageLevel = AgeDimensionFactory.createAgeDimension(
        level.getServer(), ageUID, ageUUID, director);

    if (ageLevel == null) {
      Mystcraft.LOGGER.warn("Age creation failed for player {}", player.getGameProfile().getName());
      return;
    }

    // Register the age with AgeManager so it can be found later
    ResourceLocation dimLoc = new ResourceLocation("mystcraft", "mystcraft_age_" + ageUID);
    ageManager.registerAge(ageUID, dimLoc, ageUUID);

    // Initialize the AgeData
    AgeData ageData = AgeData.get(ageLevel);
    ageData.setAgeUID(ageUID);
    ageData.setAgeUUID(ageUUID);
    ageData.setAgeName(getDisplayName(stack));
    for (String author : getAuthors(stack)) {
      ageData.addAuthor(author);
    }
    ageData.setPages(pages);

    // Copy ALL configuration from director (including instability)
    ageData.copyFromDirector(director);

    // Force sync Age data to the player BEFORE they teleport
    // This ensures colors, instability, and other visual data is available on the client
    AgeDataSyncHandler.syncAgeDataToPlayer(player, ageLevel);

    // Find a proper spawn position using the terrain heightmap.
    // getSharedSpawnPos() returns a generic default (0,64,0) which may not
    // correspond to actual terrain. Instead, force the spawn chunk to generate
    // and use the heightmap to find the real surface.
    BlockPos spawn = findAgeSpawnPosition(ageLevel);
    ageData.setSpawn(spawn.getX(), spawn.getY(), spawn.getZ());
    AgeDimensionFactory.applyMicroDimensionBorder(ageLevel, ageData);

    // Update the book with the Age's dimension ID and spawn
    LinkOptions.setDimensionUID(stack.getTag(), ageUID);
    LinkOptions.setSpawn(stack.getTag(), spawn);
    LinkOptions.setUUID(stack.getTag(), ageUUID);

    if (!builder.isComplete()) {
      Mystcraft.LOGGER.info("Created incomplete age uid={} instability={} player={}",
          ageUID, String.format("%.1f", builder.getInstability()), player.getGameProfile().getName());
    } else {
      Mystcraft.LOGGER.info("Created age uid={} player={}", ageUID, player.getGameProfile().getName());
    }

    // Link to the newly created Age
    linkToAge(stack, level, player);
  }

  /**
   * Finds a proper spawn position in a newly created Age.
   * Forces the spawn chunk to generate, then uses the heightmap to find
   * the actual terrain surface.
   */
  private BlockPos findAgeSpawnPosition(ServerLevel ageLevel) {
    // Use (8, 8) as spawn coords - center of the spawn chunk.
    // This matters for void Ages where the platform is at chunk center.
    int spawnX = 8;
    int spawnZ = 8;

    // IMPORTANT: Do NOT force synchronous chunk generation here.
    // Calling ageLevel.getChunk(x, z, ChunkStatus.FULL, true) on the server thread
    // will DEADLOCK because chunk generation schedules tasks that need the server
    // thread to complete, but the server thread is blocked waiting for chunk gen.
    //
    // Instead, estimate the spawn Y from the director/terrain type.
    // LinkingManager.findSafeY will do actual terrain validation when the player links,
    // at which point the chunk gets generated as part of the teleport process.

    // Try to get terrain info from the chunk generator
    if (ageLevel.getChunkSource().getGenerator() instanceof AgeChunkGenerator ageGen) {
      AgeDirectorImpl director = ageGen.getDirector();
      if (director != null) {
        String terrainType = director.getTerrainType();
        int spawnY;

        if ("void".equals(terrainType)) {
          // Void terrain has a platform at Y=65 (placed at Y=64, spawn on top)
          spawnY = 65;
        } else if ("flat".equals(terrainType)) {
          // Flat terrain goes up to director's ground level
          int gl = director.getAverageGroundLevel();
          spawnY = (gl > 0) ? gl + 1 : 65;
        } else if ("nether".equals(terrainType)) {
          spawnY = 33;
        } else if ("end".equals(terrainType)) {
          spawnY = 65;
        } else {
          // Normal/amplified terrain - use vanilla overworld ground level.
          // Do NOT trust director.getAverageGroundLevel() here because
          // symbol processing order can leave it at invalid values
          // (e.g., terrain_end sets ground=-20, then terrain_normal
          // overrides the type but doesn't reset the ground level).
          spawnY = 65;
        }

        art.arcane.mystcraft.Mystcraft.LOGGER.info(
            "[AgebookItem] findAgeSpawnPosition: terrain={}, estimated spawn Y={} (no chunk gen forced)",
            terrainType, spawnY);
        return new BlockPos(spawnX, spawnY, spawnZ);
      }
    }

    // Fallback if no director available
    art.arcane.mystcraft.Mystcraft.LOGGER.info("[AgebookItem] findAgeSpawnPosition: no director, using fallback Y=65");
    return new BlockPos(spawnX, 65, spawnZ);
  }

  /**
   * Extracts IAgeSymbol objects from a list of pages.
   */
  private List<IAgeSymbol> extractSymbols(List<ItemStack> pages) {
    List<IAgeSymbol> symbols = new ArrayList<>();
    for (ItemStack page : pages) {
      if (Page.isLinkPanel(page)) {
        continue; // Link panels are not symbols
      }
      ResourceLocation symbolId = Page.getSymbol(page);
      if (symbolId != null) {
        IAgeSymbol symbol = SymbolRegistry.get(symbolId);
        if (symbol != null) {
          symbols.add(symbol);
        }
      }
    }
    return symbols;
  }

  /**
   * Links the player to the Age described in this book.
   */
  private void linkToAge(ItemStack stack, ServerLevel level, ServerPlayer player) {
    CompoundTag linkData = stack.getTag();
    if (linkData == null) {
      return;
    }

    Integer ageUID = LinkOptions.getDimensionUID(linkData);
    if (ageUID == null) {
      Mystcraft.LOGGER.warn("Agebook link failed: no age UID for player {}", player.getGameProfile().getName());
      return;
    }

    // Perform the link
    LinkingManager.LinkResult result = LinkingManager.performLink(player, linkData);

    if (result != LinkingManager.LinkResult.SUCCESS) {
      Mystcraft.LOGGER.warn("Agebook link failed: result={} player={}", result.name(),
          player.getGameProfile().getName());
    }
  }

  /**
   * Gets the list of pages in this book.
   */
  public List<ItemStack> getPageList(ItemStack stack) {
    if (stack.getTag() == null) {
      return Collections.emptyList();
    }
    CompoundTag tag = stack.getTag();
    ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
    List<ItemStack> pages = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      pages.add(ItemStack.of(listTag.getCompound(i)));
    }
    return pages;
  }

  /**
   * Adds pages to this book.
   */
  public void addPages(ItemStack stack, Collection<ItemStack> pages) {
    CompoundTag tag = stack.getOrCreateTag();
    ListTag listTag = tag.getList(TAG_PAGES, Tag.TAG_COMPOUND);
    for (ItemStack page : pages) {
      listTag.add(page.save(new CompoundTag()));
    }
    tag.put(TAG_PAGES, listTag);
  }

  /**
   * Sets the page list.
   */
  public void setPageList(ItemStack stack, List<ItemStack> pages) {
    CompoundTag tag = stack.getOrCreateTag();
    ListTag listTag = new ListTag();
    for (ItemStack page : pages) {
      listTag.add(page.save(new CompoundTag()));
    }
    tag.put(TAG_PAGES, listTag);
  }

  /**
   * Adds an author to this book.
   */
  public void addAuthor(ItemStack stack, Player player) {
    CompoundTag tag = stack.getOrCreateTag();
    ListTag listTag = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
    String playerName = player.getGameProfile().getName();
    boolean found = false;
    for (int i = 0; i < listTag.size(); i++) {
      if (listTag.getString(i).equals(playerName)) {
        found = true;
        break;
      }
    }
    if (!found) {
      listTag.add(net.minecraft.nbt.StringTag.valueOf(playerName));
      tag.put(TAG_AUTHORS, listTag);
    }
  }

  /**
   * Gets the authors of this book.
   */
  public Collection<String> getAuthors(ItemStack stack) {
    if (stack.getTag() == null) {
      return Collections.emptyList();
    }
    CompoundTag tag = stack.getTag();
    ListTag listTag = tag.getList(TAG_AUTHORS, Tag.TAG_STRING);
    List<String> authors = new ArrayList<>();
    for (int i = 0; i < listTag.size(); i++) {
      authors.add(listTag.getString(i));
    }
    return authors;
  }

  /**
   * Sets the display name of the book.
   */
  public void setDisplayName(ItemStack stack, String name) {
    LinkOptions.setDisplayName(stack.getOrCreateTag(), name);
  }

  /**
   * Gets the display name of the book.
   */
  public String getDisplayName(ItemStack stack) {
    return LinkOptions.getDisplayName(stack.getTag());
  }

  /**
   * Agebooks with a linked Age have a foil effect to show they're active.
   */
  @Override
  public boolean isFoil(@NotNull ItemStack stack) {
    // Show foil if the book has an Age (dimension) linked
    return stack.getTag() != null && LinkOptions.getDimensionUID(stack.getTag()) != null;
  }

  // --- Custom Entity on Q-Drop ---

  /**
   * Forge: Q-dropped agebooks should spawn as LinkbookEntity, not ItemEntity.
   * On Forge/NeoForge, subclasses override hasCustomEntity/createEntity.
   */
  public boolean hasCustomEntity(@NotNull ItemStack stack) {
    return true;
  }

  /**
   * Creates a LinkbookEntity when Q-dropped so the book renders open on the ground.
   */
  @Nullable
  public Entity createEntity(Level level, Entity location, @NotNull ItemStack stack) {
    LinkbookEntity entity = new LinkbookEntity(level, location.getX(), location.getY(), location.getZ());
    entity.setBookItem(stack.copy());
    entity.setDeltaMovement(location.getDeltaMovement());
    return entity;
  }
}
