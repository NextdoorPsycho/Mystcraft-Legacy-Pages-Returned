package art.arcane.mystcraft.neoforge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.block.*;
import art.arcane.mystcraft.blockentity.*;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.menu.*;
import art.arcane.mystcraft.network.NeoForgeMystcraftNetwork;
import art.arcane.mystcraft.registry.ModBlockEntities;
import art.arcane.mystcraft.registry.ModBlocks;
import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.registry.ModFluids;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.registry.ModMenuTypes;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import art.arcane.mystcraft.world.structure.AbandonedLibraryStructure;
import art.arcane.mystcraft.world.structure.ScatteredLibraryStructure;
import art.arcane.mystcraft.world.structure.UndergroundArchiveStructure;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * Consolidated registry holder for all Mystcraft registrations on NeoForge.
 * Includes blocks, items, entities, block entities, fluids, sounds, menus,
 * loot modifiers, villagers, world gen, and creative tabs.
 */
public final class NeoForgeRegistries {

  // ==================== Deferred Registers ====================

  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, Mystcraft.MOD_ID);

  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(Registries.ITEM, Mystcraft.MOD_ID);

  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mystcraft.MOD_ID);

  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, Mystcraft.MOD_ID);

  public static final DeferredRegister<Fluid> FLUIDS =
      DeferredRegister.create(Registries.FLUID, Mystcraft.MOD_ID);

  public static final DeferredRegister<FluidType> FLUID_TYPES =
      DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(Registries.SOUND_EVENT, Mystcraft.MOD_ID);

  public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mystcraft.MOD_ID);

  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, Mystcraft.MOD_ID);

  public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
      DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mystcraft.MOD_ID);

  public static final DeferredRegister<PoiType> POI_TYPES =
      DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Mystcraft.MOD_ID);

  public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
      DeferredRegister.create(Registries.VILLAGER_PROFESSION, Mystcraft.MOD_ID);

  public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
      DeferredRegister.create(Registries.CHUNK_GENERATOR, Mystcraft.MOD_ID);

  public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
      DeferredRegister.create(Registries.BIOME_SOURCE, Mystcraft.MOD_ID);

  public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
      DeferredRegister.create(Registries.STRUCTURE_TYPE, Mystcraft.MOD_ID);

  // ==================== Fluid Types ====================

  public static final DeferredHolder<FluidType, FluidType> BLACK_INK_TYPE =
      FLUID_TYPES.register("black_ink", BlackInkFluidType::new);

  // ==================== Fluids ====================
  public static final DeferredHolder<Block, Block> INK_MIXER =
      BLOCKS.register("blockinkmixer",
          () -> new InkMixerBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.WOOD)
              .strength(2.5F)
              .requiresCorrectToolForDrops()));
  // Block items
  public static final DeferredHolder<Item, Item> INK_MIXER_ITEM = blockItem("blockinkmixer", INK_MIXER);  public static final DeferredHolder<Fluid, FlowingFluid> BLACK_INK_SOURCE =
      FLUIDS.register("black_ink",
          () -> new BlackInkFluid.Source(BlackInkFluid.createProperties()));
  // ==================== Blocks ====================
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InkMixerBlockEntity>> INK_MIXER_BE =
      BLOCK_ENTITIES.register("ink_mixer",
          () -> BlockEntityType.Builder.of(InkMixerBlockEntity::new, INK_MIXER.get()).build(null));
  public static final DeferredHolder<Block, Block> BOOK_BINDER =
      BLOCKS.register("blockbookbinder",
          () -> new BookBinderBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.WOOD)
              .strength(2.5F)
              .requiresCorrectToolForDrops()));  public static final DeferredHolder<Fluid, FlowingFluid> BLACK_INK_FLOWING =
      FLUIDS.register("black_ink_flowing",
          () -> new BlackInkFluid.Flowing(BlackInkFluid.createProperties()));
  public static final DeferredHolder<Item, Item> BOOK_BINDER_ITEM = blockItem("blockbookbinder", BOOK_BINDER);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookBinderBlockEntity>> BOOK_BINDER_BE =
      BLOCK_ENTITIES.register("book_binder",
          () -> BlockEntityType.Builder.of(BookBinderBlockEntity::new, BOOK_BINDER.get()).build(null));
  public static final DeferredHolder<Block, Block> BOOK_RECEPTACLE =
      BLOCKS.register("blockbookreceptacle",
          () -> new BookReceptacleBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.STONE)
              .strength(3.5F)
              .requiresCorrectToolForDrops()));
  public static final DeferredHolder<Item, Item> BOOK_RECEPTACLE_ITEM = blockItem("blockbookreceptacle", BOOK_RECEPTACLE);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookReceptacleBlockEntity>> BOOK_RECEPTACLE_BE =
      BLOCK_ENTITIES.register("book_receptacle",
          () -> BlockEntityType.Builder.of(BookReceptacleBlockEntity::new, BOOK_RECEPTACLE.get()).build(null));
  public static final DeferredHolder<Block, Block> BOOKSTAND =
      BLOCKS.register("blockbookstand",
          () -> new BookstandBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.WOOD)
              .strength(2.0F)
              .requiresCorrectToolForDrops()));
  public static final DeferredHolder<Item, Item> BOOKSTAND_ITEM = blockItem("blockbookstand", BOOKSTAND);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BookstandBlockEntity>> BOOKSTAND_BE =
      BLOCK_ENTITIES.register("bookstand",
          () -> BlockEntityType.Builder.of(BookstandBlockEntity::new, BOOKSTAND.get()).build(null));
  public static final DeferredHolder<PoiType, PoiType> ARCHIVIST_POI = POI_TYPES.register(
      "archivist",
      () -> new PoiType(
          ImmutableSet.copyOf(BOOKSTAND.get().getStateDefinition().getPossibleStates()),
          1, 1
      )
  );
  // ==================== Items ====================
  public static final DeferredHolder<VillagerProfession, VillagerProfession> ARCHIVIST = VILLAGER_PROFESSIONS.register(
      "archivist",
      () -> new VillagerProfession(
          "archivist",
          holder -> holder.value() == ARCHIVIST_POI.get(),
          holder -> holder.value() == ARCHIVIST_POI.get(),
          ImmutableSet.of(),
          ImmutableSet.of(),
          SoundEvents.VILLAGER_WORK_LIBRARIAN
      )
  );
  public static final DeferredHolder<Block, Block> LINK_MODIFIER =
      BLOCKS.register("blocklinkmodifier",
          () -> new LinkModifierBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.STONE)
              .strength(3.0F)
              .requiresCorrectToolForDrops()));
  public static final DeferredHolder<Item, Item> LINK_MODIFIER_ITEM = blockItem("blocklinkmodifier", LINK_MODIFIER);  public static final DeferredHolder<Block, LiquidBlock> FLUID_INK =
      BLOCKS.register("fluidblockblackink",
          () -> new LiquidBlock(BLACK_INK_SOURCE, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
              .mapColor(MapColor.COLOR_BLACK)
              .noLootTable()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LinkModifierBlockEntity>> LINK_MODIFIER_BE =
      BLOCK_ENTITIES.register("link_modifier",
          () -> BlockEntityType.Builder.of(LinkModifierBlockEntity::new, LINK_MODIFIER.get()).build(null));
  public static final DeferredHolder<Block, Block> WRITING_DESK =
      BLOCKS.register("writingdesk",
          () -> new WritingDeskBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.WOOD)
              .strength(2.5F)
              .requiresCorrectToolForDrops()));
  public static final DeferredHolder<Item, Item> WRITING_DESK_ITEM = blockItem("writingdesk", WRITING_DESK);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WritingDeskBlockEntity>> WRITING_DESK_BE =
      BLOCK_ENTITIES.register("writing_desk",
          () -> BlockEntityType.Builder.of(WritingDeskBlockEntity::new, WRITING_DESK.get()).build(null));
  public static final DeferredHolder<Block, Block> CRYSTAL =
      BLOCKS.register("blockcrystal",
          () -> new CrystalBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.COLOR_LIGHT_BLUE)
              .strength(1.5F)
              .lightLevel(CrystalBlock::getLightLevel)
              .noOcclusion()));
  public static final DeferredHolder<Item, Item> CRYSTAL_ITEM = blockItem("blockcrystal", CRYSTAL);
  public static final DeferredHolder<Block, Block> DECAY =
      BLOCKS.register("blockdecay",
          () -> new DecayBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.COLOR_BLACK)
              .strength(-1.0F, 3600000.0F)
              .noLootTable()
              .randomTicks()));
  public static final DeferredHolder<Item, Item> DECAY_ITEM = blockItem("blockdecay", DECAY);
  public static final DeferredHolder<Block, Block> LINK_PORTAL =
      BLOCKS.register("linkportal",
          () -> new LinkPortalBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.COLOR_BLACK)
              .noCollission()
              .strength(-1.0F)
              .noLootTable()
              .lightLevel(state -> 11)
              .pushReaction(PushReaction.BLOCK)));
  public static final DeferredHolder<Block, Block> STAR_FISSURE =
      BLOCKS.register("blockstarfissure",
          () -> new StarFissureBlock(BlockBehaviour.Properties.of()
              .mapColor(MapColor.COLOR_BLACK)
              .noCollission()
              .strength(-1.0F, 3600000.0F)
              .noLootTable()
              .lightLevel(state -> 15)
              .pushReaction(PushReaction.BLOCK)));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarFissureBlockEntity>> STAR_FISSURE_BE =
      BLOCK_ENTITIES.register("star_fissure",
          () -> BlockEntityType.Builder.of(StarFissureBlockEntity::new, STAR_FISSURE.get()).build(null));
  public static final DeferredHolder<Item, Item> PAGE =
      ITEMS.register("page",
          () -> new NeoForgePageItem(new Item.Properties().stacksTo(64)));  public static final DeferredHolder<Item, Item> INK_BUCKET =
      ITEMS.register("ink_bucket",
          () -> new BucketItem(BLACK_INK_SOURCE,
              new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
  public static final DeferredHolder<Item, Item> AGEBOOK =
      ITEMS.register("agebook",
          () -> new AgebookItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<Item, Item> LINKBOOK =
      ITEMS.register("linkbook",
          () -> new LinkbookItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<Item, Item> LINKBOOK_UNLINKED =
      ITEMS.register("linkbook_unlinked",
          () -> new LinkbookUnlinkedItem(new Item.Properties().stacksTo(16)));
  public static final DeferredHolder<Item, Item> PERSONAL_LINK_BOOK =
      ITEMS.register("personal_link_book",
          () -> new PersonalLinkBookItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<Item, Item> BOOSTER_PACK =
      ITEMS.register("booster",
          () -> new BoosterPackItem(new Item.Properties().stacksTo(16)));
  // ==================== Block Entities ====================
  public static final DeferredHolder<Item, Item> FOLDER =
      ITEMS.register("folder",
          () -> new FolderItem(new Item.Properties()));
  public static final DeferredHolder<Item, Item> PORTFOLIO =
      ITEMS.register("portfolio",
          () -> new PortfolioItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<Item, Item> INK_VIAL =
      ITEMS.register("inkvial",
          () -> new InkVialItem(new Item.Properties().stacksTo(16)));
  public static final DeferredHolder<Item, Item> GUIDEBOOK =
      ITEMS.register("guidebook",
          () -> new GuidebookItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<EntityType<?>, EntityType<LinkbookEntity>> LINKBOOK_ENTITY =
      ENTITIES.register("linkbook",
          () -> EntityType.Builder.<LinkbookEntity>of(LinkbookEntity::new, MobCategory.MISC)
              .sized(0.5F, 0.5F)
              .clientTrackingRange(10)
              .updateInterval(20)
              .build(new ResourceLocation(Mystcraft.MOD_ID, "linkbook").toString()));
  public static final DeferredHolder<EntityType<?>, EntityType<MystcraftFallingBlockEntity>> FALLING_BLOCK_ENTITY =
      ENTITIES.register("falling_block",
          () -> EntityType.Builder.<MystcraftFallingBlockEntity>of(MystcraftFallingBlockEntity::new, MobCategory.MISC)
              .sized(0.98F, 0.98F)
              .clientTrackingRange(10)
              .updateInterval(20)
              .build(new ResourceLocation(Mystcraft.MOD_ID, "falling_block").toString()));
  public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR_ENTITY =
      ENTITIES.register("meteor",
          () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
              .sized(2.0F, 2.0F)
              .clientTrackingRange(16)
              .updateInterval(10)
              .fireImmune()
              .build(new ResourceLocation(Mystcraft.MOD_ID, "meteor").toString()));
  // ==================== Entities ====================
  public static final DeferredHolder<EntityType<?>, EntityType<ColoredLightningEntity>> COLORED_LIGHTNING_ENTITY =
      ENTITIES.register("colored_lightning",
          () -> EntityType.Builder.<ColoredLightningEntity>of(ColoredLightningEntity::new, MobCategory.MISC)
              .sized(0.0F, 0.0F)
              .clientTrackingRange(16)
              .updateInterval(Integer.MAX_VALUE)
              .noSave()
              .build(new ResourceLocation(Mystcraft.MOD_ID, "colored_lightning").toString()));
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_POP = registerSound("linking.pop");
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_LINK = registerSound("linking.link");
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_DISARM = registerSound("linking.link-disarm");
  // ==================== Sounds ====================
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_FOLLOWING = registerSound("linking.link-following");
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_INTRA = registerSound("linking.link-intra");
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_FISSURE = registerSound("linking.link-fissure");
  public static final DeferredHolder<SoundEvent, SoundEvent> LINKING_PORTAL = registerSound("linking.link-portal");
  public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_ROAR = registerSound("entity.meteor.roar");
  public static final DeferredHolder<SoundEvent, SoundEvent> METEOR_IMPACT = registerSound("entity.meteor.impact");
  public static final DeferredHolder<MenuType<?>, MenuType<InkMixerMenu>> INK_MIXER_MENU =
      MENUS.register("ink_mixer",
          () -> IMenuTypeExtension.create(InkMixerMenu::new));
  public static final DeferredHolder<MenuType<?>, MenuType<BookBinderMenu>> BOOK_BINDER_MENU =
      MENUS.register("book_binder",
          () -> IMenuTypeExtension.create(BookBinderMenu::new));
  public static final DeferredHolder<MenuType<?>, MenuType<LinkModifierMenu>> LINK_MODIFIER_MENU =
      MENUS.register("link_modifier",
          () -> IMenuTypeExtension.create(LinkModifierMenu::new));
  public static final DeferredHolder<MenuType<?>, MenuType<WritingDeskMenu>> WRITING_DESK_MENU =
      MENUS.register("writing_desk",
          () -> IMenuTypeExtension.create(WritingDeskMenu::new));
  // ==================== Menus ====================
  public static final DeferredHolder<MenuType<?>, MenuType<FolderMenu>> FOLDER_MENU =
      MENUS.register("folder",
          () -> IMenuTypeExtension.create(FolderMenu::new));
  public static final DeferredHolder<MenuType<?>, MenuType<PortfolioMenu>> PORTFOLIO_MENU =
      MENUS.register("portfolio",
          () -> IMenuTypeExtension.create(PortfolioMenu::new));
  public static final DeferredHolder<Codec<? extends IGlobalLootModifier>, Codec<SymbolPageLootModifier>> SYMBOL_PAGE_LOOT =
      LOOT_MODIFIERS.register("symbol_page", SymbolPageLootModifier.CODEC);
  public static final DeferredHolder<Codec<? extends IGlobalLootModifier>, Codec<GuidebookLootModifier>> GUIDEBOOK_LOOT =
      LOOT_MODIFIERS.register("guidebook", GuidebookLootModifier.CODEC);
  public static final DeferredHolder<Codec<? extends IGlobalLootModifier>, Codec<BoosterPackLootModifier>> BOOSTER_PACK_LOOT =
      LOOT_MODIFIERS.register("booster_pack", BoosterPackLootModifier.CODEC);
  public static final DeferredHolder<Codec<? extends ChunkGenerator>, Codec<? extends ChunkGenerator>> AGE_CHUNK_GENERATOR =
      CHUNK_GENERATORS.register("age_chunk_generator", () -> AgeChunkGenerator.CODEC);
  // ==================== Loot Modifiers ====================
  public static final DeferredHolder<Codec<? extends BiomeSource>, Codec<? extends BiomeSource>> AGE_BIOME_SOURCE =
      BIOME_SOURCES.register("age_biome_source", () -> AgeBiomeSource.CODEC);
  public static final DeferredHolder<StructureType<?>, StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY =
      STRUCTURE_TYPES.register("abandoned_library",
          () -> () -> AbandonedLibraryStructure.CODEC);
  public static final DeferredHolder<StructureType<?>, StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE =
      STRUCTURE_TYPES.register("underground_archive",
          () -> () -> UndergroundArchiveStructure.CODEC);
  // ==================== World Gen ====================
  public static final DeferredHolder<StructureType<?>, StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY =
      STRUCTURE_TYPES.register("scattered_library",
          () -> () -> ScatteredLibraryStructure.CODEC);
  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MYSTCRAFT_PAGES_TAB =
      CREATIVE_TABS.register("mystcraft_pages",
          () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
              .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID + "_pages"))
              .icon(() -> Page.createLinkPage())
              .displayItems((params, output) -> {
                try {
                  output.accept(Page.createLinkPage());
                } catch (Exception e) {
                  Mystcraft.LOGGER.error("[NeoForgeRegistries] Failed to create link page", e);
                }

                int count = 0;
                for (SymbolCategory category : SymbolCategory.values()) {
                  List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
                  for (IAgeSymbol symbol : symbols) {
                    try {
                      output.accept(Page.createSymbolPage(symbol.getRegistryName()));
                      count++;
                    } catch (Exception e) {
                      Mystcraft.LOGGER.error("[NeoForgeRegistries] Failed to create page for symbol: {}", symbol.getRegistryName(), e);
                    }
                  }
                }
                Mystcraft.LOGGER.debug("[NeoForgeRegistries] displayItems populated {} symbol pages", count);
              })
              .build());
  private NeoForgeRegistries() {
  }

  private static DeferredHolder<Item, Item> blockItem(String name, Supplier<? extends Block> block) {
    return ITEMS.register(name,
        () -> new BlockItem(block.get(), new Item.Properties()));
  }

  private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, name);
    return SOUNDS.register(name.replace('.', '_').replace('-', '_'),
        () -> SoundEvent.createVariableRangeEvent(id));
  }

  /**
   * Registers all DeferredRegister instances to the mod event bus
   * and populates common registry stubs.
   */
  public static void register(IEventBus modEventBus) {
    // Register all deferred registers
    BLOCKS.register(modEventBus);
    ITEMS.register(modEventBus);
    BLOCK_ENTITIES.register(modEventBus);
    ENTITIES.register(modEventBus);
    FLUIDS.register(modEventBus);
    FLUID_TYPES.register(modEventBus);
    SOUNDS.register(modEventBus);
    CREATIVE_TABS.register(modEventBus);
    MENUS.register(modEventBus);
    LOOT_MODIFIERS.register(modEventBus);
    POI_TYPES.register(modEventBus);
    VILLAGER_PROFESSIONS.register(modEventBus);
    CHUNK_GENERATORS.register(modEventBus);
    BIOME_SOURCES.register(modEventBus);
    STRUCTURE_TYPES.register(modEventBus);

    // Populate common registry stubs
    populateCommonRegistries();

    Mystcraft.LOGGER.info("Registered all Mystcraft deferred registries");
  }

  // ==================== Structures ====================

  /**
   * Populates common registry stubs from NeoForge DeferredHolders.
   */
  private static void populateCommonRegistries() {
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
    ModBlocks.FLUID_INK = FLUID_INK;

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

    // Entities
    ModEntities.LINKBOOK = LINKBOOK_ENTITY;
    ModEntities.FALLING_BLOCK = FALLING_BLOCK_ENTITY;
    ModEntities.METEOR = METEOR_ENTITY;
    ModEntities.COLORED_LIGHTNING = COLORED_LIGHTNING_ENTITY;

    // Block Entities
    ModBlockEntities.INK_MIXER = INK_MIXER_BE;
    ModBlockEntities.BOOK_BINDER = BOOK_BINDER_BE;
    ModBlockEntities.BOOK_RECEPTACLE = BOOK_RECEPTACLE_BE;
    ModBlockEntities.BOOKSTAND = BOOKSTAND_BE;
    ModBlockEntities.WRITING_DESK = WRITING_DESK_BE;
    ModBlockEntities.STAR_FISSURE = STAR_FISSURE_BE;
    ModBlockEntities.LINK_MODIFIER = LINK_MODIFIER_BE;

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

    // Menu Types
    ModMenuTypes.INK_MIXER = INK_MIXER_MENU;
    ModMenuTypes.BOOK_BINDER = BOOK_BINDER_MENU;
    ModMenuTypes.LINK_MODIFIER = LINK_MODIFIER_MENU;
    ModMenuTypes.WRITING_DESK = WRITING_DESK_MENU;
    ModMenuTypes.FOLDER = FOLDER_MENU;
    ModMenuTypes.PORTFOLIO = PORTFOLIO_MENU;

    // Structures
    art.arcane.mystcraft.world.structure.ModStructures.ABANDONED_LIBRARY = ABANDONED_LIBRARY;
    art.arcane.mystcraft.world.structure.ModStructures.UNDERGROUND_ARCHIVE = UNDERGROUND_ARCHIVE;
    art.arcane.mystcraft.world.structure.ModStructures.SCATTERED_LIBRARY = SCATTERED_LIBRARY;

    // Config
    art.arcane.mystcraft.config.MystcraftConfig.giveGuidebookOnFirstSpawn = NeoForgeMystcraftConfig.giveGuidebookOnFirstSpawn;
    art.arcane.mystcraft.config.MystcraftConfig.maxSymbolsPerBook = NeoForgeMystcraftConfig.maxSymbolsPerBook;
    art.arcane.mystcraft.config.MystcraftConfig.deleteAgesOnStartup = NeoForgeMystcraftConfig.deleteAgesOnStartup;
    art.arcane.mystcraft.config.MystcraftConfig.enablePersonalLinkBooks = NeoForgeMystcraftConfig.enablePersonalLinkBooks;
    art.arcane.mystcraft.config.MystcraftConfig.linkPanelInBoosterPacks = NeoForgeMystcraftConfig.linkPanelInBoosterPacks;
    art.arcane.mystcraft.config.MystcraftConfig.enableBoosterLoot = NeoForgeMystcraftConfig.enableBoosterLoot;
    art.arcane.mystcraft.config.MystcraftConfig.enablePageLoot = NeoForgeMystcraftConfig.enablePageLoot;
    art.arcane.mystcraft.config.MystcraftConfig.bookBinderCoverItems = () -> List.copyOf(NeoForgeMystcraftConfig.bookBinderCoverItems.get());
    art.arcane.mystcraft.config.MystcraftConfig.allowGravityBlocksInAges = NeoForgeMystcraftConfig.allowGravityBlocksInAges;
    art.arcane.mystcraft.config.MystcraftConfig.microDimensionsEnabled = NeoForgeMystcraftConfig.microDimensionsEnabled;
    art.arcane.mystcraft.config.MystcraftConfig.microDimensionRadiusChunks = NeoForgeMystcraftConfig.microDimensionRadiusChunks;
    art.arcane.mystcraft.config.MystcraftConfig.microDimensionExtraChunks = NeoForgeMystcraftConfig.microDimensionExtraChunks;
    art.arcane.mystcraft.config.MystcraftConfig.safeStories = NeoForgeMystcraftConfig.safeStories;
    art.arcane.mystcraft.config.MystcraftConfig.droppedBooksBecomeLivingEntities = NeoForgeMystcraftConfig.droppedBooksBecomeLivingEntities;
    art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead = NeoForgeMystcraftConfig.dropBooksOnRead;
    art.arcane.mystcraft.config.MystcraftConfig.disabledSymbols = () -> List.copyOf(NeoForgeMystcraftConfig.disabledSymbols.get());
    art.arcane.mystcraft.config.MystcraftConfig.instabilityEnabled = NeoForgeMystcraftConfig.instabilityEnabled;
    art.arcane.mystcraft.config.MystcraftConfig.deathEffectsEnabled = NeoForgeMystcraftConfig.deathEffectsEnabled;
    art.arcane.mystcraft.config.MystcraftConfig.allowUnstableAges = NeoForgeMystcraftConfig.allowUnstableAges;
    art.arcane.mystcraft.config.MystcraftConfig.instabilityMultiplier = NeoForgeMystcraftConfig.instabilityMultiplier;
    art.arcane.mystcraft.config.MystcraftConfig.maxAllowedInstability = NeoForgeMystcraftConfig.maxAllowedInstability;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdDecay = NeoForgeMystcraftConfig.thresholdDecay;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdTransmute = NeoForgeMystcraftConfig.thresholdTransmute;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdLightning = NeoForgeMystcraftConfig.thresholdLightning;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdMeteor = NeoForgeMystcraftConfig.thresholdMeteor;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdPoison = NeoForgeMystcraftConfig.thresholdPoison;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdWither = NeoForgeMystcraftConfig.thresholdWither;
    art.arcane.mystcraft.config.MystcraftConfig.chanceDecay = NeoForgeMystcraftConfig.chanceDecay;
    art.arcane.mystcraft.config.MystcraftConfig.chanceTransmute = NeoForgeMystcraftConfig.chanceTransmute;
    art.arcane.mystcraft.config.MystcraftConfig.chanceLightning = NeoForgeMystcraftConfig.chanceLightning;
    art.arcane.mystcraft.config.MystcraftConfig.chanceMeteor = NeoForgeMystcraftConfig.chanceMeteor;
    art.arcane.mystcraft.config.MystcraftConfig.chancePlayerEffect = NeoForgeMystcraftConfig.chancePlayerEffect;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerHalfSizeXZ = NeoForgeMystcraftConfig.pocketInnerHalfSizeXZ;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerHalfSizeY = NeoForgeMystcraftConfig.pocketInnerHalfSizeY;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerThickness = NeoForgeMystcraftConfig.pocketInnerThickness;
    art.arcane.mystcraft.config.MystcraftConfig.pocketOuterThickness = NeoForgeMystcraftConfig.pocketOuterThickness;
    art.arcane.mystcraft.config.MystcraftConfig.pocketCenterY = NeoForgeMystcraftConfig.pocketCenterY;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerBlockPalette = () -> List.copyOf(NeoForgeMystcraftConfig.pocketInnerBlockPalette.get());
    art.arcane.mystcraft.config.MystcraftConfig.pocketOuterBlock = NeoForgeMystcraftConfig.pocketOuterBlock;

    // Network
    art.arcane.mystcraft.network.MystcraftNetwork.sendToServerHandler = NeoForgeMystcraftNetwork::sendToServer;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToPlayerHandler = NeoForgeMystcraftNetwork::sendToPlayer;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToAllHandler = NeoForgeMystcraftNetwork::sendToAll;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingHandler = NeoForgeMystcraftNetwork::sendToTracking;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingBlockHandler = NeoForgeMystcraftNetwork::sendToTrackingBlock;
  }

  /**
   * Adds symbol pages to the pages tab via NeoForge event.
   */
  public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
    if (event.getTab() == MYSTCRAFT_PAGES_TAB.get()) {
      int count = 0;
      for (SymbolCategory category : SymbolCategory.values()) {
        List<IAgeSymbol> symbols = SymbolRegistry.getByCategory(category);
        for (IAgeSymbol symbol : symbols) {
          event.accept(Page.createSymbolPage(symbol.getRegistryName()));
          count++;
        }
      }
      Mystcraft.LOGGER.info("[NeoForgeRegistries] BuildCreativeModeTabContentsEvent added {} symbol pages", count);
    }
  }



  // ==================== Villagers ====================





  // ==================== Creative Tabs ====================



  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MYSTCRAFT_TAB =
      CREATIVE_TABS.register("mystcraft",
          () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
              .title(Component.translatable("itemGroup." + Mystcraft.MOD_ID))
              .icon(() -> new ItemStack(AGEBOOK.get()))
              .displayItems((params, output) -> {
                output.accept(GUIDEBOOK.get());
                output.accept(AGEBOOK.get());
                output.accept(LINKBOOK.get());
                output.accept(LINKBOOK_UNLINKED.get());
                output.accept(PERSONAL_LINK_BOOK.get());
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
              .build());


}
