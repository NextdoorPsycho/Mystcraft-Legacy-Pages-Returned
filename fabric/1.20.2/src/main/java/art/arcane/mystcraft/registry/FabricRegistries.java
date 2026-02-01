package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.block.*;
import art.arcane.mystcraft.blockentity.*;
import art.arcane.mystcraft.config.FabricMystcraftConfig;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import art.arcane.mystcraft.fluid.FabricBlackInkFluid;
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.menu.*;
import art.arcane.mystcraft.network.FabricMystcraftNetwork;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import art.arcane.mystcraft.world.structure.AbandonedLibraryStructure;
import art.arcane.mystcraft.world.structure.ScatteredLibraryStructure;
import art.arcane.mystcraft.world.structure.UndergroundArchiveStructure;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;
import java.util.function.Supplier;

/**
 * Consolidated Fabric registrations for Mystcraft 1.20.2.
 * Combines: FabricModBlocks, FabricModItems, FabricModEntities, FabricModBlockEntities,
 * FabricModSounds, FabricModMenuTypes, FabricModFluids, FabricModStructures, ModVillagers,
 * ModWorldGen, ModCreativeTabs.
 */
public final class FabricRegistries {

  // ========== BLOCKS ==========
  public static final Supplier<Block> INK_MIXER = registerBlock("blockinkmixer",
      new InkMixerBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.WOOD)
          .strength(2.5F)
          .requiresCorrectToolForDrops()));
  public static final Supplier<Block> BOOK_BINDER = registerBlock("blockbookbinder",
      new BookBinderBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.WOOD)
          .strength(2.5F)
          .requiresCorrectToolForDrops()));
  public static final Supplier<Block> BOOK_RECEPTACLE = registerBlock("blockbookreceptacle",
      new BookReceptacleBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.STONE)
          .strength(3.5F)
          .requiresCorrectToolForDrops()));
  public static final Supplier<Block> BOOKSTAND = registerBlock("blockbookstand",
      new BookstandBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.WOOD)
          .strength(2.0F)
          .requiresCorrectToolForDrops()));
  public static final Supplier<Block> LINK_MODIFIER = registerBlock("blocklinkmodifier",
      new LinkModifierBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.STONE)
          .strength(3.0F)
          .requiresCorrectToolForDrops()));
  public static final Supplier<Block> WRITING_DESK = registerBlock("writingdesk",
      new WritingDeskBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.WOOD)
          .strength(2.5F)
          .requiresCorrectToolForDrops()));
  public static final Supplier<Block> CRYSTAL = registerBlock("blockcrystal",
      new CrystalBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.COLOR_LIGHT_BLUE)
          .strength(1.5F)
          .lightLevel(CrystalBlock::getLightLevel)
          .noOcclusion()));
  public static final Supplier<Block> DECAY = registerBlock("blockdecay",
      new DecayBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.COLOR_BLACK)
          .strength(-1.0F, 3600000.0F)
          .noLootTable()
          .randomTicks()));
  public static final Supplier<Block> LINK_PORTAL = registerBlock("linkportal",
      new LinkPortalBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.COLOR_BLACK)
          .noCollission()
          .strength(-1.0F)
          .noLootTable()
          .lightLevel(state -> 11)
          .pushReaction(PushReaction.BLOCK)));
  public static final Supplier<Block> STAR_FISSURE = registerBlock("blockstarfissure",
      new StarFissureBlock(BlockBehaviour.Properties.of()
          .mapColor(MapColor.COLOR_BLACK)
          .noCollission()
          .strength(-1.0F, 3600000.0F)
          .noLootTable()
          .lightLevel(state -> 15)
          .pushReaction(PushReaction.BLOCK)));
  public static final Supplier<Block> FLUID_INK;
  // ========== ITEMS ==========
  public static final Supplier<Item> PAGE = registerItem("page",
      new FabricPageItem(new Item.Properties().stacksTo(64)));
  public static final Supplier<Item> AGEBOOK = registerItem("agebook",
      new AgebookItem(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> LINKBOOK = registerItem("linkbook",
      new LinkbookItem(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> LINKBOOK_UNLINKED = registerItem("linkbook_unlinked",
      new LinkbookUnlinkedItem(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> PERSONAL_LINK_BOOK = registerItem("personal_link_book",
      new PersonalLinkBookItem(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> BOOSTER_PACK = registerItem("booster",
      new BoosterPackItem(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> FOLDER = registerItem("folder",
      new FolderItem(new Item.Properties()));
  public static final Supplier<Item> PORTFOLIO = registerItem("portfolio",
      new PortfolioItem(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> INK_VIAL = registerItem("inkvial",
      new InkVialItem(new Item.Properties().stacksTo(16)));
  public static final Supplier<Item> GUIDEBOOK = registerItem("guidebook",
      new GuidebookItem(new Item.Properties().stacksTo(1)));
  public static final Supplier<Item> INK_MIXER_ITEM = registerBlockItem("blockinkmixer", INK_MIXER);
  public static final Supplier<Item> BOOK_BINDER_ITEM = registerBlockItem("blockbookbinder", BOOK_BINDER);
  public static final Supplier<Item> BOOK_RECEPTACLE_ITEM = registerBlockItem("blockbookreceptacle", BOOK_RECEPTACLE);
  public static final Supplier<Item> BOOKSTAND_ITEM = registerBlockItem("blockbookstand", BOOKSTAND);
  public static final Supplier<Item> LINK_MODIFIER_ITEM = registerBlockItem("blocklinkmodifier", LINK_MODIFIER);
  public static final Supplier<Item> WRITING_DESK_ITEM = registerBlockItem("writingdesk", WRITING_DESK);
  public static final Supplier<Item> CRYSTAL_ITEM = registerBlockItem("blockcrystal", CRYSTAL);
  public static final Supplier<Item> DECAY_ITEM = registerBlockItem("blockdecay", DECAY);
  // ========== ENTITIES ==========
  public static final Supplier<EntityType<LinkbookEntity>> LINKBOOK_ENTITY =
      registerEntity("linkbook",
          EntityType.Builder.<LinkbookEntity>of(LinkbookEntity::new, MobCategory.MISC)
              .sized(0.5F, 0.5F)
              .clientTrackingRange(10)
              .updateInterval(20)
              .build(new ResourceLocation(Mystcraft.MOD_ID, "linkbook").toString()));
  public static final Supplier<EntityType<MystcraftFallingBlockEntity>> FALLING_BLOCK_ENTITY =
      registerEntity("falling_block",
          EntityType.Builder.<MystcraftFallingBlockEntity>of(MystcraftFallingBlockEntity::new, MobCategory.MISC)
              .sized(0.98F, 0.98F)
              .clientTrackingRange(10)
              .updateInterval(20)
              .build(new ResourceLocation(Mystcraft.MOD_ID, "falling_block").toString()));
  public static final Supplier<EntityType<MeteorEntity>> METEOR_ENTITY =
      registerEntity("meteor",
          EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
              .sized(2.0F, 2.0F)
              .clientTrackingRange(16)
              .updateInterval(10)
              .fireImmune()
              .build(new ResourceLocation(Mystcraft.MOD_ID, "meteor").toString()));
  public static final Supplier<EntityType<ColoredLightningEntity>> COLORED_LIGHTNING_ENTITY =
      registerEntity("colored_lightning",
          EntityType.Builder.<ColoredLightningEntity>of(ColoredLightningEntity::new, MobCategory.MISC)
              .sized(0.0F, 0.0F)
              .clientTrackingRange(16)
              .updateInterval(Integer.MAX_VALUE)
              .noSave()
              .build(new ResourceLocation(Mystcraft.MOD_ID, "colored_lightning").toString()));
  // ========== BLOCK ENTITIES ==========
  public static final Supplier<BlockEntityType<InkMixerBlockEntity>> INK_MIXER_BE =
      registerBlockEntity("ink_mixer",
          FabricBlockEntityTypeBuilder.create(InkMixerBlockEntity::new, INK_MIXER.get()).build());
  public static final Supplier<BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER_BE =
      registerBlockEntity("book_binder",
          FabricBlockEntityTypeBuilder.create(BookBinderBlockEntity::new, BOOK_BINDER.get()).build());
  public static final Supplier<BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE_BE =
      registerBlockEntity("book_receptacle",
          FabricBlockEntityTypeBuilder.create(BookReceptacleBlockEntity::new, BOOK_RECEPTACLE.get()).build());
  public static final Supplier<BlockEntityType<BookstandBlockEntity>> BOOKSTAND_BE =
      registerBlockEntity("bookstand",
          FabricBlockEntityTypeBuilder.create(BookstandBlockEntity::new, BOOKSTAND.get()).build());
  public static final Supplier<BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK_BE =
      registerBlockEntity("writing_desk",
          FabricBlockEntityTypeBuilder.create(WritingDeskBlockEntity::new, WRITING_DESK.get()).build());
  public static final Supplier<BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE_BE =
      registerBlockEntity("star_fissure",
          FabricBlockEntityTypeBuilder.create(StarFissureBlockEntity::new, STAR_FISSURE.get()).build());
  public static final Supplier<BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER_BE =
      registerBlockEntity("link_modifier",
          FabricBlockEntityTypeBuilder.create(LinkModifierBlockEntity::new, LINK_MODIFIER.get()).build());
  // ========== SOUNDS ==========
  public static final Supplier<SoundEvent> LINKING_POP = registerSound("linking.pop");
  public static final Supplier<SoundEvent> LINKING_LINK = registerSound("linking.link");
  public static final Supplier<SoundEvent> LINKING_DISARM = registerSound("linking.link-disarm");
  public static final Supplier<SoundEvent> LINKING_FOLLOWING = registerSound("linking.link-following");
  public static final Supplier<SoundEvent> LINKING_INTRA = registerSound("linking.link-intra");
  public static final Supplier<SoundEvent> LINKING_FISSURE = registerSound("linking.link-fissure");
  public static final Supplier<SoundEvent> LINKING_PORTAL = registerSound("linking.link-portal");
  public static final Supplier<SoundEvent> METEOR_ROAR = registerSound("entity.meteor.roar");
  public static final Supplier<SoundEvent> METEOR_IMPACT = registerSound("entity.meteor.impact");
  // ========== MENU TYPES ==========
  public static final Supplier<MenuType<InkMixerMenu>> INK_MIXER_MENU =
      registerMenu("ink_mixer", new ExtendedScreenHandlerType<>(InkMixerMenu::new));
  public static final Supplier<MenuType<BookBinderMenu>> BOOK_BINDER_MENU =
      registerMenu("book_binder", new ExtendedScreenHandlerType<>(BookBinderMenu::new));
  public static final Supplier<MenuType<LinkModifierMenu>> LINK_MODIFIER_MENU =
      registerMenu("link_modifier", new ExtendedScreenHandlerType<>(LinkModifierMenu::new));
  public static final Supplier<MenuType<WritingDeskMenu>> WRITING_DESK_MENU =
      registerMenu("writing_desk", new ExtendedScreenHandlerType<>(WritingDeskMenu::new));
  public static final Supplier<MenuType<FolderMenu>> FOLDER_MENU =
      registerMenu("folder", new ExtendedScreenHandlerType<>(FolderMenu::new));
  public static final Supplier<MenuType<PortfolioMenu>> PORTFOLIO_MENU =
      registerMenu("portfolio", new ExtendedScreenHandlerType<>(PortfolioMenu::new));
  // ========== FLUIDS ==========
  static final FabricBlackInkFluid.Source BLACK_INK_SOURCE_FLUID = new FabricBlackInkFluid.Source();
  public static final Supplier<FlowingFluid> BLACK_INK_SOURCE = () -> BLACK_INK_SOURCE_FLUID;
  public static final Supplier<Item> INK_BUCKET = registerItem("ink_bucket",
      new BucketItem(BLACK_INK_SOURCE_FLUID,
          new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
  static final FabricBlackInkFluid.Flowing BLACK_INK_FLOWING_FLUID = new FabricBlackInkFluid.Flowing();
  public static final Supplier<FlowingFluid> BLACK_INK_FLOWING = () -> BLACK_INK_FLOWING_FLUID;
  // ========== CREATIVE TABS ==========
  private static final ResourceKey<CreativeModeTab> MYSTCRAFT_TAB_KEY =
      ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(),
          new ResourceLocation(Mystcraft.MOD_ID, "mystcraft"));
  private static final ResourceKey<CreativeModeTab> MYSTCRAFT_PAGES_TAB_KEY =
      ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(),
          new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_pages"));
  // ========== STRUCTURES ==========
  public static Supplier<StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY;
  public static Supplier<StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE;
  public static Supplier<StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY;
  // ========== VILLAGERS ==========
  public static Supplier<PoiType> ARCHIVIST_POI;
  public static Supplier<VillagerProfession> ARCHIVIST;
  // ========== WORLD GEN ==========
  public static Supplier<Codec<? extends ChunkGenerator>> AGE_CHUNK_GENERATOR;
  public static Supplier<Codec<? extends BiomeSource>> AGE_BIOME_SOURCE;
  public static Supplier<CreativeModeTab> MYSTCRAFT_TAB;
  public static Supplier<CreativeModeTab> MYSTCRAFT_PAGES_TAB;

  static {
    LiquidBlock fluidInkBlock = new LiquidBlock(BLACK_INK_SOURCE_FLUID, BlockBehaviour.Properties.copy(Blocks.WATER)
        .mapColor(MapColor.COLOR_BLACK)
        .noLootTable()) {
    };
    FLUID_INK = () -> fluidInkBlock;
  }

  private FabricRegistries() {
  }

  // ========== REGISTRATION HELPERS ==========
  private static <T extends Block> Supplier<T> registerBlock(String name, T block) {
    Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Mystcraft.MOD_ID, name), block);
    return () -> block;
  }

  private static <T extends Item> Supplier<T> registerItem(String name, T item) {
    Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Mystcraft.MOD_ID, name), item);
    return () -> item;
  }

  private static Supplier<Item> registerBlockItem(String name, Supplier<? extends Block> block) {
    BlockItem blockItem = new BlockItem(block.get(), new Item.Properties());
    Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Mystcraft.MOD_ID, name), blockItem);
    return () -> blockItem;
  }

  private static <T extends net.minecraft.world.entity.Entity> Supplier<EntityType<T>> registerEntity(
      String name, EntityType<T> type) {
    Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(Mystcraft.MOD_ID, name), type);
    return () -> type;
  }

  private static <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(
      String name, BlockEntityType<T> type) {
    Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation(Mystcraft.MOD_ID, name), type);
    return () -> type;
  }

  private static Supplier<SoundEvent> registerSound(String name) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, name);
    SoundEvent event = SoundEvent.createVariableRangeEvent(id);
    Registry.register(BuiltInRegistries.SOUND_EVENT, new ResourceLocation(Mystcraft.MOD_ID,
        name.replace('.', '_').replace('-', '_')), event);
    return () -> event;
  }

  private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(
      String name, MenuType<T> menuType) {
    Registry.register(BuiltInRegistries.MENU, new ResourceLocation(Mystcraft.MOD_ID, name), menuType);
    return () -> menuType;
  }

  // ========== MAIN REGISTRATION ==========
  public static void register() {
    // Fluids
    Registry.register(BuiltInRegistries.FLUID, new ResourceLocation(Mystcraft.MOD_ID, "black_ink"), BLACK_INK_SOURCE_FLUID);
    Registry.register(BuiltInRegistries.FLUID, new ResourceLocation(Mystcraft.MOD_ID, "black_ink_flowing"), BLACK_INK_FLOWING_FLUID);

    // Fluid block
    Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(Mystcraft.MOD_ID, "fluidblockblackink"), FLUID_INK.get());

    // Structures
    StructureType<AbandonedLibraryStructure> abandonedLibrary = () -> AbandonedLibraryStructure.CODEC;
    Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
        new ResourceLocation(Mystcraft.MOD_ID, "abandoned_library"), abandonedLibrary);
    ABANDONED_LIBRARY = () -> abandonedLibrary;

    StructureType<UndergroundArchiveStructure> undergroundArchive = () -> UndergroundArchiveStructure.CODEC;
    Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
        new ResourceLocation(Mystcraft.MOD_ID, "underground_archive"), undergroundArchive);
    UNDERGROUND_ARCHIVE = () -> undergroundArchive;

    StructureType<ScatteredLibraryStructure> scatteredLibrary = () -> ScatteredLibraryStructure.CODEC;
    Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
        new ResourceLocation(Mystcraft.MOD_ID, "scattered_library"), scatteredLibrary);
    SCATTERED_LIBRARY = () -> scatteredLibrary;

    // Villagers
    PoiType poiType = PointOfInterestHelper.register(
        new ResourceLocation(Mystcraft.MOD_ID, "archivist"),
        1, 1, BOOKSTAND.get());
    ARCHIVIST_POI = () -> poiType;

    VillagerProfession profession = new VillagerProfession(
        "archivist",
        holder -> holder.value() == poiType,
        holder -> holder.value() == poiType,
        ImmutableSet.of(),
        ImmutableSet.of(),
        SoundEvents.VILLAGER_WORK_LIBRARIAN
    );
    Registry.register(BuiltInRegistries.VILLAGER_PROFESSION,
        new ResourceLocation(Mystcraft.MOD_ID, "archivist"), profession);
    ARCHIVIST = () -> profession;

    // World gen
    Codec<? extends ChunkGenerator> chunkGenCodec = AgeChunkGenerator.CODEC;
    AGE_CHUNK_GENERATOR = () -> chunkGenCodec;
    Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
        new ResourceLocation(Mystcraft.MOD_ID, "age_chunk_generator"), chunkGenCodec);

    Codec<? extends BiomeSource> biomeSourceCodec = AgeBiomeSource.CODEC;
    AGE_BIOME_SOURCE = () -> biomeSourceCodec;
    Registry.register(BuiltInRegistries.BIOME_SOURCE,
        new ResourceLocation(Mystcraft.MOD_ID, "age_biome_source"), biomeSourceCodec);

    // Creative tabs
    CreativeModeTab mainTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID))
        .icon(() -> new ItemStack(AGEBOOK.get()))
        .displayItems((params, output) -> {
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
          output.accept(BOOKSTAND_ITEM.get());
          output.accept(BOOK_RECEPTACLE_ITEM.get());
          output.accept(CRYSTAL_ITEM.get());
          output.accept(DECAY_ITEM.get());
        })
        .build();
    MYSTCRAFT_TAB = () -> mainTab;
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
        new ResourceLocation(Mystcraft.MOD_ID, "mystcraft"), mainTab);

    CreativeModeTab pagesTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
        .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID + "_pages"))
        .icon(() -> Page.createLinkPage())
        .displayItems((params, output) -> {
          try {
            output.accept(Page.createLinkPage());
          } catch (Exception e) {
            Mystcraft.LOGGER.error("[FabricRegistries] Failed to create link page", e);
          }
          int count = 0;
          for (SymbolCategory category : SymbolCategory.values()) {
            List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
            for (IAgeSymbol symbol : symbols) {
              try {
                output.accept(Page.createSymbolPage(symbol.getRegistryName()));
                count++;
              } catch (Exception e) {
                Mystcraft.LOGGER.error("[FabricRegistries] Failed to create page for symbol: {}", symbol.getRegistryName(), e);
              }
            }
          }
          Mystcraft.LOGGER.debug("[FabricRegistries] displayItems populated {} symbol pages", count);
        })
        .build();
    MYSTCRAFT_PAGES_TAB = () -> pagesTab;
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
        new ResourceLocation(Mystcraft.MOD_ID, "mystcraft_pages"), pagesTab);

    ItemGroupEvents.modifyEntriesEvent(MYSTCRAFT_PAGES_TAB_KEY).register(content -> {
      int count = 0;
      for (SymbolCategory category : SymbolCategory.values()) {
        List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
        for (IAgeSymbol symbol : symbols) {
          content.accept(Page.createSymbolPage(symbol.getRegistryName()));
          count++;
        }
      }
      Mystcraft.LOGGER.info("[FabricRegistries] ItemGroupEvents added {} symbol pages", count);
    });

    Mystcraft.LOGGER.info("[FabricRegistries] Registered all Mystcraft content");
  }

  // ========== POPULATE COMMON REGISTRIES ==========
  public static void populateCommonRegistries() {
    // Blocks
    ModBlocks.INK_MIXER = INK_MIXER;
    ModBlocks.BOOK_BINDER = BOOK_BINDER;
    ModBlocks.BOOK_RECEPTACLE = BOOK_RECEPTACLE;
    ModBlocks.BOOKSTAND = BOOKSTAND;
    ModBlocks.LINK_MODIFIER = LINK_MODIFIER;
    ModBlocks.WRITING_DESK = WRITING_DESK;
    ModBlocks.CRYSTAL = CRYSTAL;
    ModBlocks.DECAY = DECAY;
    ModBlocks.LINK_PORTAL = LINK_PORTAL;
    ModBlocks.STAR_FISSURE = STAR_FISSURE;
    ModBlocks.FLUID_INK = () -> (LiquidBlock) FLUID_INK.get();

    // Items
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
    ModItems.BOOKSTAND_ITEM = BOOKSTAND_ITEM;
    ModItems.LINK_MODIFIER_ITEM = LINK_MODIFIER_ITEM;
    ModItems.WRITING_DESK_ITEM = WRITING_DESK_ITEM;
    ModItems.CRYSTAL_ITEM = CRYSTAL_ITEM;
    ModItems.DECAY_ITEM = DECAY_ITEM;

    // Block entities
    ModBlockEntities.INK_MIXER = INK_MIXER_BE;
    ModBlockEntities.BOOK_BINDER = BOOK_BINDER_BE;
    ModBlockEntities.BOOK_RECEPTACLE = BOOK_RECEPTACLE_BE;
    ModBlockEntities.BOOKSTAND = BOOKSTAND_BE;
    ModBlockEntities.WRITING_DESK = WRITING_DESK_BE;
    ModBlockEntities.STAR_FISSURE = STAR_FISSURE_BE;
    ModBlockEntities.LINK_MODIFIER = LINK_MODIFIER_BE;

    // Entities
    ModEntities.LINKBOOK = LINKBOOK_ENTITY;
    ModEntities.FALLING_BLOCK = FALLING_BLOCK_ENTITY;
    ModEntities.METEOR = METEOR_ENTITY;
    ModEntities.COLORED_LIGHTNING = COLORED_LIGHTNING_ENTITY;

    // Fluids
    ModFluids.BLACK_INK_SOURCE = BLACK_INK_SOURCE;
    ModFluids.BLACK_INK_FLOWING = BLACK_INK_FLOWING;
    ModFluids.BLACK_INK_BUCKET = INK_BUCKET;

    // Sounds
    ModSounds.LINKING_POP = LINKING_POP;
    ModSounds.LINKING_LINK = LINKING_LINK;
    ModSounds.LINKING_DISARM = LINKING_DISARM;
    ModSounds.LINKING_FOLLOWING = LINKING_FOLLOWING;
    ModSounds.LINKING_INTRA = LINKING_INTRA;
    ModSounds.LINKING_FISSURE = LINKING_FISSURE;
    ModSounds.LINKING_PORTAL = LINKING_PORTAL;
    ModSounds.METEOR_ROAR = METEOR_ROAR;
    ModSounds.METEOR_IMPACT = METEOR_IMPACT;

    // Menu types
    ModMenuTypes.INK_MIXER = INK_MIXER_MENU;
    ModMenuTypes.BOOK_BINDER = BOOK_BINDER_MENU;
    ModMenuTypes.LINK_MODIFIER = LINK_MODIFIER_MENU;
    ModMenuTypes.WRITING_DESK = WRITING_DESK_MENU;
    ModMenuTypes.FOLDER = FOLDER_MENU;
    ModMenuTypes.PORTFOLIO = PORTFOLIO_MENU;

    // Config
    MystcraftConfig.giveGuidebookOnFirstSpawn = () -> FabricMystcraftConfig.giveGuidebookOnFirstSpawn.get();
    MystcraftConfig.maxSymbolsPerBook = () -> FabricMystcraftConfig.maxSymbolsPerBook.get();
    MystcraftConfig.deleteAgesOnStartup = () -> FabricMystcraftConfig.deleteAgesOnStartup.get();
    MystcraftConfig.enablePersonalLinkBooks = () -> FabricMystcraftConfig.enablePersonalLinkBooks.get();
    MystcraftConfig.linkPanelInBoosterPacks = () -> FabricMystcraftConfig.linkPanelInBoosterPacks.get();
    MystcraftConfig.enableBoosterLoot = () -> FabricMystcraftConfig.enableBoosterLoot.get();
    MystcraftConfig.enablePageLoot = () -> FabricMystcraftConfig.enablePageLoot.get();
    MystcraftConfig.bookBinderCoverItems = () -> FabricMystcraftConfig.bookBinderCoverItems.get();
    MystcraftConfig.allowGravityBlocksInAges = () -> FabricMystcraftConfig.allowGravityBlocksInAges.get();
    MystcraftConfig.microDimensionsEnabled = () -> FabricMystcraftConfig.microDimensionsEnabled.get();
    MystcraftConfig.microDimensionRadiusChunks = () -> FabricMystcraftConfig.microDimensionRadiusChunks.get();
    MystcraftConfig.microDimensionExtraChunks = () -> FabricMystcraftConfig.microDimensionExtraChunks.get();
    MystcraftConfig.safeStories = () -> FabricMystcraftConfig.safeStories.get();
    MystcraftConfig.droppedBooksBecomeLivingEntities = () -> FabricMystcraftConfig.droppedBooksBecomeLivingEntities.get();
    MystcraftConfig.dropBooksOnRead = () -> FabricMystcraftConfig.dropBooksOnRead.get();
    MystcraftConfig.disabledSymbols = () -> FabricMystcraftConfig.disabledSymbols.get();
    MystcraftConfig.instabilityEnabled = () -> FabricMystcraftConfig.instabilityEnabled.get();
    MystcraftConfig.deathEffectsEnabled = () -> FabricMystcraftConfig.deathEffectsEnabled.get();
    MystcraftConfig.allowUnstableAges = () -> FabricMystcraftConfig.allowUnstableAges.get();
    MystcraftConfig.instabilityMultiplier = () -> FabricMystcraftConfig.instabilityMultiplier.get();
    MystcraftConfig.maxAllowedInstability = () -> FabricMystcraftConfig.maxAllowedInstability.get();
    MystcraftConfig.thresholdDecay = () -> FabricMystcraftConfig.thresholdDecay.get();
    MystcraftConfig.thresholdTransmute = () -> FabricMystcraftConfig.thresholdTransmute.get();
    MystcraftConfig.thresholdLightning = () -> FabricMystcraftConfig.thresholdLightning.get();
    MystcraftConfig.thresholdMeteor = () -> FabricMystcraftConfig.thresholdMeteor.get();
    MystcraftConfig.thresholdPoison = () -> FabricMystcraftConfig.thresholdPoison.get();
    MystcraftConfig.thresholdWither = () -> FabricMystcraftConfig.thresholdWither.get();
    MystcraftConfig.chanceDecay = () -> FabricMystcraftConfig.chanceDecay.get();
    MystcraftConfig.chanceTransmute = () -> FabricMystcraftConfig.chanceTransmute.get();
    MystcraftConfig.chanceLightning = () -> FabricMystcraftConfig.chanceLightning.get();
    MystcraftConfig.chanceMeteor = () -> FabricMystcraftConfig.chanceMeteor.get();
    MystcraftConfig.chancePlayerEffect = () -> FabricMystcraftConfig.chancePlayerEffect.get();
    MystcraftConfig.pocketInnerHalfSizeXZ = () -> FabricMystcraftConfig.pocketInnerHalfSizeXZ.get();
    MystcraftConfig.pocketInnerHalfSizeY = () -> FabricMystcraftConfig.pocketInnerHalfSizeY.get();
    MystcraftConfig.pocketInnerThickness = () -> FabricMystcraftConfig.pocketInnerThickness.get();
    MystcraftConfig.pocketOuterThickness = () -> FabricMystcraftConfig.pocketOuterThickness.get();
    MystcraftConfig.pocketCenterY = () -> FabricMystcraftConfig.pocketCenterY.get();
    MystcraftConfig.pocketInnerBlockPalette = () -> FabricMystcraftConfig.pocketInnerBlockPalette.get();
    MystcraftConfig.pocketOuterBlock = () -> FabricMystcraftConfig.pocketOuterBlock.get();

    // Network
    art.arcane.mystcraft.network.MystcraftNetwork.sendToServerHandler = packet -> {
      FabricMystcraftNetwork.sendToServerGeneric(packet);
    };
    art.arcane.mystcraft.network.MystcraftNetwork.sendToPlayerHandler = (packet, player) -> {
      FabricMystcraftNetwork.sendToPlayerGeneric(packet, player);
    };
    art.arcane.mystcraft.network.MystcraftNetwork.sendToAllHandler = packet -> {
      MinecraftServer server = Mystcraft.getCurrentServer();
      if (server != null) {
        FabricMystcraftNetwork.sendToAll(packet, server);
      }
    };
    art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingHandler = (packet, player) -> {
      FabricMystcraftNetwork.sendToTracking(packet, player);
    };
    art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingBlockHandler = FabricMystcraftNetwork::sendToTrackingBlock;

    // Structure types
    art.arcane.mystcraft.world.structure.ModStructures.ABANDONED_LIBRARY = ABANDONED_LIBRARY;
    art.arcane.mystcraft.world.structure.ModStructures.UNDERGROUND_ARCHIVE = UNDERGROUND_ARCHIVE;
    art.arcane.mystcraft.world.structure.ModStructures.SCATTERED_LIBRARY = SCATTERED_LIBRARY;

    Mystcraft.LOGGER.info("[Mystcraft] Common registry stubs populated from Fabric registrations");
  }
}
