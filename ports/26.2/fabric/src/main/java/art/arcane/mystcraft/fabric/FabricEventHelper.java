package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.command.MystcraftCommands;
import art.arcane.mystcraft.event.AgeDataSyncHandler;
import art.arcane.mystcraft.event.AgeDeathHandler;
import art.arcane.mystcraft.event.AgeEffectsHandler;
import art.arcane.mystcraft.event.AgeReturnHandler;
import art.arcane.mystcraft.event.FallingBlockHandler;
import art.arcane.mystcraft.event.GuidebookHandler;
import art.arcane.mystcraft.event.PersonalPocketEscapeHandler;
import art.arcane.mystcraft.fabric.mixin.LecternBlockEntityAccessor;
import art.arcane.mystcraft.network.SymbolSyncPacket;
import art.arcane.mystcraft.platform.services.IEventHelper;
import art.arcane.mystcraft.util.MystcraftLecternHelper;
import art.arcane.mystcraft.world.AgeDimensionFactory;
import art.arcane.mystcraft.world.gen.AgeChunkGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Fabric event wiring for shared gameplay handlers. */
public final class FabricEventHelper implements IEventHelper {

  private static boolean registered;

  public static synchronized void registerAll() {
    if (registered) {
      return;
    }

    ServerTickEvents.END_LEVEL_TICK.register(level -> {
      AgeEffectsHandler.onLevelTick(level);
      PersonalPocketEscapeHandler.tickProxyCleanup(level);
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
        MystcraftCommands.registerCommands(dispatcher));

    ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
        !AgeEffectsHandler.onLivingAttack(entity, source));
    ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
      if (!(entity instanceof ServerPlayer player)) {
        return true;
      }
      if (PersonalPocketEscapeHandler.handleDeath(player, source)) {
        return false;
      }
      return !AgeReturnHandler.handleDeath(player, source);
    });
    ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
      if (entity instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
        AgeDeathHandler.onPlayerDeath(player, source, level);
      }
    });

    ServerPlayerEvents.JOIN.register(player -> {
      AgeDataSyncHandler.onPlayerLoggedIn(player);
      GuidebookHandler.onPlayerLoggedIn(player);
      PersonalPocketEscapeHandler.syncProxyForPlayer(player);
      ServerPlayNetworking.send(player, new SymbolSyncPacket());
    });
    ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
      if (newPlayer.level() instanceof ServerLevel level) {
        AgeDeathHandler.onPlayerRespawn(newPlayer, level);
      }
      AgeDataSyncHandler.onPlayerRespawn(newPlayer);
    });
    ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
      AgeDataSyncHandler.onPlayerChangeDimension(player);
      PersonalPocketEscapeHandler.syncProxyForPlayer(player);
    });

    ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) ->
        !(entity instanceof FallingBlockEntity fallingBlock)
            || !FallingBlockHandler.handle(level, fallingBlock));

    ServerLevelEvents.LOAD.register((server, level) -> handleLevelLoad(level));

    UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
      net.minecraft.core.BlockPos position = hitResult.getBlockPos();
      net.minecraft.world.level.block.state.BlockState state = level.getBlockState(position);
      if (!(state.getBlock() instanceof LecternBlock)) {
        return InteractionResult.PASS;
      }
      MystcraftLecternHelper.LecternInteractionResult result =
          MystcraftLecternHelper.handleLecternInteraction(
              level,
              position,
              state,
              player,
              hand,
              (lectern, book, pageCount) -> {
                LecternBlockEntityAccessor accessor = (LecternBlockEntityAccessor) lectern;
                accessor.mystcraft$setBook(book);
                accessor.mystcraft$setPageCount(pageCount);
              });
      return result.handled ? result.result : InteractionResult.PASS;
    });

    registered = true;
    Mystcraft.LOGGER.info("[Mystcraft] Registered Fabric gameplay events");
  }

  private static void handleLevelLoad(ServerLevel level) {
    if (!AgeDimensionFactory.isMystcraftAge(level.dimension())) {
      return;
    }
    AgeDeathHandler.configureAgeGameRules(level);
    ChunkGenerator generator = level.getChunkSource().getGenerator();
    if (generator instanceof AgeChunkGenerator ageGenerator
        && ageGenerator.needsDirectorReconstruction()) {
      ageGenerator.reconstructDirectorFromAgeData(level);
    }
  }

  @Override
  public void registerServerEvents() {
    registerAll();
  }

  @Override
  public void registerClientEvents() {
  }

  @Override
  public void registerCommonEvents() {
    registerAll();
  }

  @Override
  public void fireLevelLoadEvent(ServerLevel level) {
    handleLevelLoad(level);
  }
}
