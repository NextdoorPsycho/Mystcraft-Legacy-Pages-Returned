package art.arcane.mystcraft.block;

import art.arcane.mystcraft.blockentity.StarFissureBlockEntity;
import art.arcane.mystcraft.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Star Fissure block. The gateway out of an Age back to the overworld.
 * Falls through the void and appears as a star in the sky from the Age.
 * Entities that touch it are teleported to their spawn point in the overworld.
 */
public class StarFissureBlock extends BaseEntityBlock {

  public static final MapCodec<StarFissureBlock> CODEC = simpleCodec(StarFissureBlock::new);
  private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1.6, 16);

  public StarFissureBlock(Properties properties) {
    super(properties);
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.INVISIBLE;
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
  protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                              InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    if (level.isClientSide()) {
      return;
    }

    if (entity.isPassenger() || entity.isVehicle()) {
      return;
    }

    teleportToOverworld(level, entity);
  }

  private void teleportToOverworld(Level level, Entity entity) {
    if (!(level instanceof ServerLevel serverLevel)) {
      return;
    }

    ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) {
      return;
    }

    serverLevel.playSound(null, entity.blockPosition(), ModSounds.LINKING_FISSURE.get(),
        SoundSource.BLOCKS, 1.0f, 1.0f);

    if (level.dimension() == Level.OVERWORLD) {
      BlockPos spawn = overworld.getRespawnData().pos();
      entity.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);

      serverLevel.playSound(null, spawn, ModSounds.LINKING_LINK.get(),
          SoundSource.BLOCKS, 1.0f, 1.0f);
      return;
    }

    if (entity instanceof ServerPlayer player) {

      ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
      BlockPos respawnPos = overworld.getRespawnData().pos();
      if (respawnConfig != null && respawnConfig.respawnData().dimension().equals(Level.OVERWORLD)) {
        respawnPos = respawnConfig.respawnData().pos();
      }
      player.teleport(new TeleportTransition(
          overworld,
          Vec3.atBottomCenterOf(respawnPos),
          entity.getDeltaMovement(),
          entity.getYRot(),
          entity.getXRot(),
          TeleportTransition.DO_NOTHING));

      overworld.playSound(null, respawnPos, ModSounds.LINKING_LINK.get(),
          SoundSource.BLOCKS, 1.0f, 1.0f);
    } else {

      BlockPos spawn = overworld.getRespawnData().pos();
      entity.teleport(new TeleportTransition(
          overworld,
          Vec3.atBottomCenterOf(spawn),
          entity.getDeltaMovement(),
          entity.getYRot(),
          entity.getXRot(),
          TeleportTransition.DO_NOTHING));

      overworld.playSound(null, spawn, ModSounds.LINKING_LINK.get(),
          SoundSource.BLOCKS, 1.0f, 1.0f);
    }
  }
}
