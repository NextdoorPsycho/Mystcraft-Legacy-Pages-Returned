package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A custom falling block entity for Mystcraft. Used for special falling block
 * effects in Ages.
 */
public class MystcraftFallingBlockEntity extends Entity {

  private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
      SynchedEntityData.defineId(MystcraftFallingBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
  private BlockState blockState = Blocks.STONE.defaultBlockState();
  private BlockPos startPos = BlockPos.ZERO;
  private int time;

  public MystcraftFallingBlockEntity(EntityType<?> type, Level level) {
    super(type, level);
  }

  public MystcraftFallingBlockEntity(Level level, double x, double y, double z, BlockState state) {
    this(ModEntities.FALLING_BLOCK.get(), level);
    setPos(x, y, z);
    setBlockState(state);
    this.blocksBuilding = true;
    setDeltaMovement(Vec3.ZERO);
    this.xo = x;
    this.yo = y;
    this.zo = z;
    setStartPos(blockPosition());
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(DATA_BLOCK_STATE, Blocks.STONE.defaultBlockState());
  }

  @Override
  public void tick() {
    if (blockState.isAir()) {
      discard();
      return;
    }

    time++;
    if (!isNoGravity()) {
      setDeltaMovement(getDeltaMovement().add(0.0, -0.04, 0.0));
    }

    move(MoverType.SELF, getDeltaMovement());

    if (onGround()) {
      if (!level().isClientSide()) {
        land();
      }
      return;
    }

    setDeltaMovement(getDeltaMovement().scale(0.98));

    if (time > 600) {
      discard();
    }
  }

  private void land() {
    BlockPos landingPos = blockPosition();
    BlockState atPos = level().getBlockState(landingPos);

    if (atPos.canBeReplaced() || atPos.isAir()) {

      if (level().setBlock(landingPos, blockState, Block.UPDATE_ALL)) {

        if (blockState.getBlock() instanceof FallingBlock fallingBlock) {
          fallingBlock.onLand(level(), landingPos, blockState, atPos, null);
        }

        if (atPos.getFluidState().isSource() &&
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
          level().setBlock(landingPos,
              blockState.setValue(BlockStateProperties.WATERLOGGED, true),
              Block.UPDATE_ALL);
        }
      }
    } else {

      Block.dropResources(blockState, level(), landingPos);
    }

    discard();
  }

  @Override
  protected void readAdditionalSaveData(ValueInput input) {
    setBlockState(input.read("BlockState", BlockState.CODEC)
        .orElse(Blocks.STONE.defaultBlockState()));
    time = input.getIntOr("Time", 0);
    startPos = input.read("StartPos", BlockPos.CODEC).orElse(BlockPos.ZERO);
  }

  @Override
  protected void addAdditionalSaveData(ValueOutput output) {
    output.store("BlockState", BlockState.CODEC, blockState);
    output.putInt("Time", time);
    output.store("StartPos", BlockPos.CODEC, startPos);
  }

  @Override
  public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
    return false;
  }

  public BlockState getBlockState() {
    return entityData.get(DATA_BLOCK_STATE);
  }

  private void setBlockState(BlockState state) {
    blockState = state == null ? Blocks.STONE.defaultBlockState() : state;
    entityData.set(DATA_BLOCK_STATE, blockState);
  }

  public BlockPos getStartPos() {
    return startPos;
  }

  private void setStartPos(BlockPos pos) {
    this.startPos = pos;
  }
}
