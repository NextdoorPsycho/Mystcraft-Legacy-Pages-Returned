package art.arcane.mystcraft.forge;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.datapack.affinity.MystcraftAffinityReloadListener;
import art.arcane.mystcraft.datapack.grammar.MystcraftGrammarReloadListener;
import art.arcane.mystcraft.datapack.symbol.MystcraftSymbolReloadListener;
import art.arcane.mystcraft.event.AgeDataSyncHandler;
import art.arcane.mystcraft.event.AgeDeathHandler;
import art.arcane.mystcraft.event.AgeEffectsHandler;
import art.arcane.mystcraft.event.AgeReturnHandler;
import art.arcane.mystcraft.event.FallingBlockHandler;
import art.arcane.mystcraft.event.GuidebookHandler;
import art.arcane.mystcraft.event.PersonalPocketEscapeHandler;
import art.arcane.mystcraft.network.MystcraftNetwork;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;

/** Forge 65 event bridge for Mystcraft's loader-neutral gameplay handlers. */
final class ForgeEvents {

  private static boolean serverEventsRegistered;
  private static boolean commonEventsRegistered;
  private static boolean clientEventsRegistered;

  private ForgeEvents() {
  }

  static synchronized void registerServerEvents() {
    if (serverEventsRegistered) {
      return;
    }

    RegisterCommandsEvent.BUS.addListener(event ->
        MystcraftCommands.registerCommands(event.getDispatcher()));
    AddReloadListenerEvent.BUS.addListener(ForgeEvents::addReloadListeners);
    TickEvent.LevelTickEvent.Post.BUS.addListener(ForgeEvents::onLevelTick);
    LevelEvent.Load.BUS.addListener(ForgeEvents::onLevelLoad);

    serverEventsRegistered = true;
    Mystcraft.LOGGER.info("[Mystcraft] Registered Forge server events");
  }

  static synchronized void registerClientEvents() {
    if (clientEventsRegistered) {
      return;
    }
    ForgeClientRuntimeEvents.register();
    clientEventsRegistered = true;
  }

  static synchronized void registerCommonEvents() {
    if (commonEventsRegistered) {
      return;
    }

    LivingAttackEvent.BUS.addListener(ForgeEvents::onLivingAttack);
    LivingDeathEvent.BUS.addListener(ForgeEvents::onLivingDeath);
    EntityJoinLevelEvent.BUS.addListener(ForgeEvents::onEntityJoinLevel);
    PlayerEvent.PlayerLoggedInEvent.BUS.addListener(ForgeEvents::onPlayerLoggedIn);
    PlayerEvent.PlayerRespawnEvent.BUS.addListener(ForgeEvents::onPlayerRespawn);
    PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(ForgeEvents::onPlayerChangedDimension);
    PlayerInteractEvent.RightClickBlock.BUS.addListener(ForgeEvents::onRightClickBlock);

    commonEventsRegistered = true;
    Mystcraft.LOGGER.info("[Mystcraft] Registered Forge gameplay events");
  }

  static void fireLevelLoad(ServerLevel level) {
    LevelEvent.Load.BUS.fire(new LevelEvent.Load(level));
  }

  private static void addReloadListeners(AddReloadListenerEvent event) {
    event.addListener(new ForgeVillageStructureHandler(event.getRegistries()));
    event.addListener(new MystcraftGrammarReloadListener());
    event.addListener(new MystcraftSymbolReloadListener());
    event.addListener(new MystcraftAffinityReloadListener());
  }

  private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
    if (!(event.level() instanceof ServerLevel level)) {
      return;
    }
    AgeEffectsHandler.onLevelTick(level);
    PersonalPocketEscapeHandler.tickProxyCleanup(level);
  }

  private static void onLevelLoad(LevelEvent.Load event) {
    if (event.getLevel() instanceof ServerLevel level) {
      handleLevelLoad(level);
    }
  }

  private static void handleLevelLoad(ServerLevel level) {
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return;
    }
    AgeDeathHandler.configureAgeGameRules(level);
    ChunkGenerator generator = level.getChunkSource().getGenerator();
    if (generator instanceof AgeChunkGenerator ageGenerator
        && ageGenerator.needsDirectorReconstruction()) {
      Mystcraft.LOGGER.info("[Mystcraft] Reconstructing director for Age {}",
          level.dimension().identifier());
      ageGenerator.reconstructDirectorFromAgeData(level);
    }
  }

  private static boolean onLivingDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)
        || !(player.level() instanceof ServerLevel level)) {
      return false;
    }
    if (PersonalPocketEscapeHandler.handleDeath(player, event.getSource())) {
      return true;
    }
    if (AgeReturnHandler.handleDeath(player, event.getSource())) {
      return true;
    }
    AgeDeathHandler.onPlayerDeath(player, event.getSource(), level);
    return false;
  }

  private static boolean onLivingAttack(LivingAttackEvent event) {
    return AgeEffectsHandler.onLivingAttack(event.getEntity(), event.getSource());
  }

  private static boolean onEntityJoinLevel(EntityJoinLevelEvent event) {
    return event.getLevel() instanceof ServerLevel level
        && event.getEntity() instanceof FallingBlockEntity fallingBlock
        && FallingBlockHandler.handle(level, fallingBlock);
  }

  private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    AgeDataSyncHandler.onPlayerLoggedIn(player);
    GuidebookHandler.onPlayerLoggedIn(player);
    PersonalPocketEscapeHandler.syncProxyForPlayer(player);
    MystcraftNetwork.sendToPlayer(new SymbolSyncPacket(), player);
  }

  private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    if (player.level() instanceof ServerLevel level) {
      AgeDeathHandler.onPlayerRespawn(player, level);
    }
    AgeDataSyncHandler.onPlayerRespawn(player);
  }

  private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      AgeDataSyncHandler.onPlayerChangeDimension(player);
      PersonalPocketEscapeHandler.syncProxyForPlayer(player);
    }
  }

  private static boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    Level level = event.getLevel();
    BlockState state = level.getBlockState(event.getPos());
    if (!(state.getBlock() instanceof LecternBlock)) {
      return false;
    }

    MystcraftLecternHelper.LecternInteractionResult result =
        MystcraftLecternHelper.handleLecternInteraction(
            level,
            event.getPos(),
            state,
            event.getEntity(),
            event.getHand(),
            (lectern, book, pageCount) -> lectern.setBook(book, event.getEntity()));
    if (!result.handled) {
      return false;
    }
    event.setCancellationResult(result.result);
    return true;
  }
}
