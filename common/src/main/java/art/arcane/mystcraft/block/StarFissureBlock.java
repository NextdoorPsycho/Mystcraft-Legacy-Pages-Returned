package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.registry.ModSounds;
import art.arcane.mystcraft.util.BlockInteractionCompat;
import art.arcane.mystcraft.util.CodecCompat;
import art.arcane.mystcraft.util.ServerPlayerTeleport;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Star Fissure block.
 * The gateway out of an Age back to the overworld.
 * Falls through the void and appears as a star in the sky from the Age.
 * Entities that touch it are teleported to their spawn point in the overworld.
 */
public class StarFissureBlock extends BaseEntityBlock implements BlockInteractionCompat {

  public static final MapCodec<StarFissureBlock> CODEC = CodecCompat.simpleCodec(StarFissureBlock::new);
  private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1.6, 16);

  public StarFissureBlock(Properties properties) {
    super(properties);
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return SHAPE;
  }

  @Override
  public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new StarFissureBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return null;
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (level.isClientSide) {
      return;
    }

    // Only teleport entities that have been in the block for a moment
    // This prevents instant teleportation on touch
    if (entity.isPassenger() || entity.isVehicle()) {
      return;
    }

    // Teleport to overworld spawn
    teleportToOverworld(level, entity);
  }

  /**
   * Teleports an entity to the overworld spawn.
   */
  private void teleportToOverworld(Level level, Entity entity) {
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }

    // Get the overworld
    ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) {
      return;
    }

    // Play departure sound
    serverLevel.playSound(null, entity.blockPosition(), ModSounds.LINKING_FISSURE.get(),
        SoundSource.BLOCKS, 1.0f, 1.0f);

    // Already in overworld - teleport to world spawn
    if (level.dimension() == Level.OVERWORLD) {
      BlockPos spawn = overworld.getSharedSpawnPos();
      entity.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
      // Play arrival sound
      serverLevel.playSound(null, spawn, ModSounds.LINKING_LINK.get(),
          SoundSource.BLOCKS, 1.0f, 1.0f);
      return;
    }

    // Teleport to overworld
    if (entity instanceof ServerPlayer player) {
      // Use the player's respawn point or world spawn
      BlockPos respawnPos = player.getRespawnPosition();
      if (respawnPos == null || player.getRespawnDimension() != Level.OVERWORLD) {
        respawnPos = overworld.getSharedSpawnPos();
      }
      ServerPlayerTeleport.teleport(player, overworld, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5,
          entity.getYRot(), entity.getXRot());
      // Play arrival sound in overworld
      overworld.playSound(null, respawnPos, ModSounds.LINKING_LINK.get(),
          SoundSource.BLOCKS, 1.0f, 1.0f);
    } else {
      // For non-player entities, teleport to world spawn
      BlockPos spawn = overworld.getSharedSpawnPos();
      entity.changeDimension(overworld);
      entity.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
      // Play arrival sound
      overworld.playSound(null, spawn, ModSounds.LINKING_LINK.get(),
          SoundSource.BLOCKS, 1.0f, 1.0f);
    }
  }
}
