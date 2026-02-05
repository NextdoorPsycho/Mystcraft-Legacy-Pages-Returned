package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import art.arcane.mystcraft.event.*;
import art.arcane.mystcraft.instability.InstabilityManager;
import art.arcane.mystcraft.network.NeoForgeMystcraftNetwork;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.platform.services.IEventHelper;
import art.arcane.mystcraft.neoforge.NeoForgeRegistries;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import art.arcane.mystcraft.villager.ArchivistTradeListings;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated event handler for all NeoForge events.
 * Handles commands, reload listeners, level ticks, player events, entity events,
 * block interactions, village structure injection, and villager trades.
 */
@EventBusSubscriber(modid = Mystcraft.MOD_ID)
public class NeoForgeEventHelper implements IEventHelper {

  private static final String ARCHIVIST_HOUSE_TEMPLATE = Mystcraft.MOD_ID + ":village/archivist_house";
  private static final ResourceLocation[] VILLAGE_POOLS = {
      new ResourceLocation("minecraft", "village/plains/houses"),
      new ResourceLocation("minecraft", "village/desert/houses"),
      new ResourceLocation("minecraft", "village/savanna/houses"),
      new ResourceLocation("minecraft", "village/snowy/houses"),
      new ResourceLocation("minecraft", "village/taiga/houses")
  };
  private static Field bookField;
  private static Field pageCountField;

  static {
    try {
      bookField = LecternBlockEntity.class.getDeclaredField("book");
      bookField.setAccessible(true);
      pageCountField = LecternBlockEntity.class.getDeclaredField("pageCount");
      pageCountField.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Mystcraft.LOGGER.error("[NeoForgeEventHelper] Failed to find LecternBlockEntity fields", e);
    }
  }

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    MystcraftCommands.registerCommands(event.getDispatcher());
  }

  @SubscribeEvent
  public static void onAddReloadListeners(AddReloadListenerEvent event) {
    event.addListener(new MystcraftGrammarReloadListener());
    event.addListener(new MystcraftSymbolReloadListener());
  }

  // ==================== Command Registration ====================

  @SubscribeEvent
  public static void onLevelTick(LevelTickEvent.Post event) {
    if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

    AgeEffectsHandler.onLevelTick(serverLevel);
    InstabilityManager.onLevelTick(serverLevel);
  }

  // ==================== Reload Listeners ====================

  @SubscribeEvent
  public static void onPlayerDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
        if (PersonalPocketEscapeHandler.handleDeath(player, event.getSource())) {
          event.setCanceled(true);
          return;
        }
        if (AgeReturnHandler.handleDeath(player, event.getSource())) {
          event.setCanceled(true);
          return;
        }
        AgeDeathHandler.onPlayerDeath(player, event.getSource(), serverLevel);
      }
    }
  }

  // ==================== Level Tick Events ====================

  @SubscribeEvent
  public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
        AgeDeathHandler.onPlayerRespawn(player, serverLevel);
        AgeDataSyncHandler.onPlayerRespawn(player);
      }
    }
  }

  // ==================== Player Death/Respawn Events ====================

  @SubscribeEvent
  public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      GuidebookHandler.onPlayerLoggedIn(player);
      AgeDataSyncHandler.onPlayerLoggedIn(player);
      NeoForgeMystcraftNetwork.sendToPlayer(new SymbolSyncPacket(), player);
      Mystcraft.LOGGER.debug("Sent symbol sync packet to player {}", player.getName().getString());
    }
  }

  @SubscribeEvent
  public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      AgeDataSyncHandler.onPlayerChangeDimension(player);
      Mystcraft.LOGGER.debug("Player {} changed dimension from {} to {}",
          player.getName().getString(), event.getFrom(), event.getTo());
    }
  }

  // ==================== Player Login/Dimension Change Events ====================

  @SubscribeEvent
  public static void onLivingAttack(LivingAttackEvent event) {
    LivingEntity entity = event.getEntity();
    boolean cancel = AgeEffectsHandler.onLivingAttack(entity, event.getSource());
    if (cancel) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
      return;
    }
    if (!(event.getEntity() instanceof FallingBlockEntity fallingBlock)) {
      return;
    }
    if (FallingBlockHandler.handle(serverLevel, fallingBlock)) {
      event.setCanceled(true);
    }
  }

  // ==================== Living Attack Event ====================

  @SubscribeEvent
  public static void onServerAboutToStart(ServerAboutToStartEvent event) {
    MinecraftServer server = event.getServer();
    Registry<StructureTemplatePool> poolRegistry =
        server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
    Registry<StructureProcessorList> processorRegistry =
        server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);

    Holder<StructureProcessorList> emptyProcessor =
        processorRegistry.getHolderOrThrow(ResourceKey.create(
            Registries.PROCESSOR_LIST,
            new ResourceLocation("minecraft", "empty")));

    int injected = 0;
    for (ResourceLocation poolId : VILLAGE_POOLS) {
      StructureTemplatePool pool = poolRegistry.get(poolId);
      if (pool == null) continue;

      try {
        StructurePoolElement element = SinglePoolElement.single(
            ARCHIVIST_HOUSE_TEMPLATE, emptyProcessor
        ).apply(StructureTemplatePool.Projection.RIGID);

        Field rawTemplatesField = findField(StructureTemplatePool.class, "rawTemplates");
        Field templatesField = findField(StructureTemplatePool.class, "templates");

        if (rawTemplatesField == null || templatesField == null) {
          Mystcraft.LOGGER.warn("[Mystcraft] Could not find pool fields for {}", poolId);
          continue;
        }

        rawTemplatesField.setAccessible(true);
        templatesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Pair<StructurePoolElement, Integer>> rawTemplates =
            (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);
        @SuppressWarnings("unchecked")
        ObjectArrayList<StructurePoolElement> templates =
            (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);

        List<Pair<StructurePoolElement, Integer>> newRawTemplates = new ArrayList<>(rawTemplates);
        newRawTemplates.add(Pair.of(element, 2));
        rawTemplatesField.set(pool, newRawTemplates);

        ObjectArrayList<StructurePoolElement> newTemplates = new ObjectArrayList<>(templates);
        for (int i = 0; i < 2; i++) {
          newTemplates.add(element);
        }
        templatesField.set(pool, newTemplates);

        injected++;
      } catch (Exception e) {
        Mystcraft.LOGGER.warn("[Mystcraft] Failed to inject archivist house into pool {}: {}",
            poolId, e.getMessage());
      }
    }

    if (injected > 0) {
      Mystcraft.LOGGER.info("[Mystcraft] Injected archivist house into {} village pools", injected);
    }
  }

  // ==================== Entity Join Level Event ====================

  private static Field findField(Class<?> clazz, String name) {
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getName().equals(name)) {
        return field;
      }
    }
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getType() == List.class && name.equals("rawTemplates")) {
        return field;
      }
      if (field.getType() == ObjectArrayList.class && name.equals("templates")) {
        return field;
      }
    }
    return null;
  }

  // ==================== Village Structure Injection ====================

  @SubscribeEvent
  public static void onVillagerTrades(VillagerTradesEvent event) {
    if (event.getType() != NeoForgeRegistries.ARCHIVIST.get()) {
      return;
    }

    Mystcraft.LOGGER.debug("[Mystcraft] Registering Archivist trades");

    // Level 1 trades (Novice)
    List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
    level1.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 2),
        new ItemStack(NeoForgeRegistries.INK_VIAL.get(), 1),
        12, 1, 0.05f
    ));
    level1.add(new BasicItemListing(
        new ItemStack(NeoForgeRegistries.PAGE.get(), 8),
        new ItemStack(Items.EMERALD, 1),
        16, 2, 0.05f
    ));
    level1.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 1),
        new ItemStack(NeoForgeRegistries.PAGE.get(), 4),
        16, 1, 0.05f
    ));
    level1.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 5));

    // Level 2 trades (Apprentice)
    List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
    level2.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 5),
        new ItemStack(NeoForgeRegistries.FOLDER.get(), 1),
        8, 5, 0.05f
    ));
    level2.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 3),
        new ItemStack(NeoForgeRegistries.INK_VIAL.get(), 2),
        12, 5, 0.05f
    ));
    level2.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 8));
    level2.add(new ArchivistTradeListings.RankedSymbolTrade(2, 1, 10));

    // Level 3 trades (Journeyman)
    List<VillagerTrades.ItemListing> level3 = event.getTrades().get(3);
    level3.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 12),
        new ItemStack(NeoForgeRegistries.PORTFOLIO.get(), 1),
        4, 10, 0.05f
    ));
    level3.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 8),
        new ItemStack(NeoForgeRegistries.BOOSTER_PACK.get(), 1),
        6, 10, 0.05f
    ));
    level3.add(new ArchivistTradeListings.RankedSymbolTrade(2, 2, 12));
    level3.add(new ArchivistTradeListings.RankedSymbolTrade(3, 1, 15));

    // Level 4 trades (Expert)
    List<VillagerTrades.ItemListing> level4 = event.getTrades().get(4);
    level4.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 20),
        new ItemStack(NeoForgeRegistries.LINKBOOK_UNLINKED.get(), 1),
        3, 15, 0.05f
    ));
    level4.add(new ArchivistTradeListings.RankedSymbolTrade(3, 2, 18));
    level4.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 20));

    // Level 5 trades (Master)
    List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
    level5.add(new ArchivistTradeListings.SymbolPageTrade(1, 25));
    level5.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 25));
    level5.add(new BasicItemListing(
        new ItemStack(NeoForgeRegistries.LINKBOOK.get(), 1),
        new ItemStack(Items.EMERALD, 24),
        2, 30, 0.05f
    ));
    level5.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
    level5.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
  }

  @SubscribeEvent
  public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    Level level = event.getLevel();
    BlockPos pos = event.getPos();
    BlockState state = level.getBlockState(pos);

    if (!(state.getBlock() instanceof LecternBlock)) {
      return;
    }

    var result = MystcraftLecternHelper.handleLecternInteraction(
        level, pos, state, event.getEntity(), event.getHand(),
        (lectern, book, pageCount) -> {
          try {
            if (bookField != null && pageCountField != null) {
              bookField.set(lectern, book);
              pageCountField.set(lectern, pageCount);
            }
          } catch (Exception e) {
            Mystcraft.LOGGER.error("[NeoForgeEventHelper] Failed to set book fields", e);
          }
        });

    if (result.handled) {
      event.setCanceled(true);
      event.setCancellationResult(result.result);
    }
  }

  @Override
  public void registerServerEvents() {
    // Events are auto-registered via @Mod.EventBusSubscriber
  }

  @Override
  public void registerClientEvents() {
    // Events are auto-registered via @Mod.EventBusSubscriber
  }

  // ==================== Archivist Trades ====================

  @Override
  public void registerCommonEvents() {
    // Events are auto-registered via @Mod.EventBusSubscriber
  }

  // ==================== Block Interaction Events ====================

  @Override
  public void fireLevelLoadEvent(ServerLevel level) {
    NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
  }
}
