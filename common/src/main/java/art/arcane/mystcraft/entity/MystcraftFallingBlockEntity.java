package art.arcane.mystcraft.entity;

import art.arcane.mystcraft.registry.ModEntities;
import art.arcane.mystcraft.util.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * A custom falling block entity for Mystcraft. Used for special falling block
 * effects in Ages.
 */
public class MystcraftFallingBlockEntity extends Entity {

  private BlockState blockState = Blocks.STONE.defaultBlockState();
  private BlockPos startPos = BlockPos.ZERO;
  private int time;

  public MystcraftFallingBlockEntity(EntityType<?> type, Level level) {
    super(type, level);
  }

  public MystcraftFallingBlockEntity(Level level, double x, double y, double z, BlockState state) {
    this(ModEntities.FALLING_BLOCK.get(), level);
    setPos(x, y, z);
    this.blockState = state;
    this.blocksBuilding = true;
    setDeltaMovement(Vec3.ZERO);
    this.xo = x;
    this.yo = y;
    this.zo = z;
    setStartPos(blockPosition());
  }

  @Override
  protected void defineSynchedData() {

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
      if (!level().isClientSide) {
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
  protected void readAdditionalSaveData(CompoundTag tag) {
    blockState = NbtUtils.readBlockState(level().holderLookup(net.minecraft.core.registries.Registries.BLOCK), tag.getCompound("BlockState"));
    time = tag.getInt("Time");
    if (tag.contains("StartPos")) {
      startPos = NbtCompat.readBlockPos(tag, "StartPos");
    }
  }

  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    tag.put("BlockState", NbtUtils.writeBlockState(blockState));
    tag.putInt("Time", time);
    tag.put("StartPos", NbtUtils.writeBlockPos(startPos));
  }

  public BlockState getBlockState() {
    return blockState;
  }

  public BlockPos getStartPos() {
    return startPos;
  }

  private void setStartPos(BlockPos pos) {
    this.startPos = pos;
  }
}
