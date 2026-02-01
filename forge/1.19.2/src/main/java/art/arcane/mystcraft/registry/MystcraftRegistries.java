package art.arcane.mystcraft.registry;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.IAgeSymbol;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.config.MystcraftConfig;
import art.arcane.mystcraft.data.Page;
import art.arcane.mystcraft.loot.BoosterPackLootModifier;
import art.arcane.mystcraft.loot.GuidebookLootModifier;
import art.arcane.mystcraft.loot.SymbolPageLootModifier;
import art.arcane.mystcraft.symbol.SymbolRegistry;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import art.arcane.mystcraft.world.gen.biome.AgeBiomeSource;
import art.arcane.mystcraft.world.structure.AbandonedLibraryStructure;
import art.arcane.mystcraft.world.structure.ScatteredLibraryStructure;
import art.arcane.mystcraft.world.structure.UndergroundArchiveStructure;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Central holder for all Mystcraft DeferredRegister instances (1.19.2 version).
 * Consolidates all registry definitions including loot modifiers, villagers, and world gen.
 */
public final class MystcraftRegistries {

  // ==================== Core Registries ====================

  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(ForgeRegistries.BLOCKS, Mystcraft.MOD_ID);

  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, Mystcraft.MOD_ID);

  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<Fluid> FLUIDS =
      DeferredRegister.create(ForgeRegistries.FLUIDS, Mystcraft.MOD_ID);

  public static final DeferredRegister<FluidType> FLUID_TYPES =
      DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<SoundEvent> SOUNDS =
      DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Mystcraft.MOD_ID);

  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(ForgeRegistries.MENU_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
      DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mystcraft.MOD_ID);

  public static final DeferredRegister<PoiType> POI_TYPES =
      DeferredRegister.create(ForgeRegistries.POI_TYPES, Mystcraft.MOD_ID);

  public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
      DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Mystcraft.MOD_ID);

  // ==================== World Generation Registries ====================

  public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
      DeferredRegister.create(Registry.CHUNK_GENERATOR_REGISTRY, Mystcraft.MOD_ID);

  public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
      DeferredRegister.create(Registry.BIOME_SOURCE_REGISTRY, Mystcraft.MOD_ID);

  public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
      DeferredRegister.create(Registry.STRUCTURE_TYPE_REGISTRY, Mystcraft.MOD_ID);

  // ==================== Loot Modifier Registrations ====================

  public static final RegistryObject<Codec<SymbolPageLootModifier>> SYMBOL_PAGE_LOOT =
      LOOT_MODIFIERS.register("symbol_page", SymbolPageLootModifier.CODEC);

  public static final RegistryObject<Codec<GuidebookLootModifier>> GUIDEBOOK_LOOT =
      LOOT_MODIFIERS.register("guidebook", GuidebookLootModifier.CODEC);

  public static final RegistryObject<Codec<BoosterPackLootModifier>> BOOSTER_PACK_LOOT =
      LOOT_MODIFIERS.register("booster_pack", BoosterPackLootModifier.CODEC);

  // ==================== World Gen Registrations ====================

  public static final RegistryObject<Codec<? extends ChunkGenerator>> AGE_CHUNK_GENERATOR =
      CHUNK_GENERATORS.register("age_chunk_generator", () -> AgeChunkGenerator.CODEC);

  public static final RegistryObject<Codec<? extends BiomeSource>> AGE_BIOME_SOURCE =
      BIOME_SOURCES.register("age_biome_source", () -> AgeBiomeSource.CODEC);

  // ==================== Structure Registrations ====================

  public static final RegistryObject<StructureType<AbandonedLibraryStructure>> ABANDONED_LIBRARY =
      STRUCTURE_TYPES.register("abandoned_library",
          () -> () -> AbandonedLibraryStructure.CODEC);

  public static final RegistryObject<StructureType<UndergroundArchiveStructure>> UNDERGROUND_ARCHIVE =
      STRUCTURE_TYPES.register("underground_archive",
          () -> () -> UndergroundArchiveStructure.CODEC);

  public static final RegistryObject<StructureType<ScatteredLibraryStructure>> SCATTERED_LIBRARY =
      STRUCTURE_TYPES.register("scattered_library",
          () -> () -> ScatteredLibraryStructure.CODEC);

  // ==================== Villager Registrations ====================

  /**
   * POI type for the Archivist workstation (Bookstand).
   */
  public static final RegistryObject<PoiType> ARCHIVIST_POI = POI_TYPES.register(
      "archivist",
      () -> new PoiType(
          ImmutableSet.copyOf(ModBlocks.BOOKSTAND.get().getStateDefinition().getPossibleStates()),
          1, 1
      )
  );

  /**
   * The Archivist villager profession.
   */
  public static final RegistryObject<VillagerProfession> ARCHIVIST = VILLAGER_PROFESSIONS.register(
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

  // ==================== Creative Tab (1.19.2 style) ====================
  // Note: In 1.19.2, CreativeModeTabs are handled with the old constructor style.

  public static CreativeModeTab MYSTCRAFT_TAB;
  public static CreativeModeTab MYSTCRAFT_PAGES_TAB;

  /**
   * Initialize creative tabs for 1.19.2.
   * This should be called during mod initialization.
   */
  public static void initCreativeTabs() {
    // In 1.19.2, we use the anonymous class style for CreativeModeTab
    MYSTCRAFT_TAB = new CreativeModeTab("mystcraft") {
      @Override
      public ItemStack makeIcon() {
        return new ItemStack(ModItems.AGEBOOK.get());
      }
    };

    MYSTCRAFT_PAGES_TAB = new CreativeModeTab("mystcraft_pages") {
      @Override
      public ItemStack makeIcon() {
        return Page.createLinkPage();
      }
    };
  }

  private MystcraftRegistries() {
  }

  /**
   * Registers all DeferredRegister instances to the mod event bus.
   * Call this from the main mod constructor.
   */
  public static void register(IEventBus modEventBus) {
    BLOCKS.register(modEventBus);
    ITEMS.register(modEventBus);
    BLOCK_ENTITIES.register(modEventBus);
    ENTITIES.register(modEventBus);
    FLUIDS.register(modEventBus);
    FLUID_TYPES.register(modEventBus);
    SOUNDS.register(modEventBus);
    MENUS.register(modEventBus);
    LOOT_MODIFIERS.register(modEventBus);
    POI_TYPES.register(modEventBus);
    VILLAGER_PROFESSIONS.register(modEventBus);
    CHUNK_GENERATORS.register(modEventBus);
    BIOME_SOURCES.register(modEventBus);
    STRUCTURE_TYPES.register(modEventBus);

    // Populate common ModStructures suppliers
    art.arcane.mystcraft.world.structure.ModStructures.ABANDONED_LIBRARY = ABANDONED_LIBRARY;
    art.arcane.mystcraft.world.structure.ModStructures.UNDERGROUND_ARCHIVE = UNDERGROUND_ARCHIVE;
    art.arcane.mystcraft.world.structure.ModStructures.SCATTERED_LIBRARY = SCATTERED_LIBRARY;

    Mystcraft.LOGGER.info("Registered all Mystcraft deferred registries");
  }
}
