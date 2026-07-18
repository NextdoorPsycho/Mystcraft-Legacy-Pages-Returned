package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.api.symbol.SymbolCategory;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.event.*;
import art.arcane.mystcraft.network.FabricNetworkEvents;
import art.arcane.mystcraft.platform.services.IEventHelper;
import art.arcane.mystcraft.villager.ArchivistTradeListings;
import com.mojang.datafixers.util.Pair;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated Fabric event helper for Mystcraft 1.20.1. Combines:
 * FabricEventRegistration, FabricVillageStructureHandler,
 * FabricArchivistTrades, and the IEventHelper interface implementation.
 */
public final class FabricEventHelper implements IEventHelper {

  public FabricEventHelper() {
  }

  public static void registerAll() {

    ServerTickEvents.END_WORLD_TICK.register(level -> {
      AgeEffectsHandler.onLevelTick(level);
      PersonalPocketEscapeHandler.tickProxyCleanup(level);
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      MystcraftCommands.registerCommands(dispatcher);
    });

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

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      ServerPlayer player = handler.getPlayer();
      AgeDataSyncHandler.onPlayerLoggedIn(player);
      GuidebookHandler.onPlayerLoggedIn(player);
      PersonalPocketEscapeHandler.syncProxyForPlayer(player);
    });

    ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
      if (newPlayer.level() instanceof ServerLevel serverLevel) {
        AgeDeathHandler.onPlayerRespawn(newPlayer, serverLevel);
      }
      AgeDataSyncHandler.onPlayerRespawn(newPlayer);
    });

    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
      AgeDataSyncHandler.onPlayerChangeDimension(player);
      PersonalPocketEscapeHandler.syncProxyForPlayer(player);
    });

    FabricNetworkEvents.register();

    registerArchivistTrades();

    Mystcraft.LOGGER.info("[FabricEventHelper] Registered all event callbacks");
  }

  private static void registerArchivistTrades() {
    Mystcraft.LOGGER.debug("[Mystcraft] Registering Archivist trades");

    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 1, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 2), new ItemStack(FabricRegistries.INK_VIAL.get(), 1), 12, 1, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(FabricRegistries.PAGE.get(), 8), new ItemStack(Items.EMERALD, 1), 16, 2, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 1), new ItemStack(FabricRegistries.PAGE.get(), 4), 16, 1, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 5));
    });

    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 2, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 5), new ItemStack(FabricRegistries.FOLDER.get(), 1), 8, 5, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 3), new ItemStack(FabricRegistries.INK_VIAL.get(), 2), 12, 5, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(1, 2, 8));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(2, 1, 10));
    });

    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 3, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 12), new ItemStack(FabricRegistries.PORTFOLIO.get(), 1), 4, 10, 0.05f));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 8), new ItemStack(FabricRegistries.BOOSTER_PACK.get(), 1), 6, 10, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(2, 2, 12));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(3, 1, 15));
    });

    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 4, factories -> {
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(Items.EMERALD, 20), new ItemStack(FabricRegistries.LINKBOOK_UNLINKED.get(), 1), 3, 15, 0.05f));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(3, 2, 18));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 20));
    });

    TradeOfferHelper.registerVillagerOffers(FabricRegistries.ARCHIVIST.get(), 5, factories -> {
      factories.add(new ArchivistTradeListings.SymbolPageTrade(1, 25));
      factories.add(new ArchivistTradeListings.RankedSymbolTrade(4, 1, 25));
      factories.add((entity, random) -> new MerchantOffer(
          new ItemStack(FabricRegistries.LINKBOOK.get(), 1), new ItemStack(Items.EMERALD, 24), 2, 30, 0.05f));
      factories.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.TERRAIN, 1, 20));
      factories.add(new ArchivistTradeListings.CategorySymbolTrade(SymbolCategory.BIOME, 1, 18));
    });
  }

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

    List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(pool.rawTemplates);
    rawTemplates.add(Pair.of(element, weight));
    pool.rawTemplates = rawTemplates;

    ObjectArrayList<StructurePoolElement> templates = new ObjectArrayList<>(pool.templates);
    for (int i = 0; i < weight; i++) {
      templates.add(element);
    }
    pool.templates = templates;

    Mystcraft.LOGGER.debug("[Mystcraft] Added {} to pool {} with weight {}", pieceId, poolId, weight);
  }

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

  }
}
