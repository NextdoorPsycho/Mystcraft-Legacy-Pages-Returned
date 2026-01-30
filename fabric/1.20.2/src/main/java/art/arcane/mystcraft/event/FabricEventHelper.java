package art.arcane.mystcraft.event;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.instability.InstabilityManager;
import art.arcane.mystcraft.network.FabricNetworkEvents;
import art.arcane.mystcraft.platform.services.IEventHelper;
import art.arcane.mystcraft.registry.FabricRegistries;
import art.arcane.mystcraft.villager.ArchivistTradeListings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.lang.reflect.Field;

/**
 * Consolidated Fabric event helper for Mystcraft 1.20.2.
 * Combines: FabricEventRegistration, FabricVillageStructureHandler, FabricArchivistTrades,
 * and the IEventHelper interface implementation.
 */
public final class FabricEventHelper implements IEventHelper {

  public FabricEventHelper() {
    // Public constructor required by ServiceLoader
  }

  // ========== EVENT REGISTRATION ==========
  public static void registerAll() {
    // World tick: instability and age effects processing
    ServerTickEvents.END_WORLD_TICK.register(level -> {
      InstabilityManager.onLevelTick(level);
      AgeEffectsHandler.onLevelTick(level);
    });

    // Command registration
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      MystcraftCommands.registerCommands(dispatcher);
    });

    // Living entity death: age death effects
    ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
      if (entity instanceof ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
          if (PersonalPocketEscapeHandler.handleDeath(player, damageSource)) {
            return false;
          }
          if (AgeReturnHandler.handleDeath(player, damageSource)) {
            return false;
          }
          return true;
        }
      }
      return true;
    });
    ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
      if (entity instanceof ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
          AgeDeathHandler.onPlayerDeath(player, damageSource, serverLevel);
        }
      }
    });

    // Player login: guidebook delivery and age data sync
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayer player = handler.getPlayer();
      AgeDataSyncHandler.onPlayerLoggedIn(player);
      GuidebookHandler.onPlayerLoggedIn(player);
    });

    // Player respawn: death handler and data re-sync
    ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
      if (newPlayer.level() instanceof ServerLevel serverLevel) {
        AgeDeathHandler.onPlayerRespawn(newPlayer, serverLevel);
      }
      AgeDataSyncHandler.onPlayerRespawn(newPlayer);
    });

    // Player dimension change: age data sync
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
      AgeDataSyncHandler.onPlayerChangeDimension(player);
    });

    // Network events (symbol sync on join)
    FabricNetworkEvents.register();

    // Register Archivist trades
    registerArchivistTrades();

    Mystcraft.LOGGER.info("[FabricEventHelper] Registered all event callbacks");
  }

  // ========== ARCHIVIST TRADES ==========
  private static void registerArchivistTrades() {
    Mystcraft.LOGGER.debug("[Mystcraft] Registering Archivist trades");

    // Level 1 (Novice)
    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 1, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 2), new ItemStack(FabricRegistries.INK_VIAL.get(), 1), 12, 1, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(FabricRegistries.PAGE.get(), 8), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 1), new ItemStack(FabricRegistries.PAGE.get(), 4), 16, 1, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 5));
    });

    // Level 2 (Apprentice)
    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 2, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 5), new ItemStack(FabricRegistries.FOLDER.get(), 1), 8, 5, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 3), new ItemStack(FabricRegistries.INK_VIAL.get(), 2), 12, 5, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 8));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(2, 1, 10));
    });

    // Level 3 (Journeyman)
    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 3, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 12), new ItemStack(FabricRegistries.PORTFOLIO.get(), 1), 4, 10, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 8), new ItemStack(FabricRegistries.BOOSTER_PACK.get(), 1), 6, 10, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(2, 2, 12));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(3, 1, 15));
    });

    // Level 4 (Expert)
    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 4, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 20), new ItemStack(FabricRegistries.LINKBOOK_UNLINKED.get(), 1), 3, 15, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(3, 2, 18));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 20));
    });

    // Level 5 (Master)
    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 5, factories -> {
      factories.add(new ArchivistTradeListings.SymbolPageTrade(1, 25));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 25));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(FabricRegistries.LINKBOOK.get(), 1), new ItemStack(Items.EMERALD, 24), 2, 30, 0.05f));
      factories.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
      factories.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
    });
  }

  // ========== VILLAGE STRUCTURE INJECTION ==========
  public static void onServerStarting(MinecraftServer server) {
    try {
      Registry<StructureTemplatePool> templatePools = server.registryAccess()
          .registryOrThrow(Registries.TEMPLATE_POOL);

      addToPool(templatePools, "minecraft:village/plains/houses",
          "mystcraft:village/plains/archivist_house", 2);
      addToPool(templatePools, "minecraft:village/desert/houses",
          "mystcraft:village/desert/archivist_house", 2);
      addToPool(templatePools, "minecraft:village/savanna/houses",
          "mystcraft:village/savanna/archivist_house", 2);
      addToPool(templatePools, "minecraft:village/taiga/houses",
          "mystcraft:village/taiga/archivist_house", 2);
      addToPool(templatePools, "minecraft:village/snowy/houses",
          "mystcraft:village/snowy/archivist_house", 2);

    } catch (Exception e) {
      Mystcraft.LOGGER.error("[Mystcraft] Failed to inject village structures", e);
    }
  }

  private static void addToPool(Registry<StructureTemplatePool> pools, String poolId, String pieceId, int weight) {
    StructureTemplatePool pool = pools.get(new ResourceLocation(poolId));
    if (pool == null) return;

    StructurePoolElement element = StructurePoolElement.legacy(pieceId).apply(StructureTemplatePool.Projection.RIGID);

    try {
      Field templatesField = findField(StructureTemplatePool.class, "templates");
      if (templatesField == null) {
        Mystcraft.LOGGER.warn("[Mystcraft] Could not find templates field for pool {}", poolId);
        return;
      }

      templatesField.setAccessible(true);

      @SuppressWarnings("unchecked")
      ObjectArrayList<StructurePoolElement> templates =
          (ObjectArrayList<StructurePoolElement>) templatesField.get(pool);

      ObjectArrayList<StructurePoolElement> newTemplates = new ObjectArrayList<>(templates);
      for (int i = 0; i < weight; i++) {
        newTemplates.add(element);
      }
      templatesField.set(pool, newTemplates);

      Mystcraft.LOGGER.debug("[Mystcraft] Added {} to pool {} with weight {}", pieceId, poolId, weight);
    } catch (Exception e) {
      Mystcraft.LOGGER.warn("[Mystcraft] Failed to add {} to pool {}: {}", pieceId, poolId, e.getMessage());
    }
  }

  private static Field findField(Class<?> clazz, String name) {
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getName().equals(name)) {
        return field;
      }
    }
    for (Field field : clazz.getDeclaredFields()) {
      if (field.getType() == ObjectArrayList.class && name.equals("templates")) {
        return field;
      }
    }
    return null;
  }

  // ========== IEventHelper INTERFACE ==========
  @Override
  public void registerServerEvents() {
  }

  @Override
  public void registerClientEvents() {
  }

  @Override
  public void registerCommonEvents() {
  }

  @Override
  public void fireLevelLoadEvent(ServerLevel level) {
    // Fabric has no event bus to post to. Level load logic is called directly
    // from MystcraftFabric when dimensions are loaded.
  }
}
