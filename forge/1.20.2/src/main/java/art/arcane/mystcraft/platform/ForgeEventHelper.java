package art.arcane.mystcraft.platform;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.client.AgeColorUtils;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import art.arcane.mystcraft.event.*;
import art.arcane.mystcraft.instability.InstabilityManager;
import art.arcane.mystcraft.network.ForgeMystcraftNetwork;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.network.SyncAgeDataPacket.ClientAgeDataCache;
import art.arcane.mystcraft.client.PocketHeadClientSync;
import art.arcane.mystcraft.platform.services.IEventHelper;
import art.arcane.mystcraft.registry.ModItems;
import art.arcane.mystcraft.registry.MystcraftRegistries;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import art.arcane.mystcraft.villager.ArchivistTradeListings;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.FogRenderer;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consolidated event handler for all Forge events.
 * Registers events manually instead of using @Mod.EventBusSubscriber annotations.
 */
public class ForgeEventHelper implements IEventHelper {

  private static final Set<Integer> LOGGED_FOG_AGES = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private static final String ARCHIVIST_HOUSE_TEMPLATE = Mystcraft.MOD_ID + ":village/archivist_house";
  private static final ResourceLocation[] VILLAGE_POOLS = {
      new ResourceLocation("minecraft", "village/plains/houses"),
      new ResourceLocation("minecraft", "village/desert/houses"),
      new ResourceLocation("minecraft", "village/savanna/houses"),
      new ResourceLocation("minecraft", "village/snowy/houses"),
      new ResourceLocation("minecraft", "village/taiga/houses")
  };

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

  @Override
  public void registerServerEvents() {
    MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    MinecraftForge.EVENT_BUS.addListener(this::onLevelTick);
    MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    MinecraftForge.EVENT_BUS.addListener(this::onVillagerTrades);
  }

  // ==================== Command Registration ====================

  @Override
  public void registerClientEvents() {
    if (FMLEnvironment.dist == Dist.CLIENT) {
      MinecraftForge.EVENT_BUS.addListener(this::onComputeFogColor);
      MinecraftForge.EVENT_BUS.addListener(this::onRenderFog);
      MinecraftForge.EVENT_BUS.addListener(this::onClientLoggedIn);
      MinecraftForge.EVENT_BUS.addListener(this::onClientLoggedOut);
      MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }
  }

  // ==================== Reload Listeners ====================

  @Override
  public void registerCommonEvents() {
    // Player events
    MinecraftForge.EVENT_BUS.addListener(this::onPlayerDeath);
    MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
    MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    MinecraftForge.EVENT_BUS.addListener(this::onPlayerChangeDimension);

    // Entity events
    MinecraftForge.EVENT_BUS.addListener(this::onLivingAttack);
    MinecraftForge.EVENT_BUS.addListener(this::onEntityJoinLevel);

    // Block interaction events
    MinecraftForge.EVENT_BUS.addListener(this::onRightClickBlock);
  }

  // ==================== Level Tick Events ====================

  @Override
  public void fireLevelLoadEvent(ServerLevel level) {
    MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
  }

  // ==================== Player Death/Respawn Events ====================

  private void onRegisterCommands(RegisterCommandsEvent event) {
    MystcraftCommands.registerCommands(event.getDispatcher());
  }

  private void onAddReloadListeners(AddReloadListenerEvent event) {
    event.addListener(new MystcraftGrammarReloadListener());
    event.addListener(new MystcraftSymbolReloadListener());
  }

  // ==================== Client Networking Sync ====================

  private void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
    PocketHeadClientSync.requestSend();
  }

  private void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
    PocketHeadClientSync.reset();
  }

  private void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    PocketHeadClientSync.tick();
  }

  // ==================== Player Login/Dimension Change Events ====================

  private void onLevelTick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    if (!(event.level instanceof ServerLevel serverLevel)) return;

    // Age effects (weather, ambient, etc.)
    AgeEffectsHandler.onLevelTick(serverLevel);

    // Instability effects
    InstabilityManager.onLevelTick(serverLevel);
  }

  private void onPlayerDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
        // Personal pocket escape
        if (PersonalPocketEscapeHandler.handleDeath(player, event.getSource())) {
          event.setCanceled(true);
          return;
        }
        // Age return on death
        if (AgeReturnHandler.handleDeath(player, event.getSource())) {
          event.setCanceled(true);
          return;
        }
        // Normal death handling
        AgeDeathHandler.onPlayerDeath(player, event.getSource(), serverLevel);
      }
    }
  }

  // ==================== Living Attack Event ====================

  private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      if (player.level() instanceof ServerLevel serverLevel) {
        AgeDeathHandler.onPlayerRespawn(player, serverLevel);
        AgeDataSyncHandler.onPlayerRespawn(player);
      }
    }
  }

  // ==================== Entity Join Level Event ====================

  private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      // Guidebook delivery
      GuidebookHandler.onPlayerLoggedIn(player);

      // Age data sync
      AgeDataSyncHandler.onPlayerLoggedIn(player);

      // Symbol sync
      ForgeMystcraftNetwork.sendToPlayer(new SymbolSyncPacket(), player);
      Mystcraft.LOGGER.debug("Sent symbol sync packet to player {}", player.getName().getString());
    }
  }

  // ==================== Village Structure Injection ====================

  private void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      AgeDataSyncHandler.onPlayerChangeDimension(player);
      Mystcraft.LOGGER.debug("Player {} changed dimension from {} to {}",
          player.getName().getString(), event.getFrom(), event.getTo());
    }
  }

  private void onLivingAttack(LivingAttackEvent event) {
    LivingEntity entity = event.getEntity();
    boolean cancel = AgeEffectsHandler.onLivingAttack(entity, event.getSource());
    if (cancel) {
      event.setCanceled(true);
    }
  }

  private void onEntityJoinLevel(EntityJoinLevelEvent event) {
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

  private void onServerAboutToStart(ServerAboutToStartEvent event) {
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

  // ==================== Archivist Trades ====================

  private void onVillagerTrades(VillagerTradesEvent event) {
    if (event.getType() != MystcraftRegistries.ARCHIVIST.get()) {
      return;
    }

    Mystcraft.LOGGER.debug("[Mystcraft] Registering Archivist trades");

    // Level 1 trades (Novice)
    List<VillagerTrades.ItemListing> level1 = event.getTrades().get(1);
    level1.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 2),
        new ItemStack(ModItems.INK_VIAL.get(), 1),
        12, 1, 0.05f
    ));
    level1.add(new BasicItemListing(
        new ItemStack(ModItems.PAGE.get(), 8),
        new ItemStack(Items.EMERALD, 1),
        16, 2, 0.05f
    ));
    level1.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 1),
        new ItemStack(ModItems.PAGE.get(), 4),
        16, 1, 0.05f
    ));
    level1.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 5));

    // Level 2 trades (Apprentice)
    List<VillagerTrades.ItemListing> level2 = event.getTrades().get(2);
    level2.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 5),
        new ItemStack(ModItems.FOLDER.get(), 1),
        8, 5, 0.05f
    ));
    level2.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 3),
        new ItemStack(ModItems.INK_VIAL.get(), 2),
        12, 5, 0.05f
    ));
    level2.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 8));
    level2.add(new ArchivistTradeListings.RankedSymbolTrade(2, 1, 10));

    // Level 3 trades (Journeyman)
    List<VillagerTrades.ItemListing> level3 = event.getTrades().get(3);
    level3.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 12),
        new ItemStack(ModItems.PORTFOLIO.get(), 1),
        4, 10, 0.05f
    ));
    level3.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 8),
        new ItemStack(ModItems.BOOSTER_PACK.get(), 1),
        6, 10, 0.05f
    ));
    level3.add(new ArchivistTradeListings.RankedSymbolTrade(2, 2, 12));
    level3.add(new ArchivistTradeListings.RankedSymbolTrade(3, 1, 15));

    // Level 4 trades (Expert)
    List<VillagerTrades.ItemListing> level4 = event.getTrades().get(4);
    level4.add(new BasicItemListing(
        new ItemStack(Items.EMERALD, 20),
        new ItemStack(ModItems.LINKBOOK_UNLINKED.get(), 1),
        3, 15, 0.05f
    ));
    level4.add(new ArchivistTradeListings.RankedSymbolTrade(3, 2, 18));
    level4.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 20));

    // Level 5 trades (Master)
    List<VillagerTrades.ItemListing> level5 = event.getTrades().get(5);
    level5.add(new ArchivistTradeListings.SymbolPageTrade(1, 25));
    level5.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 25));
    level5.add(new BasicItemListing(
        new ItemStack(ModItems.LINKBOOK.get(), 1),
        new ItemStack(Items.EMERALD, 24),
        2, 30, 0.05f
    ));
    level5.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
    level5.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
  }

  // ==================== Block Interaction Events ====================

  private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    Level level = event.getLevel();
    BlockPos pos = event.getPos();
    BlockState state = level.getBlockState(pos);

    // Only handle vanilla lecterns
    if (!(state.getBlock() instanceof LecternBlock)) {
      return;
    }

    var result = MystcraftLecternHelper.handleLecternInteraction(
        level, pos, state, event.getEntity(), event.getHand(),
        (lectern, book, pageCount) -> {
          try {
            ObfuscationReflectionHelper.setPrivateValue(
                LecternBlockEntity.class, lectern, book, "f_59527_");
            ObfuscationReflectionHelper.setPrivateValue(
                LecternBlockEntity.class, lectern, pageCount, "f_59529_");
          } catch (Exception e) {
            Mystcraft.LOGGER.error("[LecternHandler] Failed to set book fields", e);
          }
        });

    if (result.handled) {
      event.setCanceled(true);
      event.setCancellationResult(result.result);
    }
  }

  // ==================== Client Fog Events ====================

  private void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
    int ageUID = AgeColorUtils.getCurrentAgeUID();
    if (ageUID < 0) return;

    int fogColor = ClientAgeDataCache.getFogColor(ageUID);
    if (fogColor != -1) {
      float r = ((fogColor >> 16) & 0xFF) / 255.0f;
      float g = ((fogColor >> 8) & 0xFF) / 255.0f;
      float b = (fogColor & 0xFF) / 255.0f;

      event.setRed(r);
      event.setGreen(g);
      event.setBlue(b);
    }

    // Apply lighting type modifications
    String lightingType = ClientAgeDataCache.getLightingType(ageUID);
    switch (lightingType) {
      case "bright" -> {
        event.setRed(Math.min(1.0f, event.getRed() * 1.2f));
        event.setGreen(Math.min(1.0f, event.getGreen() * 1.2f));
        event.setBlue(Math.min(1.0f, event.getBlue() * 1.2f));
      }
      case "dark" -> {
        event.setRed(event.getRed() * 0.5f);
        event.setGreen(event.getGreen() * 0.5f);
        event.setBlue(event.getBlue() * 0.5f);
      }
    }

    // Log once per age for fog pipeline tracing
    if (LOGGED_FOG_AGES.add(ageUID)) {
      Mystcraft.LOGGER.info("[FogRender] Age {}: fogColor=0x{}, lighting={}, applied R={} G={} B={}",
          ageUID,
          fogColor != -1 ? Integer.toHexString(fogColor) : "none",
          lightingType,
          String.format("%.3f", event.getRed()),
          String.format("%.3f", event.getGreen()),
          String.format("%.3f", event.getBlue()));
    }
  }

  private void onRenderFog(ViewportEvent.RenderFog event) {
    int ageUID = AgeColorUtils.getCurrentAgeUID();
    if (ageUID < 0) return;

    String lightingType = ClientAgeDataCache.getLightingType(ageUID);
    if ("dark".equals(lightingType)) {
      if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
        event.setNearPlaneDistance(event.getNearPlaneDistance() * 0.5f);
        event.setFarPlaneDistance(event.getFarPlaneDistance() * 0.7f);
        event.setCanceled(true);
      }
    }
  }
}
