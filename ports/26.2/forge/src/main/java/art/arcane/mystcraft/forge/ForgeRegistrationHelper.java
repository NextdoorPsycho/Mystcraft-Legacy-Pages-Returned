package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.advancements.ModAdvancements;
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
import art.arcane.mystcraft.platform.services.IRegistrationHelper;
import art.arcane.mystcraft.registry.BlockDefinitions;
import art.arcane.mystcraft.registry.ItemDefinitions;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Forge 65 deferred registration and common-supplier publication. */
public final class ForgeRegistrationHelper implements IRegistrationHelper {

  public static final ResourceKey<PoiType> ARCHIVIST_POI_KEY = key(
      Registries.POINT_OF_INTEREST_TYPE, "archivist");
  public static final ResourceKey<VillagerProfession> ARCHIVIST_PROFESSION_KEY = key(
      Registries.VILLAGER_PROFESSION, "archivist");
  private final DeferredRegister<Block> blocks =
      DeferredRegister.create(ForgeRegistries.BLOCKS, Mystcraft.MOD_ID);
  private final DeferredRegister<Item> items =
      DeferredRegister.create(ForgeRegistries.ITEMS, Mystcraft.MOD_ID);
  private final DeferredRegister<BlockEntityType<?>> blockEntities =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Mystcraft.MOD_ID);
  private final DeferredRegister<EntityType<?>> entities =
      DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Mystcraft.MOD_ID);
  private final DeferredRegister<Fluid> fluids =
      DeferredRegister.create(ForgeRegistries.FLUIDS, Mystcraft.MOD_ID);
  private final DeferredRegister<FluidType> fluidTypes =
      DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Mystcraft.MOD_ID);
  private final DeferredRegister<SoundEvent> sounds =
      DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mystcraft.MOD_ID);
  private final DeferredRegister<MenuType<?>> menus =
      DeferredRegister.create(ForgeRegistries.MENU_TYPES, Mystcraft.MOD_ID);
  private final DeferredRegister<CreativeModeTab> creativeTabs =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mystcraft.MOD_ID);
  private final DeferredRegister<CriterionTrigger<?>> advancementTriggers =
      DeferredRegister.create(Registries.TRIGGER_TYPE, Mystcraft.MOD_ID);
  private final DeferredRegister<PoiType> poiTypes =
      DeferredRegister.create(ForgeRegistries.POI_TYPES, Mystcraft.MOD_ID);
  private final DeferredRegister<VillagerProfession> professions =
      DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Mystcraft.MOD_ID);
  private final DeferredRegister<MapCodec<? extends ChunkGenerator>> chunkGenerators =
      DeferredRegister.create(Registries.CHUNK_GENERATOR, Mystcraft.MOD_ID);
  private final DeferredRegister<MapCodec<? extends BiomeSource>> biomeSources =
      DeferredRegister.create(Registries.BIOME_SOURCE, Mystcraft.MOD_ID);
  private final DeferredRegister<StructureType<?>> structureTypes =
      DeferredRegister.create(Registries.STRUCTURE_TYPE, Mystcraft.MOD_ID);
  private final DeferredRegister<MapCodec<? extends LootItemFunction>> lootFunctions =
      DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Mystcraft.MOD_ID);
  private final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> lootModifiers =
      DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
          Mystcraft.MOD_ID);

  private boolean initialized;
  private BusGroup modBusGroup;

  private RegistryObject<Block> inkMixer;
  private RegistryObject<Block> bookBinder;
  private RegistryObject<Block> bookReceptacle;
  private RegistryObject<Block> linkModifier;
  private RegistryObject<Block> writingDesk;
  private RegistryObject<Block> crystal;
  private RegistryObject<Block> decay;
  private RegistryObject<Block> linkPortal;
  private RegistryObject<Block> starFissure;
  private RegistryObject<LiquidBlock> fluidInk;

  private RegistryObject<Item> page;
  private RegistryObject<Item> agebook;
  private RegistryObject<Item> linkbook;
  private RegistryObject<Item> linkbookUnlinked;
  private RegistryObject<Item> personalLinkBook;
  private RegistryObject<Item> boosterPack;
  private RegistryObject<Item> folder;
  private RegistryObject<Item> portfolio;
  private RegistryObject<Item> inkVial;
  private RegistryObject<Item> guidebook;
  private RegistryObject<Item> inkBucket;
  private RegistryObject<Item> inkMixerItem;
  private RegistryObject<Item> bookBinderItem;
  private RegistryObject<Item> bookReceptacleItem;
  private RegistryObject<Item> linkModifierItem;
  private RegistryObject<Item> writingDeskItem;
  private RegistryObject<Item> crystalItem;
  private RegistryObject<Item> decayItem;

  private RegistryObject<BlockEntityType<InkMixerBlockEntity>> inkMixerBlockEntity;
  private RegistryObject<BlockEntityType<BookBinderBlockEntity>> bookBinderBlockEntity;
  private RegistryObject<BlockEntityType<BookReceptacleBlockEntity>> bookReceptacleBlockEntity;
  private RegistryObject<BlockEntityType<WritingDeskBlockEntity>> writingDeskBlockEntity;
  private RegistryObject<BlockEntityType<StarFissureBlockEntity>> starFissureBlockEntity;
  private RegistryObject<BlockEntityType<LinkModifierBlockEntity>> linkModifierBlockEntity;
  private RegistryObject<BlockEntityType<LinkPortalBlockEntity>> linkPortalBlockEntity;

  private RegistryObject<EntityType<LinkbookEntity>> linkbookEntity;
  private RegistryObject<EntityType<PersonalPocketProxyEntity>> personalPocketProxyEntity;
  private RegistryObject<EntityType<MystcraftFallingBlockEntity>> fallingBlockEntity;
  private RegistryObject<EntityType<MeteorEntity>> meteorEntity;
  private RegistryObject<EntityType<ColoredLightningEntity>> coloredLightningEntity;

  private RegistryObject<FluidType> blackInkType;
  private RegistryObject<FlowingFluid> blackInkSource;
  private RegistryObject<FlowingFluid> blackInkFlowing;

  private RegistryObject<SoundEvent> linkingPop;
  private RegistryObject<SoundEvent> linkingLink;
  private RegistryObject<SoundEvent> linkingDisarm;
  private RegistryObject<SoundEvent> linkingFollowing;
  private RegistryObject<SoundEvent> linkingIntra;
  private RegistryObject<SoundEvent> linkingFissure;
  private RegistryObject<SoundEvent> linkingPortal;
  private RegistryObject<SoundEvent> meteorRoar;
  private RegistryObject<SoundEvent> meteorImpact;

  private RegistryObject<MenuType<InkMixerMenu>> inkMixerMenu;
  private RegistryObject<MenuType<BookBinderMenu>> bookBinderMenu;
  private RegistryObject<MenuType<LinkModifierMenu>> linkModifierMenu;
  private RegistryObject<MenuType<WritingDeskMenu>> writingDeskMenu;
  private RegistryObject<MenuType<FolderMenu>> folderMenu;
  private RegistryObject<MenuType<PortfolioMenu>> portfolioMenu;

  private RegistryObject<PoiType> archivistPoi;
  private RegistryObject<VillagerProfession> archivistProfession;
  private RegistryObject<StructureType<AbandonedLibraryStructure>> abandonedLibrary;
  private RegistryObject<StructureType<UndergroundArchiveStructure>> undergroundArchive;
  private RegistryObject<StructureType<ScatteredLibraryStructure>> scatteredLibrary;

  @Override
  public void initialize(Object modEventBus) {
    if (initialized) {
      return;
    }
    if (!(modEventBus instanceof BusGroup busGroup)) {
      throw new IllegalArgumentException("Forge registration requires a BusGroup");
    }
    modBusGroup = busGroup;
    declareContent();
    initialized = true;
  }

  @Override
  public void register() {
    if (!initialized) {
      throw new IllegalStateException("Forge registration was not initialized");
    }
    blocks.register(modBusGroup);
    items.register(modBusGroup);
    blockEntities.register(modBusGroup);
    entities.register(modBusGroup);
    fluids.register(modBusGroup);
    fluidTypes.register(modBusGroup);
    sounds.register(modBusGroup);
    menus.register(modBusGroup);
    creativeTabs.register(modBusGroup);
    advancementTriggers.register(modBusGroup);
    poiTypes.register(modBusGroup);
    professions.register(modBusGroup);
    chunkGenerators.register(modBusGroup);
    biomeSources.register(modBusGroup);
    structureTypes.register(modBusGroup);
    lootFunctions.register(modBusGroup);
    lootModifiers.register(modBusGroup);
  }

  @Override
  public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
    return blocks.register(name, block);
  }

  @Override
  public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
    return items.register(name, item);
  }

  @Override
  public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String name,
                                                                        Supplier<T> type) {
    return blockEntities.register(name, type);
  }

  @Override
  public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> type) {
    return entities.register(name, type);
  }

  @Override
  public <T extends Fluid> Supplier<T> registerFluid(String name, Supplier<T> fluid) {
    return fluids.register(name, fluid);
  }

  @Override
  public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> sound) {
    return sounds.register(name, sound);
  }

  @Override
  public Supplier<CreativeModeTab> registerCreativeTab(String name,
                                                       Supplier<CreativeModeTab> tab) {
    return creativeTabs.register(name, tab);
  }

  @Override
  public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
    return menus.register(name, menuType);
  }

  @Override
  public void populateCommonRegistries() {
    ModBlocks.INK_MIXER = inkMixer;
    ModBlocks.BOOK_BINDER = bookBinder;
    ModBlocks.BOOK_RECEPTACLE = bookReceptacle;
    ModBlocks.LINK_MODIFIER = linkModifier;
    ModBlocks.WRITING_DESK = writingDesk;
    ModBlocks.CRYSTAL = crystal;
    ModBlocks.DECAY = decay;
    ModBlocks.LINK_PORTAL = linkPortal;
    ModBlocks.STAR_FISSURE = starFissure;
    ModBlocks.FLUID_INK = fluidInk;

    ModItems.PAGE = page;
    ModItems.AGEBOOK = agebook;
    ModItems.LINKBOOK = linkbook;
    ModItems.LINKBOOK_UNLINKED = linkbookUnlinked;
    ModItems.PERSONAL_LINK_BOOK = personalLinkBook;
    ModItems.BOOSTER_PACK = boosterPack;
    ModItems.FOLDER = folder;
    ModItems.PORTFOLIO = portfolio;
    ModItems.INK_VIAL = inkVial;
    ModItems.GUIDEBOOK = guidebook;
    ModItems.INK_BUCKET = inkBucket;
    ModItems.INK_MIXER_ITEM = inkMixerItem;
    ModItems.BOOK_BINDER_ITEM = bookBinderItem;
    ModItems.BOOK_RECEPTACLE_ITEM = bookReceptacleItem;
    ModItems.LINK_MODIFIER_ITEM = linkModifierItem;
    ModItems.WRITING_DESK_ITEM = writingDeskItem;
    ModItems.CRYSTAL_ITEM = crystalItem;
    ModItems.DECAY_ITEM = decayItem;

    ModBlockEntities.INK_MIXER = inkMixerBlockEntity;
    ModBlockEntities.BOOK_BINDER = bookBinderBlockEntity;
    ModBlockEntities.BOOK_RECEPTACLE = bookReceptacleBlockEntity;
    ModBlockEntities.WRITING_DESK = writingDeskBlockEntity;
    ModBlockEntities.STAR_FISSURE = starFissureBlockEntity;
    ModBlockEntities.LINK_MODIFIER = linkModifierBlockEntity;
    ModBlockEntities.LINK_PORTAL = linkPortalBlockEntity;

    ModEntities.LINKBOOK = linkbookEntity;
    ModEntities.PERSONAL_POCKET_PROXY = personalPocketProxyEntity;
    ModEntities.FALLING_BLOCK = fallingBlockEntity;
    ModEntities.METEOR = meteorEntity;
    ModEntities.COLORED_LIGHTNING = coloredLightningEntity;

    ModFluids.BLACK_INK_SOURCE = blackInkSource;
    ModFluids.BLACK_INK_FLOWING = blackInkFlowing;
    ModFluids.BLACK_INK_BUCKET = inkBucket;
    ForgePlatformHelper.installBlackInkFluids(blackInkSource, blackInkFlowing);

    ModSounds.LINKING_POP = linkingPop;
    ModSounds.LINKING_LINK = linkingLink;
    ModSounds.LINKING_DISARM = linkingDisarm;
    ModSounds.LINKING_FOLLOWING = linkingFollowing;
    ModSounds.LINKING_INTRA = linkingIntra;
    ModSounds.LINKING_FISSURE = linkingFissure;
    ModSounds.LINKING_PORTAL = linkingPortal;
    ModSounds.METEOR_ROAR = meteorRoar;
    ModSounds.METEOR_IMPACT = meteorImpact;

    ModMenuTypes.INK_MIXER = inkMixerMenu;
    ModMenuTypes.BOOK_BINDER = bookBinderMenu;
    ModMenuTypes.LINK_MODIFIER = linkModifierMenu;
    ModMenuTypes.WRITING_DESK = writingDeskMenu;
    ModMenuTypes.FOLDER = folderMenu;
    ModMenuTypes.PORTFOLIO = portfolioMenu;

    ModStructures.ABANDONED_LIBRARY = abandonedLibrary;
    ModStructures.UNDERGROUND_ARCHIVE = undergroundArchive;
    ModStructures.SCATTERED_LIBRARY = scatteredLibrary;
  }

  private void declareContent() {
    blackInkType = fluidTypes.register("black_ink", BlackInkFluidType::new);
    blackInkSource = fluids.register("black_ink",
        () -> new ForgeFlowingFluid.Source(blackInkProperties()));
    blackInkFlowing = fluids.register("black_ink_flowing",
        () -> new ForgeFlowingFluid.Flowing(blackInkProperties()));

    inkMixer = blocks.register("blockinkmixer", () -> new InkMixerBlock(BlockDefinitions.INK_MIXER));
    bookBinder = blocks.register("blockbookbinder", () -> new BookBinderBlock(BlockDefinitions.BOOK_BINDER));
    bookReceptacle = blocks.register("blockbookreceptacle",
        () -> new BookReceptacleBlock(BlockDefinitions.BOOK_RECEPTACLE));
    linkModifier = blocks.register("blocklinkmodifier",
        () -> new LinkModifierBlock(BlockDefinitions.LINK_MODIFIER));
    writingDesk = blocks.register("writingdesk", () -> new WritingDeskBlock(BlockDefinitions.WRITING_DESK));
    crystal = blocks.register("blockcrystal", () -> new CrystalBlock(BlockDefinitions.CRYSTAL));
    decay = blocks.register("blockdecay", () -> new DecayBlock(BlockDefinitions.DECAY));
    linkPortal = blocks.register("linkportal", () -> new LinkPortalBlock(BlockDefinitions.LINK_PORTAL));
    starFissure = blocks.register("blockstarfissure",
        () -> new StarFissureBlock(BlockDefinitions.STAR_FISSURE));
    fluidInk = blocks.register("fluidblockblackink",
        () -> new LiquidBlock(blackInkSource, fluidBlockProperties()));

    page = items.register("page", () -> new PageItem(ItemDefinitions.page()));
    agebook = items.register("agebook", () -> new AgebookItem(ItemDefinitions.agebook()));
    linkbook = items.register("linkbook", () -> new LinkbookItem(ItemDefinitions.linkbook()));
    linkbookUnlinked = items.register("linkbook_unlinked",
        () -> new LinkbookUnlinkedItem(ItemDefinitions.linkbookUnlinked()));
    personalLinkBook = items.register("personal_link_book",
        () -> new PersonalLinkBookItem(ItemDefinitions.personalLinkBook()));
    boosterPack = items.register("booster", () -> new BoosterPackItem(ItemDefinitions.boosterPack()));
    folder = items.register("folder", () -> new FolderItem(ItemDefinitions.folder()));
    portfolio = items.register("portfolio", () -> new PortfolioItem(ItemDefinitions.portfolio()));
    inkVial = items.register("inkvial", () -> new InkVialItem(ItemDefinitions.inkVial()));
    guidebook = items.register("guidebook", () -> new GuidebookItem(ItemDefinitions.guidebook()));
    inkBucket = items.register("ink_bucket", () -> new BucketItem(blackInkSource, ItemDefinitions.inkBucket()));

    inkMixerItem = blockItem("blockinkmixer", inkMixer);
    bookBinderItem = blockItem("blockbookbinder", bookBinder);
    bookReceptacleItem = blockItem("blockbookreceptacle", bookReceptacle);
    linkModifierItem = blockItem("blocklinkmodifier", linkModifier);
    writingDeskItem = blockItem("writingdesk", writingDesk);
    crystalItem = blockItem("blockcrystal", crystal);
    decayItem = blockItem("blockdecay", decay);

    inkMixerBlockEntity = blockEntities.register("ink_mixer",
        () -> blockEntityType(InkMixerBlockEntity::new, inkMixer));
    bookBinderBlockEntity = blockEntities.register("book_binder",
        () -> blockEntityType(BookBinderBlockEntity::new, bookBinder));
    bookReceptacleBlockEntity = blockEntities.register("book_receptacle",
        () -> blockEntityType(BookReceptacleBlockEntity::new, bookReceptacle));
    writingDeskBlockEntity = blockEntities.register("writing_desk",
        () -> blockEntityType(WritingDeskBlockEntity::new, writingDesk));
    starFissureBlockEntity = blockEntities.register("star_fissure",
        () -> blockEntityType(StarFissureBlockEntity::new, starFissure));
    linkModifierBlockEntity = blockEntities.register("link_modifier",
        () -> blockEntityType(LinkModifierBlockEntity::new, linkModifier));
    linkPortalBlockEntity = blockEntities.register("link_portal",
        () -> blockEntityType(LinkPortalBlockEntity::new, linkPortal));

    linkbookEntity = entities.register("linkbook", () -> EntityType.Builder
        .<LinkbookEntity>of(LinkbookEntity::new, MobCategory.MISC)
        .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(20)
        .build(entityKey("linkbook")));
    personalPocketProxyEntity = entities.register("personal_pocket_proxy", () -> EntityType.Builder
        .<PersonalPocketProxyEntity>of(PersonalPocketProxyEntity::new, MobCategory.MISC)
        .noSave().sized(0.6F, 1.8F).clientTrackingRange(10).updateInterval(3)
        .build(entityKey("personal_pocket_proxy")));
    fallingBlockEntity = entities.register("falling_block", () -> EntityType.Builder
        .<MystcraftFallingBlockEntity>of(MystcraftFallingBlockEntity::new, MobCategory.MISC)
        .sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(20)
        .build(entityKey("falling_block")));
    meteorEntity = entities.register("meteor", () -> EntityType.Builder
        .<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
        .sized(2.0F, 2.0F).clientTrackingRange(16).updateInterval(10).fireImmune()
        .build(entityKey("meteor")));
    coloredLightningEntity = entities.register("colored_lightning", () -> EntityType.Builder
        .<ColoredLightningEntity>of(ColoredLightningEntity::new, MobCategory.MISC)
        .sized(0.0F, 0.0F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE).noSave()
        .build(entityKey("colored_lightning")));

    linkingPop = sound("linking.pop");
    linkingLink = sound("linking.link");
    linkingDisarm = sound("linking.link-disarm");
    linkingFollowing = sound("linking.link-following");
    linkingIntra = sound("linking.link-intra");
    linkingFissure = sound("linking.link-fissure");
    linkingPortal = sound("linking.link-portal");
    meteorRoar = sound("entity.meteor.roar");
    meteorImpact = sound("entity.meteor.impact");

    inkMixerMenu = menus.register("ink_mixer", () -> IForgeMenuType.create(InkMixerMenu::new));
    bookBinderMenu = menus.register("book_binder", () -> IForgeMenuType.create(BookBinderMenu::new));
    linkModifierMenu = menus.register("link_modifier", () -> IForgeMenuType.create(LinkModifierMenu::new));
    writingDeskMenu = menus.register("writing_desk", () -> IForgeMenuType.create(WritingDeskMenu::new));
    folderMenu = menus.register("folder", () -> IForgeMenuType.create(FolderMenu::new));
    portfolioMenu = menus.register("portfolio", () -> IForgeMenuType.create(PortfolioMenu::new));

    creativeTabs.register("mystcraft", this::createMainCreativeTab);
    creativeTabs.register("mystcraft_pages", this::createPagesCreativeTab);

    // Advancements deserialize during resource loading, after vanilla's
    // trigger registry has frozen. Forge therefore requires these shared
    // trigger instances to participate in its deferred registry lifecycle.
    advancementTriggers.register("enter_myst_dimension_safe",
        ModAdvancements::enterMystDimensionSafe);
    advancementTriggers.register("enter_myst_dimension_quinn",
        ModAdvancements::enterMystDimensionQuinn);
    advancementTriggers.register("writing_desk_write",
        ModAdvancements::writingDeskWrite);

    archivistPoi = poiTypes.register("archivist",
        () -> new PoiType(Set.copyOf(bookBinder.get().getStateDefinition().getPossibleStates()), 1, 1));
    archivistProfession = professions.register("archivist", this::createArchivistProfession);

    chunkGenerators.register("age_chunk_generator", () -> AgeChunkGenerator.CODEC);
    biomeSources.register("age_biome_source", () -> AgeBiomeSource.CODEC);
    abandonedLibrary = structureTypes.register("abandoned_library",
        () -> () -> MapCodec.assumeMapUnsafe(AbandonedLibraryStructure.CODEC));
    undergroundArchive = structureTypes.register("underground_archive",
        () -> () -> MapCodec.assumeMapUnsafe(UndergroundArchiveStructure.CODEC));
    scatteredLibrary = structureTypes.register("scattered_library",
        () -> () -> MapCodec.assumeMapUnsafe(ScatteredLibraryStructure.CODEC));

    ModLootFunctions.register((id, codecSupplier) ->
        lootFunctions.register(id.getPath(), codecSupplier));
    lootModifiers.register("guidebook", () -> ForgeLootModifiers.Guidebook.MAP_CODEC);
    lootModifiers.register("booster_pack", () -> ForgeLootModifiers.BoosterPack.MAP_CODEC);
    lootModifiers.register("symbol_page", () -> ForgeLootModifiers.SymbolPage.MAP_CODEC);
  }

  private ForgeFlowingFluid.Properties blackInkProperties() {
    return new ForgeFlowingFluid.Properties(blackInkType, blackInkSource, blackInkFlowing)
        .slopeFindDistance(2)
        .levelDecreasePerBlock(2)
        .block(fluidInk)
        .bucket(inkBucket);
  }

  private BlockBehaviour.Properties fluidBlockProperties() {
    return BlockBehaviour.Properties.of()
        .setId(key(Registries.BLOCK, "fluidblockblackink"))
        .mapColor(MapColor.COLOR_BLACK)
        .replaceable()
        .noCollision()
        .strength(100.0F)
        .noLootTable()
        .liquid();
  }

  private RegistryObject<Item> blockItem(String path, Supplier<? extends Block> block) {
    return items.register(path, () -> new BlockItem(block.get(), ItemDefinitions.blockItem(path)));
  }

  private RegistryObject<SoundEvent> sound(String path) {
    Identifier id = id(path);
    return sounds.register(path, () -> SoundEvent.createVariableRangeEvent(id));
  }

  private CreativeModeTab createMainCreativeTab() {
    return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("itemGroup.mystcraft"))
        .icon(() -> new ItemStack(ModItems.AGEBOOK.get()))
        .displayItems((parameters, output) -> {
          output.accept(ModItems.GUIDEBOOK.get());
          output.accept(ModItems.AGEBOOK.get());
          output.accept(ModItems.LINKBOOK.get());
          output.accept(ModItems.LINKBOOK_UNLINKED.get());
          if (MystcraftConfig.enablePersonalLinkBooks.get()) {
            output.accept(ModItems.PERSONAL_LINK_BOOK.get());
          }
          output.accept(ModItems.PAGE.get());
          output.accept(ModItems.FOLDER.get());
          output.accept(ModItems.PORTFOLIO.get());
          output.accept(ModItems.BOOSTER_PACK.get());
          output.accept(ModItems.INK_VIAL.get());
          output.accept(ModItems.INK_BUCKET.get());
          output.accept(ModItems.WRITING_DESK_ITEM.get());
          output.accept(ModItems.INK_MIXER_ITEM.get());
          output.accept(ModItems.BOOK_BINDER_ITEM.get());
          output.accept(ModItems.LINK_MODIFIER_ITEM.get());
          output.accept(ModItems.BOOK_RECEPTACLE_ITEM.get());
          output.accept(ModItems.CRYSTAL_ITEM.get());
          output.accept(ModItems.DECAY_ITEM.get());
        })
        .build();
  }

  private CreativeModeTab createPagesCreativeTab() {
    return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
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
        .build();
  }

  private VillagerProfession createArchivistProfession() {
    return new VillagerProfession(
        Component.translatable("entity.mystcraft.villager.archivist"),
        holder -> holder.is(ARCHIVIST_POI_KEY),
        holder -> holder.is(ARCHIVIST_POI_KEY),
        ImmutableSet.of(),
        ImmutableSet.of(),
        SoundEvents.VILLAGER_WORK_LIBRARIAN,
        ArchivistTrades.TRADE_SETS_BY_LEVEL);
  }

  private static <T extends net.minecraft.world.level.block.entity.BlockEntity>
  BlockEntityType<T> blockEntityType(BlockEntityType.BlockEntitySupplier<T> factory,
                                     Supplier<? extends Block> block) {
    return new BlockEntityType<>(factory, Set.of(block.get()));
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }

  private static ResourceKey<EntityType<?>> entityKey(String path) {
    return key(Registries.ENTITY_TYPE, path);
  }

  private static <T> ResourceKey<T> key(ResourceKey<? extends net.minecraft.core.Registry<T>> registry,
                                        String path) {
    return ResourceKey.create(registry, id(path));
  }
}
