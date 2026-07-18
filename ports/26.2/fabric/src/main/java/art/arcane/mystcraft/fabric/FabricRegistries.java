package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.block.BookBinderBlock;
import art.arcane.mystcraft.block.BookReceptacleBlock;
import art.arcane.mystcraft.block.CrystalBlock;
import art.arcane.mystcraft.block.DecayBlock;
import art.arcane.mystcraft.block.InkMixerBlock;
import art.arcane.mystcraft.block.LinkModifierBlock;
import art.arcane.mystcraft.block.LinkPortalBlock;
import art.arcane.mystcraft.block.StarFissureBlock;
import art.arcane.mystcraft.block.WritingDeskBlock;
import art.arcane.mystcraft.blockentity.BookBinderBlockEntity;
import art.arcane.mystcraft.blockentity.BookReceptacleBlockEntity;
import art.arcane.mystcraft.blockentity.InkMixerBlockEntity;
import art.arcane.mystcraft.blockentity.LinkModifierBlockEntity;
import art.arcane.mystcraft.blockentity.LinkPortalBlockEntity;
import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.blockentity.WritingDeskBlockEntity;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import art.arcane.mystcraft.entity.PersonalPocketProxyEntity;
import art.arcane.mystcraft.item.AgebookItem;
import art.arcane.mystcraft.item.BoosterPackItem;
import art.arcane.mystcraft.item.FolderItem;
import art.arcane.mystcraft.item.GuidebookItem;
import art.arcane.mystcraft.item.InkVialItem;
import art.arcane.mystcraft.item.LinkbookItem;
import art.arcane.mystcraft.item.LinkbookUnlinkedItem;
import art.arcane.mystcraft.item.PageItem;
import art.arcane.mystcraft.item.PersonalLinkBookItem;
import art.arcane.mystcraft.item.PortfolioItem;
import art.arcane.mystcraft.menu.BookBinderMenu;
import art.arcane.mystcraft.menu.FolderMenu;
import art.arcane.mystcraft.menu.InkMixerMenu;
import art.arcane.mystcraft.menu.LinkModifierMenu;
import art.arcane.mystcraft.menu.PortfolioMenu;
import art.arcane.mystcraft.menu.WritingDeskMenu;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModFluids;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.registry.ModLootFunctions;
import art.arcane.mystcraft.registry.ModMenuTypes;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.villager.ArchivistTrades;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import art.arcane.mystcraft.world.structure.AbandonedLibraryStructure;
import art.arcane.mystcraft.world.structure.ModStructures;
import art.arcane.mystcraft.world.structure.ScatteredLibraryStructure;
import art.arcane.mystcraft.world.structure.UndergroundArchiveStructure;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Fabric's single registry manifest. The deliberately old-looking IDs are the
 * public save/network contract from Mystcraft Legacy and must not be renamed.
 */
public final class FabricRegistries {

  private static final int MAX_MENU_DATA_BYTES = 64;
  private static final StreamCodec<RegistryFriendlyByteBuf, byte[]> MENU_DATA_CODEC =
      StreamCodec.of(
          (buffer, bytes) -> buffer.writeByteArray(bytes),
          buffer -> buffer.readByteArray(MAX_MENU_DATA_BYTES));

  private static boolean registered;

  public static Supplier<Block> INK_MIXER;
  public static Supplier<Block> BOOK_BINDER;
  public static Supplier<Block> BOOK_RECEPTACLE;
  public static Supplier<Block> LINK_MODIFIER;
  public static Supplier<Block> WRITING_DESK;
  public static Supplier<Block> CRYSTAL;
  public static Supplier<Block> DECAY;
  public static Supplier<Block> LINK_PORTAL;
  public static Supplier<Block> STAR_FISSURE;
  public static Supplier<LiquidBlock> FLUID_INK;

  public static Supplier<Item> PAGE;
  public static Supplier<Item> AGEBOOK;
  public static Supplier<Item> LINKBOOK;
  public static Supplier<Item> LINKBOOK_UNLINKED;
  public static Supplier<Item> PERSONAL_LINK_BOOK;
  public static Supplier<Item> BOOSTER_PACK;
  public static Supplier<Item> FOLDER;
  public static Supplier<Item> PORTFOLIO;
  public static Supplier<Item> INK_VIAL;
  public static Supplier<Item> GUIDEBOOK;
  public static Supplier<Item> INK_BUCKET;
  public static Supplier<Item> INK_MIXER_ITEM;
  public static Supplier<Item> BOOK_BINDER_ITEM;
  public static Supplier<Item> BOOK_RECEPTACLE_ITEM;
  public static Supplier<Item> LINK_MODIFIER_ITEM;
  public static Supplier<Item> WRITING_DESK_ITEM;
  public static Supplier<Item> CRYSTAL_ITEM;
  public static Supplier<Item> DECAY_ITEM;

  public static Supplier<FlowingFluid> BLACK_INK_SOURCE;
  public static Supplier<FlowingFluid> BLACK_INK_FLOWING;

  public static Supplier<EntityType<LinkbookEntity>> LINKBOOK_ENTITY;
  public static Supplier<EntityType<PersonalPocketProxyEntity>> PERSONAL_POCKET_PROXY_ENTITY;
  public static Supplier<EntityType<MystcraftFallingBlockEntity>> FALLING_BLOCK_ENTITY;
  public static Supplier<EntityType<MeteorEntity>> METEOR_ENTITY;
  public static Supplier<EntityType<ColoredLightningEntity>> COLORED_LIGHTNING_ENTITY;

  public static Supplier<BlockEntityType<InkMixerBlockEntity>> INK_MIXER_BE;
  public static Supplier<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER_BE;
  public static Supplier<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE_BE;
  public static Supplier<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK_BE;
  public static Supplier<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE_BE;
  public static Supplier<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER_BE;
  public static Supplier<BlockEntityType<LinkPortalBlockEntity>> LINK_PORTAL_BE;

  public static Supplier<SoundEvent> LINKING_POP;
  public static Supplier<SoundEvent> LINKING_LINK;
  public static Supplier<SoundEvent> LINKING_DISARM;
  public static Supplier<SoundEvent> LINKING_FOLLOWING;
  public static Supplier<SoundEvent> LINKING_INTRA;
  public static Supplier<SoundEvent> LINKING_FISSURE;
  public static Supplier<SoundEvent> LINKING_PORTAL;
  public static Supplier<SoundEvent> METEOR_ROAR;
  public static Supplier<SoundEvent> METEOR_IMPACT;

  public static Supplier<MenuType<InkMixerMenu>> INK_MIXER_MENU;
  public static Supplier<MenuType<BookBinderMenu>> BOOK_BINDER_MENU;
  public static Supplier<MenuType<LinkModifierMenu>> LINK_MODIFIER_MENU;
  public static Supplier<MenuType<WritingDeskMenu>> WRITING_DESK_MENU;
  public static Supplier<MenuType<FolderMenu>> FOLDER_MENU;
  public static Supplier<MenuType<PortfolioMenu>> PORTFOLIO_MENU;

  public static Supplier<StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY;
  public static Supplier<StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE;
  public static Supplier<StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY;
  public static Supplier<MapCodec<? extends ChunkGenerator>> AGE_CHUNK_GENERATOR;
  public static Supplier<MapCodec<? extends BiomeSource>> AGE_BIOME_SOURCE;
  public static Supplier<PoiType> ARCHIVIST_POI;
  public static Supplier<VillagerProfession> ARCHIVIST;
  public static Supplier<CreativeModeTab> MYSTCRAFT_TAB;
  public static Supplier<CreativeModeTab> MYSTCRAFT_PAGES_TAB;

  private FabricRegistries() {
  }

  public static synchronized void register() {
    if (registered) {
      return;
    }

    ModLootFunctions.register((identifier, codecSupplier) ->
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, identifier, codecSupplier.get()));

    FabricBlackInkFluid.Source source = new FabricBlackInkFluid.Source();
    FabricBlackInkFluid.Flowing flowing = new FabricBlackInkFluid.Flowing();
    BLACK_INK_SOURCE = register(BuiltInRegistries.FLUID, "black_ink", () -> source);
    BLACK_INK_FLOWING = register(BuiltInRegistries.FLUID, "black_ink_flowing", () -> flowing);

    INK_MIXER = registerBlock("blockinkmixer", () -> new InkMixerBlock(
        blockProperties("blockinkmixer").mapColor(MapColor.WOOD).strength(2.5F)
            .requiresCorrectToolForDrops()));
    BOOK_BINDER = registerBlock("blockbookbinder", () -> new BookBinderBlock(
        blockProperties("blockbookbinder").mapColor(MapColor.WOOD).strength(2.5F)
            .requiresCorrectToolForDrops()));
    BOOK_RECEPTACLE = registerBlock("blockbookreceptacle", () -> new BookReceptacleBlock(
        blockProperties("blockbookreceptacle").mapColor(MapColor.STONE).strength(3.5F)
            .requiresCorrectToolForDrops()));
    LINK_MODIFIER = registerBlock("blocklinkmodifier", () -> new LinkModifierBlock(
        blockProperties("blocklinkmodifier").mapColor(MapColor.STONE).strength(3.0F)
            .requiresCorrectToolForDrops()));
    WRITING_DESK = registerBlock("writingdesk", () -> new WritingDeskBlock(
        blockProperties("writingdesk").mapColor(MapColor.WOOD).strength(2.5F)
            .requiresCorrectToolForDrops()));
    CRYSTAL = registerBlock("blockcrystal", () -> new CrystalBlock(
        blockProperties("blockcrystal").mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.5F)
            .lightLevel(CrystalBlock::getLightLevel).noOcclusion()));
    DECAY = registerBlock("blockdecay", () -> new DecayBlock(
        blockProperties("blockdecay").mapColor(MapColor.COLOR_BLACK)
            .strength(-1.0F, 3_600_000.0F).noLootTable().randomTicks()));
    LINK_PORTAL = registerBlock("linkportal", () -> new LinkPortalBlock(
        blockProperties("linkportal").mapColor(MapColor.COLOR_BLACK).noCollision()
            .strength(-1.0F).noLootTable().lightLevel(state -> 11)
            .pushReaction(PushReaction.BLOCK)));
    STAR_FISSURE = registerBlock("blockstarfissure", () -> new StarFissureBlock(
        blockProperties("blockstarfissure").mapColor(MapColor.COLOR_BLACK).noCollision()
            .strength(-1.0F, 3_600_000.0F).noLootTable().lightLevel(state -> 15)
            .pushReaction(PushReaction.BLOCK)));
    FLUID_INK = register(BuiltInRegistries.BLOCK, "fluidblockblackink", () ->
        new LiquidBlock(source, blockProperties("fluidblockblackink")
            .mapColor(MapColor.COLOR_BLACK).replaceable().noCollision().strength(100.0F)
            .noLootTable()) {
        });

    PAGE = registerItem("page", () -> new PageItem(itemProperties("page").stacksTo(64)
        .rarity(Rarity.UNCOMMON)));
    AGEBOOK = registerItem("agebook", () -> new AgebookItem(itemProperties("agebook").stacksTo(1)
        .rarity(Rarity.EPIC)));
    LINKBOOK = registerItem("linkbook", () -> new LinkbookItem(itemProperties("linkbook").stacksTo(1)
        .rarity(Rarity.RARE)));
    LINKBOOK_UNLINKED = registerItem("linkbook_unlinked", () -> new LinkbookUnlinkedItem(
        itemProperties("linkbook_unlinked").stacksTo(16)));
    PERSONAL_LINK_BOOK = registerItem("personal_link_book", () -> new PersonalLinkBookItem(
        itemProperties("personal_link_book").stacksTo(1).rarity(Rarity.RARE)));
    BOOSTER_PACK = registerItem("booster", () -> new BoosterPackItem(
        itemProperties("booster").stacksTo(16)));
    FOLDER = registerItem("folder", () -> new FolderItem(itemProperties("folder")));
    PORTFOLIO = registerItem("portfolio", () -> new PortfolioItem(
        itemProperties("portfolio").stacksTo(1)));
    INK_VIAL = registerItem("inkvial", () -> new InkVialItem(
        itemProperties("inkvial").stacksTo(16).rarity(Rarity.UNCOMMON)));
    GUIDEBOOK = registerItem("guidebook", () -> new GuidebookItem(
        itemProperties("guidebook").stacksTo(1)));
    INK_BUCKET = registerItem("ink_bucket", () -> new BucketItem(source,
        itemProperties("ink_bucket").stacksTo(1).craftRemainder(Items.BUCKET)
            .rarity(Rarity.UNCOMMON)));

    INK_MIXER_ITEM = registerBlockItem("blockinkmixer", INK_MIXER);
    BOOK_BINDER_ITEM = registerBlockItem("blockbookbinder", BOOK_BINDER);
    BOOK_RECEPTACLE_ITEM = registerBlockItem("blockbookreceptacle", BOOK_RECEPTACLE);
    LINK_MODIFIER_ITEM = registerBlockItem("blocklinkmodifier", LINK_MODIFIER);
    WRITING_DESK_ITEM = registerBlockItem("writingdesk", WRITING_DESK);
    CRYSTAL_ITEM = registerBlockItem("blockcrystal", CRYSTAL);
    DECAY_ITEM = registerBlockItem("blockdecay", DECAY);

    LINKBOOK_ENTITY = registerEntity("linkbook", LinkbookEntity::new, builder -> builder
        .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(20));
    PERSONAL_POCKET_PROXY_ENTITY = registerEntity("personal_pocket_proxy",
        PersonalPocketProxyEntity::new,
        builder -> builder.noSave().sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(3));
    FALLING_BLOCK_ENTITY = registerEntity("falling_block", MystcraftFallingBlockEntity::new,
        builder -> builder.sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(20));
    METEOR_ENTITY = registerEntity("meteor", MeteorEntity::new,
        builder -> builder.sized(2.0F, 2.0F).clientTrackingRange(16).updateInterval(10)
            .fireImmune());
    COLORED_LIGHTNING_ENTITY = registerEntity("colored_lightning", ColoredLightningEntity::new,
        builder -> builder.noSave().sized(0.0F, 0.0F).clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE));

    INK_MIXER_BE = registerBlockEntity("ink_mixer", InkMixerBlockEntity::new, INK_MIXER.get());
    BOOK_BINDER_BE = registerBlockEntity("book_binder", BookBinderBlockEntity::new,
        BOOK_BINDER.get());
    BOOK_RECEPTACLE_BE = registerBlockEntity("book_receptacle", BookReceptacleBlockEntity::new,
        BOOK_RECEPTACLE.get());
    WRITING_DESK_BE = registerBlockEntity("writing_desk", WritingDeskBlockEntity::new,
        WRITING_DESK.get());
    STAR_FISSURE_BE = registerBlockEntity("star_fissure", StarFissureBlockEntity::new,
        STAR_FISSURE.get());
    LINK_MODIFIER_BE = registerBlockEntity("link_modifier", LinkModifierBlockEntity::new,
        LINK_MODIFIER.get());
    LINK_PORTAL_BE = registerBlockEntity("link_portal", LinkPortalBlockEntity::new,
        LINK_PORTAL.get());

    LINKING_POP = registerSound("linking.pop");
    LINKING_LINK = registerSound("linking.link");
    LINKING_DISARM = registerSound("linking.link-disarm");
    LINKING_FOLLOWING = registerSound("linking.link-following");
    LINKING_INTRA = registerSound("linking.link-intra");
    LINKING_FISSURE = registerSound("linking.link-fissure");
    LINKING_PORTAL = registerSound("linking.link-portal");
    METEOR_ROAR = registerSound("entity.meteor.roar");
    METEOR_IMPACT = registerSound("entity.meteor.impact");

    INK_MIXER_MENU = registerMenu("ink_mixer", InkMixerMenu::new);
    BOOK_BINDER_MENU = registerMenu("book_binder", BookBinderMenu::new);
    LINK_MODIFIER_MENU = registerMenu("link_modifier", LinkModifierMenu::new);
    WRITING_DESK_MENU = registerMenu("writing_desk", WritingDeskMenu::new);
    FOLDER_MENU = registerMenu("folder", FolderMenu::new);
    PORTFOLIO_MENU = registerMenu("portfolio", PortfolioMenu::new);

    ABANDONED_LIBRARY = register(BuiltInRegistries.STRUCTURE_TYPE, "abandoned_library",
        () -> () -> MapCodec.assumeMapUnsafe(AbandonedLibraryStructure.CODEC));
    UNDERGROUND_ARCHIVE = register(BuiltInRegistries.STRUCTURE_TYPE, "underground_archive",
        () -> () -> MapCodec.assumeMapUnsafe(UndergroundArchiveStructure.CODEC));
    SCATTERED_LIBRARY = register(BuiltInRegistries.STRUCTURE_TYPE, "scattered_library",
        () -> () -> MapCodec.assumeMapUnsafe(ScatteredLibraryStructure.CODEC));
    AGE_CHUNK_GENERATOR = register(BuiltInRegistries.CHUNK_GENERATOR, "age_chunk_generator",
        () -> AgeChunkGenerator.CODEC);
    AGE_BIOME_SOURCE = register(BuiltInRegistries.BIOME_SOURCE, "age_biome_source",
        () -> AgeBiomeSource.CODEC);

    ResourceKey<PoiType> poiKey = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
        id("archivist"));
    ARCHIVIST_POI = register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, "archivist",
        () -> new PoiType(Set.copyOf(BOOK_BINDER.get().getStateDefinition().getPossibleStates()),
            1, 1));
    ARCHIVIST = register(BuiltInRegistries.VILLAGER_PROFESSION, "archivist", () ->
        new VillagerProfession(
            Component.translatable("entity.mystcraft.villager.archivist"),
            holder -> holder.is(poiKey),
            holder -> holder.is(poiKey),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_LIBRARIAN,
            ArchivistTrades.TRADE_SETS_BY_LEVEL));

    MYSTCRAFT_TAB = register(BuiltInRegistries.CREATIVE_MODE_TAB, "mystcraft", () ->
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.mystcraft"))
            .icon(() -> new ItemStack(AGEBOOK.get()))
            .displayItems((parameters, output) -> {
              output.accept(GUIDEBOOK.get());
              output.accept(AGEBOOK.get());
              output.accept(LINKBOOK.get());
              output.accept(LINKBOOK_UNLINKED.get());
              if (MystcraftConfig.enablePersonalLinkBooks.get()) {
                output.accept(PERSONAL_LINK_BOOK.get());
              }
              output.accept(PAGE.get());
              output.accept(FOLDER.get());
              output.accept(PORTFOLIO.get());
              output.accept(BOOSTER_PACK.get());
              output.accept(INK_VIAL.get());
              output.accept(INK_BUCKET.get());
              output.accept(WRITING_DESK_ITEM.get());
              output.accept(INK_MIXER_ITEM.get());
              output.accept(BOOK_BINDER_ITEM.get());
              output.accept(LINK_MODIFIER_ITEM.get());
              output.accept(BOOK_RECEPTACLE_ITEM.get());
              output.accept(CRYSTAL_ITEM.get());
              output.accept(DECAY_ITEM.get());
            })
            .build());
    MYSTCRAFT_PAGES_TAB = register(BuiltInRegistries.CREATIVE_MODE_TAB, "mystcraft_pages", () ->
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .title(Component.translatable("itemGroup.mystcraft_pages"))
            .icon(Page::createLinkPage)
            .displayItems((parameters, output) -> {
              output.accept(Page.createLinkPage());
              for (SymbolCategory category : SymbolCategory.values()) {
                List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
                for (IAgeSymbol symbol : symbols) {
                  output.accept(Page.createSymbolPage(symbol.getRegistryName()));
                }
              }
            })
            .build());

    registered = true;
    Mystcraft.LOGGER.info("[Mystcraft] Registered Fabric content with legacy-compatible IDs");
  }

  public static void populateCommonRegistries() {
    ModBlocks.INK_MIXER = INK_MIXER;
    ModBlocks.BOOK_BINDER = BOOK_BINDER;
    ModBlocks.BOOK_RECEPTACLE = BOOK_RECEPTACLE;
    ModBlocks.LINK_MODIFIER = LINK_MODIFIER;
    ModBlocks.WRITING_DESK = WRITING_DESK;
    ModBlocks.CRYSTAL = CRYSTAL;
    ModBlocks.DECAY = DECAY;
    ModBlocks.LINK_PORTAL = LINK_PORTAL;
    ModBlocks.STAR_FISSURE = STAR_FISSURE;
    ModBlocks.FLUID_INK = FLUID_INK;

    ModItems.PAGE = PAGE;
    ModItems.AGEBOOK = AGEBOOK;
    ModItems.LINKBOOK = LINKBOOK;
    ModItems.LINKBOOK_UNLINKED = LINKBOOK_UNLINKED;
    ModItems.PERSONAL_LINK_BOOK = PERSONAL_LINK_BOOK;
    ModItems.BOOSTER_PACK = BOOSTER_PACK;
    ModItems.FOLDER = FOLDER;
    ModItems.PORTFOLIO = PORTFOLIO;
    ModItems.INK_VIAL = INK_VIAL;
    ModItems.GUIDEBOOK = GUIDEBOOK;
    ModItems.INK_BUCKET = INK_BUCKET;
    ModItems.INK_MIXER_ITEM = INK_MIXER_ITEM;
    ModItems.BOOK_BINDER_ITEM = BOOK_BINDER_ITEM;
    ModItems.BOOK_RECEPTACLE_ITEM = BOOK_RECEPTACLE_ITEM;
    ModItems.LINK_MODIFIER_ITEM = LINK_MODIFIER_ITEM;
    ModItems.WRITING_DESK_ITEM = WRITING_DESK_ITEM;
    ModItems.CRYSTAL_ITEM = CRYSTAL_ITEM;
    ModItems.DECAY_ITEM = DECAY_ITEM;

    ModBlockEntities.INK_MIXER = INK_MIXER_BE;
    ModBlockEntities.BOOK_BINDER = BOOK_BINDER_BE;
    ModBlockEntities.BOOK_RECEPTACLE = BOOK_RECEPTACLE_BE;
    ModBlockEntities.WRITING_DESK = WRITING_DESK_BE;
    ModBlockEntities.STAR_FISSURE = STAR_FISSURE_BE;
    ModBlockEntities.LINK_MODIFIER = LINK_MODIFIER_BE;
    ModBlockEntities.LINK_PORTAL = LINK_PORTAL_BE;

    ModEntities.LINKBOOK = LINKBOOK_ENTITY;
    ModEntities.PERSONAL_POCKET_PROXY = PERSONAL_POCKET_PROXY_ENTITY;
    ModEntities.FALLING_BLOCK = FALLING_BLOCK_ENTITY;
    ModEntities.METEOR = METEOR_ENTITY;
    ModEntities.COLORED_LIGHTNING = COLORED_LIGHTNING_ENTITY;

    ModFluids.BLACK_INK_SOURCE = BLACK_INK_SOURCE;
    ModFluids.BLACK_INK_FLOWING = BLACK_INK_FLOWING;
    ModFluids.BLACK_INK_BUCKET = INK_BUCKET;

    ModSounds.LINKING_POP = LINKING_POP;
    ModSounds.LINKING_LINK = LINKING_LINK;
    ModSounds.LINKING_DISARM = LINKING_DISARM;
    ModSounds.LINKING_FOLLOWING = LINKING_FOLLOWING;
    ModSounds.LINKING_INTRA = LINKING_INTRA;
    ModSounds.LINKING_FISSURE = LINKING_FISSURE;
    ModSounds.LINKING_PORTAL = LINKING_PORTAL;
    ModSounds.METEOR_ROAR = METEOR_ROAR;
    ModSounds.METEOR_IMPACT = METEOR_IMPACT;

    ModMenuTypes.INK_MIXER = INK_MIXER_MENU;
    ModMenuTypes.BOOK_BINDER = BOOK_BINDER_MENU;
    ModMenuTypes.LINK_MODIFIER = LINK_MODIFIER_MENU;
    ModMenuTypes.WRITING_DESK = WRITING_DESK_MENU;
    ModMenuTypes.FOLDER = FOLDER_MENU;
    ModMenuTypes.PORTFOLIO = PORTFOLIO_MENU;

    ModStructures.ABANDONED_LIBRARY = ABANDONED_LIBRARY;
    ModStructures.UNDERGROUND_ARCHIVE = UNDERGROUND_ARCHIVE;
    ModStructures.SCATTERED_LIBRARY = SCATTERED_LIBRARY;

    FabricMystcraftConfig.bindCommonConfig();
    FabricMystcraftNetwork.installSenders();
  }

  private static BlockBehaviour.Properties blockProperties(String path) {
    return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(path)));
  }

  private static Item.Properties itemProperties(String path) {
    return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path)));
  }

  private static Supplier<Block> registerBlock(String path, Supplier<Block> factory) {
    return register(BuiltInRegistries.BLOCK, path, factory);
  }

  private static Supplier<Item> registerItem(String path, Supplier<Item> factory) {
    return register(BuiltInRegistries.ITEM, path, factory);
  }

  private static Supplier<Item> registerBlockItem(String path, Supplier<? extends Block> block) {
    return registerItem(path, () -> new BlockItem(block.get(), itemProperties(path)
        .useBlockDescriptionPrefix()));
  }

  private static <T extends Entity> Supplier<EntityType<T>> registerEntity(
      String path,
      EntityType.EntityFactory<T> factory,
      java.util.function.UnaryOperator<EntityType.Builder<T>> configure
  ) {
    ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(path));
    EntityType.Builder<T> builder = EntityType.Builder.of(factory, MobCategory.MISC);
    EntityType<T> type = configure.apply(builder).build(key);
    return register(BuiltInRegistries.ENTITY_TYPE, path, () -> type);
  }

  private static <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
      String path,
      BlockEntityType.BlockEntitySupplier<T> factory,
      Block... blocks
  ) {
    BlockEntityType<T> type = new BlockEntityType<>(factory, Set.of(blocks));
    return register(BuiltInRegistries.BLOCK_ENTITY_TYPE, path, () -> type);
  }

  private static Supplier<SoundEvent> registerSound(String eventPath) {
    String registryPath = eventPath.replace('.', '_').replace('-', '_');
    SoundEvent sound = SoundEvent.createVariableRangeEvent(id(eventPath));
    return register(BuiltInRegistries.SOUND_EVENT, registryPath, () -> sound);
  }

  private static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(
      String path,
      MenuFactory<T> factory
  ) {
    ExtendedMenuType<T, byte[]> type = new ExtendedMenuType<>(
        (containerId, inventory, data) -> createMenu(factory, containerId, inventory, data),
        MENU_DATA_CODEC);
    return register(BuiltInRegistries.MENU, path, () -> type);
  }

  private static <T extends AbstractContainerMenu> T createMenu(
      MenuFactory<T> factory,
      int containerId,
      net.minecraft.world.entity.player.Inventory inventory,
      byte[] data
  ) {
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    try {
      return factory.create(containerId, inventory, buffer);
    } finally {
      buffer.release();
    }
  }

  private static <V, T extends V> Supplier<T> register(
      Registry<V> registry,
      String path,
      Supplier<T> factory
  ) {
    T value = Registry.register(registry, id(path), factory.get());
    return () -> value;
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }

  @FunctionalInterface
  private interface MenuFactory<T extends AbstractContainerMenu> {
    T create(
        int containerId,
        net.minecraft.world.entity.player.Inventory inventory,
        FriendlyByteBuf data
    );
  }
}
