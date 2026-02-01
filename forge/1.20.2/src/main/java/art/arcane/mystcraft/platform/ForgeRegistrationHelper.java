package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.block.*;
import art.arcane.mystcraft.blockentity.*;
import art.arcane.mystcraft.config.ForgeMystcraftConfig;
import art.arcane.mystcraft.entity.ColoredLightningEntity;
import art.arcane.mystcraft.entity.LinkbookEntity;
import art.arcane.mystcraft.entity.MeteorEntity;
import art.arcane.mystcraft.entity.MystcraftFallingBlockEntity;
import art.arcane.mystcraft.fluid.BlackInkFluid;
import art.arcane.mystcraft.fluid.BlackInkFluidType;
import art.arcane.mystcraft.item.*;
import art.arcane.mystcraft.menu.*;
import art.arcane.mystcraft.network.ForgeMystcraftNetwork;
import art.arcane.mystcraft.platform.services.IRegistrationHelper;
import art.arcane.mystcraft.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

/**
 * Forge implementation of IRegistrationHelper.
 * Handles all block, item, entity, and other registrations using MystcraftRegistries.
 * This class consolidates registration logic that was previously spread across
 * ForgeModBlocks, ForgeModItems, ForgeModBlockEntities, etc.
 */
public class ForgeRegistrationHelper implements IRegistrationHelper {

  private IEventBus modEventBus;
  private boolean initialized = false;

  // Blocks
  private RegistryObject<Block> inkMixer;
  private RegistryObject<Block> bookBinder;
  private RegistryObject<Block> bookReceptacle;
  private RegistryObject<Block> bookstand;
  private RegistryObject<Block> linkModifier;
  private RegistryObject<Block> writingDesk;
  private RegistryObject<Block> crystal;
  private RegistryObject<Block> decay;
  private RegistryObject<Block> linkPortal;
  private RegistryObject<Block> starFissure;
  private RegistryObject<LiquidBlock> fluidInk;

  // Items
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
  private RegistryObject<Item> bookstandItem;
  private RegistryObject<Item> linkModifierItem;
  private RegistryObject<Item> writingDeskItem;
  private RegistryObject<Item> crystalItem;
  private RegistryObject<Item> decayItem;

  // Block Entities
  private RegistryObject<BlockEntityType<InkMixerBlockEntity>> inkMixerBE;
  private RegistryObject<BlockEntityType<BookBinderBlockEntity>> bookBinderBE;
  private RegistryObject<BlockEntityType<BookReceptacleBlockEntity>> bookReceptacleBE;
  private RegistryObject<BlockEntityType<BookstandBlockEntity>> bookstandBE;
  private RegistryObject<BlockEntityType<WritingDeskBlockEntity>> writingDeskBE;
  private RegistryObject<BlockEntityType<StarFissureBlockEntity>> starFissureBE;
  private RegistryObject<BlockEntityType<LinkModifierBlockEntity>> linkModifierBE;

  // Entities
  private RegistryObject<EntityType<LinkbookEntity>> linkbookEntity;
  private RegistryObject<EntityType<MystcraftFallingBlockEntity>> fallingBlockEntity;
  private RegistryObject<EntityType<MeteorEntity>> meteorEntity;
  private RegistryObject<EntityType<ColoredLightningEntity>> coloredLightningEntity;

  // Fluids
  private RegistryObject<FluidType> blackInkType;
  private RegistryObject<FlowingFluid> blackInkSource;
  private RegistryObject<FlowingFluid> blackInkFlowing;

  // Sounds
  private RegistryObject<SoundEvent> linkingPop;
  private RegistryObject<SoundEvent> linkingLink;
  private RegistryObject<SoundEvent> linkingDisarm;
  private RegistryObject<SoundEvent> linkingFollowing;
  private RegistryObject<SoundEvent> linkingIntra;
  private RegistryObject<SoundEvent> linkingFissure;
  private RegistryObject<SoundEvent> linkingPortal;
  private RegistryObject<SoundEvent> meteorRoar;
  private RegistryObject<SoundEvent> meteorImpact;

  // Menus
  private RegistryObject<MenuType<InkMixerMenu>> inkMixerMenu;
  private RegistryObject<MenuType<BookBinderMenu>> bookBinderMenu;
  private RegistryObject<MenuType<LinkModifierMenu>> linkModifierMenu;
  private RegistryObject<MenuType<WritingDeskMenu>> writingDeskMenu;
  private RegistryObject<MenuType<FolderMenu>> folderMenu;
  private RegistryObject<MenuType<PortfolioMenu>> portfolioMenu;

  @Override
  public void initialize(Object modEventBus) {
    if (initialized) {
      return;
    }
    this.modEventBus = (IEventBus) modEventBus;
    registerAllContent();
    initialized = true;
  }

  private void registerAllContent() {
    // Fluids first (blocks depend on them)
    registerFluids();

    // Blocks
    registerBlocks();

    // Items (after blocks for block items)
    registerItems();

    // Block Entities (after blocks)
    registerBlockEntities();

    // Entities
    registerEntities();

    // Sounds
    registerSounds();

    // Menus
    registerMenus();
  }

  private void registerFluids() {
    blackInkType = MystcraftRegistries.FLUID_TYPES.register("black_ink", BlackInkFluidType::new);

    // Set the fluid type supplier for BlackInkFluid instances
    BlackInkFluid.setFluidTypeSupplier(blackInkType);

    blackInkSource = MystcraftRegistries.FLUIDS.register("black_ink",
        () -> new BlackInkFluid.Source(BlackInkFluid.createProperties(blackInkType, blackInkSource, blackInkFlowing)));
    blackInkFlowing = MystcraftRegistries.FLUIDS.register("black_ink_flowing",
        () -> new BlackInkFluid.Flowing(BlackInkFluid.createProperties(blackInkType, blackInkSource, blackInkFlowing)));
  }

  private void registerBlocks() {
    inkMixer = MystcraftRegistries.BLOCKS.register("blockinkmixer",
        () -> new InkMixerBlock(BlockDefinitions.INK_MIXER));
    bookBinder = MystcraftRegistries.BLOCKS.register("blockbookbinder",
        () -> new BookBinderBlock(BlockDefinitions.BOOK_BINDER));
    bookReceptacle = MystcraftRegistries.BLOCKS.register("blockbookreceptacle",
        () -> new BookReceptacleBlock(BlockDefinitions.BOOK_RECEPTACLE));
    bookstand = MystcraftRegistries.BLOCKS.register("blockbookstand",
        () -> new BookstandBlock(BlockDefinitions.BOOKSTAND));
    linkModifier = MystcraftRegistries.BLOCKS.register("blocklinkmodifier",
        () -> new LinkModifierBlock(BlockDefinitions.LINK_MODIFIER));
    writingDesk = MystcraftRegistries.BLOCKS.register("writingdesk",
        () -> new WritingDeskBlock(BlockDefinitions.WRITING_DESK));
    crystal = MystcraftRegistries.BLOCKS.register("blockcrystal",
        () -> new CrystalBlock(BlockDefinitions.CRYSTAL));
    decay = MystcraftRegistries.BLOCKS.register("blockdecay",
        () -> new DecayBlock(BlockDefinitions.DECAY));
    linkPortal = MystcraftRegistries.BLOCKS.register("linkportal",
        () -> new LinkPortalBlock(BlockDefinitions.LINK_PORTAL));
    starFissure = MystcraftRegistries.BLOCKS.register("blockstarfissure",
        () -> new StarFissureBlock(BlockDefinitions.STAR_FISSURE));
    fluidInk = MystcraftRegistries.BLOCKS.register("fluidblockblackink",
        () -> new LiquidBlock(blackInkSource, BlockBehaviour.Properties.copy(Blocks.WATER)
            .mapColor(MapColor.COLOR_BLACK)
            .noLootTable()));
  }

  private void registerItems() {
    // Standalone items
    page = MystcraftRegistries.ITEMS.register("page",
        () -> new ForgePageItem(ItemDefinitions.page()));
    agebook = MystcraftRegistries.ITEMS.register("agebook",
        () -> new AgebookItem(ItemDefinitions.agebook()));
    linkbook = MystcraftRegistries.ITEMS.register("linkbook",
        () -> new LinkbookItem(ItemDefinitions.linkbook()));
    linkbookUnlinked = MystcraftRegistries.ITEMS.register("linkbook_unlinked",
        () -> new LinkbookUnlinkedItem(ItemDefinitions.linkbookUnlinked()));
    personalLinkBook = MystcraftRegistries.ITEMS.register("personal_link_book",
        () -> new PersonalLinkBookItem(ItemDefinitions.personalLinkBook()));
    boosterPack = MystcraftRegistries.ITEMS.register("booster",
        () -> new BoosterPackItem(ItemDefinitions.boosterPack()));
    folder = MystcraftRegistries.ITEMS.register("folder",
        () -> new FolderItem(ItemDefinitions.folder()));
    portfolio = MystcraftRegistries.ITEMS.register("portfolio",
        () -> new PortfolioItem(ItemDefinitions.portfolio()));
    inkVial = MystcraftRegistries.ITEMS.register("inkvial",
        () -> new InkVialItem(ItemDefinitions.inkVial()));
    guidebook = MystcraftRegistries.ITEMS.register("guidebook",
        () -> new GuidebookItem(ItemDefinitions.guidebook()));
    inkBucket = MystcraftRegistries.ITEMS.register("ink_bucket",
        () -> new BucketItem(blackInkSource, ItemDefinitions.inkBucket()));

    // Block items
    inkMixerItem = MystcraftRegistries.ITEMS.register("blockinkmixer",
        () -> new BlockItem(inkMixer.get(), ItemDefinitions.blockItem()));
    bookBinderItem = MystcraftRegistries.ITEMS.register("blockbookbinder",
        () -> new BlockItem(bookBinder.get(), ItemDefinitions.blockItem()));
    bookReceptacleItem = MystcraftRegistries.ITEMS.register("blockbookreceptacle",
        () -> new BlockItem(bookReceptacle.get(), ItemDefinitions.blockItem()));
    bookstandItem = MystcraftRegistries.ITEMS.register("blockbookstand",
        () -> new BlockItem(bookstand.get(), ItemDefinitions.blockItem()));
    linkModifierItem = MystcraftRegistries.ITEMS.register("blocklinkmodifier",
        () -> new BlockItem(linkModifier.get(), ItemDefinitions.blockItem()));
    writingDeskItem = MystcraftRegistries.ITEMS.register("writingdesk",
        () -> new BlockItem(writingDesk.get(), ItemDefinitions.blockItem()));
    crystalItem = MystcraftRegistries.ITEMS.register("blockcrystal",
        () -> new BlockItem(crystal.get(), ItemDefinitions.blockItem()));
    decayItem = MystcraftRegistries.ITEMS.register("blockdecay",
        () -> new BlockItem(decay.get(), ItemDefinitions.blockItem()));
  }

  private void registerBlockEntities() {
    inkMixerBE = MystcraftRegistries.BLOCK_ENTITIES.register("ink_mixer",
        () -> BlockEntityType.Builder.of(InkMixerBlockEntity::new, inkMixer.get()).build(null));
    bookBinderBE = MystcraftRegistries.BLOCK_ENTITIES.register("book_binder",
        () -> BlockEntityType.Builder.of(BookBinderBlockEntity::new, bookBinder.get()).build(null));
    bookReceptacleBE = MystcraftRegistries.BLOCK_ENTITIES.register("book_receptacle",
        () -> BlockEntityType.Builder.of(BookReceptacleBlockEntity::new, bookReceptacle.get()).build(null));
    bookstandBE = MystcraftRegistries.BLOCK_ENTITIES.register("bookstand",
        () -> BlockEntityType.Builder.of(BookstandBlockEntity::new, bookstand.get()).build(null));
    writingDeskBE = MystcraftRegistries.BLOCK_ENTITIES.register("writing_desk",
        () -> BlockEntityType.Builder.of(WritingDeskBlockEntity::new, writingDesk.get()).build(null));
    starFissureBE = MystcraftRegistries.BLOCK_ENTITIES.register("star_fissure",
        () -> BlockEntityType.Builder.of(StarFissureBlockEntity::new, starFissure.get()).build(null));
    linkModifierBE = MystcraftRegistries.BLOCK_ENTITIES.register("link_modifier",
        () -> BlockEntityType.Builder.of(LinkModifierBlockEntity::new, linkModifier.get()).build(null));
  }

  private void registerEntities() {
    linkbookEntity = MystcraftRegistries.ENTITIES.register("linkbook",
        () -> EntityType.Builder.<LinkbookEntity>of(LinkbookEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(20)
            .build(new ResourceLocation(Mystcraft.MOD_ID, "linkbook").toString()));
    fallingBlockEntity = MystcraftRegistries.ENTITIES.register("falling_block",
        () -> EntityType.Builder.<MystcraftFallingBlockEntity>of(MystcraftFallingBlockEntity::new, MobCategory.MISC)
            .sized(0.98F, 0.98F)
            .clientTrackingRange(10)
            .updateInterval(20)
            .build(new ResourceLocation(Mystcraft.MOD_ID, "falling_block").toString()));
    meteorEntity = MystcraftRegistries.ENTITIES.register("meteor",
        () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
            .sized(2.0F, 2.0F)
            .clientTrackingRange(16)
            .updateInterval(10)
            .fireImmune()
            .build(new ResourceLocation(Mystcraft.MOD_ID, "meteor").toString()));
    coloredLightningEntity = MystcraftRegistries.ENTITIES.register("colored_lightning",
        () -> EntityType.Builder.<ColoredLightningEntity>of(ColoredLightningEntity::new, MobCategory.MISC)
            .sized(0.0F, 0.0F)
            .clientTrackingRange(16)
            .updateInterval(Integer.MAX_VALUE)
            .noSave()
            .build(new ResourceLocation(Mystcraft.MOD_ID, "colored_lightning").toString()));
  }

  private void registerSounds() {
    linkingPop = registerSoundEvent("linking.pop");
    linkingLink = registerSoundEvent("linking.link");
    linkingDisarm = registerSoundEvent("linking.link-disarm");
    linkingFollowing = registerSoundEvent("linking.link-following");
    linkingIntra = registerSoundEvent("linking.link-intra");
    linkingFissure = registerSoundEvent("linking.link-fissure");
    linkingPortal = registerSoundEvent("linking.link-portal");
    meteorRoar = registerSoundEvent("entity.meteor.roar");
    meteorImpact = registerSoundEvent("entity.meteor.impact");
  }

  private RegistryObject<SoundEvent> registerSoundEvent(String name) {
    ResourceLocation id = new ResourceLocation(Mystcraft.MOD_ID, name);
    return MystcraftRegistries.SOUNDS.register(name.replace('.', '_').replace('-', '_'),
        () -> SoundEvent.createVariableRangeEvent(id));
  }

  private void registerMenus() {
    inkMixerMenu = MystcraftRegistries.MENUS.register("ink_mixer",
        () -> IForgeMenuType.create(InkMixerMenu::new));
    bookBinderMenu = MystcraftRegistries.MENUS.register("book_binder",
        () -> IForgeMenuType.create(BookBinderMenu::new));
    linkModifierMenu = MystcraftRegistries.MENUS.register("link_modifier",
        () -> IForgeMenuType.create(LinkModifierMenu::new));
    writingDeskMenu = MystcraftRegistries.MENUS.register("writing_desk",
        () -> IForgeMenuType.create(WritingDeskMenu::new));
    folderMenu = MystcraftRegistries.MENUS.register("folder",
        () -> IForgeMenuType.create(FolderMenu::new));
    portfolioMenu = MystcraftRegistries.MENUS.register("portfolio",
        () -> IForgeMenuType.create(PortfolioMenu::new));
  }

  @Override
  public void register() {
    MystcraftRegistries.register(modEventBus);
  }

  @Override
  public void populateCommonRegistries() {
    // Blocks
    ModBlocks.INK_MIXER = inkMixer;
    ModBlocks.BOOK_BINDER = bookBinder;
    ModBlocks.BOOK_RECEPTACLE = bookReceptacle;
    ModBlocks.BOOKSTAND = bookstand;
    ModBlocks.LINK_MODIFIER = linkModifier;
    ModBlocks.WRITING_DESK = writingDesk;
    ModBlocks.CRYSTAL = crystal;
    ModBlocks.DECAY = decay;
    ModBlocks.LINK_PORTAL = linkPortal;
    ModBlocks.STAR_FISSURE = starFissure;
    ModBlocks.FLUID_INK = fluidInk;

    // Items
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
    ModItems.BOOKSTAND_ITEM = bookstandItem;
    ModItems.LINK_MODIFIER_ITEM = linkModifierItem;
    ModItems.WRITING_DESK_ITEM = writingDeskItem;
    ModItems.CRYSTAL_ITEM = crystalItem;
    ModItems.DECAY_ITEM = decayItem;

    // Block Entities
    ModBlockEntities.INK_MIXER = inkMixerBE;
    ModBlockEntities.BOOK_BINDER = bookBinderBE;
    ModBlockEntities.BOOK_RECEPTACLE = bookReceptacleBE;
    ModBlockEntities.BOOKSTAND = bookstandBE;
    ModBlockEntities.WRITING_DESK = writingDeskBE;
    ModBlockEntities.STAR_FISSURE = starFissureBE;
    ModBlockEntities.LINK_MODIFIER = linkModifierBE;

    // Entities
    ModEntities.LINKBOOK = linkbookEntity;
    ModEntities.FALLING_BLOCK = fallingBlockEntity;
    ModEntities.METEOR = meteorEntity;
    ModEntities.COLORED_LIGHTNING = coloredLightningEntity;

    // Fluids
    ModFluids.BLACK_INK_SOURCE = blackInkSource;
    ModFluids.BLACK_INK_FLOWING = blackInkFlowing;
    ModFluids.BLACK_INK_BUCKET = inkBucket;

    // Sounds
    ModSounds.LINKING_POP = linkingPop;
    ModSounds.LINKING_LINK = linkingLink;
    ModSounds.LINKING_DISARM = linkingDisarm;
    ModSounds.LINKING_FOLLOWING = linkingFollowing;
    ModSounds.LINKING_INTRA = linkingIntra;
    ModSounds.LINKING_FISSURE = linkingFissure;
    ModSounds.LINKING_PORTAL = linkingPortal;
    ModSounds.METEOR_ROAR = meteorRoar;
    ModSounds.METEOR_IMPACT = meteorImpact;

    // Menu Types
    ModMenuTypes.INK_MIXER = inkMixerMenu;
    ModMenuTypes.BOOK_BINDER = bookBinderMenu;
    ModMenuTypes.LINK_MODIFIER = linkModifierMenu;
    ModMenuTypes.WRITING_DESK = writingDeskMenu;
    ModMenuTypes.FOLDER = folderMenu;
    ModMenuTypes.PORTFOLIO = portfolioMenu;

    // Config - wire Forge config values to common suppliers
    populateConfigSuppliers();

    // Network
    populateNetworkHandlers();
  }

  private void populateConfigSuppliers() {
    art.arcane.mystcraft.config.MystcraftConfig.giveGuidebookOnFirstSpawn = ForgeMystcraftConfig.giveGuidebookOnFirstSpawn::get;
    art.arcane.mystcraft.config.MystcraftConfig.maxSymbolsPerBook = ForgeMystcraftConfig.maxSymbolsPerBook::get;
    art.arcane.mystcraft.config.MystcraftConfig.deleteAgesOnStartup = ForgeMystcraftConfig.deleteAgesOnStartup::get;
    art.arcane.mystcraft.config.MystcraftConfig.enablePersonalLinkBooks = ForgeMystcraftConfig.enablePersonalLinkBooks::get;
    art.arcane.mystcraft.config.MystcraftConfig.linkPanelInBoosterPacks = ForgeMystcraftConfig.linkPanelInBoosterPacks::get;
    art.arcane.mystcraft.config.MystcraftConfig.enableBoosterLoot = ForgeMystcraftConfig.enableBoosterLoot::get;
    art.arcane.mystcraft.config.MystcraftConfig.enablePageLoot = ForgeMystcraftConfig.enablePageLoot::get;
    art.arcane.mystcraft.config.MystcraftConfig.bookBinderCoverItems = () -> List.copyOf(ForgeMystcraftConfig.bookBinderCoverItems.get());
    art.arcane.mystcraft.config.MystcraftConfig.allowGravityBlocksInAges = ForgeMystcraftConfig.allowGravityBlocksInAges::get;
    art.arcane.mystcraft.config.MystcraftConfig.microDimensionsEnabled = ForgeMystcraftConfig.microDimensionsEnabled::get;
    art.arcane.mystcraft.config.MystcraftConfig.microDimensionRadiusChunks = ForgeMystcraftConfig.microDimensionRadiusChunks::get;
    art.arcane.mystcraft.config.MystcraftConfig.microDimensionExtraChunks = ForgeMystcraftConfig.microDimensionExtraChunks::get;
    art.arcane.mystcraft.config.MystcraftConfig.safeStories = ForgeMystcraftConfig.safeStories::get;
    art.arcane.mystcraft.config.MystcraftConfig.droppedBooksBecomeLivingEntities = ForgeMystcraftConfig.droppedBooksBecomeLivingEntities::get;
    art.arcane.mystcraft.config.MystcraftConfig.dropBooksOnRead = ForgeMystcraftConfig.dropBooksOnRead::get;
    art.arcane.mystcraft.config.MystcraftConfig.disabledSymbols =
        () -> List.copyOf(ForgeMystcraftConfig.disabledSymbols.get());
    art.arcane.mystcraft.config.MystcraftConfig.instabilityEnabled = ForgeMystcraftConfig.instabilityEnabled::get;
    art.arcane.mystcraft.config.MystcraftConfig.deathEffectsEnabled = ForgeMystcraftConfig.deathEffectsEnabled::get;
    art.arcane.mystcraft.config.MystcraftConfig.allowUnstableAges = ForgeMystcraftConfig.allowUnstableAges::get;
    art.arcane.mystcraft.config.MystcraftConfig.instabilityMultiplier = ForgeMystcraftConfig.instabilityMultiplier::get;
    art.arcane.mystcraft.config.MystcraftConfig.maxAllowedInstability = ForgeMystcraftConfig.maxAllowedInstability::get;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdDecay = ForgeMystcraftConfig.thresholdDecay::get;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdTransmute = ForgeMystcraftConfig.thresholdTransmute::get;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdLightning = ForgeMystcraftConfig.thresholdLightning::get;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdMeteor = ForgeMystcraftConfig.thresholdMeteor::get;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdPoison = ForgeMystcraftConfig.thresholdPoison::get;
    art.arcane.mystcraft.config.MystcraftConfig.thresholdWither = ForgeMystcraftConfig.thresholdWither::get;
    art.arcane.mystcraft.config.MystcraftConfig.chanceDecay = ForgeMystcraftConfig.chanceDecay::get;
    art.arcane.mystcraft.config.MystcraftConfig.chanceTransmute = ForgeMystcraftConfig.chanceTransmute::get;
    art.arcane.mystcraft.config.MystcraftConfig.chanceLightning = ForgeMystcraftConfig.chanceLightning::get;
    art.arcane.mystcraft.config.MystcraftConfig.chanceMeteor = ForgeMystcraftConfig.chanceMeteor::get;
    art.arcane.mystcraft.config.MystcraftConfig.chancePlayerEffect = ForgeMystcraftConfig.chancePlayerEffect::get;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerHalfSizeXZ = ForgeMystcraftConfig.pocketInnerHalfSizeXZ::get;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerHalfSizeY = ForgeMystcraftConfig.pocketInnerHalfSizeY::get;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerThickness = ForgeMystcraftConfig.pocketInnerThickness::get;
    art.arcane.mystcraft.config.MystcraftConfig.pocketOuterThickness = ForgeMystcraftConfig.pocketOuterThickness::get;
    art.arcane.mystcraft.config.MystcraftConfig.pocketCenterY = ForgeMystcraftConfig.pocketCenterY::get;
    art.arcane.mystcraft.config.MystcraftConfig.pocketInnerBlockPalette = () -> List.copyOf(ForgeMystcraftConfig.pocketInnerBlockPalette.get());
    art.arcane.mystcraft.config.MystcraftConfig.pocketOuterBlock = ForgeMystcraftConfig.pocketOuterBlock::get;
  }

  private void populateNetworkHandlers() {
    art.arcane.mystcraft.network.MystcraftNetwork.sendToServerHandler = ForgeMystcraftNetwork::sendToServer;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToPlayerHandler = ForgeMystcraftNetwork::sendToPlayer;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToAllHandler = ForgeMystcraftNetwork::sendToAll;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingHandler = ForgeMystcraftNetwork::sendToTracking;
    art.arcane.mystcraft.network.MystcraftNetwork.sendToTrackingBlockHandler = ForgeMystcraftNetwork::sendToTrackingBlock;
  }

  // The following methods can be used by common code to register additional content
  @Override
  public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
    return MystcraftRegistries.BLOCKS.register(name, block);
  }

  @Override
  public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
    return MystcraftRegistries.ITEMS.register(name, item);
  }

  @Override
  public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntity(String name, Supplier<T> type) {
    return MystcraftRegistries.BLOCK_ENTITIES.register(name, type);
  }

  @Override
  public <T extends EntityType<?>> Supplier<T> registerEntity(String name, Supplier<T> type) {
    return MystcraftRegistries.ENTITIES.register(name, type);
  }

  @Override
  public <T extends Fluid> Supplier<T> registerFluid(String name, Supplier<T> fluid) {
    return MystcraftRegistries.FLUIDS.register(name, fluid);
  }

  @Override
  public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> sound) {
    return MystcraftRegistries.SOUNDS.register(name, sound);
  }

  @Override
  public Supplier<CreativeModeTab> registerCreativeTab(String name, Supplier<CreativeModeTab> tab) {
    return MystcraftRegistries.CREATIVE_TABS.register(name, tab);
  }

  @Override
  public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
    return MystcraftRegistries.MENUS.register(name, menuType);
  }

  // Accessors for version-specific code that needs the RegistryObjects
  public RegistryObject<Block> getInkMixer() {
    return inkMixer;
  }

  public RegistryObject<Block> getBookBinder() {
    return bookBinder;
  }

  public RegistryObject<Block> getBookReceptacle() {
    return bookReceptacle;
  }

  public RegistryObject<Block> getBookstand() {
    return bookstand;
  }

  public RegistryObject<Block> getLinkModifier() {
    return linkModifier;
  }

  public RegistryObject<Block> getWritingDesk() {
    return writingDesk;
  }

  public RegistryObject<Block> getCrystal() {
    return crystal;
  }

  public RegistryObject<Block> getDecay() {
    return decay;
  }

  public RegistryObject<Block> getLinkPortal() {
    return linkPortal;
  }

  public RegistryObject<Block> getStarFissure() {
    return starFissure;
  }

  public RegistryObject<LiquidBlock> getFluidInk() {
    return fluidInk;
  }

  public RegistryObject<Item> getPage() {
    return page;
  }

  public RegistryObject<Item> getAgebook() {
    return agebook;
  }

  public RegistryObject<Item> getLinkbook() {
    return linkbook;
  }

  public RegistryObject<Item> getLinkbookUnlinked() {
    return linkbookUnlinked;
  }

  public RegistryObject<Item> getPersonalLinkBook() {
    return personalLinkBook;
  }

  public RegistryObject<Item> getBoosterPack() {
    return boosterPack;
  }

  public RegistryObject<Item> getFolder() {
    return folder;
  }

  public RegistryObject<Item> getPortfolio() {
    return portfolio;
  }

  public RegistryObject<Item> getInkVial() {
    return inkVial;
  }

  public RegistryObject<Item> getGuidebook() {
    return guidebook;
  }

  public RegistryObject<Item> getInkBucket() {
    return inkBucket;
  }

  public RegistryObject<Item> getInkMixerItem() {
    return inkMixerItem;
  }

  public RegistryObject<Item> getBookBinderItem() {
    return bookBinderItem;
  }

  public RegistryObject<Item> getBookReceptacleItem() {
    return bookReceptacleItem;
  }

  public RegistryObject<Item> getBookstandItem() {
    return bookstandItem;
  }

  public RegistryObject<Item> getLinkModifierItem() {
    return linkModifierItem;
  }

  public RegistryObject<Item> getWritingDeskItem() {
    return writingDeskItem;
  }

  public RegistryObject<Item> getCrystalItem() {
    return crystalItem;
  }

  public RegistryObject<Item> getDecayItem() {
    return decayItem;
  }

  public RegistryObject<BlockEntityType<InkMixerBlockEntity>> getInkMixerBE() {
    return inkMixerBE;
  }

  public RegistryObject<BlockEntityType<BookBinderBlockEntity>> getBookBinderBE() {
    return bookBinderBE;
  }

  public RegistryObject<BlockEntityType<BookReceptacleBlockEntity>> getBookReceptacleBE() {
    return bookReceptacleBE;
  }

  public RegistryObject<BlockEntityType<BookstandBlockEntity>> getBookstandBE() {
    return bookstandBE;
  }

  public RegistryObject<BlockEntityType<WritingDeskBlockEntity>> getWritingDeskBE() {
    return writingDeskBE;
  }

  public RegistryObject<BlockEntityType<StarFissureBlockEntity>> getStarFissureBE() {
    return starFissureBE;
  }

  public RegistryObject<BlockEntityType<LinkModifierBlockEntity>> getLinkModifierBE() {
    return linkModifierBE;
  }

  public RegistryObject<EntityType<LinkbookEntity>> getLinkbookEntity() {
    return linkbookEntity;
  }

  public RegistryObject<EntityType<MystcraftFallingBlockEntity>> getFallingBlockEntity() {
    return fallingBlockEntity;
  }

  public RegistryObject<EntityType<MeteorEntity>> getMeteorEntity() {
    return meteorEntity;
  }

  public RegistryObject<EntityType<ColoredLightningEntity>> getColoredLightningEntity() {
    return coloredLightningEntity;
  }

  public RegistryObject<FlowingFluid> getBlackInkSource() {
    return blackInkSource;
  }

  public RegistryObject<FlowingFluid> getBlackInkFlowing() {
    return blackInkFlowing;
  }

  public RegistryObject<FluidType> getBlackInkType() {
    return blackInkType;
  }

  public RegistryObject<MenuType<InkMixerMenu>> getInkMixerMenu() {
    return inkMixerMenu;
  }

  public RegistryObject<MenuType<BookBinderMenu>> getBookBinderMenu() {
    return bookBinderMenu;
  }

  public RegistryObject<MenuType<LinkModifierMenu>> getLinkModifierMenu() {
    return linkModifierMenu;
  }

  public RegistryObject<MenuType<WritingDeskMenu>> getWritingDeskMenu() {
    return writingDeskMenu;
  }

  public RegistryObject<MenuType<FolderMenu>> getFolderMenu() {
    return folderMenu;
  }

  public RegistryObject<MenuType<PortfolioMenu>> getPortfolioMenu() {
    return portfolioMenu;
  }
}
